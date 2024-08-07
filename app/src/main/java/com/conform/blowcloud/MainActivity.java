package com.conform.blowcloud;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.provider.MediaStore;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import com.conform.blowcloud.dialogs.SelectBackupMethod;
import com.conform.blowcloud.dialogs.SelectDriveDialog;
import com.conform.blowcloud.recursivemethods.DissectDirRecursive;
import com.conform.blowcloud.recursivemethods.SAFRecursive;
import com.conform.blowcloud.utility.UtilFunc;
import com.conform.blowcloud.utility.UtilStorage;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

// UI
// DONE: CREATE PROGRESS BAR - CREATED NEW ACTIVITY THREADPROGRESS
// TODO: ADD VISUAL PRESENTATION OF FILE SIZE AND FREE SPACE ON DRIVE
// TODO: INCLUDE FAT32 (VFAT) FILE SIZE LIMITS OF
// DONE: SOLVE THE PROBLEM OF NOT COPYING FROM XIAOMI - java.nio.file.AccessDeniedException: /storage/8F5B-16E9/DCIM/Camera/IMG_20230527_170223.jpg
// DONE: CREATE SYSTEMATICALLY CALLS: MEDIASTORE, FILESYSTEM, SAF (DocumentFile)



public class MainActivity extends AppCompatActivity implements SelectDriveDialog.OnDriveSelectedListener, SelectBackupMethod.onSelectBackupMethodListener{

    File emulated_dir, storage_dir;
    TextView tvStoragelFiles, tvEmulatedFiles, tvStatus;
    ImageView imgEmulated, imgRemovable;
    Button btStart;
    DocsManager dmEmulated, dmRemovable;
    StorageStat sl;
    String removableRoot;  //determines destination's root directory
    String [] mountedRemovables; // list of destination's root candidates (user selects if N>1)
    long filesToBackupSize = 0;
    List<String> fBackup;
    private static final int MY_PERMISSIONS_REQUEST_WRITE_EXTERNAL_STORAGE = 1;
    ThreadRunner runner;
    SelectBackupMethod selectBackupMethod;
    SelectDriveDialog selectDriveDialog;
    DissectDirRecursive disDir;
    Context context = this;
    UtilStorage utilStorage;
    SAFRecursive saf;

    int REQUEST_CODE_OPEN_DESTINATION_DIRECTORY = 77;
    int REQUEST_CODE_OPEN_SOURCE_DIRECTORY = 777;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        tvEmulatedFiles = findViewById(R.id.txtEmulatedFiles);
        tvStoragelFiles = findViewById(R.id.txtStorageFiles);
        tvStatus = findViewById(R.id.txtStatus);
        btStart = findViewById(R.id.btStartBackup);
        imgEmulated = findViewById(R.id.imgCirleEmulated);
        imgRemovable = findViewById(R.id.imgCirleRemovable);
        tvStatus.setText(R.string.strCollectingData);


        emulated_dir = Environment.getExternalStorageDirectory();
        dmEmulated = new DocsManager();
        dmRemovable = new DocsManager();
        dmEmulated.rootDir = emulated_dir.getPath();


        if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, MY_PERMISSIONS_REQUEST_WRITE_EXTERNAL_STORAGE);
        }
        else {

           // Permission has already been granted
        }

        selectBackupMethod = new SelectBackupMethod(this,this);
        selectBackupMethod.show();


        runner = new ThreadRunner(new ThreadCallback() {
            @Override
            public void AnalyzeData() {
                // Use runOnUiThread to update the UI on the main thread
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        AnalyzeAndReport();
                    }
                });
            }

            @Override
            public void onCopyFilesFinished() {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        tvEmulatedFiles.setText("");
                        tvStoragelFiles.setText("");
                        tvStatus.setText(R.string.strCopyingFinished);
                        if(current == state.BACKUP_APPROVED_SAF && saf != null){
                            tvStatus.append(" " + saf.fileCopied + ",");
                            tvStatus.append(" " + (saf.fileSizeTotalCopied / 1000000) + " MB\n");
                            tvStatus.append("Skipped: " + saf.fileSkipped + saf.fileExcepted + "\n");
                        }
                        else if (current == state.BACKUP_APPROVED_DISSECT_DIR && disDir != null){
                            tvStatus.append(" " + disDir.fileCopied + ",");
                            tvStatus.append("" + (disDir.fileSizeTotalCopied / 1000000) + " MB\n");
                            tvStatus.append("Skipped: " + disDir.fileSkipped + "\n");


                        }
                        else if (current == state.BACKUP_APPROVED_MEDIASTORE){

                        }
                        else {

                        }
                        btStart.setVisibility(View.GONE);
                    }
                });
            }
        });



        btStart.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(current == state.BACKUP_APPROVED_SAF){
                    runner.copyFilesUri();
                }
                else if (current == state.BACKUP_APPROVED_DISSECT_DIR){
                    runner.copyFiles();
                }
                else if (current == state.BACKUP_APPROVED_MEDIASTORE){

                }
                else {

                }
            }
        });
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
        String [] splitted = driveRoot.split(" ");
        if(driveRoot.equals("Cancel")) {
            current = state.BACKUP_CANCELED;
            AnalyzeAndReport();
        }
        else {
            removableRoot = splitted[0];
            dmRemovable.rootDir = splitted[0];
            runner.collectData();
        }
    }



    @Override
    public void onSelectBackupMethod(String method) {
        if(method.equals("Cancel Backup")){
            current = state.BACKUP_CANCELED;
        }
        else if (method.equals("SAF method")){
            current = state.BACKUP_APPROVED_SAF;
        }
        else if (method.equals("File method")){
            current = state.BACKUP_APPROVED_DISSECT_DIR;
            //check number of mounted removables drives
            utilStorage = new UtilStorage(this);
            int nRemovablesMounted = utilStorage.storageList.size() - 1;
            mountedRemovables = utilStorage.mountedRemovables;
            if (nRemovablesMounted == 0) {
                current = state.NO_DESTINATION;
            } else if (nRemovablesMounted >= 1) {
                String[] copyDriveList = new String[nRemovablesMounted + 1];
                for (int i = 0; i < nRemovablesMounted; i++) {
                    copyDriveList[i] = mountedRemovables[i] + " \"" + utilStorage.storageList.get(mountedRemovables[i]) + "\"";
                }
                copyDriveList[nRemovablesMounted] = "Cancel";
                selectDriveDialog = new SelectDriveDialog(this, this);
                selectDriveDialog.show(copyDriveList);
            }
            return;

        }
        else if (method.equals("MediaStore method")){
            current = state.BACKUP_APPROVED_MEDIASTORE;
        }
        else {

        }

        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT_TREE);
        startActivityForResult(intent, REQUEST_CODE_OPEN_SOURCE_DIRECTORY);

    }

    public interface ThreadCallback {
        void AnalyzeData();
        void onCopyFilesFinished();
    }

    final Handler uiHandler = new Handler(Looper.getMainLooper());

    public class ThreadRunner {
        private final ThreadCallback callback;

        public ThreadRunner(ThreadCallback callback) {
            this.callback = callback;
        }


        public void collectData(){
            new Thread(() -> {
                if (current == state.BACKUP_APPROVED_SAF){
                    saf = new SAFRecursive(context, sourceUri, destinationUri, dmEmulated, dmRemovable);
                    // dmRemovable is defined by Uri and after processing
                    removableRoot = dmRemovable.rootDir;
                }
                else if (current == state.BACKUP_APPROVED_DISSECT_DIR){
                    dmEmulated.rootDir = emulated_dir.getAbsolutePath();
                    dmRemovable.rootDir = removableRoot;
                    disDir = new DissectDirRecursive(context,sourceUri, destinationUri,dmEmulated, dmRemovable);
                }
                else if (current == state.BACKUP_APPROVED_MEDIASTORE){
                    dmEmulated.rootDir = emulated_dir.getAbsolutePath();
                    dmRemovable.rootDir = removableRoot;

                }
                else{
                    return;
                }
                callback.AnalyzeData();
            }).start();
        }

        public void copyFiles(){
            new Thread(() -> {
                // Thread for copy files
                disDir.copyFiles();
                callback.onCopyFilesFinished();
            }).start();

        }

        public void copyFilesUri(){
            new Thread(()->{
                if(sourceUri != null && destinationUri != null){
                    try {
                        saf.copyDirectory();
                    }
                    catch (IOException e) {
                        e.printStackTrace();
                    }
                }
                callback.onCopyFilesFinished();
            }).start();
        }
        public void CancelTask(){
            new Thread(() ->{
                current = state.BACKUP_CANCELED;
                callback.AnalyzeData();
            }).start();
        }
    }

    private void AnalyzeAndReport(){
        sl = new StorageStat(removableRoot);
        tvEmulatedFiles.setText(sl.emulatedReport);
        tvStoragelFiles.setText(sl.removableReport);
        String statusreport = "";
        int iEmulatedDiameter, iRemovableDiameter;
        if(current == state.BACKUP_APPROVED || current == state.BACKUP_APPROVED_SAF || current == state.BACKUP_APPROVED_DISSECT_DIR) {
            fBackup = FilesToBackup(dmEmulated, dmRemovable);
             statusreport = "Files to backup: " + fBackup.size() + "\n" +
                    "Backup File Size : " + filesToBackupSize / 1000000 + " MB\n";
            if (fBackup.size() == 0) {
                current = state.NO_FILES_FOR_BACKUP;
            } else if ((filesToBackupSize + 10000000) > sl.removableAvailable) {
                current = state.DESTINATION_SPACE_NOT_SUFFICIENT;
            }
            // SET SIZE OF CIRCLES
            if (filesToBackupSize > 0) {
                double AvailToReqRatio = (double) sl.removableAvailable / filesToBackupSize;
                if (AvailToReqRatio > 25) {
                    iRemovableDiameter = 100;
                    iEmulatedDiameter = 20;
                } else if (AvailToReqRatio <= 25 && AvailToReqRatio > 1) {
                    iRemovableDiameter = 100;
                    iEmulatedDiameter = (int) Math.sqrt(1 / AvailToReqRatio) * 100;
                } else if (AvailToReqRatio <= 1 && AvailToReqRatio > 0.04) {
                    iEmulatedDiameter = 100;
                    iRemovableDiameter = (int) Math.sqrt(AvailToReqRatio) * 100;
                } else {
                    iEmulatedDiameter = 100;
                    iRemovableDiameter = 20;
                }
            }
            else{
                iEmulatedDiameter = 20;
                iRemovableDiameter = 100;
            }

            tvEmulatedFiles.append("Total file size: "+ dmEmulated.TotalFileSize / 1000000 + "MB\n");
            tvEmulatedFiles.append("Backup file size: " + filesToBackupSize / 1000000 + "MB\n");
        }
        else{
            iEmulatedDiameter = 20;
            iRemovableDiameter = 100;
        }

        ViewGroup.LayoutParams emulatedParams = imgEmulated.getLayoutParams();
        ViewGroup.LayoutParams removableParams = imgRemovable.getLayoutParams();
        float scale = getResources().getDisplayMetrics().density;
        emulatedParams.width = (int) scale * (iEmulatedDiameter);
        emulatedParams.height = (int) scale * (iEmulatedDiameter);
        removableParams.width = (int) scale * (iRemovableDiameter);
        removableParams.height = (int) scale * (iRemovableDiameter);
        imgEmulated.layout(20,(140 - iEmulatedDiameter)/2,20,20);
        imgRemovable.layout(20,(140 - iRemovableDiameter)/2,20,20);
        imgEmulated.setLayoutParams(emulatedParams);
        imgRemovable.setLayoutParams(removableParams);


        switch(current){
            case BACKUP_APPROVED:
                tvStatus.setText(R.string.strStateBackupApproved);
                break;
            case BACKUP_APPROVED_SAF:
                tvStatus.setText(R.string.strStateBackupApproved);
                break;
            case BACKUP_APPROVED_DISSECT_DIR:
                tvStatus.setText(R.string.strStateBackupApproved);
                break;
            case BACKUP_APPROVED_MEDIASTORE:
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
            case BACKUP_CANCELED:
                tvStatus.setText(R.string.strStateCanceled);
                btStart.setVisibility(View.GONE);
        }
        tvStatus.append("\n" + statusreport);
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
    Uri sourceUri;
    Uri destinationUri;
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        // ONLY SAF METHOD
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_CODE_OPEN_SOURCE_DIRECTORY && resultCode == RESULT_OK) {
            sourceUri = data.getData();
            // Request the URI of the destination directory
            Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT_TREE);
            startActivityForResult(intent, REQUEST_CODE_OPEN_DESTINATION_DIRECTORY);
        }
        else if(requestCode == REQUEST_CODE_OPEN_DESTINATION_DIRECTORY && resultCode == RESULT_OK){
            destinationUri = data.getData();
            if(sourceUri.getPath().equals(destinationUri.getPath())){
                current = state.NO_FILES_FOR_BACKUP;
            }
            else{

            }
            runner.collectData();
        }
        else{

        }
    }

    public enum state{
        BACKUP_APPROVED,
        BACKUP_APPROVED_DISSECT_DIR,
        BACKUP_APPROVED_MEDIASTORE,
        BACKUP_APPROVED_SAF,
        NO_DESTINATION,
        TWO_DESTINATIONS_POSSIBLE,
        DESTINATION_SPACE_NOT_SUFFICIENT,
        NO_FILES_FOR_BACKUP,
        SUSPICIOUS_SYSTEM_STATE,
        BACKUP_CANCELED,
    };

    public state current = state.NO_FILES_FOR_BACKUP;

}