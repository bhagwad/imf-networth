package com.bhagwad.projects;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;

public class LoadingScreen extends Activity {

	private ProgressDialog pd = null;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		setContentView(R.layout.loading_layout);
	
	}

	@Override
	public void onResume() {
		super.onResume();

		if (!Utilities.isServiceRunning(this)) {

			startActivity(new Intent(this, PortfolioList.class));

		}

		pd = ProgressDialog.show(this, "Sit tight!", "NAV Update in Progress");

		new UpdateChecker().execute();

	}

	private class UpdateChecker extends AsyncTask<Void, Void, Object> {

		@Override
		protected Object doInBackground(Void... arg0) {

			while (Utilities.isServiceRunning(LoadingScreen.this)) {

				try {
					Thread.sleep(2000);
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}

			}

			return null;
		}

		protected void onPostExecute(Object result) {
			if (LoadingScreen.this.pd != null) {

				try {
					LoadingScreen.this.pd.dismiss();
				} catch (Exception e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}

			}

			// Check if we came here via a redirect. If yes, then finish this
			// otherwise start
			// something new

			if (getIntent().getBooleanExtra("redirected", false)) {
				finish();
			} else {
				LoadingScreen.this.startActivity(new Intent(LoadingScreen.this,
						PortfolioList.class));
			}
		}

	}

}
