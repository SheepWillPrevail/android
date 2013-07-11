package com.grazz.pebblereactor;

import java.util.LinkedList;
import java.util.List;
import java.util.UUID;

import org.json.JSONException;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.os.IBinder;
import android.support.v4.app.NotificationCompat;
import android.util.Log;
import android.util.SparseArray;

import com.getpebble.android.kit.PebbleKit;
import com.getpebble.android.kit.util.PebbleDictionary;
import com.grazz.pebblereactor.dataprovider.BatteryDataProvider;
import com.grazz.pebblereactor.dataprovider.PhoneStateDataProvider;
import com.grazz.pebblereactor.dataprovider.LocationDataProvider;
import com.grazz.pebblereactor.dataprovider.LogDataProvider;
import com.grazz.pebblereactor.dataprovider.SMSDataProvider;

public class ReactorService extends Service {

	public class PromiscuousPebbleDataReceiver extends BroadcastReceiver {

		public void onReceive(Context context, Intent intent) {
			UUID uuid = (UUID) intent.getSerializableExtra("uuid");

			if (!uuid.toString().startsWith(ReactorConstants.REACTOR_UUIDPREFIX))
				return;

			registerApp(uuid);

			int transactionId = intent.getIntExtra("transaction_id", -1);
			String jsonData = intent.getStringExtra("msg_data");
			if (jsonData == null || jsonData.isEmpty())
				return;

			try {
				PebbleDictionary data = PebbleDictionary.fromJson(jsonData);
				Log.d("receiveData", data.toJsonString());
				if (shouldCommunicate()) {
					if (data.contains(ReactorConstants.REACTOR_COMMAND)) {
						int command = data.getUnsignedInteger(ReactorConstants.REACTOR_COMMAND).intValue();

						if (command == ReactorConstants.REACTOR_COMMAND_REFRESH) {

							PebbleDictionary dictionary = new PebbleDictionary();

							for (DataProvider provider : _providers)
								provider.addValuesToDictionary(dictionary);

							Log.d("onCommandRefresh", dictionary.toJsonString());
							PebbleKit.sendDataToPebble(context, _lastKnownWatch, dictionary);

						} else { // other command

							DataProvider provider = _providerCommands.get(command);
							if (provider != null)
								provider.onCommandReceived(command);

						}

						PebbleKit.sendAckToPebble(context, transactionId);
					} else
						PebbleKit.sendNackToPebble(context, transactionId);
				}
			} catch (JSONException e) {
				e.printStackTrace();
			}
		}

	}

	public class WakeupReceiver extends BroadcastReceiver {

		private List<Integer> _queuedSlotIds = new LinkedList<Integer>();

		@Override
		public void onReceive(Context context, Intent intent) {
			sendSlots(context, _queuedSlotIds);
		}

		public void onQueueSlot(Context context, int slotId, Boolean forcePush) {
			if (!_queuedSlotIds.contains(slotId)) {
				if (forcePush) {
					List<Integer> slotIds = new LinkedList<Integer>();
					slotIds.add(slotId);
					sendSlots(context, slotIds);
				} else
					_queuedSlotIds.add(slotId);
			}
		}

		private void sendSlots(Context context, List<Integer> slotIds) {
			if (shouldCommunicate()) {
				PebbleDictionary dictionary = new PebbleDictionary();

				for (DataProvider provider : _providers)
					provider.addValuesToDictionary(dictionary, slotIds);

				if (dictionary.size() > 0) {
					Log.d("sendUpdate", dictionary.toJsonString());
					PebbleKit.sendDataToPebble(context, _lastKnownWatch, dictionary);
				}

				slotIds.clear();
			}
		}

	}

	private PromiscuousPebbleDataReceiver _pebbleReceiver = new PromiscuousPebbleDataReceiver();
	private List<DataProvider> _providers = new LinkedList<DataProvider>();
	private SparseArray<DataProvider> _providerCommands = new SparseArray<DataProvider>();

	private final long WAKEUP_INTERVAL = 5 * 60 * 1000;
	private WakeupReceiver _wakeupReceiver = new WakeupReceiver();
	private String _wakeupAction = WakeupReceiver.class.getName();

	private PendingIntent _wakeupIntent;
	private AlarmManager _alarmManager;
	private UUID _lastKnownWatch;

	@Override
	public IBinder onBind(Intent intent) {
		throw new UnsupportedOperationException();
	}

	@Override
	public void onCreate() {
		super.onCreate();

		SharedPreferences preferences = getSharedPreferences(StaticValues.SHARED_PREFERENCE_NAME, Context.MODE_PRIVATE);
		String uuid = preferences.getString(StaticValues.PREFERENCE_LASTUUID, "");
		if (uuid.length() > 0) {
			_lastKnownWatch = UUID.fromString(uuid);
			Log.d("onCreate", String.format("Last seen app: %s", uuid));
		}

		registerReceiver(_wakeupReceiver, new IntentFilter(_wakeupAction));
		registerReceiver(_pebbleReceiver, new IntentFilter("com.getpebble.action.app.RECEIVE"));

		registerProvider(new BatteryDataProvider(this));
		registerProvider(new PhoneStateDataProvider(this));
		registerProvider(new SMSDataProvider(this));
		registerProvider(new LogDataProvider(this));
		registerProvider(new LocationDataProvider(this));

		_wakeupIntent = PendingIntent.getBroadcast(this, 0, new Intent(_wakeupAction), PendingIntent.FLAG_CANCEL_CURRENT);
		_alarmManager = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
		_alarmManager.setInexactRepeating(AlarmManager.RTC_WAKEUP, System.currentTimeMillis(), WAKEUP_INTERVAL, _wakeupIntent);

		NotificationCompat.Builder builder = new NotificationCompat.Builder(this);
		builder.setContentIntent(PendingIntent.getActivity(this, 0, new Intent(this, ConfigureActivity.class), 0));
		builder.setContentTitle(getResources().getString(R.string.app_name));
		builder.setContentText(getResources().getString(R.string.msg_service_running));
		builder.setSmallIcon(R.drawable.notification);
		startForeground(1, builder.build());
	}

	@Override
	public void onDestroy() {
		super.onDestroy();

		_alarmManager.cancel(_wakeupIntent);

		for (DataProvider provider : _providers)
			provider.onStop();

		unregisterReceiver(_pebbleReceiver);
		unregisterReceiver(_wakeupReceiver);

		stopForeground(true);
	}

	public void registerApp(UUID uuid) {
		_lastKnownWatch = uuid;

		SharedPreferences preferences = getSharedPreferences(StaticValues.SHARED_PREFERENCE_NAME, Context.MODE_PRIVATE);
		Editor editor = preferences.edit();
		editor.putString(StaticValues.PREFERENCE_LASTUUID, _lastKnownWatch.toString());
		editor.commit();
	}

	public void registerCommand(int command, DataProvider provider) {
		_providerCommands.put(command, provider);
	}

	private void registerProvider(DataProvider provider) {
		_providers.add(provider);
		provider.onStart();
	}

	public void requestPush(int slotId, Boolean forcePush) {
		_wakeupReceiver.onQueueSlot(this, slotId, forcePush);
	}

	private Boolean shouldCommunicate() {
		if (PebbleKit.isWatchConnected(this) && PebbleKit.areAppMessagesSupported(this) && _lastKnownWatch != null)
			return true;
		return false;
	}

}
