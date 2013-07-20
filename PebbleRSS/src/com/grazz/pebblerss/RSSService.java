package com.grazz.pebblerss;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;

import com.getpebble.android.kit.PebbleKit;
import com.getpebble.android.kit.PebbleKit.PebbleAckReceiver;
import com.getpebble.android.kit.PebbleKit.PebbleNackReceiver;
import com.grazz.pebblerss.feed.FeedManager;

public class RSSService extends Service {

	private FeedManager _feedManager = new FeedManager();

	public class RSSServiceBinder extends Binder {
		RSSService getService() {
			return RSSService.this;
		}
	}

	private RSSServiceBinder _binder = new RSSServiceBinder();
	private RSSDataReceiver _receiver;

	private PebbleAckReceiver _ackReceiver;
	private PebbleNackReceiver _nackReceiver;

	public RSSService() {
		_receiver = new RSSDataReceiver(this);
		_ackReceiver = new PebbleAckReceiver(StaticValues.APP_UUID) {
			@Override
			public void receiveAck(Context context, int transactionId) {
				_receiver.ack(transactionId);
			}
		};
		_nackReceiver = new PebbleNackReceiver(StaticValues.APP_UUID) {
			@Override
			public void receiveNack(Context context, int transactionId) {
				_receiver.nack(RSSService.this, transactionId);
			}
		};
	}

	@Override
	public IBinder onBind(Intent intent) {
		return _binder;
	}

	@Override
	public void onCreate() {
		super.onCreate();
		_feedManager.readConfig(this);
		PebbleKit.registerReceivedAckHandler(this, _ackReceiver);
		PebbleKit.registerReceivedNackHandler(this, _nackReceiver);
	}

	@Override
	public void onDestroy() {
		super.onDestroy();
		unregisterReceiver(_ackReceiver);
		unregisterReceiver(_nackReceiver);
	}

	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		if (intent != null && intent.hasExtra(PassiveRSSDataReceiver.INTENT))
			_receiver.onReceive(this, (Intent) intent.getExtras().get(PassiveRSSDataReceiver.INTENT));
		return super.onStartCommand(intent, flags, startId);
	}

	public FeedManager getFeedManager() {
		return _feedManager;
	}

}
