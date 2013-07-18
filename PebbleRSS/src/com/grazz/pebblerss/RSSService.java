package com.grazz.pebblerss;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

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

import android.app.Service;
import android.content.Intent;
import android.net.Uri;
import android.os.Binder;
import android.os.IBinder;

import com.getpebble.android.kit.PebbleKit.PebbleDataReceiver;
import com.grazz.pebblerss.feed.Feed;

public class RSSService extends Service {

	private static final String FEED_CONFIG_XML = "feed_config.xml";

	public class RSSServiceBinder extends Binder {
		RSSService getService() {
			return RSSService.this;
		}
	}

	private RSSServiceBinder _binder = new RSSServiceBinder();
	private PebbleDataReceiver _receiver;
	private List<Feed> _feeds = new ArrayList<Feed>();

	public RSSService() {
		_receiver = new RSSDataReceiver(this);
	}

	@Override
	public IBinder onBind(Intent intent) {
		return _binder;
	}

	@Override
	public void onCreate() {
		super.onCreate();
		readConfig();
	}

	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		if (intent != null && intent.hasExtra(PassiveRSSDataReceiver.DICTIONARY))
			_receiver.onReceive(this, (Intent) intent.getExtras().get(PassiveRSSDataReceiver.DICTIONARY));
		return super.onStartCommand(intent, flags, startId);
	}

	public List<Feed> getFeeds() {
		return _feeds;
	}

	public Feed addFeed(Uri link) {
		Feed feed = new Feed(link);
		_feeds.add(feed);
		return feed;
	}

	public void removeFeed(int position) {
		_feeds.remove(position);
	}

	private void readConfig() {
		File feedFile = new File(getFilesDir(), FEED_CONFIG_XML);
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

					Feed feed = new Feed(Uri.parse(link), name, Integer.valueOf(interval));
					feed.doParse();
					_feeds.add(feed);
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}

	public void writeConfig() {
		File feedFile = new File(getFilesDir(), FEED_CONFIG_XML);
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
}
