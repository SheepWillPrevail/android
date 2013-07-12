package com.grazz.pebblerss;

import java.util.ArrayList;
import java.util.List;
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

		private Long _lastFeed;

		@Override
		public void receiveData(Context context, int transactionId, PebbleDictionary data) {
			PebbleKit.sendAckToPebble(context, transactionId);
			Log.d("receiveData", data.toJsonString());

			Long command_id = data.getUnsignedInteger(1090);
			if (command_id != null) { // hello
				Integer total = _feeds.size();
				Integer offset = 0;
				for (Feed feed : _feeds) {
					PebbleDictionary feed_dict = new PebbleDictionary();
					feed_dict.addString(1001, substring(feed.getName(), 100));
					feed_dict.addUint8(1011, total.byteValue());
					feed_dict.addUint8(1012, offset.byteValue());
					sendData(context, feed_dict);
					offset++;
				}
			}

			Long feed_id = data.getUnsignedInteger(1091);
			if (feed_id != null) {
				Feed feed = _feeds.get(feed_id.intValue());
				List<FeedItem> items = feed.getItems();
				Integer total = items.size();
				Integer offset = 0;
				for (FeedItem item : items) {
					PebbleDictionary item_dict = new PebbleDictionary();
					item_dict.addString(1002, substring(item.getTitle(), 100));
					item_dict.addUint8(1011, total.byteValue());
					item_dict.addUint8(1012, offset.byteValue());
					sendData(context, item_dict);
					offset++;
				}

				_lastFeed = feed_id;
			}

			Long item_id = data.getUnsignedInteger(1092);
			if (item_id != null) {
				Feed feed = _feeds.get(_lastFeed.intValue());
				FeedItem item = feed.getItems().get(item_id.intValue());
				PebbleDictionary message_dict = new PebbleDictionary();
				message_dict.addString(1003, substring(item.getContent(), 100));
				sendData(context, message_dict);
			}
		}

		private void sendData(Context context, PebbleDictionary dict) {
			Log.d("sendData", dict.toJsonString());
			PebbleKit.sendDataToPebble(context, APP_UUID, dict);
		}

	};

	private String substring(String str, int maxLength) {
		int length = str.length();
		if (length > maxLength)
			length = maxLength;
		return str.substring(0, length);
	}

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
