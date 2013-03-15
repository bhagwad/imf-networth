package com.bhagwad.projects;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.regex.Pattern;
import android.app.IntentService;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.os.PowerManager;
import android.os.ResultReceiver;
import android.util.Log;

public class UpdateService extends IntentService {
	
	private boolean mSuccessStatus = true;
	private static PowerManager.WakeLock lockStatic = null;
	FundsDb forUpdation = MainApplication.getDatabaseHelper();
	
	private static final int MAX_BUFFER_SIZE = 81920;
	private static final int ESTIMATED_SIZE = 1304712;
	ResultReceiver progressReceiver;
	Bundle bundle = new Bundle();
	
	public static final String URL = "http://www.amfiindia.com/spages/NAV0.txt";
	public UpdateService() {
		super("UpdateService");
	
	}

	@Override
	protected void onHandleIntent(Intent intent) {
		
		// Pluck out the result receiver obtained via the intent
		progressReceiver = intent.getParcelableExtra("receiver");
		
		// Acquire and set the wakelock to prevent him from sleeping
		acquireWakeLock().acquire();
		
		// This try/catch loop is important to ensure that the wackelock is properly closed
		
		try {
			updateNavs();
		} finally {
		
	  		acquireWakeLock().release();
	  		stopSelf();
		}
	}
	
	private synchronized PowerManager.WakeLock acquireWakeLock() {
		
		// Get the wakelock
		if (lockStatic == null) {
			PowerManager mPowerManager = (PowerManager) getSystemService(Context.POWER_SERVICE);
			lockStatic = mPowerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "com.bhagwad.projects.Networth udpate wake lock");
			lockStatic.setReferenceCounted(true);
		}
		
		return lockStatic;
		
	}

	private void sendMessage(int percentageDone, String message) {
		
		if (progressReceiver!=null) {
			bundle.putString("message", message);
			progressReceiver.send(percentageDone, bundle);
		}
		
	}
	

	//Call this to update the database
  	public void updateNavs(){
  		
  		//First we have to get the data from the URL and covert it into a String
  		
  		StringBuffer stream = new StringBuffer();
  		  		
  		try {
			stream = downloadUrl(URL);
		} catch (IOException e1) {
			mSuccessStatus = false;
			sendFinalStatus();
			return;
		}
  		
  		
  		// Now that we have the string, start parsing it
  		
  		String navData = new String(stream.toString());
  		
  		// Break string into alternating chunks of fund houses and fund details
  		String chunks[] = navData.split("\r\n.\r\n");
  		int length = chunks.length;
  		Pattern newLineRegEx = Pattern.compile("\r\n");
  		Pattern semicolonSeparated = Pattern.compile(";");
  		
  		//forUpdation.open();
  		
  		
  		// Get the SqLite database so that we can use transactions - this is unusual
  		SQLiteDatabase mDb = forUpdation.getDataBaseHandle();
  		mDb.beginTransaction();
  		
  		// First delete all the existing data in the table
  		
  		mDb.delete(FundsDb.NAV_DATABASE_TABLE, null, null);
  		
  		try {

  			// We can get the fund house by reading the entry just before a details chunk
  	  		for (int i = 0; i < length; i++) {
  	              
  	  			// Check whether the current chunk is a batch of fund Entries.
  	  			// If not, get the next chunk
  	  			if (!Character.isDigit(chunks[i].charAt(0))) continue;
				
  	  			
  	  			//Each fund name is one a separate line. So first split up the block by newlines
  	  			String fundEntries[] = newLineRegEx.split(chunks[i]);
  	  			
  	  			// Now we have to extract the fundnames and the NAV. Split each line with semicolons
  	  			// and use the fourth and fifth elements to get them
  	  			for (int p=0;p<fundEntries.length;p++){
  	  				
  	  				String fundDetails []= semicolonSeparated.split(fundEntries[p]);
  	  				String fundName = fundDetails[3];
  	  				String fundHouse = chunks[i-1].toString();
  	  				double nav;
					try {
						nav = Double.parseDouble(fundDetails[4]);
					} catch (NumberFormatException e) {
						nav = 0;
						e.printStackTrace();
					}
  	  					
  	  				// Perform the insertions using the SqLite database directly
					// so we can make use of transactions
					
					SimpleDateFormat dateFormat = new SimpleDateFormat(FundsDb.DATE_TIME_FORMAT);
					Date currentDate = new Date();
					ContentValues initialValues = new ContentValues();

					initialValues.put(FundsDb.KEY_FUND_NAME, fundName);
					initialValues.put(FundsDb.KEY_FUND_HOUSE, fundHouse);
					initialValues.put(FundsDb.KEY_NAV, nav);
					initialValues.put(FundsDb.KEY_UPDATED_DATE, dateFormat.format(currentDate));
					
					mDb.insert(FundsDb.NAV_DATABASE_TABLE, null, initialValues);
					
					// Update the progress bar
					int percentageDone = (i*100)/length;
					sendMessage(percentageDone, "Processing...");
					
  	  			}
  	  			
  	          }
  	  		
  	  		// If the code reaches this point it means that all inserts have gone smoothly
  			mDb.setTransactionSuccessful();
  			
  		} catch (Exception e) {
  			
  			mSuccessStatus = false;
  		} finally {
  			
  			mDb.endTransaction();
  			
  		}
  		
  		sendFinalStatus();
  			
  		Utilities.updateNetWorthWidget(getApplicationContext());
  			
  			
  	}

  	private void sendFinalStatus() {
  		
  		String currentDateTime = Utilities.getCurrentDateTime();
  		
  		if (mSuccessStatus) {
  			
  			if (progressReceiver==null) {
  				// This is an autoupdate. Insert accordingly
  				forUpdation.insertUpdateStatus(currentDateTime, "Auto update Success");
  			} else forUpdation.insertUpdateStatus(currentDateTime, "Success");
  		} else {
  			forUpdation.insertUpdateStatus(currentDateTime, "Something went wrong");
  		}
  		
  		sendMessage(100, "Done");
		
	}

 	private StringBuffer downloadUrl(String urlString) throws IOException {

 		
 		URL url = new URL(urlString);
 		HttpURLConnection conn = (HttpURLConnection) url.openConnection();
 		conn.setReadTimeout(10000 /* milliseconds */);
 		conn.setConnectTimeout(15000 /* milliseconds */);
 		conn.setRequestMethod("GET");
 		conn.setDoInput(true);
 		// Starts the query
 		conn.connect();
 		int size = ESTIMATED_SIZE;
 		InputStream stream = conn.getInputStream();
 		int downloaded = 0;
 		byte buffer[];
 		StringBuffer streamBufferString = new StringBuffer();
 		buffer = new byte[MAX_BUFFER_SIZE]; 
 		while(true) {
 						
 			int read = stream.read(buffer);
 			if (read == -1) break;
 			streamBufferString.append(new String(buffer,0,read));
 			downloaded += read;
 			
 			// Make sure we multiply by 100 first because otherwise the int division will return zero
 			int percentageDone = (downloaded*100)/size;
 			sendMessage(percentageDone, "Downloading... Approx. 1 MB");
 		}
 		
 		// Check that at least something was downloaded
 		
 		if (downloaded < 1000) throw new IOException("Download problem");
 		
 		return streamBufferString;
 	}

}
