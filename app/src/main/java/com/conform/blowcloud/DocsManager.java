package com.conform.blowcloud;

import android.database.Cursor;
import android.util.Log;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;

public class DocsManager {

    public HashMap<String,Long> fileList;
    Thread calculate;
    final String TAG = "BlowCloud - DocsManager";
    int doubleCount = 0;
    static int count = 0;
    public long TotalFileSize = 0;

    public DocsManager(){
        fileList = new HashMap<String,Long>();
    }

    public void addfile(String pathFile){
        File f = new File(pathFile);
        if(f.exists() && f.isFile()) {
            count++;
            if (fileList.put(pathFile, f.length()) != null) {
                doubleCount += 1;
            }
            else{
                TotalFileSize += f.length();
            }
            if(count % 1000 == 0){
                Log.d(TAG, "addfile(), count: " + count);
            }
        }
    }

    public void fillFileSize(){
        Runnable r = new Runnable() {
            @Override
            public void run() {
                try {
                    setFileSize();
                } catch (NoSuchAlgorithmException e) {
                    throw new RuntimeException(e);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }
        };
        calculate = new Thread(r);
        calculate.start();
    }


    private synchronized void setFileSize() throws NoSuchAlgorithmException, IOException {

        for(String strPath : fileList.keySet()){
            //CHECK IF IT'S REGULAR FILE, IF IT'S NOT -- REMOVE THE WHOLE ENTRY
            // BECAUSE IT'S NOT SUITABLE NEITHER FOR HASH NOR FOR BACKUP!!!!!
            File f = new File(strPath);
            if(f.isFile() && !f.isDirectory()){
                fileList.put(strPath, f.length());
            }
            else {
                fileList.remove(strPath);
                Log.e(TAG, "Removed: " + strPath);
            }
        }
    }


    private byte [] sha256(String fullPath)throws NoSuchAlgorithmException, IOException {
        MessageDigest md = MessageDigest.getInstance("SHA-256"); // Get the SHA-256 instance


        // Use DigestInputStream to read the file and update the digest
        try (DigestInputStream dis = new DigestInputStream(new FileInputStream(fullPath), md)) {
            while (dis.read() != -1); // Read the entire file
            md = dis.getMessageDigest();
        }

        // Convert the digest to a hexadecimal string
        /*
        StringBuilder result = new StringBuilder();
        for (byte b : md.digest()) {
            result.append(String.format("%02x", b));
        }
        return result.toString();
        */
        return md.digest();
    }



}
