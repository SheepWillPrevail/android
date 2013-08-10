package com.grazz.pebblerss.feed;

import java.util.BitSet;
import java.util.List;

import android.content.Context;

import com.grazz.pebblerss.provider.RSSFeed;
import com.grazz.pebblerss.provider.RSSFeedTable;

public class FeedCursor {

	private List<RSSFeed> _feeds;
	private BitSet _sentItems;
	private int _index = 0;

	public FeedCursor(Context context) {
		_feeds = new RSSFeedTable(context).getFeeds();
		_sentItems = new BitSet(_feeds.size());
	}

	public int getPosition() {
		return _index;
	}

	public int getTotal() {
		int total = _feeds.size();
		if (total > 48)
			total = 48;
		return total;
	}

	public Boolean isDone() {
		return ((_index + 1) > getTotal()) || _index > 47; // clamp
	}

	public RSSFeed getItem(int index) {
		return _feeds.get(index);
	}

	public RSSFeed getNextItem() {
		if (!_feeds.isEmpty() && !isDone()) {
			int i = _sentItems.nextClearBit(_index);
			RSSFeed feed = _feeds.get(i);
			_sentItems.set(i);
			_index++;
			return feed;
		} else
			return null;
	}
}
