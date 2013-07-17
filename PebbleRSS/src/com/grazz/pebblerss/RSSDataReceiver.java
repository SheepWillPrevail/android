package com.grazz.pebblerss;

import java.util.ArrayDeque;
import java.util.List;
import java.util.Queue;

import android.content.Context;
import android.content.Intent;
import android.util.Log;

import com.getpebble.android.kit.PebbleKit;
import com.getpebble.android.kit.PebbleKit.PebbleDataReceiver;
import com.getpebble.android.kit.util.PebbleDictionary;
import com.grazz.pebblerss.feed.Feed;
import com.grazz.pebblerss.feed.FeedItem;

public class RSSDataReceiver extends PebbleDataReceiver {

	private static final int maxLength = 96;

	private Queue<PebbleDictionary> _msgQueue = new ArrayDeque<PebbleDictionary>();
	private int _transactionId = 0;
	private RSSService _service;
	private Long _lastFeed;

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
				Integer total = _service.getFeeds().size();
				if (total > 16)
					total = 16; // clamp
				for (int i = 0; i < total; i++) {
					PebbleDictionary feed_dict = new PebbleDictionary();
					feed_dict.addString(1001, substring(_service.getFeeds().get(i).getName(), maxLength));
					feed_dict.addUint8(1011, total.byteValue());
					feed_dict.addUint8(1012, (byte) i);
					queueData(feed_dict);
				}
				sendData(context);
			}

			if (command_id.intValue() == 1) // ack
				sendData(context);
		}

		Long feed_id = data.getUnsignedInteger(1091);
		if (feed_id != null) {
			Feed feed = _service.getFeeds().get(feed_id.intValue());

			if (System.currentTimeMillis() > feed.getNextUpdateTime())
				feed.doParse();

			List<FeedItem> items = feed.getItems();
			Integer total = items.size();
			if (total > 128)
				total = 128; // clamp
			for (int i = 0; i < total; i++) {
				PebbleDictionary item_dict = new PebbleDictionary();
				item_dict.addString(1002, substring(items.get(i).getTitle(), maxLength));
				item_dict.addUint8(1011, total.byteValue());
				item_dict.addUint8(1012, (byte) i);
				queueData(item_dict);
			}

			_lastFeed = feed_id;
			sendData(context);
		}

		Long item_id = data.getUnsignedInteger(1092);
		if (item_id != null && _lastFeed != null) {
			Feed feed = _service.getFeeds().get(_lastFeed.intValue());
			FeedItem item = feed.getItems().get(item_id.intValue());
			String content = item.getContent();
			int parts = (int) Math.ceil((double) content.length() / maxLength);
			int maxParts = 1000 / maxLength;
			if (parts > maxParts)
				parts = maxParts; // clamp
			for (int offset = 0; offset < (parts * maxLength); offset += maxLength) {
				int length = content.length() - offset;
				if (length > maxLength)
					length = maxLength;
				PebbleDictionary message_dict = new PebbleDictionary();
				message_dict.addString(1003, content.substring(offset, offset + length));
				message_dict.addUint8(1011, (byte) parts);
				message_dict.addUint16(1012, (short) offset);
				queueData(message_dict);
			}
			sendData(context);
		}

		Long open_item_id = data.getUnsignedInteger(1093);
		if (open_item_id != null && _lastFeed != null) {
			FeedItem item = _service.getFeeds().get(_lastFeed.intValue()).getItems().get(open_item_id.intValue());
			Intent url = new Intent(Intent.ACTION_VIEW, item.getLink());
			url.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
			_service.getApplicationContext().startActivity(url);
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
			PebbleKit.sendDataToPebbleWithTransactionId(context, StaticValues.APP_UUID, dictionary, _transactionId++ % 255);
		}
	}

}
