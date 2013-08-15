package com.grazz.pebblerss;

import android.annotation.TargetApi;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceChangeListener;

import com.actionbarsherlock.app.SherlockPreferenceActivity;
import com.getpebble.android.kit.PebbleKit;
import com.grazz.pebblerss.RSSService.RSSServiceBinder;

public class SettingsActivity extends SherlockPreferenceActivity {

	private RSSService _rssService;

	private ServiceConnection _rssServiceConnection = new ServiceConnection() {
		public void onServiceConnected(ComponentName name, IBinder service) {
			_rssService = ((RSSServiceBinder) service).getService();
		};

		public void onServiceDisconnected(ComponentName name) {
			_rssService = null;
		};
	};

	@SuppressWarnings("deprecation")
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setupActionBar();
		bindService(new Intent(this, RSSService.class), _rssServiceConnection, Context.BIND_AUTO_CREATE);
		if (PebbleKit.isWatchConnected(this))
			PebbleKit.startAppOnPebble(this, StaticValues.APP_UUID);

		getPreferenceManager().setSharedPreferencesMode(Context.MODE_PRIVATE);
		getPreferenceManager().setSharedPreferencesName(StaticValues.PREFERENCES_KEY);

		addPreferencesFromResource(R.xml.settings);

		OnPreferenceChangeListener listener = new OnPreferenceChangeListener() {
			@Override
			public boolean onPreferenceChange(Preference preference, Object newValue) {
				SharedPreferences preferences = getSharedPreferences(StaticValues.PREFERENCES_KEY, Context.MODE_PRIVATE);
				Editor editor = preferences.edit();
				editor.putString(preference.getKey(), (String) newValue);
				editor.commit();
				if (_rssService != null)
					_rssService.sendFontPacket();
				return true;
			}
		};

		findPreference(getResources().getString(R.string.setting_cellheight)).setOnPreferenceChangeListener(listener);
		findPreference(getResources().getString(R.string.setting_feedfont)).setOnPreferenceChangeListener(listener);
		findPreference(getResources().getString(R.string.setting_itemfont)).setOnPreferenceChangeListener(listener);
		findPreference(getResources().getString(R.string.setting_messagefont)).setOnPreferenceChangeListener(listener);
	}

	/**
	 * Set up the {@link android.app.ActionBar}, if the API is available.
	 */
	@TargetApi(Build.VERSION_CODES.HONEYCOMB)
	private void setupActionBar() {
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
			getSupportActionBar().setDisplayHomeAsUpEnabled(true);
		}
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();
		if (_rssService != null)
			unbindService(_rssServiceConnection);
	}

}
