package com.grazz.pebblerss;

import static com.getpebble.android.kit.Constants.APP_UUID;

import java.util.UUID;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public class PassiveRSSDataReceiver extends BroadcastReceiver {

	public static final String DATA = "data";

	@Override
	public void onReceive(Context context, Intent intent) {
		UUID receivedUuid = (UUID) intent.getSerializableExtra(APP_UUID);
		if (!StaticValues.APP_UUID.equals(receivedUuid))
			return;

		Intent service = new Intent(context, RSSService.class);
		service.putExtra(DATA, intent);
		context.startService(service);
	}

}
