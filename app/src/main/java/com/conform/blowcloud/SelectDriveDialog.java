package com.conform.blowcloud;

import android.content.Context;
import android.content.DialogInterface;

import androidx.appcompat.app.AlertDialog;


public class SelectDriveDialog {
    public String removableRoot = null;
    private Context context;
    public OnDriveSelectedListener listener;

    SelectDriveDialog(Context c, OnDriveSelectedListener listener, String [] driveList){
        this.listener = listener;
        this.context = c;
    }
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
            public void onClick(DialogInterface dialog, int which) {
                // The 'which' argument contains the index position of the selected item
                removableRoot = driveList[which];
                listener.onDriveSelected(driveList[which]);
            }
        });

        // Create and show the alert dialog
        builder.create().show();
    }
}
