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
import android.content.Intent;
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

public class LoginActivity extends FragmentActivity
		implements
		LoggedInDialog.LoggedInDialogListener,
		UserDoNotExistsDialog.UserDoNotExistsDialogListener,
		IncorrectPasswordDialog.IncorrectPasswordDialogListener {
	
	private final static String DEBUG_TAG = "LoginActivity";
	private final static String API_SERVICE = "login";
	private String username, password;
	private String accessToken;
	
	// Progress dialog
	private ProgressDialog mProgressDialog;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_login);
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.login, menu);
		return true;
	}
	
	/*
	 * Login with the username and password to receive an access token from the server
	 */
	public void login(View view) {
		// Check if the data in the form has been introduced correctly and retrieve it
		boolean formCorrect = true;
		EditText editTextUsername = (EditText) findViewById(R.id.editTextLoginUsername);
		username = editTextUsername.getText().toString();
		EditText editTextPassword = (EditText) findViewById(R.id.editTextLoginPassword);
		password = editTextPassword.getText().toString();
		if (formCorrect) {
			// Check if the mobile device has network connection
			ConnectivityManager connMgr = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
			NetworkInfo networkInfo = connMgr.getActiveNetworkInfo();
			if (networkInfo != null && networkInfo.isConnected()) {
				try {
					// Construct the request
					String parameters="";
					parameters += "username="+URLEncoder.encode(username, "UTF-8")+"&";
					parameters += "pass="+URLEncoder.encode(password, "UTF-8");
					String requestURL = MainActivity.API_URL+API_SERVICE+"?"+parameters;
					Log.d(DEBUG_TAG, "The request url is: " + requestURL);
					// Initialize progess dialog
					mProgressDialog = new ProgressDialog(LoginActivity.this);
					mProgressDialog.setMessage("Log in the user...");
					mProgressDialog.setIndeterminate(false);
					mProgressDialog.setMax(100);
					mProgressDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
					mProgressDialog.show();
					// Execute the action to download the data on another thread
					new LoginTask().execute(requestURL);
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
	
	/*
	 * Cancel the login
	 */
	public void cancel(View view){
		// Return result canceled
		Intent returnIntent = new Intent();
		setResult(RESULT_CANCELED, returnIntent);        
		finish();
	}
	
	/*
	 * ##################################################
	 * ##### REQUEST TO THE API
	 * ##################################################
	 * 
	 */

	// AsyncTask to perform the request to the REST API
	private class LoginTask extends AsyncTask<String, Integer, String> {
		@Override
		protected String doInBackground(String... urls) {
			// urls[0] is the URL to do the request
			try {
				return loginAPI(urls[0]);
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
			if (result != null) {
				Log.d(DEBUG_TAG, "The status of the response is: " + result);
				if (result.equals("correct login")) {
					// Show a dialog to inform
					DialogFragment newFragment = new LoggedInDialog();
				    newFragment.show(getSupportFragmentManager(), "loggedIn");
				} else if (result.equals("user do not exists")) {
					// Show a dialog to inform
					DialogFragment newFragment = new UserDoNotExistsDialog();
				    newFragment.show(getSupportFragmentManager(), "userDoNotExists");
				} else if (result.equals("incorrect password")) {
					// Show a dialog to inform
					DialogFragment newFragment = new IncorrectPasswordDialog();
				    newFragment.show(getSupportFragmentManager(), "incorrectPassword");
				}
			}
		}
	}
	
	private String loginAPI(String api_url) throws IOException {
		InputStream is = null;

		try {
			URL url = new URL(api_url);
			HttpURLConnection conn = (HttpURLConnection) url.openConnection();
			conn.setReadTimeout(12000 /* milliseconds */);
			conn.setConnectTimeout(17000 /* milliseconds */);
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
				} else if (name.equals("access_token")) {
					// Read the status of the response
					accessToken = reader.nextString();
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
	 * Actions to do when the user click back on the dialog "Logged in"
	 */
	@Override
	public void onDialogNeutralClickLoggedIn(DialogFragment dialog) {
		// Pass the access token to the parent activity (Main menu) as a result
		Intent returnIntent = new Intent();
		returnIntent.putExtra("accessToken", accessToken);
		setResult(RESULT_OK, returnIntent);
		// Close the activity
		finish();
	}
	
	/*
	 * Actions to do when the user click back on the dialog "User do not exists"
	 */
	@Override
	public void onDialogNeutralClickUserDoNotExists(DialogFragment dialog) {
		// Clear the editTexts
		EditText editTextUsername = (EditText) findViewById(R.id.editTextLoginUsername);
		editTextUsername.setText("");
		editTextUsername = (EditText) findViewById(R.id.editTextLoginPassword);
		editTextUsername.setText("");
	}
	
	/*
	 * Actions to do when the user click back on the dialog "Incorrect password"
	 */
	@Override
	public void onDialogNeutralClickIncorrectPassword(DialogFragment dialog) {
		// Clear the editText for password
		EditText editTextUsername = (EditText) findViewById(R.id.editTextLoginPassword);
		editTextUsername.setText("");
	}
}
