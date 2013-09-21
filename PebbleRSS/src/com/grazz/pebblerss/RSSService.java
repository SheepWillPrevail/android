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
	private Timer _timer = new Timer(true);
	private TimerTask _canvasTask;

	public RSSService() {
		_feedManager = new FeedManager(this);
		_receiver = new RSSDataReceiver(this);
	}

	@Override
	public IBinder onBind(Intent intent) {
		return _binder;
	}

	@Override
	public void onCreate() {
		super.onCreate();
		_feedManager.convertOldConfig(this);
		PebbleKit.registerReceivedAckHandler(this, _receiver.getAckReceiver());
		PebbleKit.registerReceivedNackHandler(this, _receiver.getNackReceiver());
		startBackgroundRefresh();
	}

	@Override
	public void onDestroy() {
		super.onDestroy();
		_timer.cancel();
		unregisterReceiver(_receiver.getAckReceiver());
		unregisterReceiver(_receiver.getNackReceiver());
	}

	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		if (intent != null) {
			if (intent.hasExtra(PebbleWakeupReceiver.DATA))
				_receiver.onReceive(this, (Intent) intent.getExtras().get(PebbleWakeupReceiver.DATA));
			if (intent.hasExtra(CanvasRSSPlugin.PLUGINSTART))
				startBackgroundRefresh();
		}
		return super.onStartCommand(intent, flags, startId);
	}

	private void startBackgroundRefresh() {
		if (isBackgroundRefreshEnabled())
			setBackgroundRefreshEnabled(true);
	}

	public FeedManager getFeedManager() {
		return _feedManager;
	}

	public void sendFontPacket() {
		_receiver.sendFontPacket(this);
	}

	public void sendIsParsingPacket() {
		_receiver.sendInRefreshPacket(this);
	}

	public boolean isBackgroundRefreshEnabled() {
		SharedPreferences preferences = getSharedPreferences(StaticValues.PREFERENCES_KEY, Context.MODE_PRIVATE);
		return preferences.getBoolean(getResources().getString(R.string.setting_backgrounddata), true);
	}

	public void setBackgroundRefreshEnabled(boolean enabled) {
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
