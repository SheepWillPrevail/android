package com.grazz.pebblerss;

import java.util.ArrayList;
import java.util.List;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;

import com.grazz.pebblerss.feed.FeedSerializer;
import com.grazz.pebblerss.feed.SerializedFeed;
import com.pennas.pebblecanvas.plugin.PebbleCanvasPlugin;

public class CanvasRSSPlugin extends PebbleCanvasPlugin {

	public static final String START_FEED_POLLING = "canvasrssplugin";
	public static final int ID_HEADLINES = 0;

	@Override
	protected ArrayList<PluginDefinition> get_plugin_definitions(Context context) {
		Intent intent = new Intent(context, RSSService.class);
		intent.putExtra(START_FEED_POLLING, true);
		context.startService(intent);

		ArrayList<PluginDefinition> plugins = new ArrayList<PluginDefinition>();

		TextPluginDefinition plugin = new TextPluginDefinition();
		plugin.id = ID_HEADLINES;
		plugin.name = "Pebble RSS for Canvas (experimental)";
		plugin.format_mask_descriptions = new ArrayList<String>();
		plugin.format_mask_examples = new ArrayList<String>();
		plugin.format_masks = new ArrayList<String>();

		List<SerializedFeed> feeds = FeedSerializer.deserialize(context);
		for (SerializedFeed feed : feeds) {
			plugin.format_masks.add("%RH" + String.valueOf(feed.getId()));
			plugin.format_mask_descriptions.add("Headline for " + feed.getName());
			plugin.format_mask_examples.add(feed.getContent());
		}

		plugins.add(plugin);
		return plugins;
	}

	@Override
	protected String get_format_mask_value(int def_id, String format_mask, Context context) {
		String substring = format_mask.substring(3);
		if (substring.length() > 0) {
			Integer id = Integer.parseInt(substring);
			List<SerializedFeed> feeds = FeedSerializer.deserialize(context);
			if (id < feeds.size()) {
				SerializedFeed feed = feeds.get(id);
				if (feed != null)
					return feed.getContent();
			}
		}
		return "";
	}

	@Override
	protected Bitmap get_bitmap_value(int def_id, Context context) {
		return null;
	}

}
