package com.bhagwad.projects;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.Date;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public class FundsDb {

	public static final String DATABASE_NAME = "data";
	public static final String NAV_DATABASE_TABLE = "nav_details";
	private static final String PORTFOLIO_DATABASE_TABLE = "portfolio_details";
	private static final String LATEST_UPDATED_TABLE = "latest_update_details";
	private static final String FULL_PORTFOLIO_VIEW = "total_fund_details";
	private static final int DATABASE_VERSION = 1;
	
	public final static String DB_PATH = "/data/data/com.bhagwad.projects/databases/";

	public static final String KEY_FUND_NAME = "key_fund_name";
	public static final String KEY_FUND_HOUSE = "key_fund_house";
	public static final String KEY_NAV = "key_nav";
	public static final String KEY_QUANTITY = "key_quantity";
	public static final String KEY_UPDATED_DATE = "key_updated_date";
	public static final String KEY_ROWID = "_id";
	public static final String KEY_NAV_UPDATEDON = "updated_on";
	public static final String KEY_UPDATESTATUS = "status";
	
	public static final String DATE_TIME_FORMAT = "dd-MM-yyyy";

	private static final String LATEST_NAV_DATABASE_CREATE = "create table "
			+ NAV_DATABASE_TABLE + " (" + KEY_ROWID + " INTEGER PRIMARY KEY, "
			+ KEY_FUND_NAME + " text not null, " + KEY_FUND_HOUSE
			+ " text not null, " + KEY_UPDATED_DATE + " text not null, " + KEY_NAV
			+ " real not null);";

	private static final String PORTFOLIO_DATABASE_CREATE = "create table "
			+ PORTFOLIO_DATABASE_TABLE + " ("
			+ KEY_ROWID + " INTEGER PRIMARY KEY, "
			+ KEY_FUND_NAME + " text not null, "
			+ KEY_QUANTITY + " real not null);";
	
	private static final String FULL_PORTFOLIO_VIEW_CREATE = "CREATE VIEW "+ FULL_PORTFOLIO_VIEW + " AS SELECT navtable.key_fund_name, portfolio.totalunits, navtable.key_nav, navtable.key_fund_house FROM nav_details navtable INNER JOIN (SELECT key_fund_name, SUM(key_quantity) AS totalunits FROM  portfolio_details GROUP BY key_fund_name) portfolio ON navtable.key_fund_name = portfolio.key_fund_name";
	
	public static final String LATEST_UPDATED_CREATE = "create table "
			+ LATEST_UPDATED_TABLE + "( " 
			+ KEY_ROWID + " INTEGER PRIMARY KEY, "
			+ KEY_NAV_UPDATEDON + " text not null, "
			+ KEY_UPDATESTATUS + " text not null);";
			
			
	private DatabaseHelper mDbHelper;
	private SQLiteDatabase mDb;

	private final Context context;

	public FundsDb(Context ctx) {
		this.context = ctx;
	}

	private static class DatabaseHelper extends SQLiteOpenHelper {
		
		// We need to create a default database in case one doesn't exist
		
		
		
		private boolean doesDatabaseExist() {
			
			File dBFile = new File(DB_PATH+DATABASE_NAME);
			return dBFile.exists();
			
		}
		
		private void copyDatabase(Context ctxt) throws IOException{
			
			InputStream myInput = ctxt.getAssets().open(DATABASE_NAME);
			String outputFilePath = DB_PATH+DATABASE_NAME;
			OutputStream myOutput = new FileOutputStream(outputFilePath);
			
			Utilities.writeFromInputToOutput(myInput, myOutput);
			
			
	        
		}
		
		DatabaseHelper(Context context) {
			super(context, DATABASE_NAME, null, DATABASE_VERSION);
		}

		@Override
		public void onCreate(SQLiteDatabase db) {
			db.execSQL(LATEST_NAV_DATABASE_CREATE);
			db.execSQL(PORTFOLIO_DATABASE_CREATE);
			db.execSQL(FULL_PORTFOLIO_VIEW_CREATE);
			db.execSQL(LATEST_UPDATED_CREATE);
		}
		


		@Override
		public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
//			db.execSQL("DROP TABLE IF EXISTS nav_details");
//	        // do same for other two tables
//	        onCreate(db);
		}
	}

	public FundsDb open() throws SQLException {

		try{
			
			mDbHelper = new DatabaseHelper(context);
			
			if(!mDbHelper.doesDatabaseExist()) {
				mDb = mDbHelper.getWritableDatabase();
				mDb.close();
				mDbHelper.copyDatabase(context);
				
			}
			
		} catch (Exception e){
			e.printStackTrace();
			
		}
		mDb = mDbHelper.getWritableDatabase();
		return this;

	}
	
	// This is meant for the "update" service so it can perform massive database inserts
	// using transactions.
	public SQLiteDatabase getDataBaseHandle(){
		return mDb;
	}

	public void close() {

		mDbHelper.close();
	}

	// Get a cursor with all the funds, nav and updated date
	public Cursor getPortfolioDetails() {
		return mDb.query(FULL_PORTFOLIO_VIEW, new String[] {KEY_FUND_NAME, "totalunits", KEY_NAV, KEY_FUND_HOUSE}, null, null, null, null, null, null);


	}

	// Get a cursor with details for a specific fund
	public Cursor getNavForFund(String fundName) {
		return null;

	}

	public long insertLatestNav(String fundName, String fundHouse, double nav) {

		SimpleDateFormat dateFormat = new SimpleDateFormat(DATE_TIME_FORMAT);
		Date currentDate = new Date();
		ContentValues initialValues = new ContentValues();

		initialValues.put(KEY_FUND_NAME, fundName);
		initialValues.put(KEY_FUND_HOUSE, fundHouse);
		initialValues.put(KEY_NAV, nav);
		initialValues.put(KEY_UPDATED_DATE, dateFormat.format(currentDate));
		
		return mDb.insert(NAV_DATABASE_TABLE, null, initialValues);

	}
	
	public long insertPortfolioItem(String fundName, double quantity) {

		ContentValues initialValues = new ContentValues();

		initialValues.put(KEY_FUND_NAME, fundName);
		initialValues.put(KEY_QUANTITY, quantity);
		return mDb.insert(PORTFOLIO_DATABASE_TABLE, null, initialValues);

	}
	
	public Cursor getAllFundNames() {
		
		return mDb.query(NAV_DATABASE_TABLE, new String[] {KEY_ROWID ,KEY_FUND_NAME}, null, null, null, null, null);
	}
	
	public Cursor getMatchingFundNames(String matchPattern) {
		
		String query = "SELECT _id, key_fund_name, key_fund_house, key_nav FROM nav_details WHERE key_fund_name LIKE ? AND key_fund_name NOT IN (SELECT key_fund_name FROM portfolio_details);";
		
		Cursor cursor = mDb.rawQuery(query, new String[] {"%" + matchPattern + "%"});
		
		if(cursor!=null){
			cursor.moveToFirst();
			return cursor;
		}
		 return null;
	}
	
	public boolean UpdatePortfolioItem(String fundName, double quantity) {

		ContentValues initialValues = new ContentValues();

		initialValues.put(KEY_QUANTITY, quantity);
		return mDb.update(PORTFOLIO_DATABASE_TABLE, initialValues, KEY_FUND_NAME + "=?", new String[] {fundName}) > 0;

	}
	
	public boolean deletePortfolioItem(String fundName) {
		
		return mDb.delete(PORTFOLIO_DATABASE_TABLE, KEY_FUND_NAME + "=?", new String[] {fundName}) > 0;
		
	}
	
	public long insertUpdateStatus(String updatedDate, String status) {

		ContentValues initialValues = new ContentValues();

		initialValues.put(KEY_NAV_UPDATEDON, updatedDate);
		initialValues.put(KEY_UPDATESTATUS, status);
		return mDb.insert(LATEST_UPDATED_TABLE, null, initialValues);

	}
	
	public Cursor getLastUpdated() {

		return mDb.query(LATEST_UPDATED_TABLE, new String[] { KEY_ROWID,
				KEY_NAV_UPDATEDON, KEY_UPDATESTATUS }, KEY_UPDATESTATUS
				+ " LIKE ?", new String[] { "%Success%" }, null, null,
				KEY_ROWID + " DESC", "1");
	}

	public Cursor getLastAutoUpdated() {

		return mDb.query(LATEST_UPDATED_TABLE, new String[] { KEY_ROWID,
				KEY_NAV_UPDATEDON, KEY_UPDATESTATUS }, KEY_UPDATESTATUS
				+ " = ?", new String[] { "Auto update Success" }, null, null,
				KEY_ROWID + " DESC", "1");
	}
	
	public boolean isDatabaseLocked() {
		
		if(mDb.isDbLockedByOtherThreads() || mDb.isDbLockedByCurrentThread()) {
			return true;
		} else return false;
		
		
	}
	
}
