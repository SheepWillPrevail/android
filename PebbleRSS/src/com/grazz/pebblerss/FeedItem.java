package com.grazz.pebblerss;

import android.net.Uri;

public class FeedItem {
	private String _title;
	private Uri _link;
	private String _content;

	public FeedItem(String title, Uri link, String content) {
		_title = title;
		_link = link;
		_content = content;
	}

	public String getTitle() {
		return _title;
	}

	public Uri getLink() {
		return _link;
	}

	public String getContent() {
		return _content;
	}
}