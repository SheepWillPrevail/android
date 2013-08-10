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

import com.grazz.pebblerss.StaticValues;

public class RSSFeedItemTable extends SQLiteOpenHelper {

	private static final String TABLE_NAME = "feeditem";

	public static final String COLUMN_ID = "_id";
	public static final String COLUMN_UNIQUE_ID = "unique_id";
	public static final String COLUMN_FEED_ID = "feed_id";
	public static final String COLUMN_PUBLICATION_DATE = "publication_date";
	public static final String COLUMN_URI = "uri";
	public static final String COLUMN_TITLE = "title";
	public static final String COLUMN_CONTENT = "content";

	public RSSFeedItemTable(Context context) {
		super(context, StaticValues.DATABASE_NAME, null, StaticValues.DATABASE_VERSION);
	}

	@Override
	public void onCreate(SQLiteDatabase db) {
		StringBuilder builder = new StringBuilder();
		builder.append("create table if not exists " + TABLE_NAME + " (");
		builder.append(COLUMN_ID + " integer primary key autoincrement,");
		builder.append(COLUMN_FEED_ID + " integer,");
		builder.append(COLUMN_UNIQUE_ID + " text,");
		builder.append(COLUMN_PUBLICATION_DATE + " long,");
		builder.append(COLUMN_URI + " text,");
		builder.append(COLUMN_TITLE + " text,");
		builder.append(COLUMN_CONTENT + " text");
		builder.append(")");
		db.execSQL(builder.toString());
	}

	@Override
	public void onOpen(SQLiteDatabase db) {
		super.onOpen(db);
		onCreate(db);
	}

	@Override
	public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
		if (oldVersion == 1 && newVersion == 2) {
			db.execSQL("alter table " + TABLE_NAME + " add column (" + COLUMN_UNIQUE_ID + " text)");
			db.execSQL("delete from " + TABLE_NAME + " where " + COLUMN_UNIQUE_ID + " is null");
		}
	}

	public void addFeedItem(RSSFeed feed, RSSFeedItem item) {
		ContentValues values = new ContentValues();
		values.put(COLUMN_FEED_ID, feed.getId());
		values.put(COLUMN_UNIQUE_ID, item.getContent());
		values.put(COLUMN_PUBLICATION_DATE, item.getPublicationDate().getTime());
		values.put(COLUMN_URI, item.getUri().toString());
		values.put(COLUMN_TITLE, item.getTitle());
		values.put(COLUMN_CONTENT, item.getContent());

		SQLiteDatabase db = getWritableDatabase();
		item.setId(db.insert(TABLE_NAME, null, values));
		db.close();
	}

	public List<RSSFeedItem> getFeedItems(RSSFeed feed) {
		List<RSSFeedItem> items = new ArrayList<RSSFeedItem>();

		SQLiteDatabase db = getReadableDatabase();

		Cursor cursor = db.query(TABLE_NAME, new String[] { COLUMN_ID, COLUMN_PUBLICATION_DATE, COLUMN_URI, COLUMN_TITLE, COLUMN_CONTENT }, COLUMN_FEED_ID
				+ "=?", new String[] { String.valueOf(feed.getId()) }, null, null, COLUMN_PUBLICATION_DATE + " DESC");

		try {
			if (cursor != null) {
				while (cursor.moveToNext()) {
					RSSFeedItem item = new RSSFeedItem();
					item.setId(cursor.getInt(cursor.getColumnIndex(COLUMN_ID)));
					item.setPublicationDate(new Date(cursor.getLong(cursor.getColumnIndex(COLUMN_PUBLICATION_DATE))));
					item.setUri(Uri.parse(cursor.getString(cursor.getColumnIndex(COLUMN_URI))));
					item.setTitle(cursor.getString(cursor.getColumnIndex(COLUMN_TITLE)));
					item.setContent(cursor.getString(cursor.getColumnIndex(COLUMN_CONTENT)));
					items.add(item);
				}
			}
		} finally {
			cursor.close();
		}

		db.close();

		return items;
	}

	public Boolean hasFeedItem(RSSFeed feed, String uniqueId) {
		SQLiteDatabase db = getReadableDatabase();

		Cursor cursor = db.query(TABLE_NAME, null, COLUMN_FEED_ID + "=? and " + COLUMN_UNIQUE_ID + "=?",
				new String[] { String.valueOf(feed.getId()), uniqueId }, null, null, null);

		Boolean found = false;
		try {
			if (cursor != null)
				found = cursor.moveToNext();
		} finally {
			cursor.close();
		}

		db.close();

		return found;
	}

	public void cleanupExpired(RSSFeed feed, int period) {
		long expiredate = System.currentTimeMillis() - (period * 3600000);

		SQLiteDatabase db = getWritableDatabase();
		db.delete(TABLE_NAME, COLUMN_FEED_ID + "=? and " + COLUMN_PUBLICATION_DATE + "<?",
				new String[] { String.valueOf(feed.getId()), String.valueOf(expiredate) });
		db.close();
	}

	public void deleteFeedItems(RSSFeed feed) {
		SQLiteDatabase db = getWritableDatabase();
		db.delete(TABLE_NAME, COLUMN_FEED_ID + "=?", new String[] { String.valueOf(feed.getId()) });
		db.close();
	}
}
