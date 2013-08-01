package com.grazz.pebblerss.feed;

import java.io.Serializable;

public class SerializedFeedItem implements Serializable {

	private static final long serialVersionUID = 1L;

	private int _id;
	private String _title;
	private String _content;

	public SerializedFeedItem(int id, String title, String content) {
		_id = id;
		_title = title;
		_content = content;
	}

	public int getId() {
		return _id;
	}

	public String getTitle() {
		return _title;
	}

	public String getContent() {
		return _content;
	}

}
