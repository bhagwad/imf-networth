package com.bhagwad.projects;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.text.DecimalFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

import android.app.ActivityManager;
import android.app.ActivityManager.RunningServiceInfo;
import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.preference.PreferenceManager;
import android.util.Log;
import android.widget.RemoteViews;

public class Utilities {

	static boolean isServiceRunning(Context ctxt) {

		ActivityManager manager = (ActivityManager) ctxt
				.getSystemService(Context.ACTIVITY_SERVICE);
		for (RunningServiceInfo service : manager
				.getRunningServices(Integer.MAX_VALUE)) {
			if ("com.bhagwad.projects.UpdateService".equals(service.service
					.getClassName())) {
				return true;
			}
		}
		return false;
	}

	public static boolean allowedToAccessNet(Context ctxt) {

		SharedPreferences sharedPrefs = PreferenceManager
				.getDefaultSharedPreferences(ctxt);
		boolean mobileDataAllowed = sharedPrefs
				.getBoolean("mobile_data", false);

		ConnectivityManager connManager = (ConnectivityManager) ctxt
				.getSystemService(Context.CONNECTIVITY_SERVICE);

		// No network service
		NetworkInfo networkInfo = connManager.getActiveNetworkInfo();

		if (networkInfo == null
				|| !connManager.getActiveNetworkInfo().isConnected()) {
			return false;
		}

		NetworkInfo mWifi = connManager
				.getNetworkInfo(ConnectivityManager.TYPE_WIFI);

		// Return false if mobile data is not allowed and wifi is not connected

		if (!mobileDataAllowed && !mWifi.isConnected()) {
			return false;
		}

		return true;

	}

	public static String getCurrentDateTime() {

		SimpleDateFormat mDateFormat = new SimpleDateFormat(
				"yyyy-MM-dd kk:mm:ss");
		Date mTodaysDate = new Date();

		return mDateFormat.format(mTodaysDate);
	}

	public static boolean soonAfterPreviousAutoUpdate(Context ctxt) {
		
		Date updatedDate = new Date();
		//FundsDb mDbHelper= new FundsDb(ctxt);
		FundsDb mDbHelper = MainApplication.getDatabaseHelper();
		//mDbHelper.open();
		
		Cursor updatedCursor = mDbHelper.getLastAutoUpdated();
		
		
		if (updatedCursor!=null) {
			
			updatedCursor.moveToFirst();
			
			if (updatedCursor.isAfterLast() == true) {
				//mDbHelper.close();
				updatedCursor.close();
				// Never updated so return false
				return false;
			}
			
			String dateString = updatedCursor.getString(updatedCursor.getColumnIndexOrThrow(FundsDb.KEY_NAV_UPDATEDON));
			
			SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd kk:mm:ss");
			
			
			try {
				updatedDate = dateFormat.parse(dateString);
			} catch (ParseException e) {
				
				e.printStackTrace();
			}
			
		} else 
			return false; //Nothing in the database go ahead
		
			
			
			updatedCursor.close();
			//mDbHelper.close();
			
			Date timeNow = new Date();
			
			Calendar lastUpdated = Calendar.getInstance();
			lastUpdated.setTime(updatedDate);
			
			Calendar todayCal = Calendar.getInstance();
			todayCal.setTime(timeNow);
			
			long diff = todayCal.getTimeInMillis() - lastUpdated.getTimeInMillis();
			
			if (diff / (60 * 60 * 1000)> 12) {
				return false;
			} 
			
			return true;
		
	}
	
	public static int calculateNetWorth(Context ctxt) {
		
		int netWorth = 0;
		
		//FundsDb mDbHelper = new FundsDb(ctxt);
		FundsDb mDbHelper = MainApplication.getDatabaseHelper();
		//mDbHelper.open();
		
		Cursor fundsList = mDbHelper.getPortfolioDetails();
		
		if(fundsList!=null) {
			fundsList.moveToFirst();
			
			while(fundsList.isAfterLast() == false) {
				double fundValue;
				double mNav;
				double mQuantity;
				
				mNav = fundsList.getDouble(fundsList.getColumnIndexOrThrow(FundsDb.KEY_NAV));
				mQuantity = fundsList.getDouble(fundsList.getColumnIndexOrThrow("totalunits"));
				
				fundValue = mNav*mQuantity;
				
				netWorth+=fundValue;
				fundsList.moveToNext();
				
			}
			fundsList.close();
			//mDbHelper.close();
			
		}
		
		return netWorth;
		
	}
	
	public static void updateNetWorthWidget(Context ctxt) {
		
		
		int mNetWorth = Utilities.calculateNetWorth(ctxt);
		DecimalFormat formatNetWorth = new DecimalFormat("##,##,##,###");
		
		
		RemoteViews updateViews = new RemoteViews(ctxt.getPackageName(), R.layout.widget_layout);
		updateViews.setTextViewText(R.id.widget_text, "Net Worth:\nRs. "
				+ formatNetWorth.format(mNetWorth));
		
		ComponentName thisOne = new ComponentName(ctxt, WidgetProvider.class);
		AppWidgetManager mgr = AppWidgetManager.getInstance(ctxt);
		
		// Create the Intent that will be sent on widget click
		
		Intent i = new Intent(ctxt, PortfolioList.class);
		i.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
		PendingIntent pi = PendingIntent.getActivity(ctxt, 0, i, 0);
		
		updateViews.setOnClickPendingIntent(R.id.widget_text, pi);
		
		mgr.updateAppWidget(thisOne, updateViews);
		
	}

	public static void exportDatabaseFile(String mFileString) {
		
		File mExportFile = new File (mFileString);
		try {
			
			OutputStream myOutput = new FileOutputStream(mExportFile);
			FileInputStream mFileInputStream = new FileInputStream(FundsDb.DB_PATH+FundsDb.DATABASE_NAME);
			
			byte[] buffer = new byte[1024];
	        int length;
	        while ((length = mFileInputStream.read(buffer)) > 0) {
	        	myOutput.write(buffer, 0, length);
	        }
			
	        myOutput.close();
	        myOutput.flush();
	        mFileInputStream.close();
	        Log.e ("Debug", "file export completed");
	        
		} catch (Exception e) {
			
			e.printStackTrace();
		}
		
	}

	public static void writeFromInputToOutput(InputStream myInput, OutputStream myOutput) throws IOException  {
		
		byte[] buffer = new byte[1024];
        int length;
        while ((length = myInput.read(buffer)) > 0) {
                myOutput.write(buffer, 0, length);
        }
        
        myOutput.close();
        myOutput.flush();
        myInput.close();
	}

}
