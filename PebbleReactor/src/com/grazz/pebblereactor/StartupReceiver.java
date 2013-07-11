package com.grazz.pebblereactor;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public class StartupReceiver extends BroadcastReceiver {

	@Override
	public void onReceive(Context context, Intent intent) {
		if (StaticCommands.shouldStartService(context))
			StaticCommands.startService(context);
	}

}
