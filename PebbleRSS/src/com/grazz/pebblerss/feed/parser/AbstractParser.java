package com.grazz.pebblerss.feed.parser;

import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserFactory;

public abstract class AbstractParser {

	public enum ParseLevel {
		DOCUMENT, FEED, ITEM
	}

	private ParseLevel _level = ParseLevel.DOCUMENT;
	private ParsedFeed _feed;
	private ParsedItem _item;

	public AbstractParser(XmlPullParser parser) throws Exception {
		int event = parser.getEventType();
		Map<String, String> attributes = new HashMap<String, String>();
		while (event != XmlPullParser.END_DOCUMENT) {
			switch (event) {
			case XmlPullParser.START_TAG:
				attributes.clear();
				for (int i = 0; i < parser.getAttributeCount(); i++)
					attributes.put(parser.getAttributeName(i), parser.getAttributeValue(i));
				handleStartTag(parser, parser.getName(), attributes);
				break;
			case XmlPullParser.END_TAG:
				handleEndTag(parser, parser.getName());
				break;
			}
			event = parser.next();
		}
	}

	protected ParseLevel getLevel() {
		return _level;
	}

	protected void setLevel(ParseLevel level) {
		_level = level;
	}

	public ParsedFeed getFeed() {
		return _feed;
	}

	protected void setFeed(ParsedFeed feed) {
		_feed = feed;
	}

	protected ParsedItem getItem() {
		return _item;
	}

	protected void setItem(ParsedItem item) {
		_item = item;
	}

	protected abstract void handleStartTag(XmlPullParser parser, String name, Map<String, String> attributes) throws Exception;

	protected abstract void handleEndTag(XmlPullParser parser, String name) throws Exception;

	public static AbstractParser findParser(InputStream stream) {
		AbstractParser result = null;
		XmlPullParserFactory factory;
		try {
			factory = XmlPullParserFactory.newInstance();
			factory.setNamespaceAware(true);
			XmlPullParser parser = factory.newPullParser();
			if (parser != null) {
				parser.setInput(stream, null);
				int event = parser.getEventType();
				while (event != XmlPullParser.START_TAG && event != XmlPullParser.END_DOCUMENT)
					event = parser.next();
				if (event == XmlPullParser.START_TAG) {
					String name = parser.getName();
					if ("rss".equals(name))
						result = new RSSParser(parser);
					else if ("feed".equals(name))
						result = new AtomParser(parser);
					else if ("RDF".equals(name))
						result = new RDFParser(parser);
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return result;
	}
}
