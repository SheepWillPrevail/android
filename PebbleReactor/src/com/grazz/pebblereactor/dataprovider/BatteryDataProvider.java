package com.grazz.pebblereactor.dataprovider;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.BatteryManager;

import com.grazz.pebblereactor.ReactorConstants;
import com.grazz.pebblereactor.DataProvider;
import com.grazz.pebblereactor.ReactorService;

public class BatteryDataProvider extends DataProvider {

	private BroadcastReceiver _batteryReceiver;

	public BatteryDataProvider(ReactorService reactor) {
		super(reactor);

		_batteryReceiver = new BroadcastReceiver() {

			@Override
			public void onReceive(Context context, Intent intent) {
				int level = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1);
				int scale = intent.getIntExtra(BatteryManager.EXTRA_SCALE, -1);
				setValue(ReactorConstants.REACTOR_PHONEBATTERYLEVEL, DataType.TYPE_UINT8, (int) (((float) level / scale) * 100), false);
			}

		};
	}

	@Override
	public void onStart() {
		IntentFilter filter = new IntentFilter(Intent.ACTION_BATTERY_CHANGED);
		getContext().registerReceiver(_batteryReceiver, filter);
		getContext().registerReceiver(null, filter); // get sticky
	}

	@Override
	public void onStop() {
		getContext().unregisterReceiver(_batteryReceiver);
	}

}
