package com.conform.blowcloud;

import static com.conform.blowcloud.BackupStates.state.BACKUP_APPROVED;
import static com.conform.blowcloud.BackupStates.state.DESTINATION_SPACE_NOT_SUFFICIENT;
import static com.conform.blowcloud.BackupStates.state.NO_DESTINATION;
import static com.conform.blowcloud.BackupStates.state.NO_FILES_FOR_BACKUP;

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
import java.util.ArrayList;
import java.util.List;


public class MainActivity extends AppCompatActivity implements SelectDriveDialog.OnDriveSelectedListener{

    File emulated_dir, storage_dir;
    TextView tvStoragelFiles, tvEmulatedFiles, tvStatus;
    Button btStart;
    DocsManager dmEmulated, dmRemovable;
    File [] storageFiles;
    File [] emulatedFiles;
    StorageList sl;
    Execution exe;
    String removableRoot;  //determines destination's root directory
    String [] commandOutput;
    String [] mountedRemovables  = new String[5]; // list of destination's root candidates (user selects if N>1)
    long filesToBackupSize = 0;
    List<String> fBackup;
    BackupStates bs;
    int nFilesBackedUp = 0;
    private static final int MY_PERMISSIONS_REQUEST_WRITE_EXTERNAL_STORAGE = 1;
    SelectDriveDialog selectDriveDialog;
    ThreadRunner runner;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        bs = new BackupStates();

        exe = new Execution(this);
        tvEmulatedFiles = findViewById(R.id.txtEmulatedFiles);
        tvStoragelFiles = findViewById(R.id.txtStorageFiles);
        tvStatus = findViewById(R.id.txtStatus);
        btStart = findViewById(R.id.btStartBackup);
        tvStatus.setText(R.string.strCollectingData);

        // find all mounted drives (except emulated)
        commandOutput = UtilFunc.run("df").split("\\r?\\n");
        int nRemovablesMounted = 0;
        for (String str : commandOutput){
            if(str.contains("/dev/fuse") && !str.contains("/storage/emulated")){
                mountedRemovables[nRemovablesMounted] = str.substring(str.lastIndexOf(" ")+1);
                nRemovablesMounted++;
            }
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            storage_dir = Environment.getStorageDirectory();
            emulated_dir = Environment.getExternalStorageDirectory();
        }
        else{
            emulated_dir = Environment.getExternalStorageDirectory();
        }

        dmEmulated = new DocsManager();
        dmRemovable = new DocsManager();
        dmEmulated.rootDir = emulated_dir.getPath();

        //check number of mounted removables drives
        if(nRemovablesMounted == 0){
            bs.current = NO_DESTINATION;
        }
        else if(nRemovablesMounted == 1){
            removableRoot = mountedRemovables[0];
            dmRemovable.rootDir = removableRoot;
            bs.current = BACKUP_APPROVED;
        }
        else if (nRemovablesMounted >= 2){
            selectDriveDialog = new SelectDriveDialog(this,this,mountedRemovables);
            selectDriveDialog.show(mountedRemovables);
            bs.current = BackupStates.state.TWO_DESTINATIONS_POSSIBLE;
        }




        if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{android.Manifest.permission.WRITE_EXTERNAL_STORAGE}, MY_PERMISSIONS_REQUEST_WRITE_EXTERNAL_STORAGE);
        }
        else {

           // Permission has already been granted
        }



        runner = new ThreadRunner(new ThreadCallback() {
            @Override
            public void onGetMediastoreDataFinished() {
                // Use runOnUiThread to update the UI on the main thread
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        // Collecting data finished -> analyze data
                        String statusreport = AnalyzeAndReport();
                        String filesToBackup = "";
                        if(fBackup.size() == 0){
                            bs.current = NO_FILES_FOR_BACKUP;
                        }
                        for (int i = 0; i < 10 && i < fBackup.size(); i++){
                            filesToBackup += (fBackup.get(i) + "\n");
                        }

                        sl = new StorageList(mountedRemovables);
                        tvEmulatedFiles.setText(sl.emulatedReport);
                        tvEmulatedFiles.append("Total file size: "+ dmEmulated.TotalFileSize / 1000000 + "MB\n");
                        tvEmulatedFiles.append("Backup file size: " + filesToBackupSize / 1000000 + "MB\n");
                        if((filesToBackupSize+10000000) > sl.removableAvailable[0]){
                            bs.current = DESTINATION_SPACE_NOT_SUFFICIENT;
                        }
                        tvEmulatedFiles.append(filesToBackup);
                        tvStoragelFiles.setText(sl.removableReport);
                        switch(bs.current){
                            case BACKUP_APPROVED:
                                tvStatus.setText(R.string.strStateBackupApproved);
                                break;
                            case NO_DESTINATION:
                                tvStatus.setText(R.string.strStateNoDestination);
                                btStart.setVisibility(View.GONE);
                                break;
                            case TWO_DESTINATIONS_POSSIBLE:
                                tvStatus.setText(R.string.strStateTwoDestinations);
                                break;
                            case DESTINATION_SPACE_NOT_SUFFICIENT:
                                tvStatus.setText(R.string.strStateNotEnoughSpace);
                                btStart.setVisibility(View.GONE);
                                break;
                            case NO_FILES_FOR_BACKUP:
                                tvStatus.setText(R.string.strStateNoFilesForBackup);
                                btStart.setVisibility(View.GONE);
                                break;
                            case SUSPICIOUS_SYSTEM_STATE:
                                tvStatus.setText(R.string.strStateSuspSystem);
                                btStart.setVisibility(View.GONE);
                                break;
                        }
                        tvStatus.append("\n" + statusreport);
                    }
                });
            }

            @Override
            public void onCopyFilesFinished() {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        tvStatus.setText(R.string.strCopyingFinished);
                        tvStatus.append(" " + nFilesBackedUp);

                    }
                });
            }

            public void onUpdateUI(int count){
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        tvStatus.setText("Files copied: " + count);

                    }
                });
            }
        });

        if(removableRoot != null) {
            runner.getMediastoreData();
        }


        btStart.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //CHECK AVAILABLE SIZE
                long requestedFileSize = dmEmulated.TotalFileSize;
                long availableSpace = sl.emulatedTotal;
                long offset = 10000000;
                if((requestedFileSize + offset) >= availableSpace) {
                    // THERE IS NOT ENOUGH SPACE - BACKUP STOPPED
                    tvStatus.setText(R.string.strStateNotEnoughSpace);
                }
                else{
                    // THERE IS ENOUGH SPACE - BACKUP MAY START

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
        if (requestCode == MY_PERMISSIONS_REQUEST_WRITE_EXTERNAL_STORAGE) {// If request is cancelled, the result arrays are empty.
            // TODO: handle first use after accepting / or declining permission by user
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // permission was granted
            } else {
                // permission denied
            }
            // other 'case' lines to check for other permissions this app might request
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
        if(cursor.moveToLast()) {
            int position = cursor.getPosition();
            cursor.moveToFirst();
            while (cursor.moveToNext()) {
                int id = cursor.getInt(columnIndex);
                String d = cursor.getString(columndata);

                if(d.contains(emulated_dir.toString())){
                    dmEmulated.addfile(d);
                }
                else if (dmRemovable.rootDir != null && d.contains(dmRemovable.rootDir)){
                    dmRemovable.addfile(d);
                }
                else{
                    //ERROR
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

    /*
    Callback function from dialog
     */
    @Override
    public void onDriveSelected(String driveRoot) {
        removableRoot = driveRoot;
        dmRemovable.rootDir = driveRoot;
        // One thing
        if(removableRoot != null) {
            runner.getMediastoreData();
        }
    }

    public interface ThreadCallback {
        void onGetMediastoreDataFinished();
        void onCopyFilesFinished();
        void onUpdateUI(int count);
    }


    public class ThreadRunner {
        private final ThreadCallback callback;

        public ThreadRunner(ThreadCallback callback) {
            this.callback = callback;
        }

        public void getMediastoreData() {
            new Thread(() -> {
                // Perform your thread operations here
                getFilesFromMediaStore(MediaStore.Files.getContentUri("external"));
                // Once the thread is done, call the callback
                callback.onGetMediastoreDataFinished();
            }).start();
        }

        public void copyFiles(){
            new Thread(() -> {
                // Thread for copy files
                int i = 0;
                nFilesBackedUp = 0;
                for (String f : dmEmulated.fileList.keySet()) {
                    i++;
                    if(exe.copyFile(f, UtilFunc.getVolumeRoot(f),removableRoot)){
                        nFilesBackedUp += 1;
                    }
                    if(i%20 == 0) {
                        callback.onUpdateUI(i);
                    }
                    if (i >= 10000) return;
                }
                callback.onCopyFilesFinished();

            }).start();
        }
    }

    private String AnalyzeAndReport(){
        fBackup = FilesToBackup(dmEmulated, dmRemovable);
        String report = "Files to backup: " + fBackup.size() + "\n" +
                "Backup File Size : " + filesToBackupSize / 1000000 + " MB\n";
        return report;
    }

    private List<String> FilesToBackup(DocsManager source, DocsManager target){
        List<String> filesToBackup = new ArrayList<>();
        for (String str : source.fileList.keySet()){
            // convert path from emulated to the same path on the removable
            String targetKey = target.rootDir + "/" + UtilFunc.getRelativePath(str);
            // TEST IF FILE FROM EMULATED ALREADY EXISTS ON REMOVABLE
            if(target.fileList.containsKey(targetKey)){
                // FILE WITH THE SAME NAME EXISTS, CHECK IF FILES' CONTENT IS IDENTICAL
                if(source.fileList.get(str) == target.fileList.get(targetKey)){
                    //FILE CONTENT IS IDENTICAL - SKIP COPYING
                    // TODO: CHECK HASH VALUE OF BOTH FILES
                }
                else{
                    // FILE NAMES ARE THE SAME, BUT FILES ARE DIFFERENT - SET ANOTHER NAME
                    //
                }
            }
            else {
                // EVERYTHING IS OK
                filesToBackup.add(str);
                filesToBackupSize += source.fileList.get(str);
            }
        }
        return filesToBackup;
    }


}