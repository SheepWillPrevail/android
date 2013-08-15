package com.grazz.pebblerss.provider;

import java.util.List;

import android.content.Context;
import android.net.Uri;

public class RSSFeed extends RSSDatabaseEntity {

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

	public void persist(Context context) {
		RSSDatabase db = new RSSDatabase(context);
		db.updateFeed(this);
		db.close();
	}

	public List<RSSFeedItem> getItems(Context context) {
		RSSDatabase db = new RSSDatabase(context);
		List<RSSFeedItem> feedItems = db.readFeedItems(this);
		db.close();
		return feedItems;
	}

	public static void createFeed(Context context, RSSFeed feed) {
		RSSDatabase db = new RSSDatabase(context);
		db.createFeed(feed);
		db.close();
	}

	public static List<RSSFeed> getFeeds(Context context) {
		RSSDatabase db = new RSSDatabase(context);
		List<RSSFeed> feeds = db.readFeeds();
		db.close();
		return feeds;
	}

	public static void cleanupFeedItems(Context context, RSSFeed feed, int retentionPeriod) {
		RSSDatabase db = new RSSDatabase(context);
		db.cleanupExpired(feed, retentionPeriod);
		db.close();
	}

	public static void deleteFeed(Context context, RSSFeed feed) {
		RSSDatabase db = new RSSDatabase(context);
		db.deleteFeedItems(feed);
		db.deleteFeed(feed);
		db.close();
	}

}
