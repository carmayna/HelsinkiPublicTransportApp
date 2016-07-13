package com.example.helsinkipublictransport;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;

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
import android.widget.EditText;
import android.widget.TextView;

public class SaveJourneyActivity extends FragmentActivity {
	private final static String DEBUG_TAG = "SaveJourneyActivity";
	private final static String API_SERVICE = "save_journey";
	
	// Views
	EditText journeyIdEditText;
	
	// Access token
	private String accessToken, from, to;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_save_journey);
		
		// Retrieve the data from the extras
		Intent intent = getIntent();
		accessToken = intent.getStringExtra("accessToken");
		from = intent.getStringExtra("from");
		to = intent.getStringExtra("to");
		
		journeyIdEditText = (EditText) findViewById(R.id.journeyIdEditText);
		
		// Show the from and to on the screen
		TextView textViewFrom = (TextView) findViewById(R.id.journeyFrom);
		textViewFrom.setText(from);
		TextView textViewTo = (TextView) findViewById(R.id.journeyTo);
		textViewTo.setText(to);
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.save_journey, menu);
		return true;
	}
	
	/*
	 * Save the journey associating it to the user
	 */
	public void saveJourney(View view){
		// Check if the mobile device has network connection
		ConnectivityManager connMgr = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
		NetworkInfo networkInfo = connMgr.getActiveNetworkInfo();
		if (networkInfo != null && networkInfo.isConnected()) {
			try {
				// Construct the request
				String parameters="";
				parameters += "access_token="+URLEncoder.encode(accessToken, "UTF-8")+"&";
				parameters += "id="+URLEncoder.encode(journeyIdEditText.getText().toString(), "UTF-8")+"&";
				parameters += "from="+URLEncoder.encode(from, "UTF-8")+"&";
				parameters += "to="+URLEncoder.encode(to, "UTF-8");
				String requestURL = MainActivity.API_URL+API_SERVICE+"?"+parameters;
				Log.d(DEBUG_TAG, "The request url is: " + requestURL);
				// Execute the action to download the data on another thread
				new SaveJourneyTask().execute(requestURL);
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
	
	/*
	 * Back to the search route results activity
	 */
	public void back(View view){
		finish();
	}
	
	/*
	 * ##################################################
	 * ##### REQUEST TO THE API
	 * ##################################################
	 * 
	 */

	// AsyncTask to perform the request to the REST API
	private class SaveJourneyTask extends AsyncTask<String, Void, String> {
		@Override
		protected String doInBackground(String... urls) {
			// urls[0] is the URL to do the request
			try {
				return saveJourneysAPI(urls[0]);
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
				} else if (result.equals("journey saved")) {
					// Show dialog to inform
					Log.d(DEBUG_TAG, "Journey saved.");
					finish();
				}
			}
		}
	}
	
	private String saveJourneysAPI(String api_url) throws IOException {
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
		
		JsonReader reader = new JsonReader(new InputStreamReader(stream, "UTF-8"));
		try {
			// Start to read the JSON object
			reader.beginObject();
			while (reader.hasNext()) {
				String name = reader.nextName();
				if (name.equals("status")) {
					// Read the status of the response
					result = reader.nextString();
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
}
