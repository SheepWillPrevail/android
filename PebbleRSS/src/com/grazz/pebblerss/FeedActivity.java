package com.grazz.pebblerss;

import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.app.AlertDialog;
import android.content.ClipData;
import android.content.ClipData.Item;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.content.res.Resources;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.app.NavUtils;
import android.text.Editable;
import android.text.TextWatcher;
import android.webkit.URLUtil;
import android.widget.EditText;
import android.widget.Toast;

import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuItem;
import com.grazz.pebblerss.feed.FeedManager;
import com.grazz.pebblerss.feed.FeedProbe;
import com.grazz.pebblerss.provider.RSSFeed;

public class FeedActivity extends RSSServiceActivity {

	private EditText _url;
	private EditText _name;
	private EditText _interval;
	private int _feedAction;
	private long _feedId;
	private Boolean _isValidFeed = false;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_feed);
		setupActionBar();

		_url = (EditText) findViewById(R.id.etURL);
		_name = (EditText) findViewById(R.id.etFeedName);
		_interval = (EditText) findViewById(R.id.etInterval);

		final SharedPreferences pref = getSharedPreferences(StaticValues.PREFERENCES_KEY, Context.MODE_PRIVATE);
		final Resources resources = getResources();

		Boolean seenWarning = pref.getBoolean(StaticValues.PREFERENCES_VALUE_SEEN_DATA_WARNING, false);
		if (!seenWarning) {
			AlertDialog.Builder builder = new AlertDialog.Builder(this);
			builder.setTitle(resources.getString(R.string.data_warning_title));
			builder.setMessage(resources.getString(R.string.data_warning_message));
			builder.setPositiveButton(resources.getString(R.string.button_agree), new DialogInterface.OnClickListener() {
				@Override
				public void onClick(DialogInterface dialog, int which) {
					Editor editor = pref.edit();
					editor.putBoolean(StaticValues.PREFERENCES_VALUE_SEEN_DATA_WARNING, true);
					editor.commit();
				}
			});
			builder.setNegativeButton(resources.getString(R.string.button_disagree), new DialogInterface.OnClickListener() {
				@Override
				public void onClick(DialogInterface dialog, int which) {
					finish();
				}
			});
			builder.create().show();
		}
	}

	/**
	 * Set up the {@link android.app.ActionBar}, if the API is available.
	 */
	@TargetApi(Build.VERSION_CODES.HONEYCOMB)
	private void setupActionBar() {
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
			getActionBar().setDisplayHomeAsUpEnabled(true);
		}
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		getSupportMenuInflater().inflate(R.menu.feed, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		FeedManager feedManager = getRSSService().getFeedManager();
		switch (item.getItemId()) {
		case android.R.id.home:
			NavUtils.navigateUpFromSameTask(this);
			return true;
		case R.id.action_save:
			String interval = _interval.getText().toString();
			if (!_isValidFeed || interval.length() == 0) {
				Toast.makeText(this, getResources().getString(R.string.error_feed_invalid), Toast.LENGTH_LONG).show();
				return false;
			}
			Integer scaledinterval = Integer.valueOf(interval);
			if (_feedAction == RSSFeed.FEED_ADD) {
				feedManager.addFeed(Uri.parse(_url.getText().toString()), _name.getText().toString(), scaledinterval);
			} else {
				RSSFeed feed = feedManager.getFeedById(_feedId);
				feed.setUri(Uri.parse(_url.getText().toString()));
				feed.setName(_name.getText().toString());
				feed.setInterval(scaledinterval);
				feed.save(this);
			}
			finish();
			return true;
		case R.id.action_delete:
			if (_feedAction == RSSFeed.FEED_EDIT)
				feedManager.removeFeed(feedManager.getFeedById(_feedId));
			finish();
			return true;
		}
		return false;
	}

	@Override
	protected void onBindToService() {
		Intent intent = getIntent();
		_feedAction = intent.getExtras().getInt(RSSFeed.FEED_ACTION);
		switch (_feedAction) {
		case RSSFeed.FEED_ADD:
			_interval.setText("30");
			break;
		case RSSFeed.FEED_EDIT:
			_feedId = intent.getExtras().getLong(RSSFeed.FEED_ID);
			RSSFeed feed = getRSSService().getFeedManager().getFeedById(_feedId);
			_url.setText(feed.getUri().toString());
			_name.setText(feed.getName());
			_interval.setText(String.valueOf(feed.getInterval()));
			_isValidFeed = true;
			break;
		}

		_url.addTextChangedListener(new TextWatcher() {
			@Override
			public void onTextChanged(CharSequence s, int start, int before, int count) {
				final String url = s.toString();
				if (url != null && URLUtil.isValidUrl(url)) {
					new Thread(new Runnable() {
						@Override
						public void run() {
							final FeedProbe probe = new FeedProbe(Uri.parse(url));
							_isValidFeed = probe.isParsed() && (probe.getNumberOfItems() > 0);
							if (probe.isParsed())
								runOnUiThread(new Runnable() {
									@Override
									public void run() {
										_name.setText(probe.getName());
									}
								});
						}
					}).start();
				} else
					_isValidFeed = false;
			}

			@Override
			public void beforeTextChanged(CharSequence s, int start, int count, int after) {
			}

			@Override
			public void afterTextChanged(Editable s) {
			}
		});

		tryGetShareItem(intent);
	}

	@SuppressLint("NewApi")
	private void tryGetShareItem(Intent intent) {
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
			ClipData clipData = intent.getClipData();
			if (clipData != null) {
				Item item = clipData.getItemAt(0);
				if (item != null)
					_url.setText(item.getText());
			}
		}
	}

}
