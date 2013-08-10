package com.grazz.pebblerss.provider;

import java.util.ArrayList;
import java.util.List;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.net.Uri;

import com.grazz.pebblerss.StaticValues;

public class RSSFeedTable extends SQLiteOpenHelper {

	private static final int DATABASE_VERSION = 1;
	private static final String TABLE_NAME = "feed";

	public static final String COLUMN_ID = "_id";
	public static final String COLUMN_URI = "uri";
	public static final String COLUMN_NAME = "name";
	public static final String COLUMN_INTERVAL = "interval";
	public static final String COLUMN_LAST_UPDATE = "last_update";

	public RSSFeedTable(Context context) {
		super(context, StaticValues.DATABASE_NAME, null, DATABASE_VERSION);
	}

	@Override
	public void onCreate(SQLiteDatabase db) {
		StringBuilder builder = new StringBuilder();
		builder.append("create table if not exists " + TABLE_NAME + " (");
		builder.append(COLUMN_ID + " integer primary key autoincrement,");
		builder.append(COLUMN_URI + " text,");
		builder.append(COLUMN_NAME + " text,");
		builder.append(COLUMN_INTERVAL + " integer,");
		builder.append(COLUMN_LAST_UPDATE + " long");
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
	}

	public void addFeed(RSSFeed feed) {
		SQLiteDatabase db = getWritableDatabase();

		ContentValues values = new ContentValues();
		values.put(COLUMN_URI, feed.getUri().toString());
		values.put(COLUMN_NAME, feed.getName());
		values.put(COLUMN_INTERVAL, feed.getInterval());
		values.put(COLUMN_LAST_UPDATE, feed.getLastUpdated());

		feed.setId(db.insert(TABLE_NAME, null, values));
		db.close();
	}

	public List<RSSFeed> getFeeds() {
		List<RSSFeed> feeds = new ArrayList<RSSFeed>();

		Cursor cursor = getReadableDatabase().query(TABLE_NAME, new String[] { COLUMN_ID, COLUMN_URI, COLUMN_NAME, COLUMN_INTERVAL, COLUMN_LAST_UPDATE }, null,
				null, null, null, COLUMN_ID + " ASC");

		try {
			if (cursor != null) {
				while (cursor.moveToNext()) {
					RSSFeed feed = new RSSFeed();
					feed.setId(cursor.getInt(cursor.getColumnIndex(COLUMN_ID)));
					feed.setUri(Uri.parse(cursor.getString(cursor.getColumnIndex(COLUMN_URI))));
					feed.setName(cursor.getString(cursor.getColumnIndex(COLUMN_NAME)));
					feed.setInterval(cursor.getInt(cursor.getColumnIndex(COLUMN_INTERVAL)));
					feed.setLastUpdated(cursor.getLong(cursor.getColumnIndex(COLUMN_LAST_UPDATE)));
					feeds.add(feed);
				}
			}
		} finally {
			cursor.close();
		}

		return feeds;
	}

	public void updateFeed(RSSFeed feed) {
		SQLiteDatabase db = getWritableDatabase();

		ContentValues values = new ContentValues();
		values.put(COLUMN_URI, feed.getUri().toString());
		values.put(COLUMN_NAME, feed.getName());
		values.put(COLUMN_INTERVAL, feed.getInterval());
		values.put(COLUMN_LAST_UPDATE, feed.getLastUpdated());

		db.update(TABLE_NAME, values, COLUMN_ID + "=?", new String[] { String.valueOf(feed.getId()) });
		db.close();
	}

	public void removeFeed(RSSFeed feed) {
		SQLiteDatabase db = getWritableDatabase();
		db.delete(TABLE_NAME, COLUMN_ID + "=?", new String[] { String.valueOf(feed.getId()) });
		db.close();
	}
}
