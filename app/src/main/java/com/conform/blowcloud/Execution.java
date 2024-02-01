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
import java.nio.file.Files;
import java.nio.file.Paths;

public class Execution {
    static Context context;

    Execution(Context c){
        context = c;
    }


    public boolean copyFile(String source, String destination){
        try {
            FileInputStream fis = new FileInputStream(source);
            FileOutputStream fos = new FileOutputStream(destination);

            byte[] buffer = new byte[1024];
            int length;
            while ((length = fis.read(buffer)) > 0) {
                fos.write(buffer, 0, length);
            }

            fis.close();
            fos.close();
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }finally {
        }
        return true;
    }

    public void BackupAll(String dir){
        Uri fileUri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI;
        /*
        String selection = MediaStore.Images.Media.BUCKET_DISPLAY_NAME + "=?";

        String[] selectionArgs = new String[]{"DCIM/Camera"};
        */
        String [] projection = {MediaStore.Images.Media._ID, MediaStore.Images.Media.BUCKET_DISPLAY_NAME, MediaStore.Images.Media.DISPLAY_NAME};
        Cursor cursor = context.getContentResolver().query(fileUri, projection, null, null, null);
        if (cursor != null && cursor.moveToFirst()) {
            while(cursor.moveToNext()) {
                int idColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media._ID);
                long id = cursor.getLong(idColumn);
                String filename = cursor.getString(cursor.getColumnIndexOrThrow(MediaStore.Images.Media.BUCKET_DISPLAY_NAME));
                Uri contentUri = ContentUris.withAppendedId(fileUri, id);
                copyFile(contentUri, filename);

                // Now you have the Uri of the file in the DCIM/Camera directory
            }
        }
    }

    /*
    How to get an Uri
    Uri fileUri = Uri.parse("android.resource://your.package.name/" + R.drawable.your_image);
     */
    public void copyFile(Uri fileUri, String filename){
        InputStream in = null;
        OutputStream out = null;
        try {
            in = context.getContentResolver().openInputStream(fileUri);
            File outFile = new File(Environment.getExternalStorageDirectory(), filename);
            out = new FileOutputStream(outFile);
            byte[] buffer = new byte[1024];
            int length;
            while ((length = in.read(buffer)) > 0){
                out.write(buffer, 0, length);
            }
            in.close();
            out.flush();
            out.close();
        } catch (IOException e) {
            Log.e("tag", "Failed to copy file", e);
        }
    }

    private void fromStoreToUri(){
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

    void copyUsingMediaStore(Uri sourceFile){
        String targetDisplayName = UtilFunc.getFilename(sourceFile.getPath());
        String relativePathName = UtilFunc.getRelativeDir(sourceFile.getPath());
        ContentValues values = new ContentValues();
        values.put(MediaStore.MediaColumns.DISPLAY_NAME, targetDisplayName);
        //values.put(MediaStore.MediaColumns.MIME_TYPE, "text/plain");
        values.put(MediaStore.MediaColumns.RELATIVE_PATH, relativePathName);

        Uri uri = context.getContentResolver().insert(MediaStore.Files.getContentUri("external"), values);

        try {
            InputStream in = context.getContentResolver().openInputStream(sourceFile);
            OutputStream outputStream = context.getContentResolver().openOutputStream(uri);
            byte[] buffer = new byte[1024];
            int length;
            while ((length = in.read(buffer)) > 0){
                outputStream.write(buffer, 0, length);
            }
            in.close();
            outputStream.flush();
            outputStream.close();

        } catch (IOException e) {
            e.printStackTrace();
        }

    }
    void copyUsingMediaStore(String sourceFile){
        String targetDisplayName = UtilFunc.getFilename(sourceFile);
        String relativePathName = UtilFunc.getRelativeDir(sourceFile);
        ContentValues values = new ContentValues();
        values.put(MediaStore.MediaColumns.DISPLAY_NAME, targetDisplayName);
        //values.put(MediaStore.MediaColumns.MIME_TYPE, "text/plain");
        values.put(MediaStore.MediaColumns.RELATIVE_PATH, relativePathName);

        Uri uri = context.getContentResolver().insert(MediaStore.Files.getContentUri("external"), values);

        try {
            InputStream in = Files.newInputStream(Paths.get(sourceFile));
            OutputStream outputStream = context.getContentResolver().openOutputStream(uri);
            byte[] buffer = new byte[1024];
            int length;
            while ((length = in.read(buffer)) > 0){
                outputStream.write(buffer, 0, length);
            }
            in.close();
            outputStream.flush();
            outputStream.close();

        } catch (IOException e) {
            e.printStackTrace();
        }

    }
}
