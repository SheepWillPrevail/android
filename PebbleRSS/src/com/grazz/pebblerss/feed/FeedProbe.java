package com.grazz.pebblerss.feed;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserFactory;

import android.net.Uri;

import com.axelby.riasel.Feed;
import com.axelby.riasel.FeedItem;
import com.axelby.riasel.FeedParser;
import com.axelby.riasel.FeedParser.FeedInfoHandler;
import com.axelby.riasel.FeedParser.FeedItemHandler;

public class FeedProbe implements FeedInfoHandler, FeedItemHandler {

	private Boolean _isParsed = false;
	private String _name;
	private int _itemCount = 0;

	public FeedProbe(Uri link) {
		XmlPullParserFactory factory = null;
		XmlPullParser pullparser = null;
		InputStream stream = null;
		FeedParser feedparser = null;

		try {
			factory = XmlPullParserFactory.newInstance();
			factory.setNamespaceAware(true);
			pullparser = factory.newPullParser();
			URL url = new URL(link.toString());
			stream = url.openStream();
			pullparser.setInput(stream, null);
			feedparser = new FeedParser();
			feedparser.setOnFeedInfoHandler(this);
			feedparser.setOnFeedItemHandler(this);
			feedparser.parseFeed(pullparser);
			stream.close();
			_isParsed = true;
		} catch (Exception e) {
		} finally {
			if (factory != null)
				factory = null;
			if (pullparser != null)
				pullparser = null;
			if (feedparser != null)
				feedparser = null;
			if (stream != null)
				try {
					stream.close();
				} catch (IOException e) {
				}
			System.gc();
		}
	}

	public Boolean isParsed() {
		return _isParsed;
	}

	public String getName() {
		if (!isParsed())
			return null;

		return _name;
	}

	public int getNumberOfItems() {
		if (!isParsed())
			return 0;

		return _itemCount;
	}

	@Override
	public void OnFeedInfo(FeedParser feedParser, Feed feed) {
		_name = feed.getTitle();
	}

	@Override
	public void OnFeedItem(FeedParser feedParser, FeedItem item) {
		_itemCount++;
	}
}
