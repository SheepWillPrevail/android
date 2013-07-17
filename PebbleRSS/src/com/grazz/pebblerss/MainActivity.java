package com.grazz.pebblerss;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnLongClickListener;
import android.widget.ListView;

import com.actionbarsherlock.view.Menu;
import com.grazz.pebblerss.feed.Feed;
import com.grazz.pebblerss.feed.FeedListAdapter;

public class MainActivity extends RSSServiceActivity {

	private static final String PEBBLERSS_PBW = "pebblerss.pbw";
	private ListView _lvFeeds;
	private FeedListAdapter _adapter;

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);
		if (requestCode == 0) {
			_lvFeeds.setAdapter(_lvFeeds.getAdapter());
			getRSSService().writeConfig();
		}
		if (requestCode == 1)
			setPebbleAppUpdated();
	}

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		_lvFeeds = (ListView) findViewById(R.id.lvFeeds);
		_lvFeeds.setEmptyView(findViewById(R.id.lvFeedsEmpty));
		startService(new Intent(this, RSSService.class));

		SharedPreferences pref = getSharedPreferences(StaticValues.PREFERENCES_KEY, Context.MODE_PRIVATE);
		int pebbleAppVersion = pref.getInt(StaticValues.PREFERENCES_VALUE_PEBBLE_APP_VERSION, 1);
		if (pebbleAppVersion < StaticValues.PEBBLE_APP_VERSION) {
			AlertDialog.Builder builder = new AlertDialog.Builder(this);
			builder.setTitle(getResources().getString(R.string.pebble_app_update_title));
			builder.setMessage(getResources().getString(R.string.pebble_app_update_message));
			builder.setPositiveButton(getResources().getString(R.string.button_positive), new DialogInterface.OnClickListener() {
				@Override
				public void onClick(DialogInterface dialog, int which) {
					sendApp();
				}
			});
			builder.setNegativeButton(getResources().getString(R.string.button_negative), new DialogInterface.OnClickListener() {
				@Override
				public void onClick(DialogInterface dialog, int which) {
					setPebbleAppUpdated();
				}
			});
			AlertDialog dialog = builder.create();
			dialog.show();
		}
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		getSupportMenuInflater().inflate(R.menu.main, menu);
		return super.onCreateOptionsMenu(menu);
	}

	@Override
	public boolean onMenuItemSelected(int featureId, MenuItem item) {
		switch (item.getItemId()) {
		case R.id.action_add:
			Intent intent = new Intent(this, FeedActivity.class);
			intent.putExtra(Feed.FEED_ACTION, Feed.FEED_ADD);
			startActivityForResult(intent, 0);
			return true;
		case R.id.action_about:
			startActivity(new Intent(this, AboutActivity.class));
			return true;
		case R.id.action_app:
			sendApp();
			return true;
		}
		return super.onMenuItemSelected(featureId, item);
	}

	@Override
	protected void onBindToService() {
		OnLongClickListener listener = new OnLongClickListener() {
			@Override
			public boolean onLongClick(View v) {
				Intent intent = new Intent(getApplicationContext(), FeedActivity.class);
				intent.putExtra(Feed.FEED_ACTION, Feed.FEED_EDIT);
				intent.putExtra(Feed.FEED_ID, (Integer) v.getTag());
				startActivityForResult(intent, 0);
				return true;
			}
		};
		_adapter = new FeedListAdapter(getRSSService().getFeeds(), listener);
		_lvFeeds.setAdapter(_adapter);
	}

	private void sendApp() {
		try {
			InputStream input = getAssets().open(PEBBLERSS_PBW);
			File file = new File(Environment.getExternalStorageDirectory(), PEBBLERSS_PBW);
			file.setReadable(true, false);
			OutputStream output = new FileOutputStream(file);
			try {
				byte[] buffer = new byte[1024];
				int read;
				while ((read = input.read(buffer)) != -1)
					output.write(buffer, 0, read);
				output.flush();
			} finally {
				output.close();
			}
			input.close();
			Intent intent = new Intent(Intent.ACTION_VIEW);
			intent.setDataAndType(Uri.fromFile(file), "application/octet-stream");
			startActivityForResult(intent, 1);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	private void setPebbleAppUpdated() {
		SharedPreferences pref = getSharedPreferences(StaticValues.PREFERENCES_KEY, Context.MODE_PRIVATE);
		Editor editor = pref.edit();
		editor.putInt(StaticValues.PREFERENCES_VALUE_PEBBLE_APP_VERSION, StaticValues.PEBBLE_APP_VERSION);
		editor.commit();
	}
}
