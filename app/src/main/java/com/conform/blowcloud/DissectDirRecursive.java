package com.conform.blowcloud;

import java.io.File;

public class DissectDirRecursive {
    public DocsManager dm;
    private int deeplevel = 0;
    public int nDir = 0;
    public DissectDirRecursive(String rootPath, DocsManager docsManager){
        dm = docsManager;
        dm.rootDir = rootPath;
        DissectDir(rootPath);
    }

    private void DissectDir(String dir){
        deeplevel++;
        File fDir = new File(dir);
        if(fDir.exists() && fDir.isDirectory()){
            File [] files = ls(fDir);
            if(files != null) {
                for (File item : files) {
                    if (item.isFile()) {
                        dm.addfile(item.getAbsolutePath());
                    } else if (item.isDirectory() && deeplevel < 10) {
                        DissectDir(item.getAbsolutePath());
                        nDir++;
                    }
                }
            }
        }
        deeplevel--;
    }


    private File[] ls(File dir){
        File [] ls = null;
        if(dir.isDirectory()){
            ls = dir.listFiles();
        }
        return ls;
    }

}
