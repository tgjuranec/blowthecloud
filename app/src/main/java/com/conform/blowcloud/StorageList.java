package com.conform.blowcloud;

import android.os.Environment;
import android.os.StatFs;

import java.io.File;

/*
CLASS FOR AVAAILABLE STORAGE
 */
public class StorageList {

    public long dataTotal = 0 , dataAvailable = 0;
    public long emulatedTotal = 0, emulatedAvailable = 0;
    public long removableTotal = 0,   removableAvailable = 0;
    public String emulatedReport = "";
    public String removableReport = "";
    public StorageList(String removablePath){
        File path = Environment.getDataDirectory();
        StatFs stat = new StatFs(path.getPath());
        long blockSize = stat.getBlockSizeLong();
        dataTotal = blockSize * stat.getBlockCountLong();
        dataAvailable = blockSize * stat.getAvailableBlocksLong();
        // If the external storage is emulated, we can use the internal storage path.
        File emulatedPath = Environment.getExternalStorageDirectory();
        StatFs emulatedStat = new StatFs(emulatedPath.getPath());
        long emulatedBS = emulatedStat.getBlockSizeLong();
        emulatedTotal = emulatedBS * emulatedStat.getBlockCountLong();
        emulatedAvailable = emulatedBS * emulatedStat.getAvailableBlocksLong();
        emulatedReport = "/storage/emulated/0\n";
        // If the external storage is physical, we need to use the external storage directory.
        if(removablePath != null){
            StatFs storageStat = new StatFs(removablePath);
            long storageBS = storageStat.getBlockSizeLong();
            removableTotal = storageBS * storageStat.getBlockCountLong();
            removableAvailable = storageBS * storageStat.getAvailableBlocksLong();
            removableReport += removablePath + "\nFree space: " + removableAvailable / 1000000 + "MB";
        }
    }
}
