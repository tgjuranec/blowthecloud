package com.conform.blowcloud;

import android.content.Context;
import android.content.DialogInterface;
import android.os.Build;
import android.os.Environment;
import android.os.storage.StorageManager;
import android.os.storage.StorageVolume;
import android.util.Log;

import androidx.appcompat.app.AlertDialog;

import java.util.List;

// INTERFACE/LISTENER IMPLEMENTATION
// CALLING EXTERNAL FUNCTION - CONTROL OTHER ACTIVITY
// THIS IS MASTER SIDE:
// CONTAINS: INTERFACE, INTERFACE OBJECT, EXTERNAL FUNCTION CALL
// SLAVE SIDE:
// CONTAINS: LISTENER, FUNCTION IMPLEMENTATION THAT'S BEING CALLED BY MASTER

// DONETODO: ADD CANCEL TO MENU
public class SelectDriveDialog {
    public String removableRoot = null;
    private Context context;
    // DECLARING OBJECT FOR CONTROL OTHER'S ACTIVITY
    public OnDriveSelectedListener listener;
    private String TAG = "SelectDriveDialog";

    SelectDriveDialog(Context c, OnDriveSelectedListener listener){
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


        builder.setTitle("Set a Destination Drive")
        // Set items to display in the dialog
            .setItems(driveList, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int iSelection) {
                // The 'iSelection' argument contains the index position of the selected item
                removableRoot = driveList[iSelection];
                // EXTERNAL FUNCTION CALL
                listener.onDriveSelected(removableRoot);
            }
        });

        // Create and show the alert dialog
        builder.create().show();
    }
    public void printStorageVolumes(Context context) {
        StorageManager storageManager = (StorageManager) context.getSystemService(Context.STORAGE_SERVICE);
        if (storageManager == null) {
            return;
        }

            List<StorageVolume> storageVolumes = storageManager.getStorageVolumes();
            for (StorageVolume volume : storageVolumes) {
                Log.d(TAG, "Storage Volume: " + volume.getDescription(context) +
                        "\nVolume Name: " + volume.getMediaStoreVolumeName() +
                        "\nPath: " + volume.getDirectory() +
                        "\nUUID: " + volume.getUuid() +
                        "\nIs Primary: " + volume.isPrimary() +
                        "\nIs Emulated: " + volume.isEmulated() +
                        "\nIs Removable: " + volume.isRemovable());
            }

    }

}
