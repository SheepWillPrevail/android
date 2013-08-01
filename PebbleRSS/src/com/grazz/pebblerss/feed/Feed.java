package com.grazz.pebblerss.feed;

import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import org.jsoup.Jsoup;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserFactory;

import android.net.Uri;

import com.axelby.riasel.FeedParser;
import com.axelby.riasel.FeedParser.FeedInfoHandler;
import com.axelby.riasel.FeedParser.FeedItemHandler;

public class Feed implements Runnable, FeedInfoHandler, FeedItemHandler {

	public static final int FEED_ADD = 0;
	public static final int FEED_EDIT = 1;
	public static final String FEED_ACTION = "feed_action";
	public static final String FEED_ID = "feed_id";

	private Uri _link;
	private String _name;
	private int _refreshInterval;

	private Long _nextUpdate = 0L;
	private Boolean _isParsed = false;
	private List<FeedItem> _items = new ArrayList<FeedItem>();

	public Feed(Uri link, String name, int refreshInterval) {
		_link = link;
		_name = name;
		_refreshInterval = refreshInterval;
	}

	public Feed(Uri link) {
		_link = link;
	}

	public void doParse() {
		setIsParsed(false);
		Thread parseThread = new Thread(this);
		parseThread.start();
		try {
			parseThread.join();
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}

	public int getInterval() {
		return _refreshInterval;
	}

	public String getName() {
		return _name;
	}

	public Uri getLink() {
		return _link;
	}

	public void setInterval(int interval) {
		_refreshInterval = interval;
	}

	public void setName(String name) {
		_name = name;
	}

	public void setLink(Uri link) {
		_link = link;
	}

	public Boolean isParsed() {
		return _isParsed;
	}

	public Boolean isStale() {
		return System.currentTimeMillis() > _nextUpdate;
	}

	public List<FeedItem> getItems() {
		return _items;
	}

	private synchronized void setIsParsed(Boolean isParsed) {
		_isParsed = isParsed;
		if (_isParsed)
			_nextUpdate = System.currentTimeMillis() + (60 * 1000 * _refreshInterval);
	}

	@Override
	public void run() {
		try {
			_items.clear();
			XmlPullParserFactory factory = XmlPullParserFactory.newInstance();
			factory.setNamespaceAware(true);
			XmlPullParser parser2 = factory.newPullParser();
			URL url = new URL(_link.toString());
			InputStream stream = url.openStream();
			parser2.setInput(stream, null);
			FeedParser parser = new FeedParser();
			parser.setOnFeedInfoHandler(this);
			parser.setOnFeedItemHandler(this);
			parser.parseFeed(parser2);
		} catch (Exception e) {
		}
	}

	@Override
	public void OnFeedInfo(FeedParser feedParser, com.axelby.riasel.Feed feed) {
		_name = feed.getTitle();
	}

	@Override
	public void OnFeedItem(FeedParser feedParser, com.axelby.riasel.FeedItem item) {
		_items.add(new FeedItem(item.getTitle(), Uri.parse(item.getLink()), Jsoup.parse(item.getDescription()).text()));
	}
}
