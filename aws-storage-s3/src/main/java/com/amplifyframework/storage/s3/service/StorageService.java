package com.amplifyframework.storage.s3.service;

import android.content.Context;

import com.amazonaws.mobileconnectors.s3.transferutility.TransferObserver;
import com.amazonaws.regions.Region;
import com.amazonaws.services.s3.AmazonS3Client;
import com.amplifyframework.storage.result.StorageListResult;

import java.io.File;
import java.util.Map;

public interface StorageService {
    //URL getPresignedUrl(String serviceKey, int expires);
    TransferObserver downloadToFile(String serviceKey, File file);
    TransferObserver uploadFile(String serviceKey, File file);
    TransferObserver uploadFile(String serviceKey, File file, Map<String, String> metadata);
    StorageListResult listFiles(String path);
    void deleteObject(String serviceKey);
    void pauseTransfer(TransferObserver transfer);
    void resumeTransfer(TransferObserver transfer);
    void cancelTransfer(TransferObserver transfer);
    AmazonS3Client getClient();

    /**
     * A method to create a storage service.
     */
    interface Factory {
        /**
         *
         * @param context
         * @param region
         * @param bucketName
         * @return
         */
        StorageService create(Context context, Region region, String bucketName);
    }
}