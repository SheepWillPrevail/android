package com.grazz.pebblerss.feed;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;

import org.jsoup.Jsoup;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserFactory;

import android.content.Context;
import android.net.Uri;

import com.axelby.riasel.FeedParser;
import com.axelby.riasel.FeedParser.FeedItemHandler;
import com.grazz.pebblerss.provider.RSSFeed;
import com.grazz.pebblerss.provider.RSSFeedItem;
import com.grazz.pebblerss.provider.RSSFeedItemTable;

public class FeedRunner implements Runnable, FeedItemHandler {

	private RSSFeed _feed;
	private Boolean _isParsed = false;
	private RSSFeedItemTable _itemDB;

	public FeedRunner(Context context, RSSFeed feed) {
		_feed = feed;
		_itemDB = new RSSFeedItemTable(context);
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

	public Boolean isParsed() {
		return _isParsed;
	}

	private synchronized void setIsParsed(Boolean isParsed) {
		_isParsed = isParsed;
		if (_isParsed)
			_feed.setLastUpdated(System.currentTimeMillis());
	}

	@Override
	public void run() {
		XmlPullParserFactory factory = null;
		XmlPullParser pullparser = null;
		InputStream stream = null;
		FeedParser feedparser = null;

		try {
			factory = XmlPullParserFactory.newInstance();
			factory.setNamespaceAware(true);
			pullparser = factory.newPullParser();
			URL url = new URL(_feed.getUri().toString());
			stream = url.openStream();
			pullparser.setInput(stream, null);
			feedparser = new FeedParser();
			feedparser.setOnFeedItemHandler(this);
			feedparser.parseFeed(pullparser);
			stream.close();
			setIsParsed(true);
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
		}
	}

	@Override
	public void OnFeedItem(FeedParser feedParser, com.axelby.riasel.FeedItem item) {
		if (!_itemDB.hasFeedItem(_feed, item.getUniqueId())) {
			RSSFeedItem feedItem = new RSSFeedItem();
			feedItem.setUniqueId(item.getUniqueId());
			feedItem.setPublicationDate(item.getPublicationDate());
			feedItem.setUri(Uri.parse(item.getLink()));
			feedItem.setTitle(item.getTitle());
			feedItem.setContent(Jsoup.parse(item.getDescription()).text());
			_itemDB.addFeedItem(_feed, feedItem);
		}
	}

}
