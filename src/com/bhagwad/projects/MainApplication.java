package com.bhagwad.projects;

import android.app.Application;

public class MainApplication extends Application {

    /**
     * see NotePad tutorial for an example implementation of DataDbAdapter
     */
    private static FundsDb mDbHelper;
    
    /**
     * Called when the application is starting, before any other 
     * application objects have been created. Implementations 
     * should be as quick as possible...
     */
    @Override
    public void onCreate() {
        super.onCreate();
        mDbHelper = new FundsDb(this);
        mDbHelper.open();
    }

    /**
     * Called when the application is stopping. There are no more 
     * application objects running and the process will exit. 
     * Note: never depend on this method being called; in many 
     * cases an unneeded application process will simply be killed 
     * by the kernel without executing any application code...
     */
    @Override
    public void onTerminate() {
        super.onTerminate();
        mDbHelper.close();
        mDbHelper = null;
    }

    public static FundsDb getDatabaseHelper() {
        return mDbHelper;
    }
    
}