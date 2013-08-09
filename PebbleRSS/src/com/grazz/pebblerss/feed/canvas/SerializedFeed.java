package com.grazz.pebblerss.feed.canvas;

import java.io.Serializable;

public class SerializedFeed implements Serializable {

	private static final long serialVersionUID = 1L;

	private int _id;
	private String _name;
	private String _content;

	public SerializedFeed(int id, String name, String content) {
		_id = id;
		_name = name;
		_content = content;
	}

	public int getId() {
		return _id;
	}

	public String getName() {
		return _name;
	}

	public String getContent() {
		return _content;
	}

}
