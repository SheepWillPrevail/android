package com.grazz.pebblerss.provider;

import java.util.List;

import android.content.Context;
import android.net.Uri;

public class RSSFeed extends RSSTableEntity {

	public static final int FEED_ADD = 0;
	public static final int FEED_EDIT = 1;
	public static final String FEED_ACTION = "feed_action";
	public static final String FEED_ID = "feed_id";

	private Uri _uri;
	private String _name;
	private int _interval;
	private long _lastUpdated;

	public Uri getUri() {
		return _uri;
	}

	public void setUri(Uri uri) {
		_uri = uri;
	}

	public String getName() {
		return _name;
	}

	public void setName(String name) {
		_name = name;
	}

	public int getInterval() {
		return _interval;
	}

	public void setInterval(int interval) {
		_interval = interval;
	}

	public long getLastUpdated() {
		return _lastUpdated;
	}

	public void setLastUpdated(long lastUpdated) {
		_lastUpdated = lastUpdated;
	}

	public Boolean isStale() {
		return System.currentTimeMillis() > getLastUpdated() + (60 * 1000 * getInterval());
	}

	public void save(Context context) {
		new RSSFeedTable(context).updateFeed(this);
	}

	public List<RSSFeedItem> getItems(Context context) {
		return new RSSFeedItemTable(context).getFeedItems(this);
	}

	public static List<RSSFeed> getFeeds(Context context) {
		return new RSSFeedTable(context).getFeeds();
	}

}
