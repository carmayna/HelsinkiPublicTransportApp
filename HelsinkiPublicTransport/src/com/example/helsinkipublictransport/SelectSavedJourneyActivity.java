package com.example.helsinkipublictransport;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;

import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.util.JsonReader;
import android.util.Log;
import android.view.Menu;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
import android.widget.TextView;

public class SelectSavedJourneyActivity extends FragmentActivity implements OnItemSelectedListener {
	private final static String DEBUG_TAG = "SelectSavedJourneyActivity";
	private final static String API_SERVICE = "retrieve_saved_journeys";
	
	// Access token
	private String accessToken;
	
	// Views and layouts
	private Spinner journeysSpinner;
	private TextView textViewId, textViewFrom, textViewTo;
	
	// List of journes
	List<Journey> listJourneys;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_select_saved_journey);
		
		// Get the access token from the extras
		Intent intent = getIntent();
		accessToken = intent.getStringExtra("accessToken");
		
		// Views
		textViewId = (TextView) findViewById(R.id.journeyId);
		textViewFrom = (TextView) findViewById(R.id.journeyFrom);
		textViewTo = (TextView) findViewById(R.id.journeyTo);
		
		// Set the listener of the selectItem action on the spinner
		journeysSpinner = (Spinner) findViewById(R.id.journeysSpinner);
		journeysSpinner.setOnItemSelectedListener(this);
		
		// Retrieve journeys from the server
		retrieveJourneys();
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.saved_journeys, menu);
		return true;
	}
	
	/*
	 * Add entries to the spinner
	 */
	private void addSpinnerEntries(){
		List<JourneySpinner> listJourneysSpinner = new ArrayList<JourneySpinner>();
		
		int i=0;
		for (Journey journey : listJourneys) {
			// Identifier: Departure time - Arrival time (Duration)
			listJourneysSpinner.add(new JourneySpinner(i, journey.getId()));
			i++;
		}
		// Insert the routes in the spinner
		ArrayAdapter<JourneySpinner> spinnerArrayAdapter = new ArrayAdapter<JourneySpinner>(this, android.R.layout.simple_spinner_item, listJourneysSpinner);

		// Step 3: Tell the spinner about our adapter
		journeysSpinner.setAdapter(spinnerArrayAdapter);
	}
	
	/*
	 * Response to select a route on the Spinner
	 */
	public void onItemSelected(AdapterView<?> parent, View view, int pos, long id) {
        // An item was selected. Change the info on the screen
		Journey journey = listJourneys.get(pos);
		textViewId.setText(journey.getId());
		textViewFrom.setText(journey.getFrom());
		textViewTo.setText(journey.getTo());
    }

    public void onNothingSelected(AdapterView<?> parent) {
        // Nothing
    }
	
    /*
	 *  Class to represent a journey
	 */
	class Journey {
		private String id, from, to;
		
		public Journey () {
			
		}

		public String getId() {
			return id;
		}

		public void setId(String id) {
			this.id = id;
		}

		public String getFrom() {
			return from;
		}

		public void setFrom(String from) {
			this.from = from;
		}

		public String getTo() {
			return to;
		}

		public void setTo(String to) {
			this.to = to;
		}
	}
    
    /*
	 *  Class to represent the journeys on the spinner
	 */
	class JourneySpinner {
		public int id = 0;
		public String name = "";
		public String abbrev = "";
		
		public JourneySpinner (int listId, String id) {
			this.id = listId;
			this.name = id;
			this.abbrev = String.valueOf(listId);
		}
		
		public String toString() {
			return name;
		}
	}
    
	/*
	 * ##################################################
	 * ##### REQUEST TO THE API
	 * ##################################################
	 * 
	 */
	private void retrieveJourneys(){
		// Check if the mobile device has network connection
		ConnectivityManager connMgr = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
		NetworkInfo networkInfo = connMgr.getActiveNetworkInfo();
		if (networkInfo != null && networkInfo.isConnected()) {
			try {
				// Construct the request
				String parameters="";
				parameters += "access_token="+URLEncoder.encode(accessToken, "UTF-8");
				String requestURL = MainActivity.API_URL+API_SERVICE+"?"+parameters;
				Log.d(DEBUG_TAG, "The request url is: " + requestURL);
				// Execute the action to download the data on another thread
				new RetrieveSavedJourneysTask().execute(requestURL);
			} catch (UnsupportedEncodingException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		} else {
			// The device has no network
			// Create an instance of the dialog fragment and show it
			NotNetworkDialog infoDialog = new NotNetworkDialog();
			infoDialog.show(getSupportFragmentManager(), "NoticeDialogFragment");
		}
	}

	// AsyncTask to perform the request to the REST API
	private class RetrieveSavedJourneysTask extends AsyncTask<String, Void, String> {
		@Override
		protected String doInBackground(String... urls) {
			// urls[0] is the URL to do the request
			try {
				return retrieveJourneysAPI(urls[0]);
			} catch (IOException e) {
				// TODO
				return null;
			}
		}

		// onPostExecute displays the results of the AsyncTask.
		@Override
		protected void onPostExecute(String result) {
			if (result != null) {
				if (result.equals("access token is not valid")) {
					// Show a dialog to inform
					Log.d(DEBUG_TAG, "Access token is not valid.");
				} else if (result.equals("journeys retrieved")) {
					// Show the journeys in the spinner
					addSpinnerEntries();
				}
			}
		}
	}
	
	private String retrieveJourneysAPI(String api_url) throws IOException {
		InputStream is = null;

		try {
			URL url = new URL(api_url);
			HttpURLConnection conn = (HttpURLConnection) url.openConnection();
			conn.setReadTimeout(10000 /* milliseconds */);
			conn.setConnectTimeout(15000 /* milliseconds */);
			conn.setRequestMethod("GET");
			conn.setDoInput(true);
			// Starts the request
			conn.connect();
			int response = conn.getResponseCode();
			Log.d(DEBUG_TAG, "The response is: " + response);
			is = conn.getInputStream();

			// Read the status of the request
			String responseStatus = readResponse(is);
			
			return responseStatus;
		} finally {
			// Closed the InputStream
			if (is != null) {
				is.close();
			}
		}
	}

	// Read the status inside the response
	public String readResponse(InputStream stream) throws IOException, UnsupportedEncodingException {
		String result = "";
		listJourneys = new ArrayList<Journey>();
		
		JsonReader reader = new JsonReader(new InputStreamReader(stream, "UTF-8"));
		try {
			// Start to read the JSON object
			reader.beginObject();
			while (reader.hasNext()) {
				String name = reader.nextName();
				if (name.equals("status")) {
					// Read the status of the response
					result = reader.nextString();
				} else if (name.equals("journeys")) {
					// Read the journeys of the response
					reader.beginArray();
					
					while (reader.hasNext()) {
						Journey journey = new Journey();
						
						reader.beginObject();
						while (reader.hasNext()) {
							name = reader.nextName();
							if (name.equals("id")) {
								// Departure time from the location
								journey.setId(reader.nextString());
							} else if (name.equals("from")) {
								// Name the location
								journey.setFrom(reader.nextString());
							} else if (name.equals("to")) {
								// Name the location
								journey.setTo(reader.nextString());
							} else {
								reader.skipValue();
							}
						}
						reader.endObject(); // End of read loc object
						
						listJourneys.add(journey);
					}
					
					reader.endArray();
				} else {
					reader.skipValue();
				}
			}
			reader.endObject();
		} finally {
			reader.close();
		}
		
		return result;
	}
	
	/*
	 * Return the parameters of the journey (from, to) as a result
	 */
	public void selectJourney(View view) {
		Intent returnIntent = new Intent();
		returnIntent.putExtra("from", textViewFrom.getText().toString());
		returnIntent.putExtra("to", textViewTo.getText().toString());
		setResult(RESULT_OK, returnIntent);
		// Close the activity
		finish();
	}
	
	public void back(View view) {
		// Return result canceled
		Intent returnIntent = new Intent();
		setResult(RESULT_CANCELED, returnIntent);        
		finish();
	}
}
