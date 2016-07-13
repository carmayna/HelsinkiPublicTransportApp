package com.example.helsinkipublictransport;

import java.util.Calendar;

import android.annotation.SuppressLint;
import android.app.Dialog;
import android.app.TimePickerDialog;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.text.format.DateFormat;

//Code extracted from: http://developer.android.com/guide/topics/ui/controls/pickers.html

@SuppressLint("ValidFragment")
public class TimePickerFragment extends DialogFragment {
    private TimePickerDialog.OnTimeSetListener listener;

	public TimePickerFragment(TimePickerDialog.OnTimeSetListener listener) {
        super();
        this.listener = listener;
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        // Use the current time as the default values for the picker
        final Calendar c = Calendar.getInstance();
        int hour = c.get(Calendar.HOUR_OF_DAY);
        int minute = c.get(Calendar.MINUTE);

        // Create a new instance of TimePickerDialog and return it
        return new TimePickerDialog(getActivity(), listener, hour, minute,
        DateFormat.is24HourFormat(getActivity()));
    }
}
