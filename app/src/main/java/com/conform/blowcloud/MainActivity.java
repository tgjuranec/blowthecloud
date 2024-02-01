package com.conform.blowcloud;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.media.AudioManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.provider.Settings;
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
    Thread thread;
    private static final int PERMISSIONS_REQUEST = 1;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        tvEmulatedFiles = findViewById(R.id.txtEmulatedFiles);
        tvStoragelFiles = findViewById(R.id.txtStorageFiles);

        tvStatus = findViewById(R.id.txtStatus);
        btStart = findViewById(R.id.btStartBackup);

        tvStatus.setText(R.string.strCollectingData);

        dmEmulated = new DocsManager();
        dmStorage = new DocsManager();
        dmStorageOTG = new DocsManager();



        if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{android.Manifest.permission.WRITE_EXTERNAL_STORAGE}, PERMISSIONS_REQUEST);
        } else {

           // Permission has already been granted
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            storage_dir = Environment.getStorageDirectory();
            external_files_dirv30 = getExternalFilesDir(null);
            emulated_dir = Environment.getExternalStorageDirectory();
        }
        else{
            emulated_dir = Environment.getExternalStorageDirectory();
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            if(!Environment.isExternalStorageManager()){
                Intent fIntent = new Intent(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION);
                //startActivityIfNeeded(fIntent, 501);
            }
        }
        storageFiles = storage_dir.listFiles();
        emulatedFiles = emulated_dir.listFiles();

        // GET
        // INTERNAL - APPLICATION'S STORAGE
        //Cursor internal = openMediaStore(MediaStore.Images.Media.getContentUri(MediaStore.VOLUME_INTERNAL));
        // EXTERNAL - ALL SHARED STORAGES - FLASH + SD CARD + USB ALL FILES
        //Cursor external = openMediaStore(MediaStore.Images.Media.getContentUri(MediaStore.VOLUME_EXTERNAL));
        // EXTERNAL - flash storage on device
        //Cursor primary = openMediaStore(MediaStore.Images.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY));
        // ALL DOCUMENTS!!!
        //Cursor primary = openMediaStore(MediaStore.Files.getContentUri("external"));


        Runnable r = new Runnable() {
            @Override
            public void run() {
                getFilesFromMediaStore(MediaStore.Files.getContentUri("external"));
            }
        };

        thread = new Thread(r);
        thread.start();


        try {
            thread.join();
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
        Map.Entry<String,Long> entry = dmStorage.fileList.entrySet().iterator().next();
        StorageList sl = new StorageList(UtilFunc.getVolumeRoot(entry.getKey()));
        tvStatus.setText(sl.storageReport + "\nTotal file size [MB]: " + dmEmulated.TotalFileSize/1000000 );


        btStart.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                try{
                    thread.join();
                    String [] strRemovableFiles = new String[10];
                    int i = 0;
                    for(String str : dmStorage.fileList.keySet()){
                        if(i == 0){
                            strRemovableFiles[i] = UtilFunc.getVolumeRoot(str) + "\n";
                            i++;
                        }
                        strRemovableFiles[i] = UtilFunc.getRelativeDir(str);
                        i++;
                        if(i >= 10){
                            break;
                        }
                    }
                    printStrings(strRemovableFiles, tvStoragelFiles);

                    String[] strEmulatedFiles = new String[10];
                    i = 0;
                    for(String str : dmEmulated.fileList.keySet()){
                        if(i == 0){
                            strEmulatedFiles[i] = "/storage/emulated/0\n";
                            i++;
                        }
                        strEmulatedFiles[i] = UtilFunc.getRelativeDir(str);
                        i++;
                        if(i >= 10){
                            break;
                        }
                    }
                    printStrings(strEmulatedFiles, tvEmulatedFiles);
                }catch (InterruptedException e){
                    e.printStackTrace();
                }
            }
        });

        // COPY FILES
        //Execution exe = new Execution(this);
        //exe.copyFile("/storage/emulated/0/DCIM/Camera/IMG_20240121_085920528.jpg", "/storage/E94B-150A/imgs.jpg");
        // CREATE URI FROM FILE!!!!!!!!
        //File imagefile= new File(emulated_dir, "DCIM/Camera/IMG_20240121_085920528.jpg");
        //Uri fileSource = Uri.fromFile(imagefile);
        //exe.copyFile(fileSource, "imguri.jpg");
        return;
    }




    private void getFilesFromMediaStore(Uri Mediastore_select){
        // Create a projection - the specific columns we want to return
        // SELECT COLUMN
        String[] projection = { MediaStore.Images.Media._ID,
                                MediaStore.Images.Media.DISPLAY_NAME,
                                MediaStore.Images.Media.RELATIVE_PATH,
                                MediaStore.Images.Media.DATA};
        // FILTER ROWS
        String filter = MediaStore.Images.Media.RELATIVE_PATH + " LIKE ?";
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
        dmEmulated.fillFileSize();
        dmStorage.fillFileSize();
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
}