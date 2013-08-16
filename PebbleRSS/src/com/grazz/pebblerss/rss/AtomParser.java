package com.grazz.pebblerss.rss;

import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import android.net.Uri;

public class AtomParser extends FeedParser {

	public AtomParser(Element root) {
		super(root);
	}

	@Override
	protected void doParse() {
		Element root = getRoot();
		Feed feed = getFeed();

		feed.setTitle(findSingleElementText(root, "title"));
		feed.setLink(Uri.parse(findSingleElementText(root, "link")));
		feed.setPublicationDate(DateUtils.parseDate(findSingleElementText(root, "published")));
		feed.setUpdatedDate(DateUtils.parseDate(findSingleElementText(root, "updated")));

		NodeList list = root.getElementsByTagName("entry");
		for (int i = 0; i < list.getLength(); i++)
			feed.addItem(parseEntry((Element) list.item(i)));

	}

	private FeedItem parseEntry(Element element) {
		FeedItem item = new FeedItem();

		item.setId(findSingleElementText(element, "id"));
		item.setTitle(findSingleElementText(element, "title"));
		item.setLink(Uri.parse(findSingleElementAttribute(element, "link", "href")));
		item.setContent(findSingleElementText(element, "content"));
		if (item.getContent() == null)
			item.setContent(findSingleElementText(element, "summary"));
		item.setPublicationDate(DateUtils.parseDate(findSingleElementText(element, "published")));
		item.setUpdatedDate(DateUtils.parseDate(findSingleElementText(element, "updated")));

		return item;
	}

}
