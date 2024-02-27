package com.conform.blowcloud;

import android.content.Context;
import android.os.Build;
import android.os.storage.StorageManager;
import android.os.storage.StorageVolume;
import android.util.Log;

import java.util.HashMap;
import java.util.List;

public class UtilStorage {
    private Context context;
    public HashMap<String,String> storageList;
    public String [] mountedRemovables  = new String[10]; // list of destination's root candidates (user selects if N>1)
    public UtilStorage(Context c){
        context = c;
        storageList = new HashMap<String,String>();  // <path,label>
        StorageManager storageManager = (StorageManager) context.getSystemService(Context.STORAGE_SERVICE);
        if (storageManager == null) {
            return;
        }

        String [] commandOutput;
        List<StorageVolume> storageVolumes = storageManager.getStorageVolumes();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            for (StorageVolume volume : storageVolumes) {
                storageList.put(volume.getDirectory().getAbsolutePath(), volume.getDescription(context));
                /*
                out += "Label: " + volume.getDescription(context) +
                        "\nVolume Name: " + volume.getMediaStoreVolumeName() +
                        "\nPath: " + volume.getDirectory() +
                        "\nUUID: " + volume.getUuid() +
                        "\nIs Primary: " + volume.isPrimary() +
                        "\nIs Emulated: " + volume.isEmulated() +
                        "\nIs Removable: " + volume.isRemovable() + "\n";
                 */

            }
            // fill mounted removable
            int i = 0;
            for(String storage: storageList.keySet()){
                if(!storage.contains("emulated") && i < 10){
                    mountedRemovables[i] = storage;
                    i++;
                }
                else{
                    continue;
                }
            }
        }
        else{
            // find all mounted drives (except emulated)
            commandOutput = UtilFunc.run("mount").split("\\r?\\n");

            int nRemovablesMounted = 0;
            for (String str : commandOutput){
                if(str.contains("vfat") && !str.contains("emulated")){
                    mountedRemovables[nRemovablesMounted] = str.substring(str.lastIndexOf(" ")+1);
                    nRemovablesMounted++;
                }
            }
            for (StorageVolume volume : storageVolumes) {
                boolean isPrimaryAndEmulated = volume.isPrimary() && volume.isEmulated();
                String value = volume.getDescription(context);
                String key = "";
                String uuid = volume.getUuid();
                if(isPrimaryAndEmulated){
                    key = "/storage/emulated/0";
                }
                else{
                    for (String dfStorage:mountedRemovables){
                        if(dfStorage != null && dfStorage.contains(uuid)){
                            key = dfStorage;
                            break;
                        }
                    }

                }
                storageList.put(key,value);
            }
        }
    }
}
