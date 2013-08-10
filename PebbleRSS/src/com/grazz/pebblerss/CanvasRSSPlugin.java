package com.grazz.pebblerss;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;

import com.grazz.pebblerss.provider.RSSFeed;
import com.grazz.pebblerss.provider.RSSFeedItem;
import com.grazz.pebblerss.provider.RSSFeedTable;
import com.pennas.pebblecanvas.plugin.PebbleCanvasPlugin;

@SuppressLint({ "SimpleDateFormat", "DefaultLocale" })
public class CanvasRSSPlugin extends PebbleCanvasPlugin {

	public static final String PLUGINSTART = "pluginstart";
	public static final int ID_RSSITEM = 0;

	private static final String MASK_TITLE = "%rt";
	private static final String MASK_TIME12 = "%rp";
	private static final String MASK_TIME24 = "%rP";
	private static final SimpleDateFormat FORMAT_TIME12 = new SimpleDateFormat("h:mma");
	private static final SimpleDateFormat FORMAT_TIME24 = new SimpleDateFormat("HH:mm");

	private void startService(Context context) {
		Intent intent = new Intent(context, RSSService.class);
		intent.putExtra(PLUGINSTART, true);
		context.startService(intent);
	}

	@Override
	protected ArrayList<PluginDefinition> get_plugin_definitions(Context context) {
		startService(context);

		ArrayList<PluginDefinition> plugins = new ArrayList<PluginDefinition>();
		TextPluginDefinition plugin = new TextPluginDefinition();

		plugin.id = ID_RSSITEM;
		plugin.name = context.getResources().getString(R.string.app_name);
		plugin.format_mask_descriptions = new ArrayList<String>();
		plugin.format_mask_examples = new ArrayList<String>();
		plugin.format_masks = new ArrayList<String>();

		plugin.format_mask_descriptions.add("Title");
		plugin.format_mask_examples.add("Martians invade earth");
		plugin.format_masks.add(MASK_TITLE);

		plugin.format_mask_descriptions.add("Publication time (12h)");
		plugin.format_mask_examples.add("11:59pm");
		plugin.format_masks.add(MASK_TIME12);

		plugin.format_mask_descriptions.add("Publication time (24h)");
		plugin.format_mask_examples.add("23:59");
		plugin.format_masks.add(MASK_TIME24);

		plugin.default_format_string = MASK_TITLE;
		plugin.params_description = "feed(0-47),item(0-)";

		plugins.add(plugin);
		return plugins;
	}

	@Override
	protected String get_format_mask_value(int def_id, String format_mask, Context context, String param) {
		String empty = "";
		String[] params = param.split(",");

		if (params.length < 2)
			return empty;

		Integer feedId = null;
		Integer itemId = null;
		try {
			feedId = Integer.parseInt(params[0]);
			itemId = Integer.parseInt(params[1]);
		} catch (Exception e) {
		}

		if (feedId == null || itemId == null || feedId < 0 || itemId < 0)
			return empty;

		List<RSSFeed> feeds = new RSSFeedTable(context).getFeeds();

		if (feedId > feeds.size() - 1)
			return empty;

		RSSFeed feed = feeds.get(feedId);
		List<RSSFeedItem> items = feed.getItems(context);

		if (items != null && itemId > items.size() - 1)
			return empty;

		RSSFeedItem item = items.get(itemId);

		if (MASK_TITLE.equals(format_mask))
			return item.getTitle();

		if (MASK_TIME12.equals(format_mask))
			return FORMAT_TIME12.format(item.getPublicationDate()).toLowerCase();

		if (MASK_TIME24.equals(format_mask))
			return FORMAT_TIME24.format(item.getPublicationDate()).toLowerCase();

		return empty;
	}

	@Override
	protected Bitmap get_bitmap_value(int def_id, Context context, String param) {
		return null;
	}

}
