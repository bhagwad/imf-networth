package com.bhagwad.projects;

import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;

public class onScheduleUpdate extends BroadcastReceiver {

	@Override
	public void onReceive(Context ctxt, Intent arg1) {
		
		FundsDb mDbHelper = MainApplication.getDatabaseHelper();
		//FundsDb mDbHelper = new FundsDb(ctxt);
		//mDbHelper.open();
		
		// Check the time since the last update. If it was less than 12 hours, don't do anything
		
		
		if (!Utilities.allowedToAccessNet(ctxt) || Utilities.isServiceRunning(ctxt)) {
			
			// If the network isn't present, enable the network receiver so that
			// it can be informed when it is. We can update from there.
			registerNetworkReceiver(ctxt);

			mDbHelper.insertUpdateStatus(Utilities.getCurrentDateTime(), "Scheduled update failed. No network");
			
		} else {
			
			if (Utilities.soonAfterPreviousAutoUpdate(ctxt)) {
				
				mDbHelper.insertUpdateStatus(Utilities.getCurrentDateTime(), "Scheduled. Too soon after previous update");
				//mDbHelper.close();
				return;
			}
			
			// Check if we're allowed to access the Internet or if another service is runnning
			
			Intent iUpdate = new Intent(ctxt, UpdateService.class);
			ctxt.startService(iUpdate);
		}
		
		//mDbHelper.close();
		return;
		
	}


	private void registerNetworkReceiver(Context ctxt) {
		ComponentName receiver = new ComponentName(ctxt, NetworkListener.class);
		
		PackageManager pm = ctxt.getPackageManager();
		pm.setComponentEnabledSetting(receiver, PackageManager.COMPONENT_ENABLED_STATE_ENABLED, PackageManager.DONT_KILL_APP);
		
	}

}
