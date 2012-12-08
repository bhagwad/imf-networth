package com.bhagwad.projects;

import java.util.Calendar;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceChangeListener;
import android.preference.PreferenceActivity;
import android.preference.PreferenceManager;

public class PortfolioPreferences extends PreferenceActivity{
	
	Context ctxt = PortfolioPreferences.this;
	
	@Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        // Load the preferences from an XML resource
        addPreferencesFromResource(R.xml.portfolio_preferences);
        
        Preference chooseTime= findPreference("time_pick");
        Preference autoUpdate = findPreference("automatic_update");
        
        // This is what happens when we actually set the date
        chooseTime.setOnPreferenceChangeListener(new OnPreferenceChangeListener() {
			
			@Override
			public boolean onPreferenceChange(Preference preference, Object newValue) {
				
					// Create the recurring alarm
					Calendar updateTime = Calendar.getInstance();
					
					// Set the calendar object to what we  just got
					updateTime.setTimeInMillis((Long)newValue);
					
					Intent fireToReceivers = new Intent(ctxt, onScheduleUpdate.class);
					PendingIntent pi = PendingIntent.getBroadcast(ctxt, 0, fireToReceivers, PendingIntent.FLAG_CANCEL_CURRENT);
					
					AlarmManager recurring = (AlarmManager)ctxt.getSystemService(Context.ALARM_SERVICE);
					
					// First cancel everything
					recurring.cancel(pi);
					
					recurring.setInexactRepeating(AlarmManager.RTC, updateTime.getTimeInMillis(), AlarmManager.INTERVAL_DAY, pi);
					
					// Set the receivers back to their original status
					NetworkListener.disableNetworkReceiver(ctxt);
					return true;
			}
		});
        
        // When the user changes the "Automatic update" field
        
        autoUpdate.setOnPreferenceChangeListener(new OnPreferenceChangeListener() {
			
			@Override
			public boolean onPreferenceChange(Preference preference, Object newValue) {
				
				// If the new value is "checked", then create a schedule with the existing
				// value in the data picker
				
				Intent fireToReceivers = new Intent(ctxt, onScheduleUpdate.class);
				PendingIntent pi = PendingIntent.getBroadcast(ctxt, 0, fireToReceivers, PendingIntent.FLAG_CANCEL_CURRENT);
				
				
			
				
				if ((Boolean)newValue) {
					
					SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(ctxt);
					long timeFromPicker = prefs.getLong("time_pick", 0);
					Calendar updateTime = Calendar.getInstance();
					updateTime.setTimeInMillis(timeFromPicker);
					
					if (timeFromPicker!=0) {
						
						AlarmManager recurring = (AlarmManager)ctxt.getSystemService(Context.ALARM_SERVICE);
						recurring.setInexactRepeating(AlarmManager.RTC, updateTime.getTimeInMillis(), AlarmManager.INTERVAL_DAY, pi);
						
					}
					
				} else {
					
					// Cancel the recurring alarm
					AlarmManager cancelAlarm= (AlarmManager)ctxt.getSystemService(Context.ALARM_SERVICE);
					cancelAlarm.cancel(pi);
					
				}
				
				// Set the receivers back to their original status
				NetworkListener.disableNetworkReceiver(ctxt);
				return true;
			}
		});
        
        
    }

}