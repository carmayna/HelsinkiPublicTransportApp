package com.example.helsinkipublictransport;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;

//Part of the code extract from:
//http://developer.android.com/guide/topics/ui/dialogs.html#PassingEvents

public class IncorrectPasswordDialog extends DialogFragment {
	
	/* The activity that creates an instance of this dialog fragment must
     * implement this interface in order to receive event callbacks.
     * Each method passes the DialogFragment in case the host needs to query it. */
    public interface IncorrectPasswordDialogListener {
        public void onDialogNeutralClickIncorrectPassword(DialogFragment dialog);
    }
    
    // Use this instance of the interface to deliver action events
    IncorrectPasswordDialogListener mListener;
    
    // Override the Fragment.onAttach() method to instantiate the RegistrartionNicknameErrorDialogListener
    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        // Verify that the host activity implements the callback interface
        try {
            // Instantiate the NoticeDialogListener so we can send events to the host
            mListener = (IncorrectPasswordDialogListener) activity;
        } catch (ClassCastException e) {
            // The activity doesn't implement the interface, throw exception
            throw new ClassCastException(activity.toString()
                    + " must implement UserRegisteredDialogDialogListener");
        }
    }
	
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        // Use the Builder class for convenient dialog construction
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setMessage(R.string.dialog_incorrect_password)
               .setNeutralButton(R.string.dialog_incorrect_password_button, new DialogInterface.OnClickListener() {
                   public void onClick(DialogInterface dialog, int id) {
                       // Close the dialog
                	   IncorrectPasswordDialog.this.getDialog().cancel();
                	   // Send the positive button event back to the host activity
                       mListener.onDialogNeutralClickIncorrectPassword(IncorrectPasswordDialog.this);
                   }
               })
               .setTitle(R.string.dialog_incorrect_password_title);
        // Create the AlertDialog object and return it
        return builder.create();
    }
    
}
