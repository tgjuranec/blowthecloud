package com.conform.blowcloud;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.util.Log;

import androidx.documentfile.provider.DocumentFile;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

class DocumentCopier {

    private Context context;
    private String emulatedRoot, removableRoot;
    public long fileSkipped = 0;
    public long fileCount = 0;
    public long fileException = 0;
    public long fileSizeTotalCopied = 0;
    public long nRequestedFilesToBackup=0;
    public long progressStep=0;
    byte[] buffer = new byte[1024*1024];

    public DocumentCopier(Context context, String emulated, String removable, final long filesTobackup) {
        this.context = context;
        this.emulatedRoot = emulated;
        this.removableRoot = removable;
        this.nRequestedFilesToBackup = filesTobackup;
        progressStep = filesTobackup/100;
    }

    public void copyDirectory(Uri sourceUri, Uri destinationUri) throws IOException {
        // OPEN REPORTING ACTIVITY
        Intent progressActivity = new Intent(context, ThreadProgressActivity.class);
        progressActivity.putExtra("Title", "Copying Data. Please Wait...");
        context.startActivity(progressActivity);
        // START COPYING
        DocumentFile sourceDirectory = DocumentFile.fromTreeUri(context, sourceUri);
        DocumentFile destinationDirectory = DocumentFile.fromTreeUri(context, destinationUri);

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
                    Log.e("DocumentCopier", "Wrong uri: " + destinationDirectory.getUri().getPath());
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
                    fileCount++;
                    fileSizeTotalCopied += sourceFile.length();
                    copyFile(sourceFile, newDestinationFile);
                }
                else{
                    fileException++;

                }

                long  i = fileCount+fileException+fileSkipped;
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
