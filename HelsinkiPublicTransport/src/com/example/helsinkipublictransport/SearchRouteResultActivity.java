package com.example.helsinkipublictransport;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.FragmentActivity;
import android.util.JsonReader;
import android.util.Log;
import android.view.Gravity;
import android.view.Menu;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.LinearLayout.LayoutParams;
import android.widget.Spinner;
import android.widget.TextView;

public class SearchRouteResultActivity extends FragmentActivity 
		implements OnItemSelectedListener, NoRouteFoundDialog.NoRouteFoundDialogListener {
	
	private final static String API_SERVICE = "search_routes";
	private final static String DEBUG_TAG = "SearchRouteResultActivity";
	
	// Query paramaters
	private String from, to, time, timeType, date, accessToken;
	private boolean userLogged, fromAsAddress;
	
	// Views and layouts
	private LinearLayout linearLayoutEnvelope;
	private Spinner routesSpinner;
	private Map<String, String> transportTypes;
	private Button saveJourneysButton;
	
	// Save the results of the request
	List<Route> routesList;
	
	// Progress dialog
	private ProgressDialog mProgressDialog;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		// Define dictionary of types of transport
		transportTypes = new HashMap<String, String>();
		transportTypes.put("1", "Helsinki internal bus line");
		transportTypes.put("2", "Tram");
		transportTypes.put("3", "Espoo internal bus line");
		transportTypes.put("4", "Vantaa internal bus line");
		transportTypes.put("5", "Regional bus line");
		transportTypes.put("6", "Metro");
		transportTypes.put("7", "Ferry");
		transportTypes.put("8", "U-line");
		transportTypes.put("12", "Commuter train");
		transportTypes.put("21", "Helsinki service line");
		transportTypes.put("22", "Helsinki night bus");
		transportTypes.put("23", "Espoo service line");
		transportTypes.put("24", "Vaanta service line");
		transportTypes.put("25", "Region night bus");
		transportTypes.put("36", "Kirkkonummi internal bus line");
		transportTypes.put("39", "Kerava internal bus line");
		
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_search_route_result);
		
		// Views and layouts
		linearLayoutEnvelope = (LinearLayout) findViewById(R.id.verticalLinearLayout);
		saveJourneysButton = (Button) findViewById(R.id.buttonSaveJourney);
		
		// Extract data from the extra passed by the SearchRoute activity
		Intent intent = getIntent();
		if (intent.hasExtra("from_address")) {
			from = intent.getStringExtra("from_address");
			fromAsAddress = true;
		} else {
			from = intent.getStringExtra("from_coord");
			fromAsAddress = false;
		}
		
		to = intent.getStringExtra("to");
		time = intent.getStringExtra("time");
		timeType = intent.getStringExtra("timeType");
		date = intent.getStringExtra("date");
		userLogged = intent.getBooleanExtra("userLogged", false);
		if (userLogged) {
			accessToken = intent.getStringExtra("accessToken");
		} else {
			// Disable save journey
			saveJourneysButton.setEnabled(false);
			saveJourneysButton.setVisibility(1);
		}
		
		// Request the routes to the server
		requestSearchRoutes();
		
		// Set the listener of the selectItem action on the spinner
		routesSpinner = (Spinner) findViewById(R.id.routesSpinner);
		routesSpinner.setOnItemSelectedListener(this);
	}
	
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.search_route_result, menu);
		return true;
	}
	
	/*
	 * Add entries to the spinner
	 */
	private void addSpinnerEntries(){
		List<RouteIdentificator> listRouteIdentifiers = new ArrayList<RouteIdentificator>();
		
		int i=0;
		for (Route route : routesList) {
			// Identifier: Departure time - Arrival time (Duration)
			listRouteIdentifiers.add(new RouteIdentificator(i, route.getDuration(),
					route.getDepartureTime(), route.getArrivalTime()));
			i++;
		}
		// Insert the routes in the spinner
		ArrayAdapter<RouteIdentificator> spinnerArrayAdapter = new ArrayAdapter<RouteIdentificator>(this, android.R.layout.simple_spinner_item, listRouteIdentifiers);

		// Step 3: Tell the spinner about our adapter
		routesSpinner.setAdapter(spinnerArrayAdapter);
	}
	
	/*
	 * Response to select a route on the Spinner
	 */
	public void onItemSelected(AdapterView<?> parent, View view, int pos, long id) {
        // An item was selected. Change the route to show on the screen
		// Clean the envelope layout
		linearLayoutEnvelope.removeAllViews();
		// Show the route on the screen
		showRoute(routesList.get(pos));
    }

    public void onNothingSelected(AdapterView<?> parent) {
        // Clean the envelope layout
    	linearLayoutEnvelope.removeAllViews();
    }
    
    /*
	 * Actions to do when the user click "Change search" on the dialog "No route found"
	 */
	@Override
	public void onDialogNeutralClickNoRouteFound(DialogFragment dialog) {
		// Finish the activity and come back to the search route activity
		finish();
	}
    
    /*
     * Function to run when the user click the button "Change search"
     */
    public void changeSearch(View view) {
    	finish();
    }
    
    /*
     * Function to save the journey of the request
     */
    public void saveJourney(View view) {
    	Intent intent = new Intent(this, SaveJourneyActivity.class);
    	int pos = routesSpinner.getSelectedItemPosition();
    	intent.putExtra("accessToken", accessToken);
		intent.putExtra("from", routesList.get(pos).getOrigin());
		intent.putExtra("to", routesList.get(pos).getDestination());
		startActivity(intent);
    }
    
    /* #############################################################################
	 *        Async task to do the request to the REST API on the server
	 * #############################################################################
	 */
    private void requestSearchRoutes(){
		// Check if the mobile device has network connection
		ConnectivityManager connMgr = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
		NetworkInfo networkInfo = connMgr.getActiveNetworkInfo();
		if (networkInfo != null && networkInfo.isConnected()) {
			// Get the data from the view
			// TODO
			try {
				// Construct the query and the complete URL with the parameters
				String parameters="";
				if (fromAsAddress) {
					parameters += "from_address="+URLEncoder.encode(from, "UTF-8")+"&";
				} else {
					parameters += "from_coord="+URLEncoder.encode(from, "UTF-8")+"&";
				}
				parameters += "to="+URLEncoder.encode(to, "UTF-8")+"&";
				parameters += "time="+URLEncoder.encode(time, "UTF-8")+"&";
				parameters += "time_type="+URLEncoder.encode(timeType, "UTF-8")+"&";
				parameters += "date="+URLEncoder.encode(date, "UTF-8");
				String urlServerAPI = MainActivity.API_URL+API_SERVICE+"?"+parameters;
				Log.d(DEBUG_TAG, "Request to Server REST API: " + urlServerAPI);
				// Initialize progess dialog
				mProgressDialog = new ProgressDialog(SearchRouteResultActivity.this);
				mProgressDialog.setMessage("Searching routes...");
				mProgressDialog.setIndeterminate(false);
				mProgressDialog.setMax(100);
				mProgressDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
				mProgressDialog.show();
				// Execute the action to download the data on another thread
				new SearchRoutesTask().execute(urlServerAPI);
			} catch (UnsupportedEncodingException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			;
		} else {
			// The device has no network
			// Create an instance of the dialog fragment and show it
			NotNetworkDialog infoDialog = new NotNetworkDialog();
			infoDialog.show(getSupportFragmentManager(), "NoticeDialogFragment");
		}
	}
	
	// AsyncTask to request the search of routes to the server
	private class SearchRoutesTask extends AsyncTask<String, Integer, String> {
		@Override
		protected String doInBackground(String... urls) {
			// urls[0] is the URL to do the request
			try {
				return requestSearchRoute(urls[0]);
			} catch (IOException e) {
				// TODO
				return null;
			}
		}
		
		@Override
		protected void onProgressUpdate(Integer... progress) {
	         // Update progress dialog
			mProgressDialog.setProgress(progress[0]);
	    }

		// onPostExecute displays the results of the AsyncTask.
		@Override
		protected void onPostExecute(String status) {
			mProgressDialog.dismiss();
			if (status != null) {
				if (status.equals("OK") && (routesList.size() > 0)) {
					Log.d(DEBUG_TAG, "Number of routes founded: " + String.valueOf(routesList.size()));
					addSpinnerEntries();
				} else {
					// Show a message of error
					DialogFragment newFragment = new NoRouteFoundDialog();
				    newFragment.show(getSupportFragmentManager(), "No route found");
				}
			} else {
				Log.d(DEBUG_TAG, "Result null");
			}
		}
	}

	// Given a URL, establishes an HttpUrlConnection and retrieves
	// the web page content as a InputStream, which it returns as
	// a string.
	private String requestSearchRoute(String urlSearchAPI) throws IOException {
		InputStream is = null;

		try {
			URL url = new URL(urlSearchAPI);
			HttpURLConnection conn = (HttpURLConnection) url.openConnection();
			conn.setReadTimeout(180000 /* milliseconds */);
			conn.setConnectTimeout(200000 /* milliseconds */);
			conn.setRequestMethod("GET");
			conn.setDoInput(true);
			// Starts the query
			Log.d(DEBUG_TAG, "Execute the request");
			conn.connect();
			int response = conn.getResponseCode();
			Log.d(DEBUG_TAG, "The response is: " + response);
			is = conn.getInputStream();

			if (response == 200) {
				// Parse the response
				Log.d(DEBUG_TAG, "Parsing the response");
				return parseResponse(is);
			} else {
				return null;
			}
		} finally {
			// Closed the InputStream
			if (is != null) {
				is.close();
			}
		}
	}
	
	// This code has been partially extract from:
	// http://developer.android.com/reference/android/util/JsonReader.html
	
	private String parseResponse (InputStream stream) throws IOException {
		// Initalize the list to save the result routes
		routesList = new ArrayList<Route>();
		// Save the status
		String status = "";
		
		JsonReader reader = new JsonReader(new InputStreamReader(stream, "UTF-8"));
		try {
			// Start to read the JSON object
			reader.beginObject();
			while (reader.hasNext()) {
				String name = reader.nextName();
				
				if (name.equals("status")) {
					status = reader.nextString();
				} else if (name.equals("routes")) {
					// Read routes array
					reader.beginArray();
					
					while (reader.hasNext()) {
						// Create a new Route object
						Route route = new Route();
						reader.beginObject();
						
						while (reader.hasNext()) {
							name = reader.nextName();
							if (name.equals("from")) {
								// Origin of the route
								route.setOrigin(reader.nextString());
							} else if (name.equals("to")) {
								// Destination of the route
								route.setDestination(reader.nextString());
							} else if (name.equals("duration")) {
								// Duration of the route in minutes
								route.setDuration(reader.nextInt());
							} else if (name.equals("depTime")) {
								// Departure time of the route
								route.setDepartureTime(reader.nextString());
							} else if (name.equals("arrTime")) {
								// Departure time of the route
								route.setArrivalTime(reader.nextString());
							} else if (name.equals("legs")) {
								// Legs arrays
								reader.beginArray();
								
								while (reader.hasNext()) {
									Leg leg = new Leg();
									
									reader.beginObject();
									while (reader.hasNext()) {
										name = reader.nextName();
										if (name.equals("type")) {
											// Type of the leg
											leg.setType(reader.nextString());
										} else if (name.equals("code")) {
											// Type of the leg
											leg.setCode(reader.nextString());
										} else if (name.equals("duration")) {
											// Duration of the leg in minutes
											leg.setDuration(reader.nextInt());
										} else if (name.equals("depTime")) {
											// Departure time of the leg
											leg.setDepartureTime(reader.nextString());
										} else if (name.equals("arrTime")) {
											// Arrival time of the leg
											leg.setArrivalTime(reader.nextString());
										} else if (name.equals("length")) {
											// Length of the leg on metres
											float lengthMetres = (float) reader.nextInt();
											// Save on kilometres
											leg.setLength(String.valueOf(lengthMetres/1000));
										} else if (name.equals("locs")) {
											// Locations of the leg
											reader.beginArray();
											
											while (reader.hasNext()) {
												TransportLocation location = new TransportLocation();
												
												reader.beginObject();
												while (reader.hasNext()) {
													name = reader.nextName();
													if (name.equals("depTime")) {
														// Departure time from the location
														location.setDepartureTime(reader.nextString());
													} else if (name.equals("name")) {
														// Name the location
														location.setName(reader.nextString());
													} else if (name.equals("shortCode")) {
														// Name the location
														location.setShortCode(reader.nextString());
													} else {
														reader.skipValue();
													}
												}
												reader.endObject(); // End of read loc object
												
												// Add location to the leg
												leg.addTransportLocation(location);
											}
											
											reader.endArray(); // End of read locs array
										} else {
											reader.skipValue();
										}
									}
									
									reader.endObject(); // End of read leg object
									
									// Add leg to the route
									route.addLeg(leg);
								}
								reader.endArray(); // End of read legs array
							} else {
								reader.skipValue();
							}
						}
						reader.endObject(); // End of read route object
						
						// Add the route to the list
						routesList.add(route);
					}
					reader.endArray(); // End of read route array
				} else {
					reader.skipValue();
				}
			}
			reader.endObject(); // End of read routes object
		} finally {
			// Close reader object
			reader.close();
		}
		
		return status;
	}
	
	/*
	 *  Show a route on the screen
	 */
	private void showRoute(Route route) {
		// Origin indicator
		insertOrigin(route.getOrigin());
		// Legs
		for (Leg leg : route.getLegs()) {
			insertLeg(leg);
		}
		// Destination indicator
		insertDestination(route.getDestination());
	}
	
	/*
	 *  Insert origin indicator
	 */
	private void insertOrigin(String origin) {
		// Insert separator
		View horizontalSeparator = new View(this);
		horizontalSeparator.setLayoutParams(new LayoutParams(LayoutParams.MATCH_PARENT, 1));
		horizontalSeparator.setBackgroundColor(Color.BLACK);
		linearLayoutEnvelope.addView(horizontalSeparator);
		
		LinearLayout linearLayoutHor = new LinearLayout(this);
		linearLayoutHor.setOrientation(LinearLayout.HORIZONTAL);
		linearLayoutHor.setPadding(5, 8, 5, 8);
		linearLayoutHor.setLayoutParams(new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT));
		
		// ImageView
		ImageView imageView = new ImageView(this);
		imageView.setImageResource(R.drawable.mark_icon);
		imageView.setPadding(10, 5, 27, 5);
		imageView.setLayoutParams(new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT, 1));
		
		// Text views
		TextView textView = new TextView(this);
		textView.setText(origin);
		textView.setGravity(Gravity.CENTER);
		textView.setLayoutParams(new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.MATCH_PARENT, 9));
		
		// Setting views in the horizontal layout
		linearLayoutHor.addView(imageView);
		linearLayoutHor.addView(textView);
		
		// Setting the horizontal layout inside the vertical layout
		linearLayoutEnvelope.addView(linearLayoutHor);
	}
	
	/*
	 *  Insert leg in the interface
	 */
	private void insertLeg(Leg leg) {
		// Create the separator (as view)
		View horizontalSeparatorUp = new View(this);
		horizontalSeparatorUp.setLayoutParams(new LayoutParams(LayoutParams.MATCH_PARENT, 6));
		horizontalSeparatorUp.setBackgroundColor(Color.BLUE);
		View horizontalSeparatorDown = new View(this);
		horizontalSeparatorDown.setLayoutParams(new LayoutParams(LayoutParams.MATCH_PARENT, 6));
		horizontalSeparatorDown.setBackgroundColor(Color.BLUE);
		
		/* ###################################
		 *      Add leg indicator
		 * ###################################
		 */
		LinearLayout linearLayoutLeg = new LinearLayout(this);
		linearLayoutLeg.setOrientation(LinearLayout.HORIZONTAL);
		linearLayoutLeg.setPadding(5, 8, 5, 8);
		linearLayoutLeg.setLayoutParams(new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT));
		
		// ImageView
		ImageView imageView = new ImageView(this);
		// Select image of the leg depends on the type of the leg
		int imageResource;
		if (leg.getType().equals("walk")) {
			imageResource = R.drawable.walk;
		} else if (leg.getType().matches("1|3|4|5|22|25|36|39")) {
			imageResource = R.drawable.pict_bussi;
		} else if (leg.getType().equals("6")) {
			imageResource = R.drawable.pict_metro;
		} else if (leg.getType().equals("2")) {
			imageResource = R.drawable.pict_ratikka;
		} else if (leg.getType().equals("7")) {
			imageResource = R.drawable.pict_lautta;
		} else {
			imageResource = R.drawable.pict_juna;
		}
		imageView.setImageResource(imageResource);
		imageView.setPadding(15, 5, 0, 5);
		imageView.setLayoutParams(new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.MATCH_PARENT, 1));
		
		// Vertical layout
		LinearLayout linearLayoutVertical = new LinearLayout(this);
		linearLayoutVertical.setOrientation(LinearLayout.VERTICAL);
		linearLayoutVertical.setPadding(35, 0, 0, 0);
		linearLayoutVertical.setLayoutParams(new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT, 9));
		
		// Text views
		TextView textViewUp = new TextView(this);
		textViewUp.setText(leg.getDepartureTime()+" ("+String.valueOf(leg.getDuration())+" min)");
		TextView textViewDown = new TextView(this);
		if (leg.getType().equals("walk")) {
			textViewDown.setText("Walking ("+leg.getLength()+" km)");
		} else {
			textViewDown.setText(leg.getCode()+" ["+transportTypes.get(leg.getType())+"]");
		}
		
		// Set the text view in the vertical
		linearLayoutVertical.addView(textViewUp);
		linearLayoutVertical.addView(textViewDown);
		
		// Setting views in the horizontal layout
		linearLayoutLeg.addView(imageView);
		linearLayoutLeg.addView(linearLayoutVertical);
		
		// Setting the horizantal layout inside the vertical layout
		linearLayoutEnvelope.addView(horizontalSeparatorUp);
		linearLayoutEnvelope.addView(linearLayoutLeg);
		linearLayoutEnvelope.addView(horizontalSeparatorDown);
		
		List<TransportLocation> locations = leg.getTransportLocations();
		int i;
		for (i=0; i<locations.size()-1; i++) {
			insertLocation(locations.get(i));
			// Insert separator
			View horizontalSeparator = new View(this);
			horizontalSeparator.setLayoutParams(new LayoutParams(LayoutParams.MATCH_PARENT, 1));
			horizontalSeparator.setBackgroundColor(Color.BLACK);
			linearLayoutEnvelope.addView(horizontalSeparator);
		}
		insertLocation(locations.get(i));
	}
	
	/*
	 *  Insert location in the interface
	 */
	private void insertLocation(TransportLocation location) {
		/* ###################################
		 *        Add loc indicator
		 * ###################################
		 */
		
		// Create the layout to hold the indicator
		LinearLayout linearLayoutLocation = new LinearLayout(this);
		linearLayoutLocation.setOrientation(LinearLayout.HORIZONTAL);
		linearLayoutLocation.setPadding(20, 7, 5, 7);
		linearLayoutLocation.setLayoutParams(new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT));
		
		// Create text view for the hour of depart from the location
		TextView textViewHour = new TextView(this);
		textViewHour.setText(location.getDepartureTime());
		
		// Create text view name of the location
		TextView textViewLocationName = new TextView(this);
		if (location.getShortCode() != null && !location.getShortCode().equals("")) {
			// Stop
			textViewLocationName.setText(location.getName()+" ("+location.getShortCode()+")");
		} else {
			// Location
			textViewLocationName.setText(location.getName());
		}
		textViewLocationName.setPadding(20, 0, 0, 0);
		
		// Set text views in the layout
		linearLayoutLocation.addView(textViewHour);
		linearLayoutLocation.addView(textViewLocationName);
		
		// Setting the horizontal layout inside the vertical layout
		linearLayoutEnvelope.addView(linearLayoutLocation);
	}
	
	/*
	 *  Insert destination indicator
	 */
	private void insertDestination(String destination) {
		// Insert separator
		View horizontalSeparator = new View(this);
		horizontalSeparator.setLayoutParams(new LayoutParams(LayoutParams.MATCH_PARENT, 1));
		horizontalSeparator.setBackgroundColor(Color.BLACK);
		linearLayoutEnvelope.addView(horizontalSeparator);
		
		LinearLayout linearLayoutHor = new LinearLayout(this);
		linearLayoutHor.setOrientation(LinearLayout.HORIZONTAL);
		linearLayoutHor.setPadding(5, 8, 5, 8);
		linearLayoutHor.setLayoutParams(new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT));
		
		// ImageView
		ImageView imageView = new ImageView(this);
		imageView.setImageResource(R.drawable.mark_icon);
		imageView.setPadding(10, 5, 27, 5);
		imageView.setLayoutParams(new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT, 1));
		
		// Text views
		TextView textView = new TextView(this);
		textView.setText(destination);
		textView.setGravity(Gravity.CENTER);
		textView.setLayoutParams(new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.MATCH_PARENT, 9));
		
		// Setting views in the horizontal layout
		linearLayoutHor.addView(imageView);
		linearLayoutHor.addView(textView);
		
		// Setting the horizontal layout inside the vertical layout
		linearLayoutEnvelope.addView(linearLayoutHor);
	}
	
	/*
	 *  Class to represent the routes on the spinner
	 */
	class RouteIdentificator {
		public int id = 0;
		public String name = "";
		public String abbrev = "";
		
		public RouteIdentificator (int listId, int duration, String arrivalTime, String departureTime) {
			this.id = listId;
			this.name = arrivalTime+" - "+departureTime+" ("+String.valueOf(duration)+" min)";
			this.abbrev = String.valueOf(listId);
		}
		
		public String toString() {
			return name;
		}
	}
}
