package com.grazz.pebblereactor;

import android.app.ActivityManager;
import android.app.ActivityManager.RunningServiceInfo;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;

public class StaticCommands {
	public static Boolean isServiceRunning(Context context) {
		ActivityManager manager = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
		for (RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)) {
			if (ReactorService.class.getName().equals(service.service.getClassName()))
				return true;
		}
		return false;
	}

	public static Boolean shouldStartService(Context context) {
		SharedPreferences preferences = context.getSharedPreferences(StaticValues.SHARED_PREFERENCE_NAME, Context.MODE_PRIVATE);
		return preferences.getBoolean(StaticValues.PREFERENCE_SERVICEENABLED, false);
	}

	public static void startService(Context context) {
		context.startService(new Intent(context, ReactorService.class));
	}

	public static void stopService(Context context) {
		context.stopService(new Intent(context, ReactorService.class));
	}
}
