package com.conform.blowcloud;

import java.nio.file.Path;

public class UtilFunc {
    static final String emulatedRoot = "/storage/emulated/0";
    static public String getFilename(String fullPath){
        int pos = fullPath.lastIndexOf('/');
        String filename = fullPath.substring(pos+1);
        return filename;
    }

    static public String getFilename(Path fullPath){
        return getFilename(fullPath.toString());
    }

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

}
