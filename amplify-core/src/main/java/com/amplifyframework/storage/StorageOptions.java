package com.amplifyframework.storage;

import com.amplifyframework.core.task.Options;

public class StorageOptions extends Options {
    private FileAccessLevel fileAccessLevel;
    private String contentType;


    public StorageOptions fileAccessLevel(FileAccessLevel fileAccessLevel) {
        this.fileAccessLevel = fileAccessLevel;
        return this;
    }

    public StorageOptions contentType(String contentType) {
        this.contentType = contentType;
        return this;
    }

    public FileAccessLevel getFileAccessLevel() {
        return fileAccessLevel;
    }

    public String getContentType() {
        return contentType;
    }
}
