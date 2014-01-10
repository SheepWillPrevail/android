package com.grazz.pebblerss.feed.parser;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;

public class ParsedFeed {

	private String _title;
	private Date _publicationDate;
	private List<ParsedItem> _items = new ArrayList<ParsedItem>();

	public String getTitle() {
		return _title;
	}

	public void setTitle(String title) {
		_title = title;
	}

	public Date getPublicationDate() {
		return _publicationDate;
	}

	public void setPublicationDate(Date publicationDate) {
		_publicationDate = publicationDate;
	}

	public List<ParsedItem> getItems() {
		return Collections.unmodifiableList(_items);
	}

	public void addItem(ParsedItem item) {
		_items.add(item);
	}

}
