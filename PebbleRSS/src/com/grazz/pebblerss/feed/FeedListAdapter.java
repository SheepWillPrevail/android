package com.grazz.pebblerss.feed;

import android.content.Context;
import android.database.DataSetObserver;
import android.view.View;
import android.view.View.OnLongClickListener;
import android.view.ViewGroup;
import android.widget.ListAdapter;
import android.widget.TextView;

import com.grazz.pebblerss.R;
import com.grazz.pebblerss.provider.RSSFeed;

public class FeedListAdapter implements ListAdapter {

	private Context _context;
	private FeedManager _manager;
	private OnLongClickListener _listener;

	public FeedListAdapter(Context context, FeedManager manager, OnLongClickListener listener) {
		_context = context;
		_manager = manager;
		_listener = listener;
	}

	@Override
	public int getCount() {
		return _manager.getFeeds().size();
	}

	@Override
	public Object getItem(int arg0) {
		return null;
	}

	@Override
	public long getItemId(int position) {
		return 0;
	}

	@Override
	public int getItemViewType(int position) {
		return 0;
	}

	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		if (convertView == null) {
			convertView = View.inflate(parent.getContext(), R.layout.cell_feed, null);
			RSSFeed feed = _manager.getFeed(position);
			TextView tvFeedName = (TextView) convertView.findViewById(R.id.tvFeedName);
			tvFeedName.setText(feed.getName());
			TextView tvFeedInfo = (TextView) convertView.findViewById(R.id.tvFeedInfo);
			tvFeedInfo.setText(String.format("%d items", feed.getItems(_context).size()));
			convertView.setTag(feed.getId());
			convertView.setOnLongClickListener(_listener);
		}
		return convertView;
	}

	@Override
	public int getViewTypeCount() {
		return 1;
	}

	@Override
	public boolean hasStableIds() {
		return false;
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

}
