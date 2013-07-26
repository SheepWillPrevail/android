package com.grazz.pebblerss;

import java.util.ArrayDeque;

import android.content.Context;
import android.content.Intent;
import android.util.Log;
import android.util.SparseArray;

import com.getpebble.android.kit.PebbleKit;
import com.getpebble.android.kit.PebbleKit.PebbleDataReceiver;
import com.getpebble.android.kit.util.PebbleDictionary;
import com.grazz.pebblerss.feed.Feed;
import com.grazz.pebblerss.feed.FeedCursor;
import com.grazz.pebblerss.feed.FeedItem;
import com.grazz.pebblerss.feed.FeedManagerCursor;

public class RSSDataReceiver extends PebbleDataReceiver {

	private static final int MAX_LENGTH = 96;

	private RSSService _service;
	private ArrayDeque<PebbleDictionary> _msgQueue = new ArrayDeque<PebbleDictionary>();
	private SparseArray<PebbleDictionary> _msgSent = new SparseArray<PebbleDictionary>();
	private int _transactionId = 0;
	private Long _lastFeed;
	private FeedManagerCursor _managerCursor;
	private FeedCursor _feedCursor;

	protected RSSDataReceiver(RSSService service) {
		super(StaticValues.APP_UUID);
		_service = service;
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
		if (command_id != null) {

			if (command_id.intValue() == 0) { // hello
				_msgSent.clear();
				_managerCursor = new FeedManagerCursor(_service.getFeedManager());
				sendFeed(context);
			}

			if (command_id.intValue() == 1) // ack
				sendData(context); // continue queue

			if (command_id.intValue() == 2 && !_managerCursor.isDone())
				sendFeed(context);

			if (command_id.intValue() == 3 && !_feedCursor.isDone())
				sendFeedItem(context);
		}

		Long feed_id = data.getUnsignedInteger(1091);
		if (feed_id != null) {
			Feed feed = _service.getFeedManager().getFeed(feed_id.intValue());
			if (feed.isStale())
				feed.doParse();
			_lastFeed = feed_id;
			_feedCursor = new FeedCursor(feed);
			sendFeedItem(context);
		}

		Long item_id = data.getUnsignedInteger(1092);
		if (item_id != null && _lastFeed != null) {
			Feed feed = _service.getFeedManager().getFeed(_lastFeed.intValue());
			FeedItem item = feed.getItems().get(item_id.intValue());
			String content = item.getContent();
			int maxParts = 1000 / MAX_LENGTH;
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

		Long open_item_id = data.getUnsignedInteger(1093);
		if (open_item_id != null && _feedCursor != null) {
			FeedItem item = _service.getFeedManager().getFeed(_lastFeed.intValue()).getItems().get(open_item_id.intValue());
			Intent url = new Intent(Intent.ACTION_VIEW, item.getLink());
			url.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
			_service.getApplicationContext().startActivity(url);
		}
	}

	private void sendFeed(Context context) {
		int position = _managerCursor.getPosition();
		Feed feed = _managerCursor.getNextItem();
		PebbleDictionary feed_dict = new PebbleDictionary();
		feed_dict.addString(1001, substring(feed.getName(), MAX_LENGTH));
		feed_dict.addUint8(1011, (byte) _managerCursor.getTotal());
		feed_dict.addUint8(1012, (byte) position);
		queueData(feed_dict);
		sendData(context);
	}

	private void sendFeedItem(Context context) {
		int position = _feedCursor.getPosition();
		FeedItem item = _feedCursor.getNextItem();
		PebbleDictionary item_dict = new PebbleDictionary();
		item_dict.addString(1002, substring(item.getTitle(), MAX_LENGTH));
		item_dict.addUint8(1011, (byte) _feedCursor.getTotal());
		item_dict.addUint8(1012, (byte) position);
		queueData(item_dict);
		sendData(context);
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

	private void queueData(PebbleDictionary dictionary) {
		Log.d("queueData", dictionary.toJsonString());
		_msgQueue.add(dictionary);
	}

	private void sendData(Context context) {
		if (!_msgQueue.isEmpty()) {
			PebbleDictionary dictionary = _msgQueue.remove();
			Log.d("sendData", dictionary.toJsonString());
			int transactionId = _transactionId++ % 256;
			_msgSent.put(transactionId, dictionary);
			PebbleKit.sendDataToPebbleWithTransactionId(context, StaticValues.APP_UUID, dictionary, transactionId);
		}
	}

}
