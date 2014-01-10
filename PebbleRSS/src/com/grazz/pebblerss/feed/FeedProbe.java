package com.grazz.pebblerss.feed;

import java.io.IOException;
import java.io.InputStream;

import android.net.Uri;

import com.grazz.pebblerss.feed.parser.AbstractParser;
import com.grazz.pebblerss.feed.parser.ParsedFeed;

public class FeedProbe {

	private boolean _isParsed;
	private String _name;
	private int _itemCount = 0;

	public FeedProbe(Uri link, String username, String password) {
		InputStream stream = null;
		try {
			stream = new ExtendedHttpClient(link, username, password).getInputStream();
			if (stream != null) {
				AbstractParser parser = AbstractParser.findParser(stream);
				if (parser != null) {
					ParsedFeed feed = parser.getFeed();
					stream.close();
					if (feed != null) {
						_isParsed = true;
						_name = feed.getTitle();
						_itemCount = feed.getItems().size();
					}
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			if (stream != null)
				try {
					stream.close();
				} catch (IOException e) {
				}
		}
	}

	public boolean isParsed() {
		return _isParsed;
	}

	public String getName() {
		if (!isParsed())
			return null;

		return _name;
	}

	public int getNumberOfItems() {
		if (!isParsed())
			return 0;

		return _itemCount;
	}

}
