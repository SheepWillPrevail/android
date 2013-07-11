package com.grazz.pebblereactor.dataprovider;

import android.content.Context;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;

import com.grazz.pebblereactor.ReactorConstants;
import com.grazz.pebblereactor.DataProvider;
import com.grazz.pebblereactor.ReactorService;

public class LocationDataProvider extends DataProvider {

	private LocationManager _manager;
	private LocationListener _locationListener;

	public LocationDataProvider(ReactorService reactor) {
		super(reactor);
		registerCommand(ReactorConstants.REACTOR_COMMAND_QUERYGPS);

		_locationListener = new LocationListener() {

			@Override
			public void onStatusChanged(String provider, int status, Bundle extras) {
			}

			@Override
			public void onProviderEnabled(String provider) {
			}

			@Override
			public void onProviderDisabled(String provider) {
			}

			@Override
			public void onLocationChanged(Location location) {
				setValue(ReactorConstants.REACTOR_GPSLATITUDE, DataType.TYPE_UINT32, location.getLatitude() * 1000, true);
				setValue(ReactorConstants.REACTOR_GPSLONGITUDE, DataType.TYPE_UINT32, location.getLongitude() * 1000, true);
				onStop();
			}
		};

		_manager = (LocationManager) getContext().getSystemService(Context.LOCATION_SERVICE);
	}

	@Override
	public void onCommandReceived(int commandId) {
		super.onCommandReceived(commandId);
		if (commandId == ReactorConstants.REACTOR_COMMAND_QUERYGPS)
			_manager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, _locationListener);
	}

	@Override
	public void onStart() {
	}

	@Override
	public void onStop() {
		_manager.removeUpdates(_locationListener);
	}

}
