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
    public long [] removableTotal,   removableAvailable;
    public String emulatedReport;
    public String removableReport = "";
    public StorageList(String [] removablePath){
        removableTotal = new long[removablePath.length];
        removableAvailable = new long[removablePath.length];

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
        emulatedReport = "/storage/emulated/0: " + ((emulatedTotal-emulatedAvailable)/1000000) + "MB\n";
        // If the external storage is physical, we need to use the external storage directory.
        for (int i = 0; i < removablePath.length && removablePath[i] != null; i++){
            StatFs storageStat = new StatFs(removablePath[i]);
            long storageBS = storageStat.getBlockSizeLong();
            removableTotal[i] = storageBS * storageStat.getBlockCountLong();
            removableAvailable[i] = storageBS * storageStat.getAvailableBlocksLong();
            removableReport += removablePath[i] + " Available: " + removableAvailable[i] /1000000 + "MB";
        }
    }
}
