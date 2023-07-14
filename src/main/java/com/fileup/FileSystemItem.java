package com.fileup;

import java.io.Serializable;

public class FileSystemItem implements Serializable {

    private String path;
    private boolean isDirectory;

    public FileSystemItem(String path, boolean isDirectory) {
        this.path = path;
        this.isDirectory = isDirectory;
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public boolean isDirectory() {
        return isDirectory;
    }

    public void setDirectory(boolean directory) {
        isDirectory = directory;
    }
}