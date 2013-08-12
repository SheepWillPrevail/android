package com.grazz.pebblerss.feed;

import java.util.BitSet;
import java.util.List;

import android.content.Context;

import com.grazz.pebblerss.provider.RSSFeed;

public class FeedCursor {

	private List<RSSFeed> _feeds;
	private BitSet _sentItems;
	private int _index = 0;

	public FeedCursor(Context context) {
		_feeds = RSSFeed.getFeeds(context);
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
		return _index + 1 > getTotal();
	}

	public RSSFeed getItem(int index) {
		return _feeds.get(index);
	}

	public RSSFeed getNextItem() {
		if (!isDone()) {
			int index = _sentItems.nextClearBit(_index++);
			_sentItems.set(index);
			return getItem(index);
		} else
			return null;
	}

	public FeedItemCursor getItemCursor(Context context, int index) {
		return new FeedItemCursor(context, getItem(index));
	}
}
