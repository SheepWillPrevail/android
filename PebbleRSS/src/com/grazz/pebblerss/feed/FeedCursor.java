package com.grazz.pebblerss.feed;

import java.util.BitSet;

public class FeedCursor {

	private Feed _feed;
	private BitSet _sentItems;
	private int _index = 0;

	public FeedCursor(Feed feed) {
		_feed = feed;
		_sentItems = new BitSet(_feed.getItems().size());
	}

	public int getPosition() {
		return _index;
	}

	public int getTotal() {
		int total = _feed.getItems().size();
		if (total > 128)
			total = 128;
		return total;
	}

	public Boolean isDone() {
		return ((_index + 1) > getTotal()) || _index > 127; // clamp
	}

	public FeedItem getNextItem() {
		int i = _sentItems.nextClearBit(_index);
		FeedItem item = _feed.getItems().get(i);
		_sentItems.set(i);
		_index++;
		return item;
	}

}
