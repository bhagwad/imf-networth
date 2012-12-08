package com.bhagwad.projects;

import android.os.Bundle;
import android.os.Handler;
import android.os.ResultReceiver;


/*
 * This is a result receiver that the update service will use to communicate with
 * any activity about the current percentage progress of the update as well as
 * the message text.
 * 
 *  We create an instance in the activity, and send it to the service in a bundle.
 *  The service then uses that instance to send back data in another bundle.
 *  
 *  We create a new interface declaration here so that the activity can implement it and
 *  the method of which will be called when the receiver gets a result.
 */

public class NavProgressResultReceiver extends ResultReceiver {
	
	private ReceiverInterface mReceiverInterface;
	
	public interface ReceiverInterface {
		public void updateProgress(int mProgress, String resultMessage);
	}

	public NavProgressResultReceiver(Handler handler) {
		super(handler);
		
	}
	
	public void setReceiver(ReceiverInterface receiver) {
		mReceiverInterface = receiver;
	}
	
	@Override
	protected void onReceiveResult(int resultCode, Bundle resultData) {
		
		String mMessage = resultData.getString("message");
		mReceiverInterface.updateProgress(resultCode, mMessage);
		
	}
	


}
