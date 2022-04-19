/*
 * Copyright 2020 Amazon.com, Inc. or its affiliates. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License").
 * You may not use this file except in compliance with the License.
 * A copy of the License is located at
 *
 *  http://aws.amazon.com/apache2.0
 *
 * or in the "license" file accompanying this file. This file is distributed
 * on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either
 * express or implied. See the License for the specific language governing
 * permissions and limitations under the License.
 */

package com.amplifyframework.storage.s3.service;

import android.content.Context;
import androidx.annotation.NonNull;

import com.amplifyframework.storage.StorageItem;

import com.amazonaws.mobileconnectors.s3.transferutility.TransferObserver;
import com.amazonaws.regions.Region;
import com.amazonaws.services.s3.model.ObjectMetadata;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.List;

/**
 * Interface to manage file transfer to and from a registered S3 bucket.
 */
public interface StorageService {

    /**
     * Generate pre-signed download URL for an object.
     * @param serviceKey key to uniquely specify item to generate URL for
     * @param expires Number of seconds before URL expires
     * @return A pre-signed URL
     */
    URL getPresignedUrl(@NonNull String serviceKey, int expires);

    /**
     * Begin downloading a specific item to a file and return an observer
     * to monitor download progress.
     * @param serviceKey key to uniquely specify item to download
     * @param file file to write downloaded item
     * @return An instance of {@link TransferObserver} to monitor download
     */
    TransferObserver downloadToFile(@NonNull String serviceKey,
                                    @NonNull File file);

    /**
     * Begin uploading a file to a key in storage and return an observer
     * to monitor upload progress. This item will be stored with specified
     * metadata.
     * @param serviceKey key to uniquely label item in storage
     * @param file file to upload
     * @param metadata metadata to attach to uploaded item
     * @return An instance of {@link TransferObserver} to monitor upload
     */
    TransferObserver uploadFile(@NonNull String serviceKey,
                                @NonNull File file,
                                @NonNull ObjectMetadata metadata);

    /**
     * Begin uploading an InputStream to a key in storage and return an observer
     * to monitor upload progress. This item will be stored with specified
     * metadata.
     * @param serviceKey key to uniquely label item in storage
     * @param inputStream InputStream from which to read content
     * @param metadata metadata to attach to uploaded item
     * @return An instance of {@link TransferObserver} to monitor upload
     * @throws IOException on error reading the InputStream, or saving it to a temporary
     *         File before the upload begins.
     */
    TransferObserver uploadInputStream(@NonNull String serviceKey,
                                       @NonNull InputStream inputStream,
                                       @NonNull ObjectMetadata metadata) throws IOException;

    /**
     * Returns a list of items from provided path inside the storage.
     * @param path path inside storage to inspect for list of items
     * @param prefix path appended to S3 keys
     * @return A list of parsed items present inside given path
     */
    List<StorageItem> listFiles(@NonNull String path, @NonNull String prefix);

    /**
     * Delete an object with specific key inside the storage.
     * @param serviceKey Key of the item to remove from storage
     */
    void deleteObject(@NonNull String serviceKey);

    /**
     * Pause the ongoing transfer.
     * @param transfer Transfer to temporarily pause
     */
    void pauseTransfer(@NonNull TransferObserver transfer);

    /**
     * Resume the paused transfer.
     * @param transfer Transfer to resume progress on
     */
    void resumeTransfer(@NonNull TransferObserver transfer);

    /**
     * Cancel the ongoign transfer.
     * @param transfer Transfer to cancel
     */
    void cancelTransfer(@NonNull TransferObserver transfer);

    /**
     * A method to create an instance of storage service.
     */
    interface Factory {
        /**
         * Factory interface to instantiate {@link StorageService} object.
         * @param context Android context
         * @param region S3 bucket region
         * @param bucketName Name of the bucket where the items are stored
         * @return An instantiated storage service instance
         */
        StorageService create(@NonNull Context context,
                              @NonNull Region region,
                              @NonNull String bucketName);
    }
}
