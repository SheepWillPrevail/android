package com.grazz.pebblerss.feed.parser;

import java.util.Date;

import android.net.Uri;

public class ParsedItem {

	private String _id;
	private String _title;
	private Date _publicationDate;
	private Uri _link;
	private String _content;
	private Uri _thumbnailLink;

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

	public Date getPublicationDate() {
		return _publicationDate;
	}

	public void setPublicationDate(Date publicationDate) {
		_publicationDate = publicationDate;
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

	public Uri getThumbnailLink() {
		return _thumbnailLink;
	}

	public void setThumbnailLink(Uri thumbnailLink) {
		_thumbnailLink = thumbnailLink;
	}

}
