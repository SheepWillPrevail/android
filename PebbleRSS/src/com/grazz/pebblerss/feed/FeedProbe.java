package com.grazz.pebblerss.feed;

import java.io.IOException;
import java.io.InputStream;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserFactory;

import android.net.Uri;

import com.axelby.riasel.Feed;
import com.axelby.riasel.FeedItem;
import com.axelby.riasel.FeedParser;
import com.axelby.riasel.FeedParser.FeedInfoHandler;
import com.axelby.riasel.FeedParser.FeedItemHandler;

public class FeedProbe implements FeedInfoHandler, FeedItemHandler {

	private boolean _isParsed;
	private String _name;
	private int _itemCount = 0;

	public FeedProbe(Uri link, String username, String password) {
		InputStream stream = null;
		try {
			XmlPullParserFactory factory = XmlPullParserFactory.newInstance();
			factory.setNamespaceAware(true);
			XmlPullParser pullparser = factory.newPullParser();
			stream = new SemiSecureHttpClient(link, username, password).getInputStream();
			if (stream != null) {
				pullparser.setInput(stream, null);
				FeedParser feedparser = new FeedParser();
				feedparser.setOnFeedInfoHandler(this);
				feedparser.setOnFeedItemHandler(this);
				feedparser.parseFeed(pullparser);
				_isParsed = true;
			}
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			if (stream != null)
				try {
					stream.close();
				} catch (IOException e) {
				}
		}
	}

	public boolean isParsed() {
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
