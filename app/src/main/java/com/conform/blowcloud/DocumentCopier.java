package com.conform.blowcloud;
import android.content.Context;
import android.net.Uri;

import androidx.documentfile.provider.DocumentFile;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

class DocumentCopier {

    private Context context;
    public int fileCount = 0;

    public DocumentCopier(Context context) {
        this.context = context;
    }

    public void copyDirectory(Uri sourceUri, Uri destinationUri) throws IOException {
        DocumentFile sourceDirectory = DocumentFile.fromTreeUri(context, sourceUri);
        DocumentFile destinationDirectory = DocumentFile.fromTreeUri(context, destinationUri);

        if (sourceDirectory.exists() && destinationDirectory.exists()) {
            copyDirectoryRecursively(sourceDirectory, destinationDirectory);
        } else {
            throw new IOException("Source or destination directory does not exist.");
        }
    }

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
                DocumentFile tempDF;
                if((tempDF = destinationDirectory.findFile(sourceFile.getName())) != null){
                    if(tempDF.length() == sourceFile.length()) {
                        continue;
                    }
                }
                DocumentFile newDestinationFile = destinationDirectory.createFile(sourceFile.getType(), sourceFile.getName());

                if (newDestinationFile != null) {
                    fileCount++;
                    copyFile(sourceFile, newDestinationFile);
                }
            }
        }
    }

    private void copyFile(DocumentFile sourceFile, DocumentFile destinationFile) throws IOException {
        try {
            InputStream in = context.getContentResolver().openInputStream(sourceFile.getUri());

            OutputStream out = context.getContentResolver().openOutputStream(destinationFile.getUri());
            byte[] buffer = new byte[1024*512];
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
