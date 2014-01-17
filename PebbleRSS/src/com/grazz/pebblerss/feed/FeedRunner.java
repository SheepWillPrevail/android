package com.grazz.pebblerss.feed;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.regex.Pattern;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Node;
import org.jsoup.nodes.TextNode;
import org.jsoup.select.NodeVisitor;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Bitmap.CompressFormat;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.util.Base64;
import android.webkit.URLUtil;

import com.grazz.pebblerss.feed.parser.AbstractParser;
import com.grazz.pebblerss.feed.parser.ParsedFeed;
import com.grazz.pebblerss.feed.parser.ParsedItem;
import com.grazz.pebblerss.kits.PebbleImageKit;
import com.grazz.pebblerss.provider.RSSDatabase;
import com.grazz.pebblerss.provider.RSSFeed;
import com.grazz.pebblerss.provider.RSSFeedItem;

public class FeedRunner implements Runnable {

	private RSSFeed _feed;
	private RSSDatabase _database;
	private boolean _isParsed;

	private final static Pattern ENTITIES = Pattern.compile("(body|div|p|table|td)", Pattern.CASE_INSENSITIVE);
	private final static Pattern PADDED = Pattern.compile("(td|dd|dt)", Pattern.CASE_INSENSITIVE);
	private final static Pattern BREAKS = Pattern.compile("(br|tr|dd|dt|h[1-6])", Pattern.CASE_INSENSITIVE);

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
			stream = new ExtendedHttpClient(_feed.getUri(), _feed.getUsername(), _feed.getPassword()).getInputStream();
			if (stream != null) {
				AbstractParser parser = AbstractParser.findParser(stream);
				if (parser != null) {
					ParsedFeed feed = parser.getFeed();
					stream.close();
					if (feed != null) {
						for (ParsedItem item : feed.getItems())
							processItem(item);
						setIsParsed(true);
					}
				}
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

	private void processItem(ParsedItem item) {
		String uniqueId = item.getId();
		if (uniqueId == null)
			uniqueId = item.getLink().toString();

		Date publicationDate = item.getPublicationDate();
		if (publicationDate == null)
			publicationDate = new Date();

		if (_database.wantsFeedItem(_feed, uniqueId, publicationDate)) {
			Uri uri = _feed.getUri();
			if (item.getLink() != null)
				uri = item.getLink();

			final RSSFeedItem feedItem = new RSSFeedItem();
			feedItem.setUniqueId(uniqueId);
			feedItem.setPublicationDate(publicationDate);
			feedItem.setUri(uri);
			feedItem.setTitle(item.getTitle());

			final StringBuilder filtered = new StringBuilder();
			final List<String> images = new ArrayList<String>();

			Jsoup.parseBodyFragment(item.getContent()).traverse(new NodeVisitor() {
				@Override
				public void head(Node node, int depth) {
					String name = node.nodeName();
					if ("#text".equalsIgnoreCase(name)) {
						String text = ((TextNode) node).getWholeText();
						String parent = node.parent().nodeName();
						boolean isEmptyEntityText = ENTITIES.matcher(parent).matches() && text.trim().length() == 0;
						if (!isEmptyEntityText)
							filtered.append(text.replace("\n", ""));
					} else if ("img".equalsIgnoreCase(name) && !("1".equals(node.attr("width")) && "1".equals(node.attr("height"))))
						images.add(node.attr("src"));
				}

				@Override
				public void tail(Node node, int depth) {
					String name = node.nodeName();
					if (BREAKS.matcher(name).matches())
						filtered.append("\n");
					else if (PADDED.matcher(name).matches())
						filtered.append(" ");
					else if ("p".equalsIgnoreCase(name))
						filtered.append("\n\n");
				}
			});

			feedItem.setContent(filtered.toString());

			String thumbnail = item.getThumbnailLink() == null ? null : item.getThumbnailLink().toString();
			if (thumbnail == null && images.size() > 0)
				for (int i = 0; i < images.size(); i++) {
					String url = images.get(i);
					if (URLUtil.isValidUrl(url)) {
						thumbnail = url;
						break;
					}
				}

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
						}
					}
				});
				fetch.start();
				try {
					fetch.join();
				} catch (InterruptedException e) {
				}
			}

			_database.createFeedItem(_feed, feedItem);
		}
	}
}
