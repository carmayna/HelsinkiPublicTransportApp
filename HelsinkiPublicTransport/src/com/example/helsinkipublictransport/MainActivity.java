package com.example.helsinkipublictransport;


import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.View;
import android.widget.Button;

public class MainActivity extends Activity {
	public final static String API_URL = "http://group36.naf.cs.hut.fi/transport_website/transport_route/";
	public final static int LOGIN_REQUEST_CODE = 1;
	
	// User variables
	private String accessToken;
	private boolean userLogged;
	
	// Views
	Button buttonRegister, buttonLogin, buttonLogout;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		
		// Register button
		buttonRegister = (Button) findViewById(R.id.buttonMenuRegister);
		buttonLogin = (Button) findViewById(R.id.buttonMenuLogin);
		buttonLogout = (Button) findViewById(R.id.buttonMenuLogout);
		
		// Initially the user is not logged
		userLogged = false;
		
		// Hide logout button
		buttonLogout.setEnabled(false);
		buttonLogout.setVisibility(1);
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.main, menu);
		return true;
	}

	public void searchRoute(View view) {
		// Start the activity to set the parameters of the route
		Intent intent = new Intent(this, SearchRouteActivity.class);
		intent.putExtra("userLogged", userLogged);
		// If user logged pass the accessToken to the activity
		if (userLogged) {
			intent.putExtra("accessToken", accessToken);
		}
		startActivity(intent);
	}
	
	public void login(View view) {
		// Start the activity to set the parameters of the route
		Intent intent = new Intent(this, LoginActivity.class);
		startActivityForResult(intent, LOGIN_REQUEST_CODE);
	}
	
	public void logout(View view) {
		userLogged = false;
		accessToken = null;
		// Enable login button
		buttonLogin.setEnabled(true);
		buttonLogin.setVisibility(View.VISIBLE);
		// Enable register button
		buttonRegister.setEnabled(true);
		buttonRegister.setVisibility(View.VISIBLE);
		// Disable logout
		buttonLogout.setEnabled(false);
		buttonLogout.setVisibility(1);
	}

	public void register(View view) {
		// Start the activity to set the parameters of the route
		Intent intent = new Intent(this, RegisterUserActivity.class);
		startActivity(intent);
	}
	
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		if (requestCode == LOGIN_REQUEST_CODE) {
			if (resultCode == RESULT_OK) {
				userLogged = true;
				// Save the access token
				accessToken = data.getStringExtra("accessToken");
				// Hide the register and login buttons
				buttonLogin.setEnabled(false);
				buttonLogin.setVisibility(1);
				buttonRegister.setEnabled(false);
				buttonRegister.setVisibility(1);
				// Enable logout button
				buttonLogout.setEnabled(true);
				buttonLogout.setVisibility(View.VISIBLE);
			}
		}
	}
}
