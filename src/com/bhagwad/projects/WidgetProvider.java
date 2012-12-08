package com.bhagwad.projects;

import android.app.IntentService;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.Context;
import android.content.Intent;

public class WidgetProvider extends AppWidgetProvider {

	public void onUpdate(Context context, AppWidgetManager appWidgetManager,
			int[] appWidgetIds) {

		Intent i = new Intent(context, UpdateNetWorth.class);
		context.startService(i);

	}

	public static class UpdateNetWorth extends IntentService {

		public UpdateNetWorth() {
			super("WidgetProvider$UpdateNetWorth");

		}

		@Override
		protected void onHandleIntent(Intent intent) {

			Utilities.updateNetWorthWidget(this);

		}

	}

}
