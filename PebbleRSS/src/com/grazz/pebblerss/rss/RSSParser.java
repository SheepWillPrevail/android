package com.grazz.pebblerss.rss;

import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import android.net.Uri;

public class RSSParser extends FeedParser {

	public RSSParser(Element root) {
		super(root);
	}

	@Override
	protected void doParse() {
		Element root = getRoot();
		Feed feed = getFeed();

		feed.setTitle(findSingleElementText(root, "title"));
		feed.setLink(Uri.parse(findSingleElementText(root, "link")));
		feed.setDescription(findSingleElementText(root, "description"));
		feed.setPublicationDate(DateUtils.parseDate(findSingleElementText(root, "pubDate")));
		feed.setUpdatedDate(DateUtils.parseDate(findSingleElementText(root, "lastBuildDate")));

		NodeList list = root.getElementsByTagName("item");
		for (int i = 0; i < list.getLength(); i++)
			feed.addItem(parseEntry((Element) list.item(i)));
	}

	private FeedItem parseEntry(Element element) {
		FeedItem item = new FeedItem();

		item.setId(findSingleElementText(element, "guid"));
		item.setTitle(findSingleElementText(element, "title"));
		item.setLink(Uri.parse(findSingleElementText(element, "link")));
		item.setContent(findSingleElementText(element, "description"));
		item.setPublicationDate(DateUtils.parseDate(findSingleElementText(element, "pubDate")));

		return item;
	}

}
