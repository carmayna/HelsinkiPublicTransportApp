package com.example.helsinkipublictransport;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;

import android.app.ProgressDialog;
import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.FragmentActivity;
import android.util.JsonReader;
import android.util.Log;
import android.view.Menu;
import android.view.View;
import android.widget.EditText;

public class RegisterUserActivity extends FragmentActivity implements 
		UserRegisteredDialog.UserRegisteredDialogListener,
		RegistrartionNicknameErrorDialog.RegistrartionNicknameErrorDialogListener {
	private final static String API_SERVICE = "register_user";
	private final static String DEBUG_TAG = "RegisterUserActivity";
	
	// Form data
	private String name, username, password;
	
	// Progress dialog
	private ProgressDialog mProgressDialog;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_register_user);
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.register_user, menu);
		return true;
	}
	
	/*
	 * ##################################################
	 * ##### REQUEST TO THE API
	 * ##################################################
	 * 
	 */

	/** Called when the user clicks the Retrieve data button in the Network tab */
	public void registerUser(View view) {
		// Check if the data in the form has been introduced correctly and retrieve it
		boolean formCorrect = true;
		EditText editTextName = (EditText) findViewById(R.id.editTextRegName);
		name = editTextName.getText().toString();
		EditText editTextUsername = (EditText) findViewById(R.id.editTextRegUsername);
		username = editTextUsername.getText().toString();
		EditText editTextPassword = (EditText) findViewById(R.id.editTextRegPassword);
		password = editTextPassword.getText().toString();
		if (formCorrect) {
			// Check if the mobile device has network connection
			ConnectivityManager connMgr = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
			NetworkInfo networkInfo = connMgr.getActiveNetworkInfo();
			if (networkInfo != null && networkInfo.isConnected()) {
				try {
					// Construct the request
					String parameters="";
					parameters += "name="+URLEncoder.encode(name, "UTF-8")+"&";
					parameters += "username="+URLEncoder.encode(username, "UTF-8")+"&";
					parameters += "pass="+URLEncoder.encode(password, "UTF-8");
					String requestURL = MainActivity.API_URL+API_SERVICE+"?"+parameters;
					Log.d(DEBUG_TAG, "The request url is: " + requestURL);
					// Initialize progess dialog
					mProgressDialog = new ProgressDialog(RegisterUserActivity.this);
					mProgressDialog.setMessage("Registering the user...");
					mProgressDialog.setIndeterminate(false);
					mProgressDialog.setMax(100);
					mProgressDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
					mProgressDialog.show();
					// Execute the action to download the data on another thread
					new RegisterUserTask().execute(requestURL);
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
		} else {
			// Show the error
			Log.d(DEBUG_TAG, "Error in the form for registration");
			// TODO
		}
	}

	// AsyncTask to perform the request to the REST API
	private class RegisterUserTask extends AsyncTask<String, Integer, String> {
		@Override
		protected String doInBackground(String... urls) {
			// urls[0] is the URL to do the request
			try {
				return registerUserAPI(urls[0]);
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
		protected void onPostExecute(String result) {
			mProgressDialog.dismiss();
			if (result != null) {
				Log.d(DEBUG_TAG, "The status of the response is: " + result);
				if (result.equals("user created")) {
					// Show a dialog to inform
					DialogFragment newFragment = new UserRegisteredDialog();
				    newFragment.show(getSupportFragmentManager(), "userRegistred");
				} else if (result.equals("username already exists")) {
					// Show a dialog to inform
					DialogFragment newFragment = new RegistrartionNicknameErrorDialog();
				    newFragment.show(getSupportFragmentManager(), "registrationError");
				}
			}
		}
	}
	
	private String registerUserAPI(String api_url) throws IOException {
		InputStream is = null;

		try {
			URL url = new URL(api_url);
			HttpURLConnection conn = (HttpURLConnection) url.openConnection();
			conn.setReadTimeout(10000 /* milliseconds */);
			conn.setConnectTimeout(15000 /* milliseconds */);
			conn.setRequestMethod("GET");
			conn.setDoInput(true);
			// Starts the query
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

	/*
	 * Actions to do when the user click back on the dialog "Nickname already exists"
	 */
	@Override
	public void onDialogNeutralClickUserRegistered(DialogFragment dialog) {
		// Close the activity and come back to the menu
		finish();
	}
	
	/*
	 * Actions to do when the user click back on the dialog "Nickname already exists"
	 */
	@Override
	public void onDialogNeutralClickNicknameError(DialogFragment dialog) {
		// Clear the editText for nickname
		EditText editTextUsername = (EditText) findViewById(R.id.editTextRegUsername);
		editTextUsername.setText("");
	}
}
