package com.grazz.pebblerss.feed;

import java.io.File;
import java.util.List;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import android.content.Context;
import android.content.SharedPreferences;
import android.net.Uri;

import com.grazz.pebblerss.CanvasRSSPlugin;
import com.grazz.pebblerss.R;
import com.grazz.pebblerss.StaticValues;
import com.grazz.pebblerss.provider.RSSFeed;
import com.grazz.pebblerss.provider.RSSFeedItemTable;
import com.grazz.pebblerss.provider.RSSFeedTable;
import com.pennas.pebblecanvas.plugin.PebbleCanvasPlugin;

public class FeedManager {

	private Context _context;
	private Boolean _isRefreshingFeeds = false;

	public FeedManager(Context context) {
		_context = context;
	}

	public List<RSSFeed> getFeeds() {
		return RSSFeed.getFeeds(_context);
	}

	public RSSFeed getFeed(int index) {
		return getFeeds().get(index);
	}

	public RSSFeed getFeedById(long id) {
		for (RSSFeed feed : getFeeds())
			if (feed.getId() == id)
				return feed;
		return null;
	}

	public RSSFeed addFeed(Uri uri, String name, int interval) {
		RSSFeed feed = new RSSFeed();
		feed.setUri(uri);
		feed.setName(name);
		feed.setInterval(interval);

		RSSFeedTable db = new RSSFeedTable(_context);
		db.addFeed(feed);
		db.close();

		notifyCanvas(_context);

		return feed;
	}

	public void removeFeed(RSSFeed feed) {
		new RSSFeedTable(_context).removeFeed(feed);
		new RSSFeedItemTable(_context).deleteFeedItems(feed);
		notifyCanvas(_context);
	}

	public Boolean checkFeeds(Boolean parseIfStale) {
		RSSFeedItemTable itemTable = new RSSFeedItemTable(_context);
		SharedPreferences pref = _context.getSharedPreferences(StaticValues.PREFERENCES_KEY, Context.MODE_PRIVATE);
		int retentionPeriod = Integer.valueOf(pref.getString(_context.getResources().getString(R.string.setting_retention), "24"));

		Boolean doRefresh = false;
		synchronized (this) {
			if (!_isRefreshingFeeds) {
				_isRefreshingFeeds = true;
				doRefresh = true;
			}
		}

		Boolean wasStale = false;
		for (RSSFeed feed : getFeeds()) {
			if (feed.isStale()) {
				if (doRefresh && parseIfStale) {
					new FeedRunner(_context, feed).doParse();
					feed.save(_context);
					wasStale = true;
				}
			}
			itemTable.cleanupExpired(feed, retentionPeriod);
		}

		if (doRefresh)
			_isRefreshingFeeds = false;

		return wasStale;
	}

	public void convertOldConfig(Context context) {
		String FEED_CONFIG_XML = "feed_config.xml";
		File feedFile = new File(context.getFilesDir(), FEED_CONFIG_XML);
		if (feedFile.exists()) {
			DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
			try {
				DocumentBuilder builder = factory.newDocumentBuilder();
				Document document = builder.parse(feedFile);
				Element documentElement = document.getDocumentElement();
				NodeList nodeList = documentElement.getElementsByTagName("feed");
				for (int i = 0; i < nodeList.getLength(); i++) {
					Node node = nodeList.item(i);
					String link = node.getAttributes().getNamedItem("link").getNodeValue();
					String name = node.getAttributes().getNamedItem("name").getNodeValue();
					String interval = "30";
					Node intervalNode = node.getAttributes().getNamedItem("interval");
					if (intervalNode != null)
						interval = intervalNode.getNodeValue();
					addFeed(Uri.parse(link), name, Integer.valueOf(interval));
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
			feedFile.delete();
		}
	}

	public void notifyCanvas(Context context) {
		PebbleCanvasPlugin.notify_canvas_updates_available(CanvasRSSPlugin.ID_RSSITEM, context);
	}

}
