package com.grazz.pebblerss.provider;

import java.util.Date;
import java.util.List;

import android.content.Context;
import android.net.Uri;

public class RSSFeedItem extends RSSDatabaseEntity {

	private String _uniqueId;
	private Date _publicationDate;
	private Uri _uri;
	private String _title;
	private String _content;

	public Date getPublicationDate() {
		return _publicationDate;
	}

	public void setPublicationDate(Date publicationDate) {
		_publicationDate = publicationDate;
	}

	public String getUniqueId() {
		return _uniqueId;
	}

	public void setUniqueId(String uniqueId) {
		_uniqueId = uniqueId;
	}

	public Uri getUri() {
		return _uri;
	}

	public void setUri(Uri uri) {
		_uri = uri;
	}

	public String getTitle() {
		return _title;
	}

	public void setTitle(String title) {
		_title = title;
	}

	public String getContent() {
		return _content;
	}

	public void setContent(String content) {
		_content = content;
	}

	public RSSFeed getFeed(Context context) {
		RSSDatabase db = new RSSDatabase(context);
		RSSFeed feed = db.getFeedOf(this);
		db.close();
		return feed;
	}

	public static List<RSSFeedItem> getAllFeedItems(Context context) {
		RSSDatabase db = new RSSDatabase(context);
		List<RSSFeedItem> feedItems = db.readAllFeedItems();
		db.close();
		return feedItems;
	}

}
