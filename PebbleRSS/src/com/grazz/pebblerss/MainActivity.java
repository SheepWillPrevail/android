package com.grazz.pebblerss;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;
import java.util.Queue;
import java.util.UUID;

import android.app.Activity;
import android.content.Context;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;

import com.getpebble.android.kit.PebbleKit;
import com.getpebble.android.kit.PebbleKit.PebbleDataReceiver;
import com.getpebble.android.kit.util.PebbleDictionary;

public class MainActivity extends Activity {

	private static final UUID APP_UUID = UUID.fromString("1941e614-9163-49bd-ba01-6d7fa71eedac");
	private List<Feed> _feeds = new ArrayList<Feed>();
	private PebbleDataReceiver _receiver = new PebbleDataReceiver(APP_UUID) {

		private static final int maxLength = 96;
		private Queue<PebbleDictionary> _msgQueue = new ArrayDeque<PebbleDictionary>();
		private int _transactionId = 0;
		private Long _lastFeed;

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
					Integer total = _feeds.size();
					if (total > 16)
						total = 16; // clamp
					for (int i = 0; i < total; i++) {
						PebbleDictionary feed_dict = new PebbleDictionary();
						feed_dict.addString(1001, substring(_feeds.get(i).getName(), maxLength));
						feed_dict.addUint8(1011, total.byteValue());
						feed_dict.addUint8(1012, (byte) i);
						queueData(context, feed_dict);
					}
					sendData(context);
				}

				if (command_id.intValue() == 1) // ack
					sendData(context);

			}

			Long feed_id = data.getUnsignedInteger(1091);
			if (feed_id != null) {
				Feed feed = _feeds.get(feed_id.intValue());
				List<FeedItem> items = feed.getItems();
				Integer total = items.size();
				if (total > 128)
					total = 128; // clamp
				for (int i = 0; i < total; i++) {
					PebbleDictionary item_dict = new PebbleDictionary();
					item_dict.addString(1002, substring(items.get(i).getTitle(), maxLength));
					item_dict.addUint8(1011, total.byteValue());
					item_dict.addUint8(1012, (byte) i);
					queueData(context, item_dict);
				}

				_lastFeed = feed_id;
				sendData(context);
			}

			Long item_id = data.getUnsignedInteger(1092);
			if (item_id != null && _lastFeed != null) {
				Feed feed = _feeds.get(_lastFeed.intValue());
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
					queueData(context, message_dict);
				}
				sendData(context);
			}

		}

		private void queueData(Context context, PebbleDictionary dictionary) {
			Log.d("queueData", dictionary.toJsonString());
			_msgQueue.add(dictionary);
		}

		private void sendData(Context context) {
			if (!_msgQueue.isEmpty()) {
				PebbleDictionary dictionary = _msgQueue.remove();
				Log.d("sendData", dictionary.toJsonString());
				PebbleKit.sendDataToPebbleWithTransactionId(context, APP_UUID, dictionary, _transactionId++ % 255);
			}
		}

	};

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);

		_feeds.add(new Feed(Uri.parse("http://www.nu.nl/deeplink_rss2/index.jsp?r=Algemeen")));

		PebbleKit.registerReceivedDataHandler(this, _receiver);
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.main, menu);
		return true;
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();
		unregisterReceiver(_receiver);
	}

}
