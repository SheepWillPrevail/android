package com.grazz.pebblerss.feed.parser;

import java.util.Map;

import org.xmlpull.v1.XmlPullParser;

import android.net.Uri;
import android.webkit.URLUtil;

public class RSSParser extends AbstractParser {

	private int _thumbWidth = 0;

	public RSSParser(XmlPullParser parser) throws Exception {
		super(parser);
	}

	@Override
	protected void handleStartTag(XmlPullParser parser, String name, Map<String, String> attributes) throws Exception {
		if (getLevel() == ParseLevel.DOCUMENT && parser.getDepth() == 2) {
			if ("channel".equals(name)) {
				setFeed(new ParsedFeed());
				setLevel(ParseLevel.FEED);
			}
		} else if (getLevel() == ParseLevel.FEED && parser.getDepth() == 3) {
			if ("item".equals(name)) {
				setItem(new ParsedItem());
				setLevel(ParseLevel.ITEM);
				_thumbWidth = 0;
			} else if ("title".equals(name))
				getFeed().setTitle(parser.nextText());
			else if ("pubDate".equals(name))
				getFeed().setPublicationDate(DateUtil.parseDate(parser.nextText()));
		} else if (getLevel() == ParseLevel.ITEM && parser.getDepth() == 4) {
			if ("guid".equals(name)) {
				getItem().setId(parser.nextText());
			} else if ("title".equals(name))
				getItem().setTitle(parser.nextText());
			else if ("pubDate".equals(name))
				getItem().setPublicationDate(DateUtil.parseDate(parser.nextText()));
			else if ("link".equals(name)) {
				String link = parser.nextText();
				if (URLUtil.isValidUrl(link))
					getItem().setLink(Uri.parse(link));
			} else if ("description".equals(name))
				getItem().setContent(parser.nextText());
			else if ("enclosure".equals(name)) {
				String link = attributes.get("url");
				String type = attributes.get("type");
				if (("image/jpeg".equals(type) || "image/png".equals(type)) && URLUtil.isValidUrl(link))
					getItem().setThumbnailLink(Uri.parse(link));
			} else if ("thumbnail".equals(name)) {
				int width = Integer.parseInt(attributes.get("width"));
				if (getItem().getThumbnailLink() == null || ((_thumbWidth > 0) && (width > _thumbWidth))) {
					String link = attributes.get("url");
					if (URLUtil.isValidUrl(link)) {
						_thumbWidth = width;
						getItem().setThumbnailLink(Uri.parse(link));
					}
				}
			}
		}
	}

	@Override
	protected void handleEndTag(XmlPullParser parser, String name) {
		if ("item".equals(name)) {
			getFeed().addItem(getItem());
			setLevel(ParseLevel.FEED);
		}
	}

}
