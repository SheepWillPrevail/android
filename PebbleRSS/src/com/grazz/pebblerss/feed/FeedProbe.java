package com.grazz.pebblerss.feed;

import org.mcsoxford.rss.RSSFeed;
import org.mcsoxford.rss.RSSReader;

import android.net.Uri;

public class FeedProbe {

	private Boolean _isParsed = false;
	private RSSFeed _feed;

	public FeedProbe(Uri link) {
		RSSReader reader = new RSSReader();
		try {
			_feed = reader.load(link.toString());
			_isParsed = true;
		} catch (Exception e) {
		} finally {
			reader.close();
		}
	}

	public Boolean isParsed() {
		return _isParsed;
	}

	public String getName() {
		if (!isParsed())
			return null;

		return _feed.getTitle();
	}

	public int getNumberOfItems() {
		if (!isParsed())
			return 0;

		return _feed.getItems().size();
	}
}
