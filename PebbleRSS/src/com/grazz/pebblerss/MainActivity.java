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
import android.content.res.Resources;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemLongClickListener;
import android.widget.ListView;
import android.widget.Toast;

import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuItem;
import com.grazz.pebblerss.feed.FeedListAdapter;
import com.grazz.pebblerss.feed.FeedManager;
import com.grazz.pebblerss.provider.RSSFeed;

public class MainActivity extends RSSServiceActivity {

	private static final int ID_ACTIVITY_FEED = 0;
	private static final int ID_ACTIVITY_UPDATEWATCHAPP = 1;
	private static final int ID_ACTIVITY_SETTINGS = 2;
	private static final String WATCHAPP_FILENAME = "pebblerss.pbw";

	private ListView _lvFeeds;

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);
		RSSService service = getRSSService();
		switch (requestCode) {
		case ID_ACTIVITY_FEED:
			if (service.isCanvasEnabled())
				service.getFeedManager().notifyCanvas(this);
			refreshFeeds();
			break;
		case ID_ACTIVITY_UPDATEWATCHAPP:
			setWatchAppUpdated();
			break;
		case ID_ACTIVITY_SETTINGS:
			service.setCanvasEnabled(service.isCanvasEnabled());
			break;
		}
	}

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		startService(new Intent(this, RSSService.class));
		_lvFeeds = (ListView) findViewById(R.id.lvFeeds);
		_lvFeeds.setEmptyView(findViewById(R.id.lvFeedsEmpty));
		checkWatchAppUpdate();
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		getSupportMenuInflater().inflate(R.menu.main, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case R.id.action_add:
			Intent intent = new Intent(this, FeedActivity.class);
			intent.putExtra(RSSFeed.FEED_ACTION, RSSFeed.FEED_ADD);
			startActivityForResult(intent, ID_ACTIVITY_FEED);
			return true;
		case R.id.action_app:
			sendAppToWatch();
			return true;
		case R.id.action_settings:
			startActivityForResult(new Intent(this, SettingsActivity.class), ID_ACTIVITY_SETTINGS);
			return true;
		case R.id.action_about:
			startActivity(new Intent(this, AboutActivity.class));
			return true;
		}
		return false;
	}

	@Override
	protected void onBindToService() {
		final FeedManager manager = getRSSService().getFeedManager();
		_lvFeeds.setAdapter(new FeedListAdapter(this, manager));
		_lvFeeds.setOnItemLongClickListener(new OnItemLongClickListener() {
			@Override
			public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
				RSSFeed feed = manager.getFeedById(id);
				Intent intent = new Intent(getApplicationContext(), FeedActivity.class);
				intent.putExtra(RSSFeed.FEED_ACTION, RSSFeed.FEED_EDIT);
				intent.putExtra(RSSFeed.FEED_ID, feed.getId());
				startActivityForResult(intent, ID_ACTIVITY_FEED);
				return true;
			}
		});
		refreshFeeds();
	}

	@Override
	protected void onResume() {
		super.onResume();
		refreshFeedView();
	}

	private void refreshFeeds() {
		new Thread(new Runnable() {
			@Override
			public void run() {
				getRSSService().getFeedManager().checkFeeds(true);
				refreshFeedView();
			}
		}).start();
	}

	private void refreshFeedView() {
		runOnUiThread(new Runnable() {
			@Override
			public void run() {
				_lvFeeds.setAdapter(_lvFeeds.getAdapter());
			}
		});
	}

	private void checkWatchAppUpdate() {
		final SharedPreferences pref = getSharedPreferences(StaticValues.PREFERENCES_KEY, Context.MODE_PRIVATE);
		final Resources resources = getResources();
		int pebbleAppVersion = pref.getInt(StaticValues.PREFERENCES_VALUE_PEBBLE_APP_VERSION, 1);
		if (pebbleAppVersion < StaticValues.PEBBLE_APP_VERSION) {
			AlertDialog.Builder builder = new AlertDialog.Builder(this);
			builder.setTitle(resources.getString(R.string.watchapp_update_title));
			builder.setMessage(resources.getString(R.string.watchapp_update_message));
			builder.setPositiveButton(resources.getString(R.string.button_positive), new DialogInterface.OnClickListener() {
				@Override
				public void onClick(DialogInterface dialog, int which) {
					sendAppToWatch();
				}
			});
			builder.setNegativeButton(resources.getString(R.string.button_negative), new DialogInterface.OnClickListener() {
				@Override
				public void onClick(DialogInterface dialog, int which) {
					setWatchAppUpdated();
				}
			});
			builder.create().show();
		}
	}

	private void sendAppToWatch() {
		try {
			InputStream input = getAssets().open(WATCHAPP_FILENAME);
			File file = new File(Environment.getExternalStorageDirectory(), WATCHAPP_FILENAME);
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
			startActivityForResult(intent, ID_ACTIVITY_UPDATEWATCHAPP);
		} catch (Exception e) {
			Toast.makeText(this, getResources().getString(R.string.message_deploy_failed), Toast.LENGTH_LONG).show();
		}
	}

	private void setWatchAppUpdated() {
		SharedPreferences pref = getSharedPreferences(StaticValues.PREFERENCES_KEY, Context.MODE_PRIVATE);
		Editor editor = pref.edit();
		editor.putInt(StaticValues.PREFERENCES_VALUE_PEBBLE_APP_VERSION, StaticValues.PEBBLE_APP_VERSION);
		editor.commit();
	}

}
