package com.grazz.pebblereactor.dataprovider;

import android.content.Context;
import android.database.Cursor;
import android.provider.CallLog;
import android.provider.CallLog.Calls;
import android.telephony.PhoneStateListener;
import android.telephony.ServiceState;
import android.telephony.SignalStrength;
import android.telephony.TelephonyManager;

import com.grazz.pebblereactor.DataProvider;
import com.grazz.pebblereactor.ReactorConstants;
import com.grazz.pebblereactor.ReactorService;

public class PhoneStateDataProvider extends DataProvider {

	private TelephonyManager _manager;
	private PhoneStateListener _listener;

	public PhoneStateDataProvider(ReactorService reactor) {
		super(reactor);

		_listener = new PhoneStateListener() {

			private Boolean _wasRinging = false;

			@Override
			public void onCallStateChanged(int state, String incomingNumber) {
				if (_wasRinging) {
					_wasRinging = false;
					setMissedCallsCount();
				}

				if (state == TelephonyManager.CALL_STATE_RINGING)
					_wasRinging = true;
			}

			@Override
			public void onServiceStateChanged(ServiceState serviceState) {
				setValue(ReactorConstants.REACTOR_PHONERADIOSTATE, DataType.TYPE_UINT8, serviceState.getState(), true);
			}

			@Override
			public void onSignalStrengthsChanged(SignalStrength signalStrength) {
				if (signalStrength.isGsm()) {
					int strength = signalStrength.getGsmSignalStrength();
					int scaledStrength = (int) (((float) strength / 31) * 100);
					if (strength == 99)
						scaledStrength = 0;

					setValue(ReactorConstants.REACTOR_PHONESIGNALLEVEL, DataType.TYPE_UINT8, scaledStrength, false);
				}
			}

		};
	}

	@Override
	public void onStart() {
		_manager = (TelephonyManager) getContext().getSystemService(Context.TELEPHONY_SERVICE);
		_manager.listen(_listener, PhoneStateListener.LISTEN_CALL_STATE | PhoneStateListener.LISTEN_SERVICE_STATE | PhoneStateListener.LISTEN_SIGNAL_STRENGTHS);

		setValue(ReactorConstants.REACTOR_PHONENETWORKTYPE, DataType.TYPE_UINT8, _manager.getNetworkType(), false);
		setMissedCallsCount();
	}

	@Override
	public void onStop() {
		_manager.listen(_listener, PhoneStateListener.LISTEN_NONE);
	}

	private void setMissedCallsCount() {
		int missedCount = 0;
		String[] projection = new String[] { Calls.TYPE, Calls.NEW };
		Cursor cursor = getContext().getContentResolver().query(CallLog.Calls.CONTENT_URI, projection, null, null, null);
		while (cursor.moveToNext()) {
			int type = cursor.getInt(cursor.getColumnIndex(Calls.TYPE));
			int isnew = cursor.getInt(cursor.getColumnIndex(Calls.NEW));
			if (type == Calls.MISSED_TYPE && isnew == 1)
				missedCount++;
		}
		cursor.close();
		setValue(ReactorConstants.REACTOR_NUMBERMISSEDCALLS, DataType.TYPE_UINT8, missedCount, true);
	}

}
