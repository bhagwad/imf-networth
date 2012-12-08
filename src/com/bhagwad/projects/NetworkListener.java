package com.bhagwad.projects;

import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;

public class NetworkListener extends BroadcastReceiver {

	@Override
	public void onReceive(Context ctxt, Intent arg1) {
		
		// Check if we're allowed to access the Internet or if another service
		// is runnning

		FundsDb mDbHelper = MainApplication.getDatabaseHelper();
		//FundsDb mDbHelper = new FundsDb(ctxt);
		//mDbHelper.open();
		


		if (!Utilities.allowedToAccessNet(ctxt) || Utilities.isServiceRunning(ctxt)) {
			
			mDbHelper.insertUpdateStatus(Utilities.getCurrentDateTime(), "Network state changed. No network");
			
		} else {
			
			// Check the time since the last update. If it was less than 12 hours, don't do anything
			
			if (Utilities.soonAfterPreviousAutoUpdate(ctxt)) {
				
				mDbHelper.insertUpdateStatus(Utilities.getCurrentDateTime(), "Network. Too soon after previous update");
				//mDbHelper.close();
				disableNetworkReceiver(ctxt);
				return;
			}
			
			Intent iUpdate = new Intent(ctxt, UpdateService.class);
			ctxt.startService(iUpdate);
			
			// Disable the network receiver now and enable the regular scheduled update receiver
			disableNetworkReceiver(ctxt);
		}
		
		//mDbHelper.close();
		return;

	}

	public static void disableNetworkReceiver(Context ctxt) {
		ComponentName receiver = new ComponentName(ctxt, NetworkListener.class);

		PackageManager pm = ctxt.getPackageManager();
		pm.setComponentEnabledSetting(receiver,
				PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
				PackageManager.DONT_KILL_APP);
		
	}

}
