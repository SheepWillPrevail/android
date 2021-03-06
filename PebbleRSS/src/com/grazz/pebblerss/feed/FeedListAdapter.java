package com.grazz.pebblerss.feed;

import android.content.Context;
import android.util.SparseIntArray;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import com.grazz.pebblerss.R;
import com.grazz.pebblerss.provider.RSSFeed;

public class FeedListAdapter extends BaseAdapter {

	private Context _context;
	private FeedManager _manager;
	private SparseIntArray _itemCache;
	private String _itemText;

	public FeedListAdapter(Context context, FeedManager manager) {
		_context = context;
		_manager = manager;
		_itemCache = new SparseIntArray();
		_itemText = context.getResources().getString(R.string.main_item_count);
	}

	@Override
	public boolean areAllItemsEnabled() {
		return false;
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
	public View getView(int position, View convertView, ViewGroup parent) {
		if (convertView == null)
			convertView = View.inflate(parent.getContext(), R.layout.cell_feed, null);

		RSSFeed feed = _manager.getFeedAt(position);

		TextView tvFeedId = (TextView) convertView.findViewById(R.id.tvURL);
		tvFeedId.setText("#" + (position + 1));

		TextView tvFeedName = (TextView) convertView.findViewById(R.id.tvFeedName);
		tvFeedName.setText(feed.getName());

		TextView tvFeedInfo = (TextView) convertView.findViewById(R.id.tvFeedInfo);
		if (_itemCache.indexOfKey(position) < 0)
			_itemCache.put(position, feed.getItems(_context).size());
		tvFeedInfo.setText(String.format("%d %s", _itemCache.get(position), _itemText));

		return convertView;
	}

	@Override
	public int getViewTypeCount() {
		return 1;
	}

	@Override
	public boolean isEnabled(int position) {
		return false;
	}

	@Override
	public boolean hasStableIds() {
		return true;
	}

	@Override
	public void notifyDataSetChanged() {
		super.notifyDataSetChanged();
		_itemCache.clear();
	}

	@Override
	public boolean isEmpty() {
		return _manager.getFeeds().isEmpty();
	}

}
