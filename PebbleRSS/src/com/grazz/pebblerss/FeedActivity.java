package com.grazz.pebblerss;

import android.annotation.TargetApi;
import android.content.Intent;
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
import com.grazz.pebblerss.feed.Feed;

public class FeedActivity extends RSSServiceActivity {

	private EditText _url;
	private EditText _name;
	private EditText _interval;
	private int _feedAction;
	private int _feedId;
	private Boolean _isValidFeed = false;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_feed);
		setupActionBar();

		_url = (EditText) findViewById(R.id.etURL);
		_name = (EditText) findViewById(R.id.etFeedName);
		_interval = (EditText) findViewById(R.id.etInterval);
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
		switch (item.getItemId()) {
		case android.R.id.home:
			NavUtils.navigateUpFromSameTask(this);
			return true;
		case R.id.action_save:
			String interval = _interval.getText().toString();
			if (!_isValidFeed || interval.length() == 0) {
				Toast toast = Toast.makeText(this, getResources().getString(R.string.error_feed_invalid), Toast.LENGTH_LONG);
				toast.show();
				return false;
			}
			Integer scaledinterval = Integer.valueOf(interval);
			if (_feedAction == Feed.FEED_ADD) {
				Feed feed = getRSSService().addFeed(Uri.parse(_url.getText().toString()));
				feed.doParse();
				feed.setName(_name.getText().toString());
				feed.setInterval(scaledinterval);
			} else {
				Feed feed = getRSSService().getFeeds().get(_feedId);
				feed.setLink(Uri.parse(_url.getText().toString()));
				feed.setName(_name.getText().toString());
				feed.setInterval(scaledinterval);
			}
			finish();
			return true;
		case R.id.action_delete:
			if (_feedAction == Feed.FEED_EDIT)
				getRSSService().removeFeed(_feedId);
			finish();
			return true;
		}
		return false;
	}

	@Override
	protected void onBindToService() {
		Intent intent = getIntent();
		_feedAction = intent.getExtras().getInt(Feed.FEED_ACTION);
		switch (_feedAction) {
		case Feed.FEED_ADD:
			_interval.setText("30");
			break;
		case Feed.FEED_EDIT:
			_feedId = intent.getExtras().getInt(Feed.FEED_ID);
			Feed feed = getRSSService().getFeeds().get(_feedId);
			_url.setText(feed.getLink().toString());
			_name.setText(feed.getName());
			_interval.setText(String.valueOf(feed.getInterval()));
			_isValidFeed = true;
			break;
		}

		_url.addTextChangedListener(new TextWatcher() {
			@Override
			public void onTextChanged(CharSequence s, int start, int before, int count) {
				String url = s.toString();
				if (url != null && URLUtil.isValidUrl(url)) {
					Feed feed = new Feed(Uri.parse(url));
					feed.doParse();
					_isValidFeed = feed.isParsed() && (feed.getItems().size() > 0);
					if (feed.getName() != null)
						_name.setText(feed.getName());
				}

			}

			@Override
			public void beforeTextChanged(CharSequence s, int start, int count, int after) {
			}

			@Override
			public void afterTextChanged(Editable s) {
			}
		});
	}

}
