package com.grazz.pebblerss.feed;

import java.io.InputStream;
import java.net.URL;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserFactory;

import android.net.Uri;

import com.axelby.riasel.Feed;
import com.axelby.riasel.FeedParser;
import com.axelby.riasel.FeedParser.FeedInfoHandler;

public class FeedProbe implements FeedInfoHandler {

	private Boolean _isParsed = false;
	private String _name;

	public FeedProbe(Uri link) {
		try {
			XmlPullParserFactory factory = XmlPullParserFactory.newInstance();
			factory.setNamespaceAware(true);
			XmlPullParser parser2 = factory.newPullParser();
			URL url = new URL(link.toString());
			InputStream stream = url.openStream();
			parser2.setInput(stream, null);
			FeedParser parser = new FeedParser();
			parser.setOnFeedInfoHandler(this);
			parser.parseFeed(parser2);
			_isParsed = true;
		} catch (Exception e) {
		}
	}

	public Boolean isParsed() {
		return _isParsed;
	}

	public String getName() {
		if (!isParsed())
			return null;

		return _name;
	}

	public int getNumberOfItems() {
		if (!isParsed())
			return 0;

		return 1;
	}

	@Override
	public void OnFeedInfo(FeedParser feedParser, Feed feed) {
		_name = feed.getTitle();
	}
}
