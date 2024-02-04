package com.conform.blowcloud;

import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Log;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;

public class Execution {
    static Context context;

    Execution(Context c) {
        context = c;
    }


    public void BackupAll(String dir) {
        Uri fileUri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI;
        /*
        String selection = MediaStore.Images.Media.BUCKET_DISPLAY_NAME + "=?";

        String[] selectionArgs = new String[]{"DCIM/Camera"};
        */
        String[] projection = {MediaStore.Images.Media._ID, MediaStore.Images.Media.BUCKET_DISPLAY_NAME, MediaStore.Images.Media.DISPLAY_NAME};
        Cursor cursor = context.getContentResolver().query(fileUri, projection, null, null, null);
        if (cursor != null && cursor.moveToFirst()) {
            while (cursor.moveToNext()) {
                int idColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media._ID);
                long id = cursor.getLong(idColumn);
                String filename = cursor.getString(cursor.getColumnIndexOrThrow(MediaStore.Images.Media.BUCKET_DISPLAY_NAME));
                Uri contentUri = ContentUris.withAppendedId(fileUri, id);
                copyFile(contentUri, UtilFunc.getVolumeRoot(contentUri.getPath()),filename);

                // Now you have the Uri of the file in the DCIM/Camera directory
            }
        }
    }

    /*
    How to get an Uri
    Uri fileUri = Uri.parse("android.resource://your.package.name/" + R.drawable.your_image);
     */
    public void copyFile(Uri sourceFileUri, String sourceRoot, String targetRoot){
        InputStream in = null;
        OutputStream out = null;
        String sourceFile = sourceFileUri.getPath();
        String targetFile = sourceFile.replace(sourceRoot, targetRoot);
        File fTargetFile = new File(targetFile);
        // SKIP IF FILE ALREADY EXISTS!!!
        if(fTargetFile.exists()){
            return;
        }
        File targetAbsDir = new File(UtilFunc.getAbsoluteDir(targetFile));
        if(!targetAbsDir.exists()){
            targetAbsDir.mkdirs();
        }

        Path sourcePath = Paths.get(sourceFile);
        Path targetPath = Paths.get(targetFile);

        try {
            Files.copy(sourcePath, targetPath, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public boolean copyFile(String sourceFile, String sourceRoot, String targetRoot) {
        InputStream in = null;
        OutputStream out = null;
        String targetFile = sourceFile.replace(sourceRoot, targetRoot);
        // TEST IF FILE EXISTS
        File fTargetFile = new File(targetFile);
        if(fTargetFile.exists()){
            File fSourceFile = new File(sourceFile);
            if(fSourceFile.length() == fTargetFile.length()){
                return false;
            }
            else {
                UtilFunc.IncrementFileName(targetFile);
            }
        }
        File targetAbsDir = new File(UtilFunc.getAbsoluteDir(targetFile));
        if(!targetAbsDir.exists()){
            targetAbsDir.mkdirs();
        }
        Path sourcePath = Paths.get(sourceFile);
        Path targetPath = Paths.get(targetFile);
        try {
            Files.copy(sourcePath, targetPath);
        } catch (FileAlreadyExistsException ex){
            // TODO: HANDLE THAT EXCEPTION (RENAME AND TRY AGAIN)
            return false;
        }
        catch (IOException e) {
            return false;
        }
        return true;
    }

    private void fromStoreToUri() {
        // Define a projection that specifies which columns from the database you will use after this query.
        String[] projection = new String[]{
                MediaStore.Images.Media._ID,
                MediaStore.Images.Media.DISPLAY_NAME
                // Add more columns here as needed
        };

        // Add a selection criterion
        String selection = MediaStore.Images.Media.RELATIVE_PATH + "=?";
        String[] selectionArgs = new String[]{
                "DCIM/Camera" // This specifies the folder name
        };

        // Query the MediaStore to get a cursor with the desired files
        try (Cursor cursor = context.getContentResolver().query(
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                projection,
                selection,
                selectionArgs,
                null)) {
            // Cache column indices
            int idColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media._ID);

            while (cursor.moveToNext()) {
                // Get the ID of the image file
                long id = cursor.getLong(idColumn);

                // Create the Uri for the file
                Uri contentUri = ContentUris.withAppendedId(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, id);

                // Now you can use the contentUri as needed
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

    }
}