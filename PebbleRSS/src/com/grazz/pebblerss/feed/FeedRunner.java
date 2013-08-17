package com.grazz.pebblerss.feed;

import java.io.IOException;
import java.io.InputStream;
import java.util.Date;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Node;
import org.jsoup.select.NodeVisitor;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserFactory;

import android.content.Context;
import android.net.Uri;

import com.axelby.riasel.FeedItem;
import com.axelby.riasel.FeedParser;
import com.axelby.riasel.FeedParser.FeedItemHandler;
import com.grazz.pebblerss.provider.RSSDatabase;
import com.grazz.pebblerss.provider.RSSFeed;
import com.grazz.pebblerss.provider.RSSFeedItem;

public class FeedRunner implements Runnable, FeedItemHandler {

	private RSSFeed _feed;
	private RSSDatabase _database;
	private boolean _isParsed = false;

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

	public boolean isParsed() {
		return _isParsed;
	}

	private synchronized void setIsParsed(boolean isParsed) {
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
			stream = new SemiSecureHttpClient(_feed.getUri(), _feed.getUsername(), _feed.getPassword()).getInputStream();
			if (stream != null) {
				pullparser.setInput(stream, null);
				FeedParser feedparser = new FeedParser();
				feedparser.setOnFeedItemHandler(this);
				feedparser.parseFeed(pullparser);
				setIsParsed(true);
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

	@Override
	public void OnFeedItem(FeedParser feedParser, FeedItem item) {
		String uniqueId = item.getUniqueId();
		if (uniqueId == null)
			uniqueId = item.getLink();
		Date publicationDate = item.getPublicationDate();
		if (publicationDate == null)
			publicationDate = new Date();

		if (_database.wantsFeedItem(_feed, uniqueId, publicationDate)) {
			RSSFeedItem feedItem = new RSSFeedItem();
			feedItem.setUniqueId(uniqueId);
			feedItem.setPublicationDate(publicationDate);
			feedItem.setUri(Uri.parse(item.getLink()));
			feedItem.setTitle(item.getTitle());

			final StringBuilder filtered = new StringBuilder();
			Jsoup.parse(item.getDescription()).traverse(new NodeVisitor() {
				@Override
				public void head(Node node, int depth) {
					String name = node.nodeName();
					if ("#text".equalsIgnoreCase(name))
						filtered.append(Jsoup.parse(node.outerHtml()).text());
					if ("a".equalsIgnoreCase(name))
						filtered.append(" ");
				}

				@Override
				public void tail(Node node, int depth) {
					String name = node.nodeName();
					if ("br".equalsIgnoreCase(name) || "tr".equalsIgnoreCase(name) || "dd".equalsIgnoreCase(name))
						filtered.append("\n");
					if ("p".equalsIgnoreCase(name))
						filtered.append("\n\n");
					if ("a".equalsIgnoreCase(name) || "td".equalsIgnoreCase(name) | "dt".equalsIgnoreCase(name))
						filtered.append(" ");
				}
			});
			feedItem.setContent(filtered.toString());

			_database.createFeedItem(_feed, feedItem);
		}
	}

}
