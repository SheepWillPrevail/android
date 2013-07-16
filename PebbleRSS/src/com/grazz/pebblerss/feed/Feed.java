package com.grazz.pebblerss.feed;

import java.util.ArrayList;
import java.util.List;

import org.mcsoxford.rss.RSSFeed;
import org.mcsoxford.rss.RSSItem;
import org.mcsoxford.rss.RSSReader;
import org.mcsoxford.rss.RSSReaderException;

import android.net.Uri;

public class Feed implements Runnable {

	public static final int FEED_ADD = 0;
	public static final int FEED_EDIT = 1;
	public static final String FEED_ACTION = "feed_action";
	public static final String FEED_ID = "feed_id";

	private Thread _parseThread;
	private Boolean _isParsed = false;
	private Uri _link;
	private String _name;
	private List<FeedItem> _items = new ArrayList<FeedItem>();

	public Feed(Uri link, String name) {
		_link = link;
		_name = name;
	}

	public Feed(Uri link) {
		_link = link;
	}

	public void doParse() {
		_parseThread = new Thread(this);
		_parseThread.start();
		try {
			_parseThread.join();
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}

	public String getName() {
		return _name;
	}

	public Uri getLink() {
		return _link;
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

	public List<FeedItem> getItems() {
		return _items;
	}

	private synchronized void setIsParsed(Boolean value) {
		_isParsed = true;
	}

	@Override
	public void run() {
		RSSReader reader = new RSSReader();
		try {
			RSSFeed feed = reader.load(_link.toString());

			if (_name == null)
				_name = feed.getTitle();

			_items.clear();
			for (RSSItem item : feed.getItems())
				_items.add(new FeedItem(item.getTitle(), item.getLink(), item.getDescription()));

			setIsParsed(true);
		} catch (RSSReaderException e) {
			e.printStackTrace();
		} finally {
			reader.close();
		}
	}
}
