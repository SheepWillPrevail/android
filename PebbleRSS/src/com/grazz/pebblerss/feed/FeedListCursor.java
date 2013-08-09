package com.grazz.pebblerss.feed;

import java.util.BitSet;

public class FeedListCursor {
	private FeedManager _manager;
	private BitSet _sentItems;
	private int _index = 0;

	public FeedListCursor(FeedManager manager) {
		_manager = manager;
		_sentItems = new BitSet(_manager.getFeeds().size());
	}

	public int getPosition() {
		return _index;
	}

	public int getTotal() {
		int total = _manager.getFeeds().size();
		if (total > 48)
			total = 48;
		return total;
	}

	public Boolean isDone() {
		return ((_index + 1) > getTotal()) || _index > 47; // clamp
	}

	public Feed getNextItem() {
		if (!_manager.getFeeds().isEmpty() && !isDone()) {
			int i = _sentItems.nextClearBit(_index);
			Feed feed = _manager.getFeeds().get(i);
			_sentItems.set(i);
			_index++;
			return feed;
		} else
			return null;
	}
}
