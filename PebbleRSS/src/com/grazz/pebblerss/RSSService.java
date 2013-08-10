package com.grazz.pebblerss;

import java.util.Timer;
import java.util.TimerTask;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Binder;
import android.os.IBinder;

import com.getpebble.android.kit.PebbleKit;
import com.getpebble.android.kit.PebbleKit.PebbleAckReceiver;
import com.getpebble.android.kit.PebbleKit.PebbleNackReceiver;
import com.grazz.pebblerss.feed.FeedManager;

public class RSSService extends Service {

	private FeedManager _feedManager;

	public class RSSServiceBinder extends Binder {
		RSSService getService() {
			return RSSService.this;
		}
	}

	private RSSServiceBinder _binder = new RSSServiceBinder();
	private RSSDataReceiver _receiver;
	private PebbleAckReceiver _ackReceiver;
	private PebbleNackReceiver _nackReceiver;

	private Timer _timer = new Timer(true);
	private TimerTask _canvasTask;

	public RSSService() {
		_feedManager = new FeedManager(this);
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
		_feedManager.convertOldConfig(this);
		PebbleKit.registerReceivedAckHandler(this, _ackReceiver);
		PebbleKit.registerReceivedNackHandler(this, _nackReceiver);
	}

	@Override
	public void onDestroy() {
		super.onDestroy();
		_timer.cancel();
		unregisterReceiver(_ackReceiver);
		unregisterReceiver(_nackReceiver);
	}

	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		if (intent != null) {
			if (intent.hasExtra(PebbleWakeupReceiver.DATA))
				_receiver.onReceive(this, (Intent) intent.getExtras().get(PebbleWakeupReceiver.DATA));
			if (intent.hasExtra(CanvasRSSPlugin.PLUGINSTART) && isCanvasEnabled())
				setCanvasEnabled(true);
		}
		return super.onStartCommand(intent, flags, startId);
	}

	public FeedManager getFeedManager() {
		return _feedManager;
	}

	public void sendFontPacket() {
		_receiver.sendFontPacket(this);
	}

	public Boolean isCanvasEnabled() {
		SharedPreferences preferences = getSharedPreferences(StaticValues.PREFERENCES_KEY, Context.MODE_PRIVATE);
		return preferences.getBoolean(getResources().getString(R.string.setting_enablecanvas), true);
	}

	public void setCanvasEnabled(Boolean enabled) {
		if (enabled) {
			if (_canvasTask == null) {
				_canvasTask = new TimerTask() {
					@Override
					public void run() {
						if (_feedManager.checkFeeds(true))
							_feedManager.notifyCanvas(RSSService.this);
					}
				};
				_timer.schedule(_canvasTask, 0, 60 * 1000);
			}
		} else if (_canvasTask != null) {
			_canvasTask.cancel();
			_canvasTask = null;
		}
	}

}
