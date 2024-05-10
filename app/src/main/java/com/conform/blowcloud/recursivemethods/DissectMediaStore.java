package com.conform.blowcloud.recursivemethods;

import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.provider.MediaStore;

import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import com.conform.blowcloud.DocsManager;
import com.conform.blowcloud.MainActivity;
import com.conform.blowcloud.ThreadProgressActivity;

public class DissectMediaStore {
    private Context context;
    private String emulatedRoot, removableRoot;
    public DocsManager dmEmulated, dmRemovable;
    public long fileSkipped = 0;
    public long fileCopied = 0;
    public long fileExcepted = 0;
    public long fileSizeTotalCopied = 0;
    public long nRequestedFilesToBackup=0;
    public long nSkippedFilesToBackup = 0;
    public long progressStep=0;
    private Uri emulatedUri, removableUri;

    DissectMediaStore(Context c,  Uri emulated, Uri removable, DocsManager dmEmulated, DocsManager dmRemovable){
        this.context = c;
        this.dmEmulated = dmEmulated;
        this.dmRemovable = dmRemovable;
        this.emulatedRoot = dmEmulated.rootDir;
        this.removableRoot = dmRemovable.rootDir;
        final Uri Mediastore_select = MediaStore.Files.getContentUri("external");
        boolean progressActivityStarted = false;
        // Create a projection - the specific columns we want to return
        // SELECT COLUMN

        String[] projection = { MediaStore.Images.Media._ID,
                MediaStore.Images.Media.DISPLAY_NAME,
                MediaStore.Images.Media.DATA};
        // FILTER ROWS
        String filter = MediaStore.Images.Media.DATA + " LIKE ?";
        String [] filterArg = {"%emulated%"};
        // Create a cursor pointing to the SDCard
        Cursor cursor = context.getContentResolver().query(Mediastore_select,
                projection, // Which columns to return
                null,       // Return all rows
                null,       // Return all rows
                null);      // Default sort order

        int columnIndex = cursor.getColumnIndexOrThrow(MediaStore.Images.Media._ID);
        int columndata = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA);
        int stepProgress = 0;
        // Use the cursor to get the image IDs
        if(cursor.moveToLast()) {
            int nExeCopyFiles = cursor.getPosition();
            if(nExeCopyFiles > 1000){
                Intent progressActivity = new Intent(context, ThreadProgressActivity.class);
                progressActivity.putExtra("Title", "Collecting and Analyzing Data...");
                context.startActivity(progressActivity);
                progressActivityStarted = true;
            }
            stepProgress = nExeCopyFiles /100;
            int cExeCopyFile = 0;

            cursor.moveToFirst();
            while (cursor.moveToNext()) {
                int id = cursor.getInt(columnIndex);
                String d = cursor.getString(columndata);

                if(d.contains(emulatedRoot.toString())){
                    dmEmulated.addfile(d);
                    nRequestedFilesToBackup++;
                }
                else if (dmRemovable.rootDir != null && d.contains(dmRemovable.rootDir)){
                    dmRemovable.addfile(d);
                }
                else{
                    //ERROR
                }
                cExeCopyFile++;
                //  UPDATE PROGRESS
                if(progressActivityStarted == true && cExeCopyFile % stepProgress == 0){
                    Intent intent = new Intent("com.conform.ACTION_UPDATE_PROGRESS");
                    int progress = 100* cExeCopyFile / nExeCopyFiles;
                    intent.putExtra("Progress", progress);
                    LocalBroadcastManager.getInstance(context).sendBroadcast(intent);
                }
            }
        }
        cursor.close();
        if(progressActivityStarted == true){
            // STOP PROGRESS BAR ACTIVITY
            Intent intent = new Intent("com.conform.ACTION_FINISH");
            LocalBroadcastManager.getInstance(c).sendBroadcast(intent);
        }
    }

    public void copy(){

    }


}
