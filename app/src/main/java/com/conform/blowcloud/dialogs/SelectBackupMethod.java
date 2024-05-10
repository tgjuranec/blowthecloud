package com.conform.blowcloud.dialogs;

import android.content.Context;
import android.content.DialogInterface;

import androidx.appcompat.app.AlertDialog;

public class SelectBackupMethod {
    final String [] methods = {
        "SAF method",
        "File method",
        "MediaStore method",
        "Cancel Backup"
    };
    private Context context;
    // DECLARING OBJECT FOR CONTROL OTHER'S ACTIVITY
    public onSelectBackupMethodListener listener;
    private String TAG = "SelectBackupMethod";

    public SelectBackupMethod(Context c, onSelectBackupMethodListener listener){
        this.context = c;
        this.listener = listener;

    }
    public interface onSelectBackupMethodListener {
        void onSelectBackupMethod(String method);
    }
    public void show() {
        // Prepare the dialog by setting up the builder
        AlertDialog.Builder builder = new AlertDialog.Builder(context);


        builder.setTitle("Set a Destination Drive")
                // Set items to display in the dialog
                .setItems(methods, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int iSelection) {
                        // The 'iSelection' argument contains the index position of the selected item
                        // EXTERNAL FUNCTION CALL
                        listener.onSelectBackupMethod(methods[iSelection]);
                    }
                });

        // Create and show the alert dialog
        builder.create().show();
    }
}
