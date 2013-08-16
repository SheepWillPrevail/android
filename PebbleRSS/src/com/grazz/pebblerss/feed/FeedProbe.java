package com.grazz.pebblerss.feed;

import android.net.Uri;

import com.grazz.pebblerss.rss.FeedParser;
import com.grazz.pebblerss.rss.FeedParserFactory;

public class FeedProbe {

	private FeedParser _parser;

	public FeedProbe(Uri link) {
		_parser = FeedParserFactory.getParser(link);
	}

	public boolean isParsed() {
		return _parser != null;
	}

	public String getName() {
		if (!isParsed())
			return null;

		return _parser.getFeed().getTitle();
	}

	public int getNumberOfItems() {
		if (!isParsed())
			return 0;

		return _parser.getFeed().getItems().size();
	}

}
