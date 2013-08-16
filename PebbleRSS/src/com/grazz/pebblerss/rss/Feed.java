package com.grazz.pebblerss.rss;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;

import android.net.Uri;

public class Feed {

	private String _title;
	private Uri _link;
	private String _description;
	private String _language;
	private Date _publicationDate;
	private Date _updatedDate;

	private List<FeedItem> _items = new ArrayList<FeedItem>();

	public String getTitle() {
		return _title;
	}

	public void setTitle(String title) {
		_title = title;
	}

	public Uri getLink() {
		return _link;
	}

	public void setLink(Uri link) {
		_link = link;
	}

	public String getDescription() {
		return _description;
	}

	public void setDescription(String description) {
		_description = description;
	}

	public String getLanguage() {
		return _language;
	}

	public void setLanguage(String language) {
		_language = language;
	}

	public Date getPublicationDate() {
		return _publicationDate;
	}

	public void setPublicationDate(Date publicationDate) {
		_publicationDate = publicationDate;
	}

	public Date getUpdatedDate() {
		return _updatedDate;
	}

	public void setUpdatedDate(Date updatedDate) {
		_updatedDate = updatedDate;
	}

	public List<FeedItem> getItems() {
		return Collections.unmodifiableList(_items);
	}

	public void addItem(FeedItem item) {
		_items.add(item);
	}

}
