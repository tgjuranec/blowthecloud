package com.conform.blowcloud;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Environment;
import android.util.Log;

import androidx.documentfile.provider.DocumentFile;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Objects;

class SAFRecursive {
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
    byte[] buffer = new byte[1024*1024];

    public SAFRecursive(Context context, Uri emulated, Uri removable, DocsManager dmEm, DocsManager dmRem) {
        this.context = context;

        this.emulatedUri = emulated;
        this.removableUri = removable;
        this.emulatedRoot = Environment.getExternalStorageDirectory().getAbsolutePath();
        this.removableRoot = removable.getPath().replace("tree","storage").replace(":","");
        this.dmEmulated = dmEm;
        this.dmRemovable = dmRem;
        dmEmulated.rootDir = emulatedRoot;
        dmRemovable.rootDir = removableRoot;
        collectData();
        if(nRequestedFilesToBackup == 0) nRequestedFilesToBackup = 10000;
        progressStep = nRequestedFilesToBackup/100;

    }

    private void collectData(){
        // OPEN REPORTING ACTIVITY
        Intent progressActivity = new Intent(context, ThreadProgressActivity.class);
        progressActivity.putExtra("Title", "Collecting and Analyzing data. Please Wait...");
        context.startActivity(progressActivity);
        // START COPYING
        DocumentFile sourceDirectory = DocumentFile.fromTreeUri(context, emulatedUri);
        DocumentFile destinationDirectory = DocumentFile.fromTreeUri(context, removableUri);
        try {
            if (sourceDirectory.exists() && destinationDirectory.exists()) {
                collectDataRecursively(sourceDirectory, destinationDirectory);
            } else {
                throw new IOException("Source or destination directory does not exist.");
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        LocalBroadcastManager.getInstance(context).sendBroadcast(new Intent("com.conform.ACTION_FINISH"));
    }

    /*
    Function counts number of files allowed to backup
    Function retrieves two parameters source dir and destination dir
    If destination dir is null (does not exist), all files from source are
    allowed to backup
     */
    private void collectDataRecursively(DocumentFile dfSource, DocumentFile dfDest){
        for (DocumentFile source: dfSource.listFiles()){
            if(source.isDirectory()){
                //CHECK IF DIRECTORY EXISTS IN DESTINATION
                DocumentFile tmpDir = null;
                if(dfDest != null && (tmpDir = dfDest.findFile(Objects.requireNonNull(source.getName()))) != null)
                    collectDataRecursively(source, tmpDir);
                else{
                    collectDataRecursively(source,null);
                }
            }
            else{
                String [] uriSplit  = dfSource.getUri().getPath().split(":");
                String relativePath;
                if (uriSplit.length == 3) {
                    relativePath = uriSplit[2] + "/";
                }
                else if(uriSplit.length == 2){
                    relativePath = "/";
                }
                else{
                    Log.e("SAFRecursive", "Wrong uri: " + dfSource.getUri().getPath());
                    continue;
                }
                dmEmulated.addfile(emulatedRoot +"/" + relativePath + source.getName());

                if(dfDest == null){
                    nRequestedFilesToBackup++;
                }
                else{
                    // CHECK IF FILE ALREADY EXISTS ON DESTINATION
                    File tmpFile = new File (removableRoot + "/" + relativePath + source.getName());
                    if(tmpFile.exists()){
                        // FILE EXISTS ON DESTINATION
                        dmRemovable.addfile(removableRoot +"/" + relativePath + source.getName());
                        if(tmpFile.length() == source.length()) {
                            // FILE EXISTS AND IT'S THE SAME SIZE AS A SOURCE FILE -> NO BACKUP
                            nSkippedFilesToBackup++;
                        }
                        else{
                            // FILE EXISTS BUT WITH DIFFERENT SIZE
                            nRequestedFilesToBackup++;

                        }
                    }
                    else{
                        nRequestedFilesToBackup++;
                    }
                }
            }
        }
    }


    public void copyDirectory() throws IOException {
        // OPEN REPORTING ACTIVITY
        Intent progressActivity = new Intent(context, ThreadProgressActivity.class);
        progressActivity.putExtra("Title", "Copying Data. Please Wait...");
        context.startActivity(progressActivity);
        // START COPYING
        DocumentFile sourceDirectory = DocumentFile.fromTreeUri(context, emulatedUri);
        DocumentFile destinationDirectory = DocumentFile.fromTreeUri(context, removableUri);

        if (sourceDirectory.exists() && destinationDirectory.exists()) {
            copyDirectoryRecursively(sourceDirectory, destinationDirectory);
        } else {
            throw new IOException("Source or destination directory does not exist.");
        }

        LocalBroadcastManager.getInstance(context).sendBroadcast(new Intent("com.conform.ACTION_FINISH"));
    }

    //
    private void copyDirectoryRecursively(DocumentFile sourceDirectory, DocumentFile destinationDirectory) throws IOException {
        for (DocumentFile sourceFile : sourceDirectory.listFiles()) {
            if (sourceFile.isDirectory()) {
                DocumentFile tmpDir, newDestinationDirectory = null;
                tmpDir = destinationDirectory.findFile(sourceFile.getName());
                if(tmpDir != null && tmpDir.exists()){
                    // IF DIR ALREADY EXISTS
                    newDestinationDirectory = tmpDir;
                }
                else{
                    // IF DIR DOES NOT EXIST
                    newDestinationDirectory = destinationDirectory.createDirectory(sourceFile.getName());
                }

                if (newDestinationDirectory != null) {
                    copyDirectoryRecursively(sourceFile, newDestinationDirectory);
                }
            } else {
                String [] uriSplit  = destinationDirectory.getUri().getPath().split(":");
                String relativePath;
                if (uriSplit.length == 3) {
                    relativePath = uriSplit[2] + "/";
                }
                else if(uriSplit.length == 2){
                    relativePath = "/";
                }
                else{
                    Log.e("SAFRecursive", "Wrong uri: " + destinationDirectory.getUri().getPath());
                    continue;
                }

                File tmpFile = new File (removableRoot + "/" + relativePath + sourceFile.getName());
                if(tmpFile.exists()){
                    if(tmpFile.length() == sourceFile.length()) {
                        fileSkipped++;
                        continue;
                    }
                }
                DocumentFile newDestinationFile;
                newDestinationFile = destinationDirectory.createFile(sourceFile.getType(), sourceFile.getName());

                if (newDestinationFile != null) {
                    fileCopied++;
                    fileSizeTotalCopied += sourceFile.length();
                    copyFile(sourceFile, newDestinationFile);
                }
                else{
                    fileExcepted++;

                }

                long  i = fileCopied + fileExcepted +fileSkipped;
                if((i % progressStep) == 0) {
                    Intent intent = new Intent("com.conform.ACTION_UPDATE_PROGRESS");
                    long progress = 100* i / ((int) nRequestedFilesToBackup);
                    intent.putExtra("Progress", (int) progress);
                    String TAG = "BlowCloud";
                    Log.d(TAG,"" + progress);
                    LocalBroadcastManager.getInstance(context).sendBroadcast(intent);
                }
            }
        }
    }

    private void copyFile(DocumentFile sourceFile, DocumentFile destinationFile) throws IOException {
        try {
            InputStream in = context.getContentResolver().openInputStream(sourceFile.getUri());

            OutputStream out = context.getContentResolver().openOutputStream(destinationFile.getUri());

            int nRead;
            while ((nRead = in.read(buffer)) != -1) {
                out.write(buffer,  0, nRead);
            }
            in.close();
            out.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
