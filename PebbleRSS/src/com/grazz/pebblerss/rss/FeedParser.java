package com.grazz.pebblerss.rss;

import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

public abstract class FeedParser {

	private Element _root;
	private Feed _feed;

	public FeedParser(Element root) {
		_root = root;
		_feed = new Feed();
		doParse();
	}

	protected Element getRoot() {
		return _root;
	}

	public Feed getFeed() {
		return _feed;
	}

	protected abstract void doParse();

	protected Element findSingleElement(Element parent, String tagName) {
		NodeList list = parent.getElementsByTagName(tagName);
		if (list.getLength() > 0)
			return (Element) list.item(0);
		return null;
	}

	protected String findSingleElementText(Element parent, String tagName) {
		Element element = findSingleElement(parent, tagName);
		if (element != null)
			return element.getTextContent();
		else
			return null;
	}

	protected String findSingleElementAttribute(Element parent, String tagName, String attributeName) {
		Element element = findSingleElement(parent, tagName);
		if (element != null)
			return element.getAttribute(attributeName);
		else
			return null;
	}

}
