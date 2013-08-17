package com.grazz.pebblerss.provider;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.net.Uri;

public class RSSDatabase extends SQLiteOpenHelper {

	public static final String FEED_COLUMN_ID = "_id";
	public static final String FEED_COLUMN_URI = "uri";
	public static final String FEED_COLUMN_NAME = "name";
	public static final String FEED_COLUMN_INTERVAL = "interval";
	public static final String FEED_COLUMN_RETENTION = "retention";
	public static final String FEED_COLUMN_LAST_UPDATE = "last_update";
	public static final String FEED_COLUMN_USERNAME = "username";
	public static final String FEED_COLUMN_PASSWORD = "password";

	public static final String FEEDITEM_COLUMN_ID = "_id";
	public static final String FEEDITEM_COLUMN_FEED_ID = "feed_id";
	public static final String FEEDITEM_COLUMN_UNIQUE_ID = "unique_id";
	public static final String FEEDITEM_COLUMN_PUBLICATION_DATE = "publication_date";
	public static final String FEEDITEM_COLUMN_URI = "uri";
	public static final String FEEDITEM_COLUMN_TITLE = "title";
	public static final String FEEDITEM_COLUMN_CONTENT = "content";

	private static final String[] FEED_ALL_COLUMNS = new String[] { FEED_COLUMN_ID, FEED_COLUMN_URI, FEED_COLUMN_NAME, FEED_COLUMN_INTERVAL,
			FEED_COLUMN_RETENTION, FEED_COLUMN_LAST_UPDATE, FEED_COLUMN_USERNAME, FEED_COLUMN_PASSWORD };
	private static final String[] FEEDITEM_ALL_COLUMNS = new String[] { FEEDITEM_COLUMN_ID, FEEDITEM_COLUMN_FEED_ID, FEEDITEM_COLUMN_UNIQUE_ID,
			FEEDITEM_COLUMN_PUBLICATION_DATE, FEEDITEM_COLUMN_URI, FEEDITEM_COLUMN_TITLE, FEEDITEM_COLUMN_CONTENT };

	private static final String FEED_TABLE_NAME = "feed";
	private static final String FEEDITEM_TABLE_NAME = "feeditem";

	public RSSDatabase(Context context) {
		super(context, "pebblerss.db", null, 4);
	}

	@Override
	public void onCreate(SQLiteDatabase db) {
		StringBuilder feedBuilder = new StringBuilder();
		feedBuilder.append("create table " + FEED_TABLE_NAME + " (");
		feedBuilder.append(FEED_COLUMN_ID + " integer primary key autoincrement,");
		feedBuilder.append(FEED_COLUMN_URI + " text,");
		feedBuilder.append(FEED_COLUMN_NAME + " text,");
		feedBuilder.append(FEED_COLUMN_INTERVAL + " integer,");
		feedBuilder.append(FEED_COLUMN_RETENTION + " integer,");
		feedBuilder.append(FEED_COLUMN_USERNAME + " text,");
		feedBuilder.append(FEED_COLUMN_PASSWORD + " text,");
		feedBuilder.append(FEED_COLUMN_LAST_UPDATE + " integer");
		feedBuilder.append(")");
		db.execSQL(feedBuilder.toString());

		StringBuilder feedItembuilder = new StringBuilder();
		feedItembuilder.append("create table " + FEEDITEM_TABLE_NAME + " (");
		feedItembuilder.append(FEEDITEM_COLUMN_ID + " integer primary key autoincrement,");
		feedItembuilder.append(FEEDITEM_COLUMN_FEED_ID + " integer,");
		feedItembuilder.append(FEEDITEM_COLUMN_UNIQUE_ID + " text,");
		feedItembuilder.append(FEEDITEM_COLUMN_PUBLICATION_DATE + " integer,");
		feedItembuilder.append(FEEDITEM_COLUMN_URI + " text,");
		feedItembuilder.append(FEEDITEM_COLUMN_TITLE + " text,");
		feedItembuilder.append(FEEDITEM_COLUMN_CONTENT + " text");
		feedItembuilder.append(")");
		db.execSQL(feedItembuilder.toString());
	}

	@Override
	public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
		if (oldVersion < 2 && newVersion > 1) {
			db.execSQL("alter table " + FEEDITEM_TABLE_NAME + " add column " + FEEDITEM_COLUMN_UNIQUE_ID + " text");
			db.execSQL("delete from " + FEEDITEM_TABLE_NAME + " where " + FEEDITEM_COLUMN_UNIQUE_ID + " is null");
		}
		if (oldVersion < 3 && newVersion > 2) {
			db.execSQL("alter table " + FEED_TABLE_NAME + " add column " + FEED_COLUMN_RETENTION + " integer");
			db.execSQL("update " + FEED_TABLE_NAME + " set " + FEED_COLUMN_RETENTION + "=24 where " + FEED_COLUMN_RETENTION + " is null");
		}
		if (oldVersion < 4 && newVersion > 3) {
			db.execSQL("alter table " + FEED_TABLE_NAME + " add column " + FEED_COLUMN_USERNAME + " text");
			db.execSQL("alter table " + FEED_TABLE_NAME + " add column " + FEED_COLUMN_PASSWORD + " text");
		}
	}

	public void createFeed(RSSFeed feed) {
		ContentValues values = new ContentValues();
		values.put(FEED_COLUMN_URI, feed.getUri().toString());
		values.put(FEED_COLUMN_NAME, feed.getName());
		values.put(FEED_COLUMN_INTERVAL, feed.getInterval());
		values.put(FEED_COLUMN_RETENTION, feed.getRetention());
		values.put(FEED_COLUMN_USERNAME, feed.getUsername());
		values.put(FEED_COLUMN_PASSWORD, feed.getPassword());
		values.put(FEED_COLUMN_LAST_UPDATE, feed.getLastUpdated());

		SQLiteDatabase db = getWritableDatabase();
		feed.setId(db.insert(FEED_TABLE_NAME, null, values));
		db.close();
	}

	private RSSFeed cursorToFeed(Cursor cursor) {
		RSSFeed feed = new RSSFeed();
		feed.setId(cursor.getInt(cursor.getColumnIndex(FEED_COLUMN_ID)));
		feed.setUri(Uri.parse(cursor.getString(cursor.getColumnIndex(FEED_COLUMN_URI))));
		feed.setName(cursor.getString(cursor.getColumnIndex(FEED_COLUMN_NAME)));
		feed.setInterval(cursor.getInt(cursor.getColumnIndex(FEED_COLUMN_INTERVAL)));
		feed.setRetention(cursor.getInt(cursor.getColumnIndex(FEED_COLUMN_RETENTION)));
		feed.setUsername(cursor.getString(cursor.getColumnIndex(FEED_COLUMN_USERNAME)));
		feed.setPassword(cursor.getString(cursor.getColumnIndex(FEED_COLUMN_PASSWORD)));
		feed.setLastUpdated(cursor.getLong(cursor.getColumnIndex(FEED_COLUMN_LAST_UPDATE)));
		return feed;
	}

	public void createFeedItem(RSSFeed feed, RSSFeedItem item) {
		ContentValues values = new ContentValues();
		values.put(FEEDITEM_COLUMN_FEED_ID, feed.getId());
		values.put(FEEDITEM_COLUMN_UNIQUE_ID, item.getUniqueId());
		values.put(FEEDITEM_COLUMN_PUBLICATION_DATE, item.getPublicationDate().getTime());
		values.put(FEEDITEM_COLUMN_URI, item.getUri().toString());
		values.put(FEEDITEM_COLUMN_TITLE, item.getTitle());
		values.put(FEEDITEM_COLUMN_CONTENT, item.getContent());

		SQLiteDatabase db = getWritableDatabase();
		item.setId(db.insert(FEEDITEM_TABLE_NAME, null, values));
		db.close();
	}

	private RSSFeedItem cursorToFeedItem(Cursor cursor) {
		RSSFeedItem item = new RSSFeedItem();
		item.setId(cursor.getLong(cursor.getColumnIndex(FEEDITEM_COLUMN_ID)));
		item.setParentId(cursor.getLong(cursor.getColumnIndex(FEEDITEM_COLUMN_FEED_ID)));
		item.setUniqueId(cursor.getString(cursor.getColumnIndex(FEEDITEM_COLUMN_UNIQUE_ID)));
		item.setPublicationDate(new Date(cursor.getLong(cursor.getColumnIndex(FEEDITEM_COLUMN_PUBLICATION_DATE))));
		item.setUri(Uri.parse(cursor.getString(cursor.getColumnIndex(FEEDITEM_COLUMN_URI))));
		item.setTitle(cursor.getString(cursor.getColumnIndex(FEEDITEM_COLUMN_TITLE)));
		item.setContent(cursor.getString(cursor.getColumnIndex(FEEDITEM_COLUMN_CONTENT)));
		return item;
	}

	public RSSFeed getFeedOf(RSSFeedItem item) {
		RSSFeed feed = null;
		SQLiteDatabase db = getReadableDatabase();

		Cursor cursor = db.query(FEED_TABLE_NAME, FEED_ALL_COLUMNS, FEED_COLUMN_ID + "=?", new String[] { String.valueOf(item.getParentId()) }, null, null,
				null);

		if (cursor != null && cursor.moveToNext()) {
			feed = cursorToFeed(cursor);
			cursor.close();
		}

		db.close();
		return feed;
	}

	public List<RSSFeed> readFeeds() {
		List<RSSFeed> feeds = new ArrayList<RSSFeed>();
		SQLiteDatabase db = getReadableDatabase();

		Cursor cursor = db.query(FEED_TABLE_NAME, FEED_ALL_COLUMNS, null, null, null, null, FEED_COLUMN_ID + " ASC");

		if (cursor != null) {
			while (cursor.moveToNext())
				feeds.add(cursorToFeed(cursor));
			cursor.close();
		}

		db.close();
		return feeds;
	}

	public List<RSSFeedItem> readAllFeedItems() {
		List<RSSFeedItem> items = new ArrayList<RSSFeedItem>();
		SQLiteDatabase db = getReadableDatabase();

		Cursor cursor = db.query(FEEDITEM_TABLE_NAME, FEEDITEM_ALL_COLUMNS, null, null, null, null, FEEDITEM_COLUMN_PUBLICATION_DATE + " DESC");

		if (cursor != null) {
			while (cursor.moveToNext())
				items.add(cursorToFeedItem(cursor));
			cursor.close();
		}

		db.close();
		return items;
	}

	public List<RSSFeedItem> readFeedItems(RSSFeed feed) {
		List<RSSFeedItem> items = new ArrayList<RSSFeedItem>();
		SQLiteDatabase db = getReadableDatabase();

		Cursor cursor = db.query(FEEDITEM_TABLE_NAME, FEEDITEM_ALL_COLUMNS, FEEDITEM_COLUMN_FEED_ID + "=?", new String[] { String.valueOf(feed.getId()) },
				null, null, FEEDITEM_COLUMN_PUBLICATION_DATE + " DESC");

		if (cursor != null) {
			while (cursor.moveToNext())
				items.add(cursorToFeedItem(cursor));
			cursor.close();
		}

		db.close();
		return items;
	}

	public void updateFeed(RSSFeed feed) {
		ContentValues values = new ContentValues();
		values.put(FEED_COLUMN_URI, feed.getUri().toString());
		values.put(FEED_COLUMN_NAME, feed.getName());
		values.put(FEED_COLUMN_INTERVAL, feed.getInterval());
		values.put(FEED_COLUMN_RETENTION, feed.getRetention());
		values.put(FEED_COLUMN_LAST_UPDATE, feed.getLastUpdated());

		SQLiteDatabase db = getWritableDatabase();
		db.update(FEED_TABLE_NAME, values, FEED_COLUMN_ID + "=?", new String[] { String.valueOf(feed.getId()) });
		db.close();
	}

	public void cleanupExpired(RSSFeed feed, int period) {
		long expiredate = feed.getLastUpdated() - (period * 3600000);

		SQLiteDatabase db = getWritableDatabase();
		db.delete(FEEDITEM_TABLE_NAME, FEEDITEM_COLUMN_FEED_ID + "=? and " + FEEDITEM_COLUMN_PUBLICATION_DATE + "<?",
				new String[] { String.valueOf(feed.getId()), String.valueOf(expiredate) });
		db.close();
	}

	public void deleteFeed(RSSFeed feed) {
		SQLiteDatabase db = getWritableDatabase();
		db.delete(FEED_TABLE_NAME, FEED_COLUMN_ID + "=?", new String[] { String.valueOf(feed.getId()) });
		db.close();
	}

	public void deleteFeedItems(RSSFeed feed) {
		SQLiteDatabase db = getWritableDatabase();
		db.delete(FEEDITEM_TABLE_NAME, FEEDITEM_COLUMN_FEED_ID + "=?", new String[] { String.valueOf(feed.getId()) });
		db.close();
	}

	public boolean wantsFeedItem(RSSFeed feed, String uniqueId, Date publicationDate) {
		SQLiteDatabase db = getReadableDatabase();

		String query = FEEDITEM_COLUMN_FEED_ID + "=? and " + FEEDITEM_COLUMN_UNIQUE_ID + "=?";
		String[] parameters = new String[] { String.valueOf(feed.getId()), uniqueId };
		Cursor cursor = db.query(FEEDITEM_TABLE_NAME, new String[] { FEEDITEM_COLUMN_ID, FEEDITEM_COLUMN_PUBLICATION_DATE }, query, parameters, null, null,
				null);

		boolean wanted = true;
		if (cursor != null) {
			wanted = !cursor.moveToNext();
			if (!wanted) {
				Date foundDate = new Date(cursor.getLong(cursor.getColumnIndex(FEEDITEM_COLUMN_PUBLICATION_DATE)));
				if (publicationDate.after(foundDate)) {
					SQLiteDatabase writeDb = getWritableDatabase();
					writeDb.delete(FEEDITEM_TABLE_NAME, query, parameters);
					writeDb.close();
					wanted = true;
				}
			}
			cursor.close();
		}

		db.close();
		return wanted;
	}

}
