package com.example.helsinkipublictransport;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;

public class NotNetworkDialog extends DialogFragment {
	
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        // Use the Builder class for convenient dialog construction
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setMessage(R.string.dialog_not_network)
               .setNeutralButton(R.string.dialog_network_neutral_button, new DialogInterface.OnClickListener() {
                   public void onClick(DialogInterface dialog, int id) {
                       // Close the dialog
                	   NotNetworkDialog.this.getDialog().cancel();
                   }
               })
               .setTitle(R.string.dialog_network_title);
        // Create the AlertDialog object and return it
        return builder.create();
    }
    
}
