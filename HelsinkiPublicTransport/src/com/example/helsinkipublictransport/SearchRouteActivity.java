package com.example.helsinkipublictransport;

import android.app.Activity;
import android.app.DatePickerDialog;
import android.app.Dialog;
import android.app.TimePickerDialog;
import android.content.Intent;
import android.content.IntentSender;
import android.graphics.Color;
import android.location.Location;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.FragmentActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnFocusChangeListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.RadioButton;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesClient;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.android.gms.location.LocationClient;

public class SearchRouteActivity extends FragmentActivity implements
		GooglePlayServicesClient.ConnectionCallbacks,
		GooglePlayServicesClient.OnConnectionFailedListener {
	
	private final static String DEBUG_TAG = "SearchRouteActivity";
	public final static String LIST_ROUTES_EXTRA = "routes";
	public final static int SAVED_JOURNEYS_REQUEST_CODE = 1;
	public final static int CURRENT_LOCATION_REQUEST_CODE = 2;
	public final static int CONNECTION_FAILURE_RESOLUTION_REQUEST = 3;
	
	// User variables
	private boolean userLogged;
	private String accessToken;
	
	// Views
	Button savedJourneysButton, searchRouteButton;
	ImageButton getLocationButton;
	EditText editTextFrom, editTextTo;
	
	// Location service
	private LocationClient mLocationClient;
	private Location mCurrentLocation;
	private boolean myLocation;
	
	// Time type
	String timeType;
	
	private TimePickerDialog.OnTimeSetListener mTimeSetListener; 
	private DatePickerDialog.OnDateSetListener mDateSetListener; 

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_search_route);
		
		// Retrieve extra data (access token)
		Intent intent = getIntent();
		userLogged = intent.getBooleanExtra("userLogged", false);
		if (userLogged) {
			accessToken = intent.getStringExtra("accessToken");
		} else {
			// Hide Saved Journeys button
			savedJourneysButton = (Button) findViewById(R.id.savedJourneyButton);
			savedJourneysButton.setEnabled(false);
			savedJourneysButton.setVisibility(1);
		}
		
		// Views
		getLocationButton = (ImageButton) findViewById(R.id.buttonGetLocation);
		searchRouteButton = (Button) findViewById(R.id.searchRouteButton);
		editTextFrom = (EditText) findViewById(R.id.editTextFrom);
		editTextTo = (EditText) findViewById(R.id.editTextTo);
		
		// Listener to retrieve data from TimePicker
		mTimeSetListener = new TimePickerDialog.OnTimeSetListener() {
			@Override
			public void onTimeSet(android.widget.TimePicker view, int hourOfDay, int minute) {
				TextView textViewTime = (TextView) findViewById(R.id.textViewPickTime);
				textViewTime.setText(String.format("%02d", hourOfDay)+ ":"+ String.format("%02d", minute));
			}
		};
	            
		// Listener to retrieve data from DatePicker
		mDateSetListener = new DatePickerDialog.OnDateSetListener() {
			@Override
			public void onDateSet(android.widget.DatePicker view, int year, int month, int day) {
				TextView textViewDate = (TextView) findViewById(R.id.textViewPickDate);
				textViewDate.setText(String.format("%04d", year) + "/" + String.format("%02d", month) + "/" + String.format("%02d", day));
			}
		};
		
		/*
         * Create a new location client, using the enclosing class to
         * handle callbacks.
         */
        mLocationClient = new LocationClient(this, this, this);
        myLocation = false;
        
        // Listener of editTextFrom
		editTextFrom.setOnFocusChangeListener(new OnFocusChangeListener() {
			@Override
			public void onFocusChange(View v, boolean hasFocus) {
				if (hasFocus) {
					if (myLocation) {
						// Remove the current location data
						editTextFrom.setText("");
						editTextFrom.setTextColor(Color.BLACK);
						myLocation = false;
						// Enable getLocation button
						getLocationButton.setEnabled(true);
					}
				}
			}
		});
		
		// Set default time type
		timeType = "departure";
	}

    // Called when the Activity is no longer visible.
    @Override
    protected void onStop() {
        // Disconnecting the location client invalidates it.
        mLocationClient.disconnect();
        super.onStop();
    }

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.search_route, menu);
		return true;
	}
	
	/*
	 * Pick a time of departure
	 */
	public void showTimePickerDialog(View v) {
		DialogFragment newFragment = new TimePickerFragment(mTimeSetListener);
		newFragment.show(getSupportFragmentManager(), "timePicker");
	}

	/*
	 * Pick a date
	 */
	public void showDatePickerDialog(View v) {
		DialogFragment newFragment = new DatePickerFragment(mDateSetListener);
		newFragment.show(getSupportFragmentManager(), "datePicker");
	}
	
	/*
	 * Request to the server to retrieve the possible routes to the destination
	 * Execute when the button "Search routes" is clicked
	 */
	public void searchRoute(View view) {
		// Start the SearchRouteResultActivity
		Intent intent = new Intent(this, SearchRouteResultActivity.class);
		// Pass form data to the activity
		TextView textViewAux;
		// From
		if (myLocation) {
			Log.d(DEBUG_TAG, "From: long="+String.valueOf(mCurrentLocation.getLongitude())
					+ " lat="+String.valueOf(mCurrentLocation.getLatitude()));
			intent.putExtra("from_coord", String.valueOf(mCurrentLocation.getLatitude())
					+ "," + String.valueOf(mCurrentLocation.getLongitude()));
		} else {
			Log.d(DEBUG_TAG, "From: "+editTextFrom.getText().toString());
			intent.putExtra("from_address", editTextFrom.getText().toString());
		}
		// To
		Log.d(DEBUG_TAG, "To: "+editTextTo.getText().toString());
		intent.putExtra("to", editTextTo.getText().toString());
		// Time
		textViewAux = (TextView) findViewById(R.id.textViewPickTime);
		Log.d(DEBUG_TAG, "Time: "+textViewAux.getText().toString());
		String time = textViewAux.getText().toString().replace(":", "");
		intent.putExtra("time", time);
		// Time type
		Log.d(DEBUG_TAG, "Time type: "+timeType);
		intent.putExtra("timeType", timeType);
		// Date
		textViewAux = (TextView) findViewById(R.id.textViewPickDate);
		Log.d(DEBUG_TAG, "Date: "+textViewAux.getText().toString());
		String date = textViewAux.getText().toString().replace("/", "");
		intent.putExtra("date", date);
		// User logged
		intent.putExtra("userLogged", userLogged);
		if (userLogged) {
			intent.putExtra("accessToken", accessToken);
		}
		
		// Start the activity
		startActivity(intent);
	}
	
	/*
	 * Request to the server to retrieve the previous saved routes
	 */
	public void checkSavedJourneys(View view) {
		// Start the activity to show the saved journeys
		Intent intent = new Intent(this, SelectSavedJourneyActivity.class);
		// Pass the access token as an extra
		intent.putExtra("accessToken", accessToken);
		startActivityForResult(intent, SAVED_JOURNEYS_REQUEST_CODE);
	}
	
	/*
	 * Set the time type based on the radio button group
	 */
	public void onRadioButtonClicked(View view) {
	    // Is the button now checked?
	    boolean checked = ((RadioButton) view).isChecked();
	    
	    // Check which radio button was clicked
	    switch(view.getId()) {
	        case R.id.radioButtonDeparture:
	            if (checked) {
	            	timeType = "departure";
	            }
	            break;
	        case R.id.radioButtonArrival:
	            if (checked)
	            	timeType = "arrival";
	            break;
	    }
	}
	
	/* #########################################################
	 *                GET CURRENT LOCATION
	 * #########################################################
	 * Parts of the code extracted from:
	 * https://developer.android.com/training/location/retrieve-current.html
	 */
	
	// Function to run when the user click the location button
	public void getLocation(View view) {
		// Check that Google Play services is available
		boolean servicesAvailable = servicesConnected();
		if (servicesAvailable) {
			// Connect the location client.
	        mLocationClient.connect();
	        // Disable getLocation button
        	getLocationButton.setEnabled(false);
        	// Disable searchRoute button
        	searchRouteButton.setEnabled(false);
		}
	}
	
	private boolean servicesConnected() {
        // Check that Google Play services is available
        int resultCode = GooglePlayServicesUtil.isGooglePlayServicesAvailable(this);
        // If Google Play services is available
        if (ConnectionResult.SUCCESS == resultCode) {
            // In debug mode, log the status
            Log.d(DEBUG_TAG, "Google Play services is available.");
            // Continue
            return true;
        // Google Play services was not available for some reason
        } else {
            // Get the error dialog from Google Play services
            Dialog errorDialog = GooglePlayServicesUtil.getErrorDialog(resultCode,
                    this, CONNECTION_FAILURE_RESOLUTION_REQUEST);

            // If Google Play services can provide an error dialog
            if (errorDialog != null) {
                // Create a new DialogFragment for the error dialog
                ErrorDialogFragment errorFragment = new ErrorDialogFragment();
                // Set the dialog in the DialogFragment
                errorFragment.setDialog(errorDialog);
                // Show the error dialog in the DialogFragment
                errorFragment.show(getSupportFragmentManager(), "Location Updates");
            }
            return false;
        }
    }
	
	// Define a DialogFragment that displays the error dialog for the Google Play Services
	// to let the user activate it
    public static class ErrorDialogFragment extends DialogFragment {
        // Global field to contain the error dialog
        private Dialog mDialog;
        // Default constructor. Sets the dialog field to null
        public ErrorDialogFragment() {
            super();
            mDialog = null;
        }
        // Set the dialog to display
        public void setDialog(Dialog dialog) {
            mDialog = dialog;
        }
        // Return a Dialog to the DialogFragment.
        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            return mDialog;
        }
    }
    
    /*  -----------------------------------------------------------
     *  Interfaces to communicate the Location Service with the app
     *  ----------------------------------------------------------- */
    
    /*
     * Called by Location Services when the request to connect the
     * client finishes successfully. At this point, you can
     * request the current location or start periodic updates
     */
    @Override
    public void onConnected(Bundle dataBundle) {
        // Display the connection status
        Toast.makeText(this, "Location Service connected", Toast.LENGTH_SHORT).show();
        // Get the current location
        mCurrentLocation = mLocationClient.getLastLocation();
        myLocation = true;
        // Show "My Location" in the edit text
        editTextFrom.setText("My location");
        editTextFrom.setTextColor(Color.BLUE);
        // Enable searchRoute button
    	searchRouteButton.setEnabled(true);
    }
    
    /*
     * Called by Location Services if the connection to the
     * location client drops because of an error.
     */
    @Override
    public void onDisconnected() {
        // Display the connection status
        Toast.makeText(this, "Disconnected. Please re-connect.", Toast.LENGTH_SHORT).show();
    }
    
    /*
     * Called by Location Services if the attempt to
     * Location Services fails.
     */
    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {
        /*
         * Google Play services can resolve some errors it detects.
         * If the error has a resolution, try sending an Intent to
         * start a Google Play services activity that can resolve
         * error.
         */
        if (connectionResult.hasResolution()) {
            try {
                // Start an Activity that tries to resolve the error
                connectionResult.startResolutionForResult(this, CONNECTION_FAILURE_RESOLUTION_REQUEST);
                /*
                 * Thrown if Google Play services canceled the original
                 * PendingIntent
                 */
            } catch (IntentSender.SendIntentException e) {
                // Log the error
                e.printStackTrace();
            }
        } else {
            /*
             * If no resolution is available, display a dialog to the
             * user with the error.
             */
        	showErrorDialog(connectionResult.getErrorCode());
        }
    }
    
    void showErrorDialog(int code) {
		  GooglePlayServicesUtil.getErrorDialog(code, this, CONNECTION_FAILURE_RESOLUTION_REQUEST).show();
	}
    
	/*  ############################################################
	 *                    On activity result
	 *  ############################################################
	 */
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		if (requestCode == SAVED_JOURNEYS_REQUEST_CODE) {
			if (resultCode == RESULT_OK) {
				// Get the from and to from the Extras
				editTextFrom.setText(data.getStringExtra("from"));
				editTextTo.setText(data.getStringExtra("to"));
			} 
		} else if (requestCode == CONNECTION_FAILURE_RESOLUTION_REQUEST) {
			if (resultCode == Activity.RESULT_OK) {
				// Location Service activated, try the request again
				// TODO
			}
		}
	}
}
