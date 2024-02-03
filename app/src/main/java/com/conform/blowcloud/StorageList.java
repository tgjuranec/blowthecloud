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
    String storageReport;
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
        // If the external storage is physical, we need to use the external storage directory.
        StatFs storageStat = new StatFs(removablePath);
        long storageBS = storageStat.getBlockSizeLong();
        removableTotal = storageBS * storageStat.getBlockCountLong();
        removableAvailable = storageBS * storageStat.getAvailableBlocksLong();

        storageReport =
                        "Emulated occupied Space: " + ((emulatedTotal-emulatedAvailable)/1000000) + "MB\n"  +
                        "Removable Space: " + removableAvailable /1000000 + "MB";
    }
}
