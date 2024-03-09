package com.conform.blowcloud;

import static com.conform.blowcloud.BackupStates.state.BACKUP_APPROVED;
import static com.conform.blowcloud.BackupStates.state.BACKUP_APPROVED_DISSECT_DIR;
import static com.conform.blowcloud.BackupStates.state.BACKUP_APPROVED_MEDIASTORE;
import static com.conform.blowcloud.BackupStates.state.BACKUP_APPROVED_SAF;
import static com.conform.blowcloud.BackupStates.state.BACKUP_CANCELED;
import static com.conform.blowcloud.BackupStates.state.DESTINATION_SPACE_NOT_SUFFICIENT;
import static com.conform.blowcloud.BackupStates.state.NO_DESTINATION;
import static com.conform.blowcloud.BackupStates.state.NO_FILES_FOR_BACKUP;

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
import android.os.Build;
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

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

// UI
// DONETODO: CREATE PROGRESS BAR - CREATED NEW ACTIVITY THREADPROGRESS
// TODO: ADD VISUAL PRESENTATION OF FILE SIZE AND FREE SPACE ON DRIVE
// TODO: INCLUDE FAT32 (VFAT) FILE SIZE LIMITS OF
// TODO: SOLVE THE PROBLEM OF NOT COPYING FROM XIAOMI - java.nio.file.AccessDeniedException: /storage/8F5B-16E9/DCIM/Camera/IMG_20230527_170223.jpg
// TODO: CREATE SYSTEMATICALLY CALLS: MEDIASTORE, FILESYSTEM, SAF (DocumentFile)



public class MainActivity extends AppCompatActivity implements SelectDriveDialog.OnDriveSelectedListener, SelectBackupMethod.onSelectBackupMethodListener{

    File emulated_dir, storage_dir;
    TextView tvStoragelFiles, tvEmulatedFiles, tvStatus;
    ImageView imgEmulated, imgRemovable;
    Button btStart;
    DocsManager dmEmulated, dmRemovable;
    File [] removableFiles;
    File [] emulatedFiles;
    StorageStat sl;
    Execution exe;
    String removableRoot;  //determines destination's root directory
    String [] mountedRemovables; // list of destination's root candidates (user selects if N>1)
    long filesToBackupSize = 0;
    List<String> fBackup;
    BackupStates bs;
    int nFilesBackedUp = 0;
    private static final int MY_PERMISSIONS_REQUEST_WRITE_EXTERNAL_STORAGE = 1;
    ThreadRunner runner;
    SelectBackupMethod selectBackupMethod;
    DissectDirRecursive ddrEmulated, disDir;
    Context context = this;
    UtilStorage utilStorage;
    SAFRecursive saf;

    int REQUEST_CODE_OPEN_DESTINATION_DIRECTORY = 77;
    int REQUEST_CODE_OPEN_SOURCE_DIRECTORY = 777;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        bs = new BackupStates();

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

                        tvStatus.setText(R.string.strCopyingFinished);
                        if(bs.current == BACKUP_APPROVED_SAF){
                            tvStatus.append(" " + saf.fileCopied + "\n");
                            tvStatus.append(" " + (saf.fileSizeTotalCopied / 1000000) + " MB");

                        }
                        else if (bs.current == BACKUP_APPROVED_DISSECT_DIR){
                            tvStatus.append(" " + disDir.fileCopied + "\n");
                            tvStatus.append(" " + (disDir.fileSizeTotalCopied / 1000000) + " MB");


                        }
                        else if (bs.current == BACKUP_APPROVED_MEDIASTORE){

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
                if(bs.current == BACKUP_APPROVED_SAF){
                    runner.copyFilesUri();
                }
                else if (bs.current == BACKUP_APPROVED_DISSECT_DIR){
                    runner.copyFiles();
                }
                else if (bs.current == BACKUP_APPROVED_MEDIASTORE){

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


    private void DissectMediaStore(){
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
        Cursor cursor = getContentResolver().query(Mediastore_select,
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
                Intent progressActivity = new Intent(MainActivity.this, ThreadProgressActivity.class);
                progressActivity.putExtra("Title", "Collecting and Analyzing Data...");
                startActivity(progressActivity);
                progressActivityStarted = true;
            }
            stepProgress = nExeCopyFiles /100;
            int cExeCopyFile = 0;

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
                cExeCopyFile++;
                //  UPDATE PROGRESS
                if(progressActivityStarted == true && cExeCopyFile % stepProgress == 0){
                    Intent intent = new Intent("com.conform.ACTION_UPDATE_PROGRESS");
                    int progress = 100* cExeCopyFile / nExeCopyFiles;
                    intent.putExtra("Progress", progress);
                    LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
                }
            }
        }
        cursor.close();
        if(progressActivityStarted == true){
            // STOP PROGRESS BAR ACTIVITY
            Intent intent = new Intent("com.conform.ACTION_FINISH");
            LocalBroadcastManager.getInstance(context).sendBroadcast(intent);
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
            bs.current = BACKUP_CANCELED;
            AnalyzeAndReport();
        }
        else {
            removableRoot = splitted[0];
            dmRemovable.rootDir = splitted[0];
            bs.current = BACKUP_APPROVED_DISSECT_DIR;
            runner.collectData();
        }
    }



    @Override
    public void onSelectBackupMethod(String method) {
        if(method.equals("Cancel Backup")){
            bs.current = BACKUP_CANCELED;
        }
        else if (method.equals("SAF method")){
            bs.current = BACKUP_APPROVED_SAF;
        }
        else if (method.equals("File method")){
            bs.current = BACKUP_APPROVED_DISSECT_DIR;
        }
        else if (method.equals("MediaStore method")){
            bs.current = BACKUP_APPROVED_MEDIASTORE;
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

        public void getMediastoreData() {
            new Thread(() -> {
                // Perform your thread operations here
                DissectMediaStore();
                // Once the thread is done, call the callback
                callback.AnalyzeData();
            }).start();
        }

        public void collectData(){
            new Thread(() -> {
                if (bs.current == BACKUP_APPROVED_SAF){
                    saf = new SAFRecursive(context, sourceUri, destinationUri, dmEmulated, dmRemovable);
                    removableRoot = dmRemovable.rootDir;
                }
                else if (bs.current == BACKUP_APPROVED_DISSECT_DIR){
                    dmEmulated.rootDir = emulated_dir.getAbsolutePath();
                    removableRoot = dmRemovable.rootDir;
                    disDir = new DissectDirRecursive(context,sourceUri, destinationUri,dmEmulated, dmRemovable);
                }
                else if (bs.current == BACKUP_APPROVED_MEDIASTORE){

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
                bs.current = BACKUP_CANCELED;
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
        if(bs.current == BACKUP_APPROVED) {
            fBackup = FilesToBackup(dmEmulated, dmRemovable);
             statusreport = "Files to backup: " + fBackup.size() + "\n" +
                    "Backup File Size : " + filesToBackupSize / 1000000 + " MB\n";
            if (fBackup.size() == 0) {
                bs.current = NO_FILES_FOR_BACKUP;
            } else if ((filesToBackupSize + 10000000) > sl.removableAvailable) {
                bs.current = DESTINATION_SPACE_NOT_SUFFICIENT;
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


        switch(bs.current){
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
                bs.current = NO_DESTINATION;
            }
            else{

            }
            runner.collectData();
        }
        else{

        }
    }

}