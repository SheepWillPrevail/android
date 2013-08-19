package com.grazz.pebblerss.feed;

import android.annotation.SuppressLint;
import android.content.Context;
import android.database.DataSetObserver;
import android.util.SparseArray;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListAdapter;
import android.widget.TextView;

import com.grazz.pebblerss.R;
import com.grazz.pebblerss.provider.RSSFeed;

public class FeedListAdapter implements ListAdapter {

	private Context _context;
	private FeedManager _manager;
	private SparseArray<Integer> _itemCache;
	private String _itemText;

	@SuppressLint("UseSparseArrays")
	public FeedListAdapter(Context context, FeedManager manager) {
		_context = context;
		_manager = manager;
		_itemCache = new SparseArray<Integer>();
		_itemText = context.getResources().getString(R.string.main_item_count);
	}

	@Override
	public int getCount() {
		return _manager.getFeeds().size();
	}

	@Override
	public Object getItem(int position) {
		return _manager.getFeedAt(position);
	}

	@Override
	public long getItemId(int position) {
		return _manager.getFeedAt(position).getId();
	}

	@Override
	public int getItemViewType(int position) {
		return 0;
	}

	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		if (convertView == null)
			convertView = View.inflate(parent.getContext(), R.layout.cell_feed, null);
		RSSFeed feed = _manager.getFeedAt(position);
		TextView tvFeedId = (TextView) convertView.findViewById(R.id.tvURL);
		TextView tvFeedName = (TextView) convertView.findViewById(R.id.tvFeedName);
		TextView tvFeedInfo = (TextView) convertView.findViewById(R.id.tvFeedInfo);
		tvFeedName.setText(feed.getName());
		tvFeedId.setText("#" + (position + 1));
		if (_itemCache.get(position) == null)
			_itemCache.put(position, feed.getItems(_context).size());
		tvFeedInfo.setText(String.format("%d %s", _itemCache.get(position), _itemText));
		return convertView;
	}

	@Override
	public int getViewTypeCount() {
		return 1;
	}

	@Override
	public boolean hasStableIds() {
		return true;
	}

	@Override
	public boolean isEmpty() {
		return _manager.getFeeds().isEmpty();
	}

	@Override
	public void registerDataSetObserver(DataSetObserver observer) {
	}

	@Override
	public void unregisterDataSetObserver(DataSetObserver observer) {
	}

	@Override
	public boolean areAllItemsEnabled() {
		return false;
	}

	@Override
	public boolean isEnabled(int position) {
		return false;
	}

	public void invalidateCache() {
		_itemCache.clear();
	}

}
