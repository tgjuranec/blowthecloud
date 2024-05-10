package com.conform.blowcloud.recursivemethods;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Environment;
import android.util.Log;

import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import com.conform.blowcloud.DocsManager;
import com.conform.blowcloud.utility.Execution;
import com.conform.blowcloud.ThreadProgressActivity;
import com.conform.blowcloud.utility.UtilFunc;

import java.io.File;
import java.util.Stack;

public class DissectDirRecursive {
    private Context context;
    private String emulatedRoot, removableRoot;
    public DocsManager dmEmulated, dmRemovable;
    private int deeplevel = 0;
    public long fileSkipped = 0;
    public long fileCopied = 0;
    public long fileExcepted = 0;
    public long fileSizeTotalCopied = 0;
    public long nRequestedFilesToBackup=0;
    public long nSkippedFilesToBackup = 0;
    public long progressStep=0;

    public DissectDirRecursive(Context context, Uri emulated, Uri removable, DocsManager dmEmulated, DocsManager dmRemovable){
        this.context = context;
        Intent progressActivity = new Intent(context, ThreadProgressActivity.class);
        progressActivity.putExtra("Title", "Collecting and Analyzing Data...");
        context.startActivity(progressActivity);
        this.emulatedRoot = Environment.getExternalStorageDirectory().getAbsolutePath();
        if(removable != null) {
            this.removableRoot = removable.getPath().replace("tree","storage").replace(":","");
            dmRemovable.rootDir = this.removableRoot;
        }
        this.dmEmulated = dmEmulated;
        this.dmRemovable = dmRemovable;
        DissectDirIterative(dmEmulated.rootDir, dmEmulated);
        DissectDirIterative(dmRemovable.rootDir, dmRemovable);
        Intent intent = new Intent("com.conform.ACTION_FINISH");
        LocalBroadcastManager.getInstance(context).sendBroadcast(intent);
    }

    private void DissectDir(String dir, DocsManager dm){
        deeplevel++;
        File fDir = new File(dir);
        if(fDir.exists() && fDir.isDirectory()){
            File [] files = ls(fDir);
            if(files != null) {
                for (File item : files) {
                    if (item.isFile()) {
                        dm.addfile(item.getAbsolutePath());
                        nRequestedFilesToBackup++;
                    } else if (item.isDirectory() && deeplevel < 10) {
                        DissectDir(item.getAbsolutePath(), dm);
                    }
                }
            }
        }
        deeplevel--;
     }

    private void DissectDirIterative(String dir, DocsManager dm){
        Stack<File> stack = new Stack<>();
        File fDir = new File(dir);
        if(fDir == null || !fDir.exists()){
            Log.e("DissectDirIterative()", dir + " does not exist!");
            return;
        }
        stack.push(new File(dir));

        while (!stack.isEmpty()) {
            File current = stack.pop();
            if (current.isDirectory()) {
                File[] files = current.listFiles();
                if (files != null) {
                    for (File file : files) {
                        stack.push(file);
                    }
                }
            }
            else {
                // Process the file
                dm.addfile(current.getAbsolutePath());
                nRequestedFilesToBackup++;
            }
        }
    }
    public void copyFiles(){
        if(dmRemovable.rootDir == null || dmEmulated.rootDir == null){
            return;
        }
        if(nRequestedFilesToBackup == 0){
            return;
        }
        Execution exe = new Execution(context);
        int i = 0;
        final long step = nRequestedFilesToBackup / 100;
        Intent progressActivity = new Intent(context, ThreadProgressActivity.class);
        progressActivity.putExtra("Title", "Copying Data. Please Wait...");
        context.startActivity(progressActivity);
        for (String f : dmEmulated.fileList.keySet()) {
            i++;
            if(exe.copyFile(f, UtilFunc.getVolumeRoot(f),dmRemovable.rootDir)){
                fileCopied++;
                fileSizeTotalCopied += f.length();
            }
            else{
                fileSkipped++;
            }
            if((i % step) == 0) {
                Intent intent = new Intent("com.conform.ACTION_UPDATE_PROGRESS");
                long progress = 100* i / nRequestedFilesToBackup;
                intent.putExtra("Progress", progress);
                String TAG = "BlowCloud";
                Log.d(TAG,"" + progress);
                LocalBroadcastManager.getInstance(context).sendBroadcast(intent);
            }
        }
        LocalBroadcastManager.getInstance(context).sendBroadcast(new Intent("com.conform.ACTION_FINISH"));
    }


    private File[] ls(File dir){
        File [] ls = null;
        if(dir.isDirectory()){
            try {
                ls = dir.listFiles();
            }
            catch (StackOverflowError err){
            }
        }
        return ls;
    }
}
