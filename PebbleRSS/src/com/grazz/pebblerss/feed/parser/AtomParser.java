package com.grazz.pebblerss.feed.parser;

import java.util.Map;

import org.xmlpull.v1.XmlPullParser;

import android.net.Uri;
import android.webkit.URLUtil;

public class AtomParser extends AbstractParser {

	public AtomParser(XmlPullParser parser) throws Exception {
		super(parser);
	}

	@Override
	protected void handleStartTag(XmlPullParser parser, String name, Map<String, String> attributes) throws Exception {
		if (getLevel() == ParseLevel.DOCUMENT && parser.getDepth() == 1) {
			setFeed(new ParsedFeed());
			setLevel(ParseLevel.FEED);
		} else if (getLevel() == ParseLevel.FEED && parser.getDepth() == 2) {
			if ("entry".equals(name)) {
				setItem(new ParsedItem());
				setLevel(ParseLevel.ITEM);
			} else if ("title".equals(name))
				getFeed().setTitle(parser.nextText());
			else if ("updated".equals(name))
				getFeed().setPublicationDate(DateUtil.parseDate(parser.nextText()));
		} else if (getLevel() == ParseLevel.ITEM && parser.getDepth() == 3) {
			if ("id".equals(name)) {
				getItem().setId(parser.nextText());
			} else if ("title".equals(name))
				getItem().setTitle(parser.nextText());
			else if ("updated".equals(name))
				getItem().setPublicationDate(DateUtil.parseDate(parser.nextText()));
			else if ("link".equals(name)) {
				String link = attributes.get("href");
				if ("text/html".equals(attributes.get("type")) && URLUtil.isValidUrl(link))
					getItem().setLink(Uri.parse(link));
			} else if ("content".equals(name))
				getItem().setContent(parser.nextText());
			else if ("summary".equals(name))
				if (getItem().getContent() == null)
					getItem().setContent(parser.nextText());
		}
	}

	@Override
	protected void handleEndTag(XmlPullParser parser, String name) {
		if ("entry".equals(name)) {
			getFeed().addItem(getItem());
			setLevel(ParseLevel.FEED);
		}
	}

}
