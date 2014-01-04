package com.grazz.pebblerss.feed;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Node;
import org.jsoup.nodes.TextNode;
import org.jsoup.select.NodeVisitor;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserFactory;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Bitmap.CompressFormat;
import android.net.Uri;
import android.util.Base64;
import android.webkit.URLUtil;

import com.axelby.riasel.FeedItem;
import com.axelby.riasel.FeedParser;
import com.axelby.riasel.FeedParser.FeedItemHandler;
import com.grazz.pebblerss.kits.PebbleImageKit;
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
			stream = new SemiSecureHttpClient(_feed.getUri(), _feed.getUsername(), _feed.getPassword()).getInputStream();
			if (stream != null) {
				XmlPullParserFactory factory = XmlPullParserFactory.newInstance();
				factory.setNamespaceAware(true);
				XmlPullParser pullparser = factory.newPullParser();
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

			final RSSFeedItem feedItem = new RSSFeedItem();
			feedItem.setUniqueId(uniqueId);
			feedItem.setPublicationDate(publicationDate);
			feedItem.setUri(uri);
			feedItem.setTitle(item.getTitle());

			final List<String> images = new ArrayList<String>();
			final StringBuilder filtered = new StringBuilder();

			String content = item.getContent();
			if (content == null)
				content = item.getDescription();

			Jsoup.parseBodyFragment(content).traverse(new NodeVisitor() {
				@Override
				public void head(Node node, int depth) {
					String name = node.nodeName();
					if ("#text".equalsIgnoreCase(name)) {
						String text = ((TextNode) node).getWholeText();
						String parent = node.parent().nodeName();
						boolean isEmptyEntityText = ("body".equalsIgnoreCase(parent) || "td".equalsIgnoreCase(parent) || "tr".equalsIgnoreCase(parent) || "table"
								.equalsIgnoreCase(parent)) && text.trim().length() == 0;
						if (!isEmptyEntityText)
							filtered.append(text.replace("\n", ""));
					}
					if ("img".equalsIgnoreCase(name))
						images.add(node.attr("src"));
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

			String thumbnail = item.getThumbnailURL();
			if (thumbnail == null && images.size() > 0)
				thumbnail = images.get(0);

			final String finalThumbnail = thumbnail;
			if (_feed.shouldDownloadImages() && finalThumbnail != null) {
				Thread fetch = new Thread(new Runnable() {
					@Override
					public void run() {
						try {
							URL url = new URL(finalThumbnail);
							InputStream input = url.openStream();
							Bitmap bitmap = BitmapFactory.decodeStream(input);
							input.close();
							if (bitmap != null) {
								Bitmap conformed = PebbleImageKit.conformImageToPebble(bitmap);
								bitmap = null;
								ByteArrayOutputStream output = new ByteArrayOutputStream();
								conformed.compress(CompressFormat.PNG, 0, output);
								conformed = null;
								feedItem.setThumbnail(Base64.encodeToString(output.toByteArray(), Base64.DEFAULT));
								output.close();
							}
						} catch (Exception e) {
							e.printStackTrace();
						}
					}
				});
				fetch.start();
				try {
					fetch.join();
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}

			_database.createFeedItem(_feed, feedItem);
		}
	}
}
