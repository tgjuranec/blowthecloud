package com.conform.blowcloud;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import java.io.File;
import java.util.Map;


public class MainActivity extends AppCompatActivity {

    File emulated_dir, storage_dir, external_files_dirv30;
    TextView tvStoragelFiles, tvEmulatedFiles, tvStatus;
    Button btStart;
    DocsManager dmEmulated, dmStorage, dmStorageOTG;
    File [] storageFiles;
    File [] emulatedFiles;
    StorageList sl;
    Execution exe;
    String removableRoot;
    String [] commandOutput;
    String [] mountedRoots;
    private static final int MY_PERMISSIONS_REQUEST_WRITE_EXTERNAL_STORAGE = 1;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        exe = new Execution(this);
        tvEmulatedFiles = findViewById(R.id.txtEmulatedFiles);
        tvStoragelFiles = findViewById(R.id.txtStorageFiles);

        tvStatus = findViewById(R.id.txtStatus);
        btStart = findViewById(R.id.btStartBackup);

        tvStatus.setText(R.string.strCollectingData);

        dmEmulated = new DocsManager();
        dmStorage = new DocsManager();
        dmStorageOTG = new DocsManager();
        mountedRoots = new String[3];
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            // find all mounted drives (except emulated)
            commandOutput = UtilFunc.run("df").split("\\r?\\n");
            int i = 0;
            for (String str : commandOutput){
                if(str.indexOf("/dev/fuse") != -1 && str.indexOf("/storage/emulated") == -1){
                    mountedRoots[i] = str.substring(str.lastIndexOf(" ")+1);
                    i++;
                }
            }
            storage_dir = Environment.getStorageDirectory();
            emulated_dir = Environment.getExternalStorageDirectory();
        }
        else{
            emulated_dir = Environment.getExternalStorageDirectory();
        }


        if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{android.Manifest.permission.WRITE_EXTERNAL_STORAGE}, MY_PERMISSIONS_REQUEST_WRITE_EXTERNAL_STORAGE);
        } else {

           // Permission has already been granted
        }


        ThreadRunner runner = new ThreadRunner(new ThreadCallback() {
            @Override
            public void onGetMediastoreDataFinished() {
                // Use runOnUiThread to update the UI on the main thread
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        sl = new StorageList(removableRoot);
                        tvStatus.setText(sl.storageReport);
                    }
                });
            }

            @Override
            public void onCopyFilesFinished() {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        tvStatus.setText(R.string.strCopyingFinished);

                    }
                });
            }
        });
        runner.getMediastoreData();



        btStart.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //CHECK AVAILABLE SIZE
                long requestedFileSize = dmEmulated.TotalFileSize;
                long availableSpace = sl.emulatedTotal;
                long offset = 10000000;
                if((requestedFileSize + offset) >= availableSpace) {
                    // THERE IS NOT ENOUGH SPACE - BACKUP STOPPED
                    tvStatus.setText(R.string.strNotEnoughSpace + "\n" +
                            sl.storageReport + "\nTotal file size [MB]: " + dmEmulated.TotalFileSize / 1000000);
                }
                else{
                    // THERE IS ENOUGH SPACE - BACKUP MAY START
                    tvStatus.setText(R.string.strCopyingStarted + "\n" + sl.storageReport + "\nTotal file size [MB]: " + dmEmulated.TotalFileSize / 1000000);
                    runner.copyFiles();
                }
            }
        });


        return;
    }

    /*
    CALLBACK function after permission are processed (enabled or otherwise)
     */
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        switch (requestCode) {
            case MY_PERMISSIONS_REQUEST_WRITE_EXTERNAL_STORAGE: {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    // permission was granted
                } else {
                    // permission denied
                }
                return;
            }
            // other 'case' lines to check for other permissions this app might request
        }
    }


    private void startCopy(){
        int i = 0;
        for (String f : dmEmulated.fileList.keySet()) {
            i++;
            exe.copyFile(f, removableRoot);
            if (i >= 10000) return;
        }
    }

    private void getFilesFromMediaStore(Uri Mediastore_select){
        // Create a projection - the specific columns we want to return
        // SELECT COLUMN
        String[] projection = { MediaStore.Images.Media._ID,
                                MediaStore.Images.Media.DISPLAY_NAME,
                                MediaStore.Images.Media.DATA};
        // FILTER ROWS
        String filter = MediaStore.Images.Media.DATA + " LIKE ?";
        String [] filterArg = {"%emulated%"};
        // Create a cursor pointing to the SDCard
        Cursor cursor = getContentResolver().query(Mediastore_select,
                projection, // Which columns to return
                null,       // Return all rows
                null,       // Return all rows
                null);      // Default sort order

        int columnIndex = cursor.getColumnIndexOrThrow(MediaStore.Images.Media._ID);
        int columndata = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA);

        // Use the cursor to get the image IDs
        if(cursor != null && cursor.moveToLast()) {
            int position = cursor.getPosition();
            cursor.moveToFirst();
            while (cursor.moveToNext()) {
                int id = cursor.getInt(columnIndex);
                String d = cursor.getString(columndata);

                if(d.contains(emulated_dir.toString())){
                    dmEmulated.addfile(d);
                }
                else{
                    dmStorage.addfile(d);

                }
            }
        }
        cursor.close();
    }


    private File [] ls(File dir){
        File [] ls = null;
        if(dir.isDirectory()){
            ls = dir.listFiles();
        }
        return ls;
    }


    private void printStrings(String [] strList, TextView tv){
        if(tv == null){
            return;
        }
        if(strList == null){
            tv.setText(R.string.strNoFiles);
            return;
        }
        String strPrint= "";
        for(int i = 0 ; i < 10; i++){
            strPrint = strPrint + strList[i] + "\n";
        }
        tv.setText(strPrint);
    }

    public interface ThreadCallback {
        void onGetMediastoreDataFinished();
        void onCopyFilesFinished();
    }

    public interface ThreadCallbackCF{

    }
    public class ThreadRunner {
        private ThreadCallback callback;

        public ThreadRunner(ThreadCallback callback) {
            this.callback = callback;
        }

        public void getMediastoreData() {
            new Thread(() -> {
                // Perform your thread operations here
                getFilesFromMediaStore(MediaStore.Files.getContentUri("external"));
                Map.Entry<String,Long> entry = dmStorage.fileList.entrySet().iterator().next(); //only to get '/storage/E230-3541'
                //TODO: IF dmStorage.size() == 0 -> code breaks, find another way to get VolumeRoot
                removableRoot = UtilFunc.getVolumeRoot(entry.getKey());
                // Once the thread is done, call the callback
                callback.onGetMediastoreDataFinished();
            }).start();
        }

        public void copyFiles(){
            new Thread(() -> {
                // Thread for copy files
                startCopy();
                callback.onCopyFilesFinished();

            }).start();
        }
    }

}