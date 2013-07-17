package com.grazz.pebblerss.feed;

import java.util.ArrayList;
import java.util.List;

import org.jsoup.Jsoup;
import org.mcsoxford.rss.RSSFeed;
import org.mcsoxford.rss.RSSItem;
import org.mcsoxford.rss.RSSReader;

import android.net.Uri;

public class Feed implements Runnable {

	public static final int FEED_ADD = 0;
	public static final int FEED_EDIT = 1;
	public static final String FEED_ACTION = "feed_action";
	public static final String FEED_ID = "feed_id";

	private Uri _link;
	private String _name;
	private int _refreshInterval;

	private Thread _parseThread;
	private List<FeedItem> _items = new ArrayList<FeedItem>();
	private Boolean _isParsed = false;
	private Long _nextUpdate = 0L;

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
		_parseThread = new Thread(this);
		_parseThread.start();
		try {
			_parseThread.join();
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

	public Long getNextUpdateTime() {
		return _nextUpdate;
	}

	public Boolean isParsed() {
		return _isParsed;
	}

	public List<FeedItem> getItems() {
		return _items;
	}

	private synchronized void setIsParsed(Boolean isParsed) {
		_isParsed = isParsed;
		if (_isParsed)
			_nextUpdate = System.currentTimeMillis() + _refreshInterval;
	}

	@Override
	public void run() {
		RSSReader reader = new RSSReader();
		try {
			RSSFeed feed = reader.load(_link.toString());
			if (_name == null)
				_name = feed.getTitle();
			_items.clear();
			for (RSSItem item : feed.getItems()) {
				String description = item.getDescription();
				_items.add(new FeedItem(item.getTitle(), item.getLink(), Jsoup.parse(description).text()));
			}
			setIsParsed(true);
		} catch (Exception e) {
		} finally {
			reader.close();
		}
	}

}
