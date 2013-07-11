package com.grazz.pebblereactor;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;

public class ConfigureActivity extends Activity {

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_configure);

		if (!StaticCommands.isServiceRunning(this) && StaticCommands.shouldStartService(this))
			StaticCommands.startService(this);

		CheckBox service = (CheckBox) findViewById(R.id.cbEnableService);
		service.setChecked(StaticCommands.isServiceRunning(this));
		service.setOnCheckedChangeListener(new OnCheckedChangeListener() {
			@Override
			public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
				SharedPreferences preferences = getApplicationContext().getSharedPreferences(StaticValues.SHARED_PREFERENCE_NAME, Context.MODE_PRIVATE);
				Editor editor = preferences.edit();
				editor.putBoolean(StaticValues.PREFERENCE_SERVICEENABLED, isChecked);
				editor.commit();

				if (isChecked)
					StaticCommands.startService(getApplicationContext());
				else
					StaticCommands.stopService(getApplicationContext());
			}
		});
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		getMenuInflater().inflate(R.menu.configure, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case R.id.action_about:
			startActivity(new Intent(this, AboutActivity.class));
			break;
		}
		return true;
	}

}
