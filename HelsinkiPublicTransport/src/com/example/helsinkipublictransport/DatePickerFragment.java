package com.example.helsinkipublictransport;

import java.util.Calendar;

import android.annotation.SuppressLint;
import android.app.DatePickerDialog;
import android.app.Dialog;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;

// Code extracted from: http://developer.android.com/guide/topics/ui/controls/pickers.html

@SuppressLint("ValidFragment")
public class DatePickerFragment extends DialogFragment {
	private DatePickerDialog.OnDateSetListener listener;

	public DatePickerFragment(DatePickerDialog.OnDateSetListener listener) {
        super();
        this.listener = listener;
    }
	
	@Override
	public Dialog onCreateDialog(Bundle savedInstanceState) {
		// Use the current date as the default date in the picker
		final Calendar c = Calendar.getInstance();
		int year = c.get(Calendar.YEAR);
		int month = c.get(Calendar.MONTH);
		int day = c.get(Calendar.DAY_OF_MONTH);
		
		// Create a new instance of DatePickerDialog and return it
		return new DatePickerDialog(getActivity(), listener, year, month, day);
	}
}
