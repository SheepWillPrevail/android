package com.grazz.pebblereactor.dataprovider;

import java.util.LinkedList;
import java.util.List;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.TrafficStats;

import com.grazz.pebblereactor.DataProvider;
import com.grazz.pebblereactor.ReactorConstants;
import com.grazz.pebblereactor.ReactorService;

public class LogDataProvider extends DataProvider {

	public abstract class ScheduledTask {

		public abstract void doExecute();

	}

	public class WakeupReceiver extends BroadcastReceiver {

		private List<ScheduledTask> _tasks = new LinkedList<ScheduledTask>();

		public void addTask(ScheduledTask task) {
			_tasks.add(task);
		}

		@Override
		public void onReceive(Context context, Intent intent) {
			for (ScheduledTask task : _tasks)
				task.doExecute();
		}

	}

	private final long WAKEUP_INTERVAL = AlarmManager.INTERVAL_FIFTEEN_MINUTES;
	private WakeupReceiver _wakeupReceiver = new WakeupReceiver();
	private String _wakeupAction = WakeupReceiver.class.getName();

	private PendingIntent _wakeupIntent;
	private AlarmManager _alarmManager;

	public LogDataProvider(ReactorService reactor) {
		super(reactor);

		_wakeupReceiver.addTask(new ScheduledTask() {

			@Override
			public void doExecute() {
				setValue(ReactorConstants.REACTOR_NUMBERCELLRX, DataType.TYPE_UINT32, TrafficStats.getMobileRxBytes(), false);
				setValue(ReactorConstants.REACTOR_NUMBERCELLTX, DataType.TYPE_UINT32, TrafficStats.getMobileTxBytes(), false);
			}

		});
	}

	@Override
	public void onStart() {
		getContext().registerReceiver(_wakeupReceiver, new IntentFilter(_wakeupAction));
		_wakeupIntent = PendingIntent.getBroadcast(getContext(), 0, new Intent(_wakeupAction), PendingIntent.FLAG_CANCEL_CURRENT);
		_alarmManager = (AlarmManager) getContext().getSystemService(Context.ALARM_SERVICE);
		_alarmManager.setInexactRepeating(AlarmManager.RTC_WAKEUP, System.currentTimeMillis(), WAKEUP_INTERVAL, _wakeupIntent);
	}

	@Override
	public void onStop() {
		_alarmManager.cancel(_wakeupIntent);
		getContext().unregisterReceiver(_wakeupReceiver);
	}

}
