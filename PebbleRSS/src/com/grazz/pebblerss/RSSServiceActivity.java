package com.grazz.pebblerss;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;

import com.actionbarsherlock.app.SherlockActivity;
import com.grazz.pebblerss.RSSService.RSSServiceBinder;

public abstract class RSSServiceActivity extends SherlockActivity {

	private RSSService _rssService;

	private ServiceConnection _rssServiceConnection = new ServiceConnection() {
		public void onServiceConnected(ComponentName name, IBinder service) {
			_rssService = ((RSSServiceBinder) service).getService();
			onBindToService();
		};

		public void onServiceDisconnected(ComponentName name) {
			_rssService = null;
		};
	};

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		bindService(new Intent(this, RSSService.class), _rssServiceConnection, Context.BIND_AUTO_CREATE);
	}

	protected abstract void onBindToService();

	@Override
	protected void onDestroy() {
		super.onDestroy();
		if (_rssService != null)
			unbindService(_rssServiceConnection);
	}

	protected RSSService getRSSService() {
		return _rssService;
	}

}
