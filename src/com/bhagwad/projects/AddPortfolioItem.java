package com.bhagwad.projects;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.text.method.ScrollingMovementMethod;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FilterQueryProvider;
import android.widget.SimpleCursorAdapter;
import android.widget.SimpleCursorAdapter.CursorToStringConverter;
import android.widget.TextView;
import android.widget.Toast;

public class AddPortfolioItem extends Activity {
	
	private FundsDb mDbHelper;
	private Cursor fundsList;
	
	TextView txtFundName;
	TextView txtFundHouse;
	TextView txtNav;
	
	Button mSaveButton;
	EditText mQuantity;
	
	TextView txtQuantityText;
	
	@Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.portfolio_add);
        
        if (Utilities.isServiceRunning(this)){
			Intent i = new Intent(this,LoadingScreen.class);
			i.putExtra("redirected", true);
			startActivity(i);
		}
        
        //Initialize Everything
        txtFundName = (TextView) findViewById(R.id.selected_fund_name);
    	txtFundHouse= (TextView) findViewById(R.id.selected_fund_house);
    	txtNav= (TextView) findViewById(R.id.selected_nav);
    	txtQuantityText = (TextView)findViewById(R.id.quantity_text);
    	
    	mQuantity = (EditText) findViewById(R.id.enter_quantity);
    	mSaveButton = (Button)findViewById(R.id.save_entry);
    	
    	hideStuffWeDontNeed();
    	
    	
    	Button mGoBack = (Button) findViewById(R.id.button_go_home);
    	mGoBack.setOnClickListener(new View.OnClickListener() {
			
			@Override
			public void onClick(View v) {
				finish();
				
			}
		});
		
        
        //mDbHelper = new FundsDb(this);
        mDbHelper = MainApplication.getDatabaseHelper();
		
		        
        
    }
	
	private void hideStuffWeDontNeed() {
		txtQuantityText.setVisibility(View.INVISIBLE);
		mQuantity.setVisibility(View.INVISIBLE);
		mSaveButton.setVisibility(View.INVISIBLE);
		
	}

	public void setUpAutoCompleteCursor() {
		
		 // Create the SimpleCursorAdapter for easy fund name listing
        String[] from = new String[] {FundsDb.KEY_FUND_NAME};
        int[] to = new int[] {R.id.text1}; //This is the text view INSIDE the main layout
        
        
        fundsList  = mDbHelper.getAllFundNames();
        //startManagingCursor(fundsList);
        
        TextView fundsListItem = (TextView)findViewById(R.id.textView1);
        fundsListItem.setMovementMethod(new ScrollingMovementMethod());
        
        SimpleCursorAdapter adapter = new SimpleCursorAdapter(getApplicationContext(), R.layout.fund_name_row, fundsList, from, to);
        AutoCompleteTextView autocompleteText = (AutoCompleteTextView)findViewById(R.id.type_funds);
        autocompleteText.setAdapter(adapter);
        
       // This function extracts the string from the selected view
        adapter.setCursorToStringConverter(new CursorToStringConverter() {
		
		@Override
		public CharSequence convertToString(Cursor cursor) {
			
			return cursor.getString(cursor.getColumnIndexOrThrow(FundsDb.KEY_FUND_NAME));
		}
	});
        
        
        // This function is called when the text in the autocomplete changes
        // We have to filter the queries based on the stuff entered
        adapter.setFilterQueryProvider(new FilterQueryProvider() {
			
			@Override
			public Cursor runQuery(CharSequence constraint) {
				
				if (constraint!=null) {
					
					Cursor cursor = mDbHelper.getMatchingFundNames(constraint.toString());
					//startManagingCursor(cursor);
					return cursor;
				}
				
				return null;
			}
		});
        
        autocompleteText.setOnItemClickListener(new OnItemClickListener() {
            public void onItemClick(AdapterView<?> listView, View view, int position, long id) {
            	
            	//Show everything
            	showStuff();
            	
            	
            	// Set the textbox and display the details of the selection
            	Cursor mSelectedItem = (Cursor)listView.getItemAtPosition(position);
            	String mFundName = mSelectedItem.getString(mSelectedItem.getColumnIndexOrThrow(FundsDb.KEY_FUND_NAME));
            	String mFundHouse = mSelectedItem.getString(mSelectedItem.getColumnIndexOrThrow(FundsDb.KEY_FUND_HOUSE));
            	double mNav = mSelectedItem.getDouble(mSelectedItem.getColumnIndexOrThrow(FundsDb.KEY_NAV));
            	

            	
            	txtFundName.setText(mFundName);
            	txtFundHouse.setText(mFundHouse);
            	txtNav.setText("NAV: " + mNav);
            	
            	// Enable the save button
            	Button mSaveButton = (Button)findViewById(R.id.save_entry);
            	mSaveButton.setEnabled(true);
            	
            	
            }
        });
        
        mSaveButton.setOnClickListener(new View.OnClickListener() {
			
			@Override
			public void onClick(View v) {
				
				
				
				if(mQuantity.getText().toString().equals("")) {
					new AlertDialog.Builder( AddPortfolioItem.this )
					.setTitle( "" )
					.setMessage( "Enter the number of units" )
					.setPositiveButton( "Ok", new DialogInterface.OnClickListener() {
						public void onClick(DialogInterface dialog, int which) {
							

						}
					})
					.show();
					return;
					

				}
				
				// Insert the fund name and quantity into the database
				TextView txtFundName = (TextView)findViewById(R.id.selected_fund_name);
				double mUnits = Double.valueOf(mQuantity.getText().toString());
				String mFundName = txtFundName.getText().toString();
				
				mDbHelper.insertPortfolioItem(mFundName, mUnits);
				
				// Empty the textbox and the autocomplete
				
				emptyAutocompleteAndQuantity();
				
				
				Utilities.updateNetWorthWidget(AddPortfolioItem.this);
				txtFundName.setText("");
				txtFundHouse.setText("");
				txtNav.setText("");
				Toast.makeText(AddPortfolioItem.this, "Portfolio Saved", Toast.LENGTH_SHORT).show();
				
				//Hide everything we don't need once more
				hideStuffWeDontNeed();
				
			}

			private void emptyAutocompleteAndQuantity() {
				AutoCompleteTextView mSearch = (AutoCompleteTextView)findViewById(R.id.type_funds);
				EditText mQuantity = (EditText)findViewById(R.id.enter_quantity);
				
				mSearch.setText("");
				mSearch.requestFocus();
				
				mQuantity.setText("");
				
			}
		});
		
	}
	
	protected void showStuff() {
		txtQuantityText.setVisibility(View.VISIBLE);
		mQuantity.setVisibility(View.VISIBLE);
		mSaveButton.setVisibility(View.VISIBLE);
		
	}

	@Override
	protected void onPause(){
		super.onPause();
		//mDbHelper.close();
		finish();
		

	}
	
	protected void onResume(){
		super.onResume();
		//mDbHelper.open();
		setUpAutoCompleteCursor();
		
	}
	
}