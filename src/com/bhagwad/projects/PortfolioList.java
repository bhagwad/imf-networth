package com.bhagwad.projects;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.text.DecimalFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.app.ListFragment;
import android.support.v4.view.MenuCompat;
import android.text.format.DateUtils;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.animation.AccelerateInterpolator;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.view.animation.Animation.AnimationListener;
import android.view.animation.AnimationSet;
import android.view.animation.ScaleAnimation;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView.AdapterContextMenuInfo;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import com.bhagwad.projects.NavProgressResultReceiver.ReceiverInterface;

public class PortfolioList extends FragmentActivity implements
		ReceiverInterface {

	ProgressDialog dialog;
	public static final int REQUEST_SAVE = 0;
	public static final int REQUEST_LOAD = 1;

	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		// Since this main activity has almost no content of its own, we don't
		// have to
		// do anything here. List Fragment is associated with its class in the
		// xml file
		setContentView(R.layout.main);
		RatingHelper.app_launched(this);
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {

		switch (item.getItemId()) {

		case R.id.menu_update_navs:

			updateNavFromScreen();

			return true;

		case R.id.menu_add_portfolio:
			Intent iAdd = new Intent(this, AddPortfolioItem.class);
			startActivityForResult(iAdd, 1);
			return true;

		case R.id.menu_preferences:
			Intent iPreferences = new Intent(this, PortfolioPreferences.class);
			startActivity(iPreferences);
			return true;

		case R.id.menu_export:
			Intent i = new Intent(this, FileDialog.class);
			i.putExtra(FileDialog.CAN_SELECT_DIR, false);
			i.putExtra(FileDialog.START_PATH, "/sdcard");
			i.putExtra(FileDialog.SELECTION_MODE, SelectionMode.MODE_CREATE);
			startActivityForResult(i, REQUEST_SAVE);

			return true;

		case R.id.menu_import:
			
			new AlertDialog.Builder(this)
			.setTitle("")
			.setMessage("If you import a corrupted or incorrect file, the app will crash. Be careful!")
			.setPositiveButton("Ok",
					new DialogInterface.OnClickListener() {
						public void onClick(
								DialogInterface dialog,
								int which) {
							
							Intent mImport = new Intent(PortfolioList.this, FileDialog.class);
							mImport.putExtra(FileDialog.CAN_SELECT_DIR, false);
							mImport.putExtra(FileDialog.START_PATH, "/sdcard");
							mImport.putExtra(FileDialog.SELECTION_MODE, SelectionMode.MODE_OPEN);
							mImport.putExtra(FileDialog.FORMAT_FILTER, new String[] { "bhag" });
							startActivityForResult(mImport, REQUEST_LOAD);

						}
					}).show();

			
			return true;
		}

		return false;

	}

	// Returning back from the choose or file dialog.
	
	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {

		if (resultCode != RESULT_OK)
			return;
		
		String mFileString;
		File mSelectedFile;
		
		if (requestCode == REQUEST_SAVE) {
			
			new AlertDialog.Builder(this)
			.setTitle("")
			.setMessage("Don't modify the contents of the export file!")
			.setPositiveButton("Ok",
					new DialogInterface.OnClickListener() {
						public void onClick(
								DialogInterface dialog,
								int which) {
							

						}
					}).show();

			try {
				
				// Append .bhag to a file that doesn't end with .bhag so that the user
				// only select those files that are exported.
				
				if (!data.getStringExtra(FileDialog.RESULT_PATH).endsWith(".bhag"))
					mFileString = data.getStringExtra(FileDialog.RESULT_PATH) + ".bhag";
				else
					mFileString = data.getStringExtra(FileDialog.RESULT_PATH);
				
				mSelectedFile = new File(mFileString);
				
				OutputStream myOutput = new FileOutputStream(mSelectedFile, false);
				FileInputStream mFileInputStream = new FileInputStream(
						FundsDb.DB_PATH + FundsDb.DATABASE_NAME);

				Utilities.writeFromInputToOutput(mFileInputStream, myOutput);

			} catch (Exception e) {
				e.printStackTrace();
			}

		} else if (requestCode == REQUEST_LOAD) {

			try {
				
				mFileString = data.getStringExtra(FileDialog.RESULT_PATH);
				mSelectedFile = new File(mFileString);
				
				OutputStream myOutput = new FileOutputStream(FundsDb.DB_PATH + FundsDb.DATABASE_NAME);
				FileInputStream mDatabaseInputStream = new FileInputStream(mSelectedFile);
				
				Utilities.writeFromInputToOutput(mDatabaseInputStream, myOutput);
				
				
			} catch (Exception e) {
				
				e.printStackTrace();
			}

		}
		
		PortfolioListFragment f = (PortfolioListFragment) getSupportFragmentManager()
				.findFragmentById(R.id.portfolio_list_holder);
		if (f != null) {
			f.fillData();
		}

	}

	public void updateNavFromScreen() {
		// Check if we're allowed to access the Internet here

		if (!Utilities.allowedToAccessNet(this)) {

			new AlertDialog.Builder(this)
					.setTitle("")
					.setMessage("Check your Internet settings")
					.setPositiveButton("Ok",
							new DialogInterface.OnClickListener() {
								public void onClick(DialogInterface dialog,
										int which) {

								}
							}).show();

			return;

		}

		// If an existing service is running, exit

		if (Utilities.isServiceRunning(this)) {
			Toast.makeText(this, "Update already in progress...",
					Toast.LENGTH_SHORT).show();
			return;
		}

		// Lock the orientation for the duration of the service
		// Set up the result receiver and initiate the dialog

		dialog = new ProgressDialog(this);
		dialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
		dialog.setMessage("");
		dialog.setCancelable(false);
		dialog.show();

		NavProgressResultReceiver receiver = new NavProgressResultReceiver(
				new Handler());
		receiver.setReceiver(this);

		Intent iUpdate = new Intent(this, UpdateService.class);
		iUpdate.putExtra("receiver", receiver);
		startService(iUpdate);

	}

	@SuppressWarnings("deprecation")
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {

		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.action_menu, menu);
		MenuCompat.setShowAsAction(menu.findItem(R.id.menu_add_portfolio), 1);
		return true;
	}

	public static class PortfolioListFragment extends ListFragment {

		private ArrayList<PortfolioListEntry> entries = null;
		private PortfolioListAdapter adapter;
		boolean mDualPane;

		FundsDb mDbHelper;

		/** Called when the activity is first created. */
		@Override
		public void onActivityCreated(Bundle savedInstanceState) {
			super.onActivityCreated(savedInstanceState);

			if (Utilities.isServiceRunning(getActivity())) {
				Intent i = new Intent(getActivity(), LoadingScreen.class);
				i.putExtra("redirected", true);
				getActivity().startActivity(i);

			}

			entries = new ArrayList<PortfolioListEntry>();
			adapter = new PortfolioListAdapter(getActivity()
					.getApplicationContext(), R.layout.portfolio_list_item,
					entries);
			setListAdapter(adapter);

			setEmptyText("Add Portfolio items from the menu");

			mDbHelper = MainApplication.getDatabaseHelper();

			registerForContextMenu(getListView());

			// Let's see whether we're using the portrait or the landscape
			// layout
			View portfolioDetailsFrame = getActivity().findViewById(
					R.id.portfolio_details_frame);
			mDualPane = portfolioDetailsFrame != null
					&& portfolioDetailsFrame.getVisibility() == View.VISIBLE;

		}

		@Override
		public void onResume() {
			super.onResume();

			if (!Utilities.isServiceRunning(getActivity())) {
				fillData();
			}
		}

		// This will show the details pane regardless of orientation
		private void showPortfolioDetails(String mFundName, String mFundHouse,
				double mNav, double mQuantity) {

			// Are we in landscape or portrait
			if (mDualPane) {

				// Is some other details pane already showing?
				PortfolioDetailsFragment fragment = (PortfolioDetailsFragment) getFragmentManager()
						.findFragmentById(R.id.portfolio_details_frame);
				if (fragment == null
						|| !fragment.getFundName().equals(mFundName)) {

					// Create a new fragment and attach it in
					fragment = PortfolioDetailsFragment.newInstance(mFundName,
							mFundHouse, mNav, mQuantity);
					FragmentTransaction ft = getFragmentManager()
							.beginTransaction();

					// Replace is important instead of add otherwise the one
					// fragment will
					// sit on one another and you won't be able to see anything
					ft.replace(R.id.portfolio_details_frame, fragment);
					ft.setTransition(FragmentTransaction.TRANSIT_FRAGMENT_FADE);
					ft.commit();
				}
			} else {

				// We're in portrait mode. Start up the activity
				Intent i = new Intent();

				// Put in the arguments to help the new activity get started
				i.putExtra(FundsDb.KEY_FUND_NAME, mFundName);
				i.putExtra(FundsDb.KEY_FUND_HOUSE, mFundHouse);
				i.putExtra(FundsDb.KEY_NAV, mNav);
				i.putExtra(FundsDb.KEY_QUANTITY, mQuantity);
				i.setClass(getActivity(), PortfolioDetailsActivity.class);
				startActivity(i);

			}

		}

		// Create an array of portfolio items and populate the array adapter
		protected void fillData() {

			entries.clear();

			String fundName;
			String fundHouse;
			double totalUnits;
			double latestNav;
			int mNetWorth = 0;

			Cursor fundsList = mDbHelper.getPortfolioDetails();

			if (fundsList != null) {
				fundsList.moveToFirst();

				while (fundsList.isAfterLast() == false) {

					PortfolioListEntry portfolioItem = new PortfolioListEntry();

					fundName = fundsList.getString(fundsList
							.getColumnIndexOrThrow(FundsDb.KEY_FUND_NAME));
					fundHouse = fundsList.getString(fundsList
							.getColumnIndexOrThrow(FundsDb.KEY_FUND_HOUSE));
					latestNav = fundsList.getDouble(fundsList
							.getColumnIndexOrThrow(FundsDb.KEY_NAV));
					totalUnits = fundsList.getDouble(fundsList
							.getColumnIndexOrThrow("totalunits"));

					portfolioItem.setFundName(fundName);
					portfolioItem.setFundHouse(fundHouse);
					portfolioItem.setNav(latestNav);
					portfolioItem.setQuantity(totalUnits);

					mNetWorth += totalUnits * latestNav;

					// Add this item to the array of portfolio entries
					entries.add(portfolioItem);
					fundsList.moveToNext();
				}
				fundsList.close();
				updateNetWorthAndUpdated(mNetWorth);

			}

			// Update the adapter
			adapter.notifyDataSetChanged();

		}

		private void updateNetWorthAndUpdated(int mNetWorth) {

			// Update the net worth
			DecimalFormat formatNetWorth = new DecimalFormat("##,##,##,###");

			TextView txtNetWorth = (TextView) getActivity().findViewById(
					R.id.net_worth);
			txtNetWorth.setText("Net Worth = Rs."
					+ formatNetWorth.format(mNetWorth));

			// Insert the last udpated date into the textbox

			Cursor updatedCursor = mDbHelper.getLastUpdated();

			if (updatedCursor != null) {
				updatedCursor.moveToFirst();

				TextView txtUpdatedTime = (TextView) getActivity()
						.findViewById(R.id.updated_on);

				if (updatedCursor.isAfterLast() == true) {

					txtUpdatedTime.setText("Last updated: Never");

					new AlertDialog.Builder(getActivity())
							.setTitle("")
							.setMessage("Use Menu option to update the NAVs")
							.setPositiveButton("Ok",
									new DialogInterface.OnClickListener() {
										public void onClick(
												DialogInterface dialog,
												int which) {

										}
									}).show();

				} else {

					String dateString = updatedCursor.getString(updatedCursor
							.getColumnIndexOrThrow(FundsDb.KEY_NAV_UPDATEDON));

					SimpleDateFormat dateFormat = new SimpleDateFormat(
							"yyyy-MM-dd kk:mm:ss");
					Date updatedDate = null;

					try {
						updatedDate = dateFormat.parse(dateString);
					} catch (ParseException e) {
						
						e.printStackTrace();
					}

					String elapsedTime = (String) DateUtils
							.getRelativeDateTimeString(getActivity(),
									updatedDate.getTime(),
									DateUtils.MINUTE_IN_MILLIS,
									DateUtils.WEEK_IN_MILLIS, 0);

					txtUpdatedTime.setText("Last updated: " + elapsedTime);

				}

			}

		}

		protected void showAlertDialog(String message) {

			new AlertDialog.Builder(getActivity())
					.setTitle("")
					.setMessage(message)
					.setPositiveButton("Ok",
							new DialogInterface.OnClickListener() {
								public void onClick(DialogInterface dialog,
										int which) {

								}
							}).show();

		}

		public void onListItemClick(ListView l, View v, int position, long id) {

			// We receive the view that was clicked "v". Using this we can
			// extract the
			// details of the funds etc. Easier to pass along all the values
			// rather than
			// querying the database separately

			String mFundName;
			String mFundHouse;
			double mNav;
			double mQuantity;

			TextView txtFundName = (TextView) v.findViewById(R.id.fund_name);
			TextView txtFundHouse = (TextView) v.findViewById(R.id.fund_house);
			TextView txtFundNav = (TextView) v.findViewById(R.id.latest_nav);
			TextView txtFundQuantity = (TextView) v.findViewById(R.id.quantity);

			mFundName = txtFundName.getText().toString();
			mFundHouse = txtFundHouse.getText().toString();
			mNav = Double.valueOf(txtFundNav.getText().toString());
			mQuantity = Double.valueOf(txtFundQuantity.getText().toString());

			showPortfolioDetails(mFundName, mFundHouse, mNav, mQuantity);

		}

		@Override
		public void onCreateContextMenu(ContextMenu menu, View v,
				ContextMenuInfo menuInfo) {
			super.onCreateContextMenu(menu, v, menuInfo);
			MenuInflater mi = new MenuInflater(getActivity());
			mi.inflate(R.menu.long_press_key, menu);
		}

		@Override
		public boolean onContextItemSelected(MenuItem item) {

			switch (item.getItemId()) {
			case R.id.delete_menu_item:

				AdapterContextMenuInfo adapterInfo = (AdapterContextMenuInfo) item
						.getMenuInfo();
				View v = adapterInfo.targetView;
				TextView txtFundName = (TextView) v
						.findViewById(R.id.fund_name);
				String fundName = txtFundName.getText().toString();

				mDbHelper.deletePortfolioItem(fundName);

				animateDelete(v);

				// Update the widget
				Utilities.updateNetWorthWidget(getActivity());

				// Detach the details fragment so it's pristine again

				if (mDualPane) {
					Fragment f = getFragmentManager().findFragmentById(
							R.id.portfolio_details_frame);
					FragmentTransaction fTransaction = getFragmentManager()
							.beginTransaction();
					fTransaction.detach(f);
					fTransaction.commit();
				}

				break;

			}

			return super.onContextItemSelected(item);
		}

		private void animateDelete(View v) {

			AnimationSet mAnimationSet = new AnimationSet(true);
			mAnimationSet.setDuration(1500);

			AlphaAnimation mAlphaAnimation = new AlphaAnimation(1, 0);
			mAlphaAnimation.setDuration(1500);
			mAnimationSet.addAnimation(mAlphaAnimation);

			v.startAnimation(mAnimationSet);

			mAnimationSet.setAnimationListener(new AnimationListener() {

				@Override
				public void onAnimationStart(Animation animation) {
					

				}

				@Override
				public void onAnimationRepeat(Animation animation) {
				

				}

				@Override
				public void onAnimationEnd(Animation animation) {
					fillData();

				}
			});

		}

		private class PortfolioListAdapter extends
				ArrayAdapter<PortfolioListEntry> {

			private final ArrayList<PortfolioListEntry> items;
			private Context context;
			int resource;

			public PortfolioListAdapter(Context context,
					int textViewResourceId,
					ArrayList<PortfolioListEntry> objects) {
				super(context, textViewResourceId, objects);
				this.items = objects;
				this.context = context;
				resource = textViewResourceId;
			}

			@Override
			/*
			 * The system calls this method when it wants to display a new list
			 * item. It passes the view that it's going to use to display it.
			 * It's our job to put the appropriate information in the view
			 * Sometimes, earlier views are reused, so we check to see if it's
			 * null before inflating it up. Otherwise resources are wasted.
			 */
			public View getView(int position, View convertView, ViewGroup parent) {

				final View v;

				// Get the details of the item we're about to display
				PortfolioListEntry entry = items.get(position);

				if (convertView == null) {
					LayoutInflater li = (LayoutInflater) context
							.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
					v = li.inflate(resource, null);

				} else {
					v = convertView;
				}

				TextView fundName = (TextView) v.findViewById(R.id.fund_name);
				TextView fundHouse = (TextView) v.findViewById(R.id.fund_house);
				TextView latestNav = (TextView) v.findViewById(R.id.latest_nav);
				TextView quantity = (TextView) v.findViewById(R.id.quantity);
				TextView mTextViewFundValue = (TextView) v
						.findViewById(R.id.total_fund_value);

				fundName.setText(entry.getFundName());
				fundHouse.setText(entry.getFundHouse());
				latestNav.setText(String.valueOf(entry.getNav()));
				quantity.setText(String.valueOf(entry.getQuantity()));

				double mTotalFundValue = entry.getNav() * entry.getQuantity();
				DecimalFormat formatIndian = new DecimalFormat("##,##,##,###");
				mTextViewFundValue.setText("Value: Rs. "
						+ formatIndian.format(mTotalFundValue));

				return v;
			}

		}

		// Class representing a single portfolio entry
		private class PortfolioListEntry {

			private String fundName;
			private String fundHouse;
			private double quantity;
			private double nav;

			public String getFundName() {
				return fundName;
			}

			public String getFundHouse() {
				return fundHouse;
			}

			public double getNav() {
				return nav;
			}

			public double getQuantity() {
				return quantity;
			}

			public void setFundName(String fundName) {
				this.fundName = fundName;
			}

			public void setFundHouse(String fundHouse) {
				this.fundHouse = fundHouse;
			}

			public void setNav(double nav) {
				this.nav = nav;
			}

			public String toString() {
				return "Fund Name: " + fundName + "\n" + fundHouse + "\n NAV: "
						+ nav;
			}

			public void setQuantity(double quantity) {
				this.quantity = quantity;
			}

		}

	}

	public static class PortfolioDetailsFragment extends Fragment {

		String mFundName;
		String mFundHouse;
		double mNav;
		double mQuantity;

		// Returns a new instance of this fragment with all the details needed
		// These arguments are passed along to every new details fragment
		// greated
		public static PortfolioDetailsFragment newInstance(String fundName,
				String fundHouse, double nav, double quantity) {

			PortfolioDetailsFragment f = new PortfolioDetailsFragment();
			Bundle args = new Bundle();
			args.putString(FundsDb.KEY_FUND_NAME, fundName);
			args.putString(FundsDb.KEY_FUND_HOUSE, fundHouse);
			args.putDouble(FundsDb.KEY_NAV, nav);
			args.putDouble(FundsDb.KEY_QUANTITY, quantity);

			f.setArguments(args);
			return f;
		}

		public String getFundName() {
			return getArguments().getString(FundsDb.KEY_FUND_NAME);
		}

		@Override
		public View onCreateView(LayoutInflater inflater, ViewGroup container,
				Bundle savedInstanceState) {
			// Means that the fragment shouldn't be displayed cause it's in
			// portrait mode
			// and the parent viewgroup doesn't exist

			if (container == null) {
				return null;
			}
			// Create the view that we want, populate it and return it
			View v = inflater.inflate(R.layout.portfolio_details, container,
					false);
			TextView txtFundName = (TextView) v
					.findViewById(R.id.details_fund_name);
			TextView txtFundHouse = (TextView) v
					.findViewById(R.id.details_fund_house);
			TextView txtFundNav = (TextView) v
					.findViewById(R.id.details_fund_nav);
			TextView txtFundQuantity = (TextView) v
					.findViewById(R.id.details_fund_quantity);

			txtFundName
					.setText(getArguments().getString(FundsDb.KEY_FUND_NAME));
			txtFundHouse.setText(getArguments().getString(
					FundsDb.KEY_FUND_HOUSE));
			txtFundNav.setText(String.valueOf(getArguments().getDouble(
					FundsDb.KEY_NAV)));
			txtFundQuantity.setText(String.valueOf(getArguments().getDouble(
					FundsDb.KEY_QUANTITY)));

			/*
			 * Set the onClicks for the radio buttons and the save button. It's
			 * irritating to have to do it this way instead of setting "onClick"
			 * in xml, but fragments are fucked up
			 */

			setOnClicksForRadioButtons(v);
			setOnClickForSaveButton(v);

			return v;

		}

		private void setOnClickForSaveButton(View v) {

			Button changeFundsSave = (Button) v
					.findViewById(R.id.change_funds_save);
			changeFundsSave.setOnClickListener(saveListener);

		}

		private void setOnClicksForRadioButtons(View v) {

			RadioButton rb1 = (RadioButton) v.findViewById(R.id.add_units);
			RadioButton rb2 = (RadioButton) v.findViewById(R.id.remove_units);
			RadioButton rb3 = (RadioButton) v.findViewById(R.id.set_units);

			rb1.setOnClickListener(addRemoveSetRadioListener);
			rb2.setOnClickListener(addRemoveSetRadioListener);
			rb3.setOnClickListener(addRemoveSetRadioListener);

		}

		protected void showAlertDialog(String message) {

			new AlertDialog.Builder(getActivity())
					.setTitle("")
					.setMessage(message)
					.setPositiveButton("Ok",
							new DialogInterface.OnClickListener() {
								public void onClick(DialogInterface dialog,
										int which) {

								}
							}).show();

		}

		// All the heavy work is happening here

		Button.OnClickListener saveListener = new Button.OnClickListener() {

			@Override
			public void onClick(View v) {

				// Get the rootview
				View rootView = (View) v.getRootView();
				EditText changeFundsEdit = (EditText) rootView
						.findViewById(R.id.change_funds_edit);

				// Make sure there's a value in the edit text field
				if (changeFundsEdit.getText().toString().equals("")) {

					showAlertDialog("Enter the number of units");
					return;

				}

				// Get all the relevant values
				TextView txtQuantity = (TextView) rootView
						.findViewById(R.id.details_fund_quantity);

				String fundName = getArguments().getString(
						FundsDb.KEY_FUND_NAME); // txtFundName.getText().toString();
				double existingQuantity = Double.valueOf(txtQuantity.getText()
						.toString());
				double quantityEntered = Double.valueOf(changeFundsEdit
						.getText().toString());

				// Grab the radio group to find out which radio button was
				// clicked
				RadioGroup rbChoice = (RadioGroup) rootView
						.findViewById(R.id.add_remove_set_funds);
				FundsDb mDbHelper = MainApplication.getDatabaseHelper();

				// Initialize the new quantity
				double newQuantity = 0;

				// Depending on which button was clicked, do the stuff we need
				switch (rbChoice.getCheckedRadioButtonId()) {

				case R.id.add_units:

					newQuantity = existingQuantity + quantityEntered;

					mDbHelper.UpdatePortfolioItem(fundName, newQuantity);
					break;

				case R.id.remove_units:

					newQuantity = existingQuantity - quantityEntered;

					// Check to see if they're trying to remove too many units

					if ((existingQuantity - quantityEntered) < 0) {
						showAlertDialog("You can't remove so many units");

					}

					mDbHelper.UpdatePortfolioItem(fundName, existingQuantity
							- quantityEntered);
					break;

				case R.id.set_units:

					newQuantity = quantityEntered;

					mDbHelper.UpdatePortfolioItem(fundName, quantityEntered);
					break;

				}

				// Update the values of this fragment
				txtQuantity.setText(String.valueOf(newQuantity));
				changeFundsEdit.setText("");
				Toast.makeText(getActivity(), "Quantity Updated",
						Toast.LENGTH_SHORT).show();

				// Animate the views so that users get visual feedback that
				// something has changed
				doAnimations(txtQuantity);

				// Update the listview if we're in landscape mode
				PortfolioListFragment refreshList = (PortfolioListFragment) getFragmentManager()
						.findFragmentById(R.id.portfolio_list_holder);
				if (refreshList != null) {
					refreshList.fillData();
				}

				// Update the widget
				Utilities.updateNetWorthWidget(getActivity());

			}

			private void doAnimations(View mViewToScale) {

				ScaleAnimation mScaleAnimation = new ScaleAnimation(1, 3, 1, 3,
						mViewToScale.getWidth() / 2,
						mViewToScale.getHeight() / 2);
				mScaleAnimation
						.setInterpolator(new AccelerateInterpolator(2.0f));
				mScaleAnimation.setFillAfter(true);
				mScaleAnimation.setDuration(350);

				mScaleAnimation.setRepeatCount(1);
				mScaleAnimation.setRepeatMode(Animation.REVERSE);

				mViewToScale.setAnimation(mScaleAnimation);

			}
		};

		OnClickListener addRemoveSetRadioListener = new OnClickListener() {

			@Override
			public void onClick(View v) {

				// Since we only have the radiobutton view, we need to get the
				// parent
				View rootView = (View) v.getRootView();

				// Make the controls visible
				TextView changeFundsText = (TextView) rootView
						.findViewById(R.id.change_funds_text);
				EditText changeFundsEdit = (EditText) rootView
						.findViewById(R.id.change_funds_edit);
				Button changeFundsSave = (Button) rootView
						.findViewById(R.id.change_funds_save);

				changeFundsText.setVisibility(View.VISIBLE);
				changeFundsEdit.setVisibility(View.VISIBLE);
				changeFundsSave.setVisibility(View.VISIBLE);

				switch (v.getId()) {
				case R.id.add_units:
					changeFundsText
							.setText("How many units do you want to add?");
					break;
				case R.id.remove_units:
					changeFundsText
							.setText("How many units do you want to remove?");
					break;
				case R.id.set_units:
					changeFundsText.setText("Set number of units:");
					break;
				}

				InputMethodManager imm = (InputMethodManager) getActivity()
						.getSystemService(Context.INPUT_METHOD_SERVICE);
				imm.showSoftInput(changeFundsEdit,
						InputMethodManager.SHOW_IMPLICIT);

			}

		};

	}

	public static class PortfolioDetailsActivity extends FragmentActivity {
		public void onCreate(Bundle savedInstanceState) {
			super.onCreate(savedInstanceState);

			// Set the actionbar navigation
			// ActionBar actionBar = getActionBar();
			// actionBar.setDisplayHomeAsUpEnabled(true);

			// We can exit when in landscape cause it's handled by a different
			// code

			// We need to show the portfolio details fragment here
			if (savedInstanceState == null) {
				PortfolioDetailsFragment fragment = new PortfolioDetailsFragment();

				// Pass along the Intent and all all the associated arguments
				// that brought us here
				fragment.setArguments(getIntent().getExtras());
				getSupportFragmentManager().beginTransaction()
						.replace(android.R.id.content, fragment).commit();

			}
		}

		protected void onPause() {
			super.onPause();
			finish();
		}

	}

	@Override
	public void updateProgress(int resultCode, String resultMessage) {

		if (resultMessage.equals("Done")) {

			// This will screw up when the orientation changes, so handle it!

			try {
				if (dialog.isShowing()) {
					dialog.dismiss();

					// Refresh the list
					PortfolioListFragment f = (PortfolioListFragment) getSupportFragmentManager()
							.findFragmentById(R.id.portfolio_list_holder);
					if (f != null) {
						f.fillData();
					}

					return;

				}
			} catch (Exception e) {

				e.printStackTrace();
			}

		}

		dialog.setMessage(resultMessage);
		dialog.setProgress(resultCode);

	}

}