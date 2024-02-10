package com.conform.blowcloud;

import android.content.Context;
import android.content.DialogInterface;

import androidx.appcompat.app.AlertDialog;

// INTERFACE/LISTENER IMPLEMENTATION
// CALLING EXTERNAL FUNCTION - CONTROL OTHER ACTIVITY
// THIS IS MASTER SIDE:
// CONTAINS: INTERFACE, INTERFACE OBJECT, EXTERNAL FUNCTION CALL
// SLAVE SIDE:
// CONTAINS: LISTENER, FUNCTION IMPLEMENTATION THAT'S BEING CALLED BY MASTER
public class SelectDriveDialog {
    public String removableRoot = null;
    private Context context;
    // DECLARING OBJECT FOR CONTROL OTHER'S ACTIVITY
    public OnDriveSelectedListener listener;

    SelectDriveDialog(Context c, OnDriveSelectedListener listener, String [] driveList){
        this.listener = listener;
        this.context = c;
    }
    // DEFINING ABSTRACTION INTERFACE FOR CALLING FUNCTION OF OTHER'S ACTIVITY
    public interface OnDriveSelectedListener{
        void onDriveSelected(String driveRoot);
    }
    public void show(String [] driveList) {
        // Prepare the dialog by setting up the builder
        AlertDialog.Builder builder = new AlertDialog.Builder(context);

        // WE COPY ALL Strings from driveList which are not null to copyDriveList (we filter it)
        int nDrives = 0;
        for(String str : driveList){
            if(str != null){
                nDrives++;
            }
        }
        String [] copyDriveList = new String[nDrives];
        for (int i = 0; i < nDrives; i++){
            copyDriveList[i] = driveList[i];
        }
        builder.setTitle("Set a Destination Drive")
        // Set items to display in the dialog
            .setItems(copyDriveList, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int iSelection) {
                // The 'iSelection' argument contains the index position of the selected item
                removableRoot = driveList[iSelection];
                // EXTERNAL FUNCTION CALL
                listener.onDriveSelected(driveList[iSelection]);
            }
        });

        // Create and show the alert dialog
        builder.create().show();
    }
}
