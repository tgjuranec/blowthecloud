package com.conform.blowcloud;

import android.util.Log;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Path;

public class UtilFunc {
    static final String emulatedRoot = "/storage/emulated/0";
    static public String getFilename(String fullPath){
        int pos = fullPath.lastIndexOf('/');
        String filename = fullPath.substring(pos+1);
        return filename;
    }

    static public String getAbsoluteDir(String fullPath){
        int pos = fullPath.lastIndexOf('/');
        String absolutPath = fullPath.substring(0,pos);
        return absolutPath;
    }

    static public String getFilename(Path fullPath){
        return getFilename(fullPath.toString());
    }

    /*
    Function returns 'volume root' - directory where drive is mounted from full path
    e.g. /storage/0652-H45/DCIM/Camera/IMG_lklk.jpg -> /storage/0652-H45
     */
    static public String getVolumeRoot(String fullPath){
        // return value must be e.g. '/storage/E320-2540'
        // '/storage/' is fixed so we must find string after it: 'E320-2540'
        // and finally connect them.
        String storageDir = "/storage/";
        int posVolumeNamestart = storageDir.length();
        int posVolumeNameend = fullPath.indexOf('/', posVolumeNamestart);
        if(posVolumeNameend == -1) {
           return null;
        }
        String VolumeName = fullPath.substring(posVolumeNamestart, posVolumeNameend);
        if(VolumeName.contains("emulated")){
            return emulatedRoot;
        }
        return storageDir + VolumeName;
    }

    static public String getRelativePath(String fullPath){
        String output = "";
        if(fullPath.indexOf(emulatedRoot) == 0){
            //IT's emulated
            output = fullPath.substring(emulatedRoot.length()+1);

        }
        else if(fullPath.indexOf(emulatedRoot) == -1){
            // It's REMOVABLE
            String volumeRoot = getVolumeRoot(fullPath);
            if(volumeRoot == null){
                return null;
            }
            output = fullPath.substring(volumeRoot.length()+1);
        }
        else{

        }
        return output;
    }

    /*
   FUNCTION RETURNS RELATIVE PATH WITHOUT FILENAME AND WITH FINAL SLASH
   E.G. /storage/emulated/0/DCIM/Camera/IMG_2121.jpg -> DCIM/Camera/
     */
    static public String getRelativeDir(String fullPath){
        String output = "";
        if(fullPath.indexOf(emulatedRoot) == 0){
            //IT's emulated
            output = fullPath.substring(emulatedRoot.length()+1);

        }
        else if(fullPath.indexOf(emulatedRoot) == -1){
            // It's REMOVABLE
            String volumeRoot = getVolumeRoot(fullPath);
            if(volumeRoot == null){
                return null;
            }
            output = fullPath.substring(volumeRoot.length()+1);
        }
        else{

        }
        // REMOVE FILE NAME FROM THE END
        int iLastPos = output.lastIndexOf('/');
        if(iLastPos == -1 || iLastPos > (output.length())-1){
            return null;
        }
        String finalOutput = output.substring(0,iLastPos+1);

        return finalOutput;
    }

    /*
    imported from phind.com https://www.phind.com/search?cache=t38qnormvtxxdedangk5zxdj
     */
    static public String getSDCardPath() {
        File[] rv = null;
        // Check both possible paths
        rv = new File("/mnt/").listFiles(new FileFilter() {
            public boolean accept(File pathname) {
                return pathname.getName().startsWith("sdcard") && pathname.isDirectory();
            }
        });
        if (rv == null || rv.length == 0) {
            rv = new File("/storage/").listFiles(new FileFilter() {
                public boolean accept(File pathname) {
                    return pathname.getName().startsWith("sdcard") && pathname.isDirectory();
                }
            });
        }
        if (rv != null && rv.length > 0) {
            return rv[0].getAbsolutePath();
        } else {
            return null;
        }
    }

    static public String run(String command){
        String output = "";
        try {
            Process process = Runtime.getRuntime().exec(command);
            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            int read;
            char[] buffer = new char[4096];
            while ((read = reader.read(buffer)) > 0) {
                output += new String(buffer, 0, read);
            }
            reader.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        Log.d("UtilStorage", output);
        return output;
    }

    static public String IncrementFileName(String fullpath){
        String incFullPath;
        int posPeriod = fullpath.lastIndexOf(".");
        if(Character.isDigit(fullpath.charAt(posPeriod-1))){
            if(fullpath.charAt(posPeriod-1)==9){
                incFullPath = fullpath.substring(0,posPeriod-1) + "10" + fullpath.substring(posPeriod);
            }
            else{
                int d = fullpath.charAt(posPeriod-1)+1;
                incFullPath = fullpath.substring(0,posPeriod) + Integer.toString(d) + fullpath.substring(posPeriod);

            }
        }
        else{
            incFullPath = fullpath.substring(0,posPeriod) + "0" + fullpath.substring(posPeriod);
        }
        return incFullPath;
    }


}
