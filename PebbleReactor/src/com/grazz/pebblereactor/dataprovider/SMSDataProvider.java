package com.grazz.pebblereactor.dataprovider;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.Cursor;
import android.net.Uri;

import com.grazz.pebblereactor.DataProvider;
import com.grazz.pebblereactor.ReactorConstants;
import com.grazz.pebblereactor.ReactorService;

public class SMSDataProvider extends DataProvider {

	public class SMSReceiver extends BroadcastReceiver {

		@Override
		public void onReceive(Context context, Intent intent) {
			setUnreadSmsCount(false);
		}

	}

	private BroadcastReceiver _receiver = new SMSReceiver();

	public SMSDataProvider(ReactorService reactor) {
		super(reactor);
	}

	@Override
	public void onStart() {
		getContext().registerReceiver(_receiver, new IntentFilter("android.provider.Telephony.SMS_RECEIVED"));
		setUnreadSmsCount(true);
	}

	@Override
	public void onStop() {
		getContext().unregisterReceiver(_receiver);
	}

	private void setUnreadSmsCount(Boolean initialScan) {
		int unreadCount = 0;
		String[] projection = new String[] { "read" };
		Cursor cursor = getContext().getContentResolver().query(Uri.parse("content://sms/inbox"), projection, null, null, null);
		while (cursor.moveToNext()) {
			int isread = cursor.getInt(cursor.getColumnIndex("read"));
			if (isread == 0)
				unreadCount++;
		}
		cursor.close();
		if (!initialScan)
			unreadCount++; // the new message is not stored yet
		setValue(ReactorConstants.REACTOR_NUMBERUNREADSMS, DataType.TYPE_UINT8, unreadCount, true);
	}

}
