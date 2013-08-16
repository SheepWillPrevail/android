package com.grazz.pebblerss.rss;

import java.util.Date;

import android.net.Uri;

public class FeedItem {

	private String _id;
	private String _title;
	private Uri _link;
	private String _content;
	private Date _publicationDate;
	private Date _updatedDate;

	public String getId() {
		return _id;
	}

	public void setId(String id) {
		_id = id;
	}

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

	public String getContent() {
		return _content;
	}

	public void setContent(String content) {
		_content = content;
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

}
