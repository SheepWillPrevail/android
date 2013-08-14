package com.grazz.pebblerss.feed;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;

import org.jsoup.Jsoup;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserFactory;

import android.content.Context;
import android.net.Uri;

import com.axelby.riasel.FeedItem;
import com.axelby.riasel.FeedParser;
import com.axelby.riasel.FeedParser.FeedItemHandler;
import com.grazz.pebblerss.provider.RSSFeed;
import com.grazz.pebblerss.provider.RSSFeedItem;
import com.grazz.pebblerss.provider.RSSDatabase;

public class FeedRunner implements Runnable, FeedItemHandler {

	private Boolean _isParsed = false;
	private RSSFeed _feed;
	private RSSDatabase _database;

	public FeedRunner(Context context, RSSFeed feed) {
		_feed = feed;
		_database = new RSSDatabase(context);
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
		InputStream stream = null;
		try {
			XmlPullParserFactory factory = XmlPullParserFactory.newInstance();
			factory.setNamespaceAware(true);
			XmlPullParser pullparser = factory.newPullParser();
			stream = new URL(_feed.getUri().toString()).openStream();
			pullparser.setInput(stream, null);
			FeedParser feedparser = new FeedParser();
			feedparser.setOnFeedItemHandler(this);
			feedparser.parseFeed(pullparser);
			setIsParsed(true);
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

	@Override
	public void OnFeedItem(FeedParser feedParser, FeedItem item) {
		String uniqueId = item.getUniqueId();
		if (uniqueId == null)
			uniqueId = item.getLink();
		if (_database.wantsFeedItem(_feed, uniqueId, item.getPublicationDate())) {
			RSSFeedItem feedItem = new RSSFeedItem();
			feedItem.setUniqueId(uniqueId);
			feedItem.setPublicationDate(item.getPublicationDate());
			feedItem.setUri(Uri.parse(item.getLink()));
			feedItem.setTitle(item.getTitle());
			feedItem.setContent(Jsoup.parse(item.getDescription()).text());
			_database.createFeedItem(_feed, feedItem);
		}
	}
}
