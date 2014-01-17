package com.grazz.pebblerss.feed.parser;

import java.util.Map;

import org.xmlpull.v1.XmlPullParser;

import android.net.Uri;
import android.webkit.URLUtil;

public class RDFParser extends AbstractParser {

	public RDFParser(XmlPullParser parser) throws Exception {
		super(parser);
	}

	@Override
	protected void handleStartTag(XmlPullParser parser, String name, Map<String, String> attributes) throws Exception {
		if (getLevel() == ParseLevel.DOCUMENT && parser.getDepth() == 2) {
			if ("item".equals(name)) {
				ParsedItem item = new ParsedItem();
				item.setId(attributes.get("about"));
				setItem(item);
				setLevel(ParseLevel.ITEM);
			} else if ("channel".equals(name)) {
				setFeed(new ParsedFeed());
				setLevel(ParseLevel.FEED);
			}
		} else if (getLevel() == ParseLevel.FEED && parser.getDepth() == 3) {
			if ("title".equals(name))
				getFeed().setTitle(parser.nextText());
			else if ("updateBase".equals(name))
				getFeed().setPublicationDate(DateUtil.parseDate(parser.nextText()));
		} else if (getLevel() == ParseLevel.ITEM && parser.getDepth() == 3) {
			if ("title".equals(name))
				getItem().setTitle(parser.nextText());
			else if ("date".equals(name))
				getItem().setPublicationDate(DateUtil.parseDate(parser.nextText()));
			else if ("link".equals(name)) {
				String link = parser.nextText();
				if (URLUtil.isValidUrl(link))
					getItem().setLink(Uri.parse(link));
			} else if ("description".equals(name) || "encoded".equals(name))
				getItem().setContent(parser.nextText());
		}
	}

	@Override
	protected void handleEndTag(XmlPullParser parser, String name) {
		if ("item".equals(name)) {
			getFeed().addItem(getItem());
			setLevel(ParseLevel.DOCUMENT);
		} else if ("channel".equals(name))
			setLevel(ParseLevel.DOCUMENT);
	}

}
