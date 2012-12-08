package com.bhagwad.projects;

import java.util.Calendar;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

public class BootReceiver extends BroadcastReceiver {

	@Override
	public void onReceive(Context ctxt, Intent intent) {
		
		// Check if updates are scheduled and if so, create the recurring alarms
		
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(ctxt);
		
		// Do nothing if the automatic update is not enabled
		if (!prefs.getBoolean("automatic_update", false)) {
			return;
		}
		
		Calendar updateTime = Calendar.getInstance();
		updateTime.setTimeInMillis(prefs.getLong("time_pick", 0));
		
		// Set the recurring alarms
		
		Intent fireToReceivers = new Intent(ctxt, onScheduleUpdate.class);
		PendingIntent pi = PendingIntent.getBroadcast(ctxt, 0, fireToReceivers, PendingIntent.FLAG_CANCEL_CURRENT);
		
		AlarmManager recurring = (AlarmManager)ctxt.getSystemService(Context.ALARM_SERVICE);
		recurring.setInexactRepeating(AlarmManager.RTC, updateTime.getTimeInMillis(), AlarmManager.INTERVAL_DAY, pi);

	}

}
