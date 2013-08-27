package com.grazz.pebblerss.feed;

import java.io.IOException;
import java.io.InputStream;
import java.util.Date;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Node;
import org.jsoup.nodes.TextNode;
import org.jsoup.select.NodeVisitor;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserFactory;

import android.content.Context;
import android.net.Uri;
import android.webkit.URLUtil;

import com.axelby.riasel.FeedItem;
import com.axelby.riasel.FeedParser;
import com.axelby.riasel.FeedParser.FeedItemHandler;
import com.grazz.pebblerss.provider.RSSDatabase;
import com.grazz.pebblerss.provider.RSSFeed;
import com.grazz.pebblerss.provider.RSSFeedItem;

public class FeedRunner implements Runnable, FeedItemHandler {

	private RSSFeed _feed;
	private RSSDatabase _database;
	private boolean _isParsed;

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
		_database.close();
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
			Uri uri = _feed.getUri();
			if (URLUtil.isValidUrl(item.getLink()))
				uri = Uri.parse(item.getLink());

			RSSFeedItem feedItem = new RSSFeedItem();
			feedItem.setUniqueId(uniqueId);
			feedItem.setPublicationDate(publicationDate);
			feedItem.setUri(uri);
			feedItem.setTitle(item.getTitle());

			final StringBuilder filtered = new StringBuilder();
			Jsoup.parseBodyFragment(item.getDescription()).traverse(new NodeVisitor() {
				@Override
				public void head(Node node, int depth) {
					if ("#text".equalsIgnoreCase(node.nodeName())) {
						String text = ((TextNode) node).getWholeText();
						String name = node.parent().nodeName();
						boolean isEmptyEntityText = ("body".equalsIgnoreCase(name) || "td".equalsIgnoreCase(name) || "tr".equalsIgnoreCase(name) || "table"
								.equalsIgnoreCase(name)) && text.trim().length() == 0;
						if (!isEmptyEntityText)
							filtered.append(text.replace("\n", ""));
					}
				}

				@Override
				public void tail(Node node, int depth) {
					String name = node.nodeName();
					if ("br".equalsIgnoreCase(name) || "tr".equalsIgnoreCase(name) || "dd".equalsIgnoreCase(name) || "dt".equalsIgnoreCase(name))
						filtered.append("\n");
					else if ("p".equalsIgnoreCase(name))
						filtered.append("\n\n");
				}
			});
			feedItem.setContent(filtered.toString());

			_database.createFeedItem(_feed, feedItem);
		}
	}
}
