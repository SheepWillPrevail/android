package com.grazz.pebblerss.feed;

import java.util.BitSet;
import java.util.List;

import android.content.Context;

import com.grazz.pebblerss.provider.RSSFeed;
import com.grazz.pebblerss.provider.RSSFeedItem;

public class FeedItemCursor {

	private List<RSSFeedItem> _items;
	private BitSet _sentItems;
	private int _index = 0;

	public FeedItemCursor(Context context, RSSFeed feed) {
		_items = feed.getItems(context);
		_sentItems = new BitSet(_items.size());
	}

	public int getPosition() {
		return _index;
	}

	public int getTotal() {
		int total = _items.size();
		if (total > 128)
			total = 128;
		return total;
	}

	public Boolean isDone() {
		return ((_index + 1) > getTotal()) || _index > 127; // clamp
	}

	public RSSFeedItem getItem(int index) {
		return _items.get(index);
	}

	public RSSFeedItem getNextItem() {
		if (!_items.isEmpty() && !isDone()) {
			int i = _sentItems.nextClearBit(_index);
			RSSFeedItem item = _items.get(i);
			_sentItems.set(i);
			_index++;
			return item;
		} else
			return null;
	}

}
