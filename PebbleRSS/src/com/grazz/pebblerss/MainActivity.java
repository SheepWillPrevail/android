package com.grazz.pebblerss;

import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnLongClickListener;
import android.widget.ListView;

import com.grazz.pebblerss.feed.Feed;
import com.grazz.pebblerss.feed.FeedActivity;
import com.grazz.pebblerss.feed.FeedListAdapter;

public class MainActivity extends RSSServiceActivity {

	private ListView _lvFeeds;
	private FeedListAdapter _adapter;

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);
		_lvFeeds.setAdapter(_lvFeeds.getAdapter());
	}

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		_lvFeeds = (ListView) findViewById(R.id.lvFeeds);
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		getMenuInflater().inflate(R.menu.main, menu);
		return true;
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
}
