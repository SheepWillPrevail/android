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

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.Uri;
import android.os.Binder;
import android.os.IBinder;

import com.getpebble.android.kit.PebbleKit;
import com.getpebble.android.kit.PebbleKit.PebbleDataReceiver;
import com.grazz.pebblerss.feed.Feed;

public class RSSService extends Service {

	private static final String FEED_CONFIG_XML = "feed_config.xml";

	public class RSSServiceBinder extends Binder {
		RSSService getService() {
			return RSSService.this;
		}
	}

	public class AlarmReceiver extends BroadcastReceiver {
		@Override
		public void onReceive(Context context, Intent intent) {
			for (Feed feed : _feeds)
				feed.doParse();
		}
	}

	private RSSServiceBinder _binder = new RSSServiceBinder();
	private AlarmReceiver _alarmReceiver = new AlarmReceiver();
	private String _alarmAction = AlarmReceiver.class.getName();
	private PendingIntent _alarmIntent;
	private AlarmManager _alarmManager;
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

		registerReceiver(_alarmReceiver, new IntentFilter(_alarmAction));

		_alarmIntent = PendingIntent.getBroadcast(this, 0, new Intent(_alarmAction), PendingIntent.FLAG_UPDATE_CURRENT);
		_alarmManager = (AlarmManager) getSystemService(ALARM_SERVICE);
		_alarmManager.setInexactRepeating(AlarmManager.RTC_WAKEUP, System.currentTimeMillis(), AlarmManager.INTERVAL_HALF_HOUR, _alarmIntent);

		PebbleKit.registerReceivedDataHandler(this, _receiver);

		// NotificationCompat.Builder builder = new
		// NotificationCompat.Builder(this);
		// builder.setContentTitle(getResources().getString(R.string.app_name));
		// builder.setContentText(getResources().getString(R.string.msg_service_running));
		// builder.setSmallIcon(R.drawable.icon);
		// startForeground(1, builder.build());
	}

	@Override
	public void onDestroy() {
		super.onDestroy();

		writeConfig();

		unregisterReceiver(_alarmReceiver);
		_alarmManager.cancel(_alarmIntent);
		unregisterReceiver(_receiver);

		// stopForeground(true);
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
					Feed feed = new Feed(Uri.parse(link), name);
					feed.doParse();
					_feeds.add(feed);
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}

	private void writeConfig() {
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
