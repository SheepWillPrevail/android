package com.grazz.pebblerss.feed;

import android.annotation.TargetApi;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.app.NavUtils;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.Menu;
import android.view.MenuItem;
import android.webkit.URLUtil;
import android.widget.TextView;

import com.grazz.pebblerss.R;
import com.grazz.pebblerss.RSSServiceActivity;

public class FeedActivity extends RSSServiceActivity {

	private TextView _url;
	private TextView _name;
	private int _feedAction;
	private int _feedId;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_feed);
		setupActionBar();

		_url = (TextView) findViewById(R.id.etURL);
		_name = (TextView) findViewById(R.id.etFeedName);

		_url.addTextChangedListener(new TextWatcher() {
			@Override
			public void onTextChanged(CharSequence s, int start, int before, int count) {
				String url = s.toString();
				if (URLUtil.isValidUrl(url)) {
					Feed feed = new Feed(Uri.parse(url));
					feed.doParse();
					if (feed.getName() != null) {
						_name.setText(feed.getName());
					}
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
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.feed, menu);
		return true;
	}

	@Override
	public boolean onMenuItemSelected(int featureId, MenuItem item) {
		switch (item.getItemId()) {
		case R.id.action_save:
			if (!URLUtil.isValidUrl(_url.getText().toString()))
				return false;
			if (_feedAction == Feed.FEED_ADD) {
				Feed feed = getRSSService().addFeed(Uri.parse(_url.getText().toString()));
				feed.setName(_name.getText().toString());
			} else {
				Feed feed = getRSSService().getFeeds().get(_feedId);
				feed.setName(_name.getText().toString());
				feed.setLink(Uri.parse(_url.getText().toString()));
			}
			finish();
			return true;
		case R.id.action_delete:
			if (_feedAction == Feed.FEED_EDIT)
				getRSSService().removeFeed(_feedId);
			finish();
			return true;
		}
		return super.onMenuItemSelected(featureId, item);
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case android.R.id.home:
			NavUtils.navigateUpFromSameTask(this);
			return true;
		}
		return super.onOptionsItemSelected(item);
	}

	@Override
	protected void onBindToService() {
		Intent intent = getIntent();
		_feedAction = intent.getExtras().getInt(Feed.FEED_ACTION);
		switch (_feedAction) {
		case Feed.FEED_EDIT:
			_feedId = intent.getExtras().getInt(Feed.FEED_ID);
			Feed feed = getRSSService().getFeeds().get(_feedId);
			_url.setText(feed.getLink().toString());
			_name.setText(feed.getName());
			break;
		}
	}

}
