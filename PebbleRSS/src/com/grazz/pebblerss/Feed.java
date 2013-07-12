package com.grazz.pebblerss;

import java.util.ArrayList;
import java.util.List;

import org.mcsoxford.rss.RSSFeed;
import org.mcsoxford.rss.RSSItem;
import org.mcsoxford.rss.RSSReader;
import org.mcsoxford.rss.RSSReaderException;

import android.net.Uri;

public class Feed implements Runnable {

	private Thread _parseThread;
	private Boolean _isParsed = false;
	private Uri _link;
	private String _name;
	private List<FeedItem> _items = new ArrayList<FeedItem>();

	public Feed(Uri link) {
		_link = link;
		_parseThread = new Thread(this);
		_parseThread.start();
	}

	public String getName() {
		return _name;
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
