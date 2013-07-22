package com.grazz.pebblerss.feed;

import java.io.File;
import java.util.ArrayList;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.Result;
import javax.xml.transform.Source;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import android.content.Context;
import android.net.Uri;

import com.grazz.pebblerss.CanvasRSSPlugin;
import com.pennas.pebblecanvas.plugin.PebbleCanvasPlugin;

public class FeedManager {

	private static final String FEED_CONFIG_XML = "feed_config.xml";

	private ArrayList<Feed> _feeds = new ArrayList<Feed>();

	public Feed getFeed(int id) {
		return _feeds.get(id);
	}

	public ArrayList<Feed> getFeeds() {
		return _feeds;
	}

	public Feed addFeed(Uri link) {
		Feed feed = new Feed(link);
		_feeds.add(feed);
		return feed;
	}

	public void removeFeed(int id) {
		_feeds.remove(id);
	}

	public Boolean hasStaleFeeds(Boolean parseIfStale) {
		Boolean wasStale = false;
		for (Feed feed : _feeds)
			if (feed.isStale()) {
				if (parseIfStale)
					feed.doParse();
				wasStale = true;
			}
		return wasStale;
	}

	public void readConfig(Context context) {
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
					_feeds.add(new Feed(Uri.parse(link), name, Integer.valueOf(interval)));
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}

	public void writeConfig(Context context) {
		File feedFile = new File(context.getFilesDir(), FEED_CONFIG_XML);
		if (feedFile.exists())
			feedFile.delete();
		DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
		try {
			DocumentBuilder builder = factory.newDocumentBuilder();
			Document document = builder.newDocument();
			Element documentElement = document.createElement("feeds");
			for (Feed feed : _feeds) {
				Element feedNode = document.createElement("feed");
				feedNode.setAttribute("link", feed.getLink().toString());
				feedNode.setAttribute("name", feed.getName());
				feedNode.setAttribute("interval", String.valueOf(feed.getInterval()));
				documentElement.appendChild(feedNode);
			}
			document.appendChild(documentElement);
			Transformer transformer = TransformerFactory.newInstance().newTransformer();
			Result output = new StreamResult(feedFile);
			Source input = new DOMSource(document);
			transformer.transform(input, output);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public void writeFeedsAndNotifyCanvas(Context context) {
		FeedSerializer.serialize(context, this);
		PebbleCanvasPlugin.notify_canvas_updates_available(CanvasRSSPlugin.ID_HEADLINES, context);
	}
}
