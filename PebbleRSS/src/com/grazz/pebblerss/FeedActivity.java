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
import android.view.View;
import android.view.View.OnClickListener;
import android.webkit.URLUtil;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ScrollView;
import android.widget.Toast;

import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuItem;
import com.grazz.pebblerss.feed.FeedManager;
import com.grazz.pebblerss.feed.FeedProbe;
import com.grazz.pebblerss.provider.RSSFeed;

public class FeedActivity extends RSSServiceActivity {

	private ScrollView _view;
	private EditText _url;
	private EditText _name;
	private EditText _interval;
	private EditText _retention;
	private EditText _username;
	private EditText _password;
	private Button _login;
	private int _feedAction;
	private long _feedId;
	private boolean _isValidFeed = false;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_feed);
		setupActionBar();

		_view = (ScrollView) findViewById(R.id.svScroll);
		_url = (EditText) findViewById(R.id.etURL);
		_name = (EditText) findViewById(R.id.etName);
		_interval = (EditText) findViewById(R.id.etInterval);
		_retention = (EditText) findViewById(R.id.etRetention);
		_username = (EditText) findViewById(R.id.etUsername);
		_password = (EditText) findViewById(R.id.etPassword);

		_login = (Button) findViewById(R.id.bnLogin);
		_login.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				checkFeed(_url.getText().toString());
			}
		});

		final SharedPreferences pref = getSharedPreferences(StaticValues.PREFERENCES_KEY, Context.MODE_PRIVATE);
		final Resources resources = getResources();
		boolean seenWarning = pref.getBoolean(StaticValues.PREFERENCES_VALUE_SEEN_DATA_WARNING, false);
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
			getSupportActionBar().setDisplayHomeAsUpEnabled(true);
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
			String intervalText = _interval.getText().toString();
			String retentionText = _retention.getText().toString();
			if (!_isValidFeed || intervalText.length() == 0 || retentionText.length() == 0) {
				Toast.makeText(this, getResources().getString(R.string.message_feed_invalid), Toast.LENGTH_LONG).show();
				return false;
			}
			Uri uri = Uri.parse(_url.getText().toString());
			String name = _name.getText().toString();
			String username = _username.getText().toString();
			String password = _password.getText().toString();
			Integer interval = Integer.valueOf(intervalText);
			Integer retention = Integer.valueOf(retentionText);
			if (_feedAction == RSSFeed.FEED_ADD) {
				feedManager.addFeed(uri, name, interval, retention, username, password);
			} else {
				RSSFeed feed = feedManager.getFeedById(_feedId);
				feed.setUri(uri);
				feed.setName(name);
				feed.setInterval(interval);
				feed.setRetention(retention);
				feed.setUsername(username);
				feed.setPassword(password);
				feed.persist(this);
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
			_retention.setText("24");
			break;
		case RSSFeed.FEED_EDIT:
			_feedId = intent.getExtras().getLong(RSSFeed.FEED_ID);
			RSSFeed feed = getRSSService().getFeedManager().getFeedById(_feedId);
			_url.setText(feed.getUri().toString());
			_name.setText(feed.getName());
			_interval.setText(String.valueOf(feed.getInterval()));
			_retention.setText(String.valueOf(feed.getRetention()));
			String username = feed.getUsername();
			if (username != null)
				_username.setText(username);
			String password = feed.getPassword();
			if (password != null)
				_password.setText(password);
			_isValidFeed = true;
			break;
		}

		_url.addTextChangedListener(new TextWatcher() {
			@Override
			public void onTextChanged(CharSequence s, int start, int before, int count) {
				checkFeed(s);
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

	private void checkFeed(CharSequence s) {
		final String url = s.toString();
		final String username = _username.getText().toString();
		final String password = _password.getText().toString();
		if (url != null && URLUtil.isValidUrl(url)) {
			new Thread(new Runnable() {
				@Override
				public void run() {
					String user = username;
					if (user.length() == 0)
						user = null;
					final FeedProbe probe = new FeedProbe(Uri.parse(url), user, password);
					_isValidFeed = probe.isParsed() && (probe.getNumberOfItems() > 0);
					if (_isValidFeed)
						runOnUiThread(new Runnable() {
							@Override
							public void run() {
								_view.scrollTo(0, 0);
								_name.setText(probe.getName());
								_name.requestFocus();
							}
						});
				}
			}).start();
		} else
			_isValidFeed = false;
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
