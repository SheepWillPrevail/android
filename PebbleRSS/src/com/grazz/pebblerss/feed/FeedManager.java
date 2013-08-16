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
import android.net.Uri;

import com.grazz.pebblerss.CanvasRSSPlugin;
import com.grazz.pebblerss.RSSService;
import com.grazz.pebblerss.provider.RSSFeed;
import com.pennas.pebblecanvas.plugin.PebbleCanvasPlugin;

public class FeedManager {

	private RSSService _service;
	private boolean _isRefreshingFeeds = false;

	public FeedManager(RSSService service) {
		_service = service;
	}

	public List<RSSFeed> getFeeds() {
		return RSSFeed.getFeeds(_service);
	}

	public RSSFeed getFeedAt(int position) {
		return getFeeds().get(position);
	}

	public RSSFeed getFeedById(long id) {
		for (RSSFeed feed : getFeeds())
			if (feed.getId() == id)
				return feed;
		return null;
	}

	public RSSFeed addFeed(Uri uri, String name, int interval, int retention) {
		RSSFeed feed = new RSSFeed();
		feed.setUri(uri);
		feed.setName(name);
		feed.setInterval(interval);
		feed.setRetention(retention);

		RSSFeed.createFeed(_service, feed);
		notifyCanvas(_service);

		return feed;
	}

	public void removeFeed(RSSFeed feed) {
		RSSFeed.deleteFeed(_service, feed);
		notifyCanvas(_service);
	}

	public boolean checkFeeds(boolean parseIfStale) {
		boolean doRefresh = false;
		synchronized (this) {
			if (!_isRefreshingFeeds) {
				_isRefreshingFeeds = true;
				doRefresh = true;
			}
		}

		int count = 0;
		boolean wasStale = false;
		for (RSSFeed feed : getFeeds()) {
			if (feed.isStale()) {
				if (doRefresh && parseIfStale) {
					if (count++ == 0)
						_service.sendIsParsingPacket();
					new FeedRunner(_service, feed).doParse();
					feed.persist(_service);
					wasStale = true;
				}
			}
			RSSFeed.cleanupFeedItems(_service, feed, feed.getRetention());
		}

		if (doRefresh)
			_isRefreshingFeeds = false;

		return wasStale;
	}

	public void convertOldConfig(Context context) {
		File feedFile = new File(context.getFilesDir(), "feed_config.xml");
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
					addFeed(Uri.parse(link), name, Integer.valueOf(interval), 24);
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
