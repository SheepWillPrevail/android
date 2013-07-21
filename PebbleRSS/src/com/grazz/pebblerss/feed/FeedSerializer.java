package com.grazz.pebblerss.feed;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.List;

import android.content.Context;

public class FeedSerializer {

	private static final String FEED_FILE = "feeds.dat";

	public static void serialize(Context context, FeedManager manager) {
		ArrayList<Feed> feeds = manager.getFeeds();
		File serialized = new File(context.getFilesDir(), FEED_FILE);
		if (serialized.exists())
			serialized.delete();
		try {
			ObjectOutputStream output = null;
			try {
				output = new ObjectOutputStream(new FileOutputStream(serialized));
				for (Feed feed : feeds) {
					FeedItem feedItem = feed.getItems().get(0);
					if (feedItem != null)
						output.writeObject(new SerializedFeed(feeds.indexOf(feed), feed.getName(), feedItem.getTitle()));
				}
			} finally {
				if (output != null)
					output.close();
			}
		} catch (Exception e) {
		}
	}

	public static List<SerializedFeed> deserialize(Context context) {
		List<SerializedFeed> list = new ArrayList<SerializedFeed>();

		File serialized = new File(context.getFilesDir(), FEED_FILE);
		if (serialized.exists()) {
			ObjectInputStream input = null;
			try {
				try {
					input = new ObjectInputStream(new FileInputStream(serialized));
					SerializedFeed read;
					while ((read = (SerializedFeed) input.readObject()) != null)
						list.add(read);
				} finally {
					if (input != null)
						input.close();
				}
			} catch (Exception e) {
			}
		}

		return list;
	}

}
