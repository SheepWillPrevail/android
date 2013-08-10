package com.grazz.pebblerss;

import java.util.ArrayDeque;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.util.Log;
import android.util.SparseArray;

import com.getpebble.android.kit.PebbleKit;
import com.getpebble.android.kit.PebbleKit.PebbleDataReceiver;
import com.getpebble.android.kit.util.PebbleDictionary;
import com.grazz.pebblerss.feed.FeedItemCursor;
import com.grazz.pebblerss.feed.FeedCursor;
import com.grazz.pebblerss.provider.RSSFeed;
import com.grazz.pebblerss.provider.RSSFeedItem;

public class RSSDataReceiver extends PebbleDataReceiver {

	private static final int MAX_LENGTH = 90;

	private RSSService _service;
	private ArrayDeque<PebbleDictionary> _msgQueue = new ArrayDeque<PebbleDictionary>();
	private SparseArray<PebbleDictionary> _msgSent = new SparseArray<PebbleDictionary>();
	private int _transactionId = 0;
	private FeedCursor _feedCursor;
	private FeedItemCursor _feedItemCursor;

	protected RSSDataReceiver(RSSService service) {
		super(StaticValues.APP_UUID);
		_service = service;
	}

	private void queueData(PebbleDictionary dictionary) {
		Log.d("queueData", dictionary.toJsonString());
		_msgQueue.add(dictionary);
	}

	private void sendData(Context context) {
		if (!_msgQueue.isEmpty()) {
			PebbleDictionary dictionary = _msgQueue.remove();
			Log.d("sendData", dictionary.toJsonString());
			_msgSent.put(_transactionId, dictionary);
			PebbleKit.sendDataToPebbleWithTransactionId(context, StaticValues.APP_UUID, dictionary, _transactionId);
			_transactionId = (_transactionId + 1) % 256;
		}
	}

	public void ack(int transactionId) {
		_msgSent.remove(transactionId);
	}

	public void nack(Context context, int transactionId) {
		PebbleDictionary dictionary = _msgSent.get(transactionId);
		if (dictionary != null) {
			_msgQueue.push(dictionary);
			sendData(context);
		}
	}

	private String substring(String str, int maxLength) {
		int length = str.length();
		if (length > maxLength)
			length = maxLength;
		return str.substring(0, length);
	}

	@Override
	public void receiveData(Context context, int transactionId, PebbleDictionary data) {
		Log.d("receiveData", data.toJsonString());
		PebbleKit.sendAckToPebble(context, transactionId);

		Long command_id = data.getUnsignedInteger(1090);
		if (command_id != null)
			switch (command_id.intValue()) {
			case 0: // hello
				_msgSent.clear();
				_service.getFeedManager().checkFeeds(true);
				_feedCursor = new FeedCursor(context);
				sendFontPacket(context);
				sendFeed(context);
				break;
			case 1: // ack, continue queue
				sendData(context);
				break;
			case 2: // continue feed list
				if (_feedCursor != null && !_feedCursor.isDone())
					sendFeed(context);
				break;
			case 3: // continue feed item list
				if (_feedItemCursor != null && !_feedItemCursor.isDone())
					sendFeedItem(context);
				break;
			}

		Long feed_id = data.getUnsignedInteger(1091);
		if (feed_id != null && _feedCursor != null) {
			RSSFeed feed = _feedCursor.getItem(feed_id.intValue());
			_feedItemCursor = new FeedItemCursor(context, feed);
			sendFeedItem(context);
		}

		Long item_id = data.getUnsignedInteger(1092);
		if (item_id != null && _feedItemCursor != null)
			sendFeedItemText(context, item_id);

		Long open_item_id = data.getUnsignedInteger(1093);
		if (open_item_id != null && _feedItemCursor != null) {
			RSSFeedItem item = _feedItemCursor.getItem(open_item_id.intValue());
			Intent url = new Intent(Intent.ACTION_VIEW, item.getUri());
			url.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
			_service.getApplicationContext().startActivity(url);
		}
	}

	private void sendFeed(Context context) {
		int position = _feedCursor.getPosition();
		RSSFeed feed = _feedCursor.getNextItem();
		PebbleDictionary feed_dict = new PebbleDictionary();
		feed_dict.addString(1001, substring(feed.getName(), MAX_LENGTH));
		feed_dict.addUint8(1011, (byte) _feedCursor.getTotal());
		feed_dict.addUint8(1012, (byte) position);
		queueData(feed_dict);
		sendData(context);
	}

	private void sendFeedItem(Context context) {
		int position = _feedItemCursor.getPosition();
		RSSFeedItem item = _feedItemCursor.getNextItem();
		PebbleDictionary item_dict = new PebbleDictionary();
		item_dict.addString(1002, substring(item.getTitle(), MAX_LENGTH));
		item_dict.addUint8(1011, (byte) _feedItemCursor.getTotal());
		item_dict.addUint8(1012, (byte) position);
		queueData(item_dict);
		sendData(context);
	}

	private void sendFeedItemText(Context context, Long item_id) {
		RSSFeedItem item = _feedItemCursor.getItem(item_id.intValue());
		String content = item.getContent();
		int maxParts = 2000 / MAX_LENGTH;
		int parts = (content.length() + (MAX_LENGTH - 1)) / MAX_LENGTH;
		if (parts > maxParts)
			parts = maxParts; // clamp
		for (int offset = 0; offset < (parts * MAX_LENGTH); offset += MAX_LENGTH) {
			int length = content.length() - offset;
			if (length > MAX_LENGTH)
				length = MAX_LENGTH;
			PebbleDictionary message_dict = new PebbleDictionary();
			message_dict.addString(1003, content.substring(offset, offset + length));
			message_dict.addUint8(1011, (byte) parts);
			message_dict.addUint16(1012, (short) offset);
			queueData(message_dict);
			sendData(context);
		}
	}

	public void sendFontPacket(Context context) {
		SharedPreferences preferences = context.getSharedPreferences(StaticValues.PREFERENCES_KEY, Context.MODE_PRIVATE);
		Resources resources = context.getResources();
		PebbleDictionary dictionary = new PebbleDictionary();
		dictionary.addUint8(1013, Integer.valueOf(preferences.getString(resources.getString(R.string.setting_feedfont), "5")).byteValue());
		dictionary.addUint8(1014, Integer.valueOf(preferences.getString(resources.getString(R.string.setting_itemfont), "0")).byteValue());
		dictionary.addUint8(1015, Integer.valueOf(preferences.getString(resources.getString(R.string.setting_messagefont), "0")).byteValue());
		dictionary.addUint8(1016, Integer.valueOf(preferences.getString(resources.getString(R.string.setting_cellheight), "33")).byteValue());
		queueData(dictionary);
		sendData(context);
	}

}
