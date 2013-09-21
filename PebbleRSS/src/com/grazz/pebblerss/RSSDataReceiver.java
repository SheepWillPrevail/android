package com.grazz.pebblerss;

import java.nio.ByteBuffer;
import java.util.ArrayDeque;
import java.util.List;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Base64;
import android.util.Log;
import android.util.SparseArray;
import android.util.SparseIntArray;

import com.getpebble.android.kit.PebbleKit;
import com.getpebble.android.kit.PebbleKit.PebbleAckReceiver;
import com.getpebble.android.kit.PebbleKit.PebbleDataReceiver;
import com.getpebble.android.kit.PebbleKit.PebbleNackReceiver;
import com.getpebble.android.kit.util.PebbleDictionary;
import com.grazz.pebblerss.feed.FeedCursor;
import com.grazz.pebblerss.feed.FeedItemCursor;
import com.grazz.pebblerss.kits.ChunkTransferKit;
import com.grazz.pebblerss.kits.PebbleImageKit;
import com.grazz.pebblerss.provider.RSSFeed;
import com.grazz.pebblerss.provider.RSSFeedItem;

public class RSSDataReceiver extends PebbleDataReceiver {

	private static final int MAX_TRANSMIT_SIZE = 96;
	private static final int MAX_RETRY = 8;

	private RSSService _service;
	private ArrayDeque<PebbleDictionary> _msgQueue = new ArrayDeque<PebbleDictionary>();
	private SparseArray<PebbleDictionary> _msgSent = new SparseArray<PebbleDictionary>();
	private SparseIntArray _msgSentRetry = new SparseIntArray();
	private int _transactionId;
	private FeedCursor _feedCursor;
	private FeedItemCursor _feedItemCursor;

	private PebbleAckReceiver _ackReceiver = new PebbleAckReceiver(StaticValues.APP_UUID) {
		@Override
		public void receiveAck(Context context, int transactionId) {
			_msgSent.remove(transactionId);
		}
	};

	private PebbleNackReceiver _nackReceiver = new PebbleNackReceiver(StaticValues.APP_UUID) {
		@Override
		public void receiveNack(Context context, int transactionId) {
			Log.d("receiveNack", "TID " + _transactionId);
			PebbleDictionary dictionary = _msgSent.get(transactionId);
			if (dictionary != null) {
				Integer retryCount = _msgSentRetry.get(transactionId) + 1;
				if (retryCount > MAX_RETRY)
					return;
				_msgQueue.push(dictionary);
				sendData(context, retryCount);
			}
		}
	};

	protected RSSDataReceiver(RSSService service) {
		super(StaticValues.APP_UUID);
		_service = service;
	}

	private void resetConnectionData(Context context) {
		_feedItemCursor = null;
		_feedCursor = null;
		_msgQueue.clear();
		_msgSent.clear();
		_msgSentRetry.clear();
		_transactionId = 0;
		_service.getFeedManager().checkFeeds(true);
		_feedCursor = new FeedCursor(context);
	}

	private void queueData(PebbleDictionary dictionary) {
		Log.d("queueData", dictionary.toJsonString());
		_msgQueue.add(dictionary);
	}

	private void queueData(List<PebbleDictionary> list) {
		for (PebbleDictionary dictionary : list)
			queueData(dictionary);
	}

	private boolean sillyIsWatchConnected(Context context) {
		// why a SecurityException sometimes?
		try {
			return PebbleKit.isWatchConnected(context);
		} catch (SecurityException e) {
		}
		return false;
	}

	private void sendData(Context context, int retryCount) {
		if (!_msgQueue.isEmpty() && sillyIsWatchConnected(context)) {
			PebbleDictionary dictionary = _msgQueue.remove();
			_msgSent.put(_transactionId, dictionary);
			_msgSentRetry.put(_transactionId, Integer.valueOf(retryCount));
			Log.d("sendData", "TID " + _transactionId + ": " + dictionary.toJsonString());
			PebbleKit.sendDataToPebbleWithTransactionId(context, StaticValues.APP_UUID, dictionary, _transactionId);
			_transactionId = (_transactionId + 1) % 256;
		}
	}

	@Override
	public void receiveData(final Context context, int transactionId, PebbleDictionary data) {
		Log.d("receiveData", data.toJsonString());
		PebbleKit.sendAckToPebble(context, transactionId);

		Long command_id = data.getUnsignedInteger(1090);
		if (command_id != null)
			switch (command_id.intValue()) {
			case 0: // hello
				resetConnectionData(context);
				sendFontPacket(context);
				sendFeed(context);
				break;
			case 1: // ack, continue queue
				sendData(context, 0);
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
			_feedItemCursor = null;
			_feedItemCursor = _feedCursor.getItemCursor(context, feed_id.intValue());
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

		Long thumbnail_item_id = data.getUnsignedInteger(1094);
		if (thumbnail_item_id != null && _feedItemCursor != null) {
			final RSSFeedItem item = _feedItemCursor.getItem(thumbnail_item_id.intValue());
			String thumbnail = item.getThumbnailData(context);
			if (thumbnail != null) {
				byte[] decoded = Base64.decode(thumbnail, Base64.DEFAULT);
				thumbnail = null;
				Bitmap bitmap = BitmapFactory.decodeByteArray(decoded, 0, decoded.length);
				decoded = null;
				ByteBuffer buffer = PebbleImageKit.convertBitmapToBytes(bitmap);
				PebbleDictionary metadata = new PebbleDictionary();
				metadata.addUint16(1018, (short) bitmap.getWidth());
				metadata.addUint16(1019, (short) bitmap.getHeight());
				metadata.addUint8(1020, (byte) PebbleImageKit.calculateBytesPerRow(bitmap.getWidth()));
				bitmap = null;
				queueData(metadata);
				ChunkTransferKit kit = new ChunkTransferKit(buffer);
				queueData(kit.getDictionaries());
				kit = null;
				buffer = null;
				sendData(context, 0);
			}
		}
	}

	public void sendFontPacket(Context context) {
		SharedPreferences preferences = context.getSharedPreferences(StaticValues.PREFERENCES_KEY, Context.MODE_PRIVATE);
		Resources resources = context.getResources();
		PebbleDictionary dictionary = new PebbleDictionary();
		dictionary.addUint8(1013, Integer.valueOf(preferences.getString(resources.getString(R.string.setting_feedfont), "5")).byteValue());
		dictionary.addUint8(1014, Integer.valueOf(preferences.getString(resources.getString(R.string.setting_itemfont), "0")).byteValue());
		dictionary.addUint8(1015, Integer.valueOf(preferences.getString(resources.getString(R.string.setting_messagefont), "2")).byteValue());
		dictionary.addUint8(1016, Integer.valueOf(preferences.getString(resources.getString(R.string.setting_cellheight), "33")).byteValue());
		queueData(dictionary);
		sendData(context, 0);
	}

	public void sendInRefreshPacket(Context context) {
		PebbleDictionary dictionary = new PebbleDictionary();
		dictionary.addUint8(1017, (byte) 0);
		queueData(dictionary);
		sendData(context, 0);
	}

	private void sendFeed(Context context) {
		int position = _feedCursor.getPosition();
		RSSFeed feed = _feedCursor.getNextItem();
		if (feed != null) {
			PebbleDictionary feed_dict = new PebbleDictionary();
			feed_dict.addString(1001, substring(feed.getName(), MAX_TRANSMIT_SIZE));
			feed_dict.addUint8(1011, (byte) _feedCursor.getTotal());
			feed_dict.addUint8(1012, (byte) position);
			queueData(feed_dict);
			sendData(context, 0);
		}
	}

	private void sendFeedItem(Context context) {
		int position = _feedItemCursor.getPosition();
		RSSFeedItem item = _feedItemCursor.getNextItem();
		if (item != null) {
			PebbleDictionary item_dict = new PebbleDictionary();
			item_dict.addString(1002, substring(item.getTitle(), MAX_TRANSMIT_SIZE));
			item_dict.addUint8(1021, (byte) _feedItemCursor.getTotal());
			item_dict.addUint8(1022, (byte) position);
			queueData(item_dict);
			sendData(context, 0);
		}
	}

	private void sendFeedItemText(Context context, Long item_id) {
		RSSFeedItem item = _feedItemCursor.getItem(item_id.intValue());
		PebbleDictionary metadata = new PebbleDictionary();
		metadata.addUint8(1023, (byte) (item.getThumbnailData(context) == null ? 0 : 1));
		queueData(metadata);
		byte[] content = null;
		try {
			content = item.getContent().getBytes("UTF-8");
		} catch (Exception e) {
			e.printStackTrace();
		}
		if (content != null) {
			ByteBuffer buffer = ByteBuffer.allocate(Math.min(3024, content.length));
			for (int i = 0; i < content.length && i < 3024; i++)
				buffer.put(content[i]);
			content = null;
			buffer.rewind();
			ChunkTransferKit kit = new ChunkTransferKit(buffer);
			queueData(kit.getDictionaries());
			kit = null;
			buffer = null;
			sendData(context, 0);
		}
	}

	private String substring(String string, int maxLength) {
		int length = string.length();
		if (length > maxLength)
			length = maxLength;
		return string.substring(0, length);
	}

	public PebbleAckReceiver getAckReceiver() {
		return _ackReceiver;
	}

	public PebbleNackReceiver getNackReceiver() {
		return _nackReceiver;
	}

}
