package com.grazz.pebblerss.feed;

import java.util.Date;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Node;
import org.jsoup.select.NodeVisitor;

import android.content.Context;

import com.grazz.pebblerss.provider.RSSDatabase;
import com.grazz.pebblerss.provider.RSSFeed;
import com.grazz.pebblerss.provider.RSSFeedItem;
import com.grazz.pebblerss.rss.FeedItem;
import com.grazz.pebblerss.rss.FeedParser;
import com.grazz.pebblerss.rss.FeedParserFactory;

public class FeedRunner implements Runnable {

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
		FeedParser parser = FeedParserFactory.getParser(_feed.getUri());
		if (parser != null) {
			for (FeedItem item : parser.getFeed().getItems())
				readFeedItem(item);
			setIsParsed(true);
		}
	}

	public void readFeedItem(FeedItem item) {
		String id = item.getId();
		if (id == null)
			id = item.getLink().toString();
		Date publicationDate = item.getPublicationDate();
		if (publicationDate == null)
			publicationDate = new Date();
		Date updatedDate = item.getUpdatedDate();
		if (updatedDate != null && updatedDate.after(publicationDate))
			publicationDate = updatedDate;

		if (_database.wantsFeedItem(_feed, id, publicationDate)) {
			RSSFeedItem feedItem = new RSSFeedItem();
			feedItem.setUniqueId(id);
			feedItem.setPublicationDate(publicationDate);
			feedItem.setUri(item.getLink());
			feedItem.setTitle(item.getTitle());

			final StringBuilder filtered = new StringBuilder();
			Jsoup.parse(item.getContent()).traverse(new NodeVisitor() {
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
