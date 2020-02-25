/*
 * Copyright 2019 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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

package com.amplifyframework.testutils;

import androidx.annotation.NonNull;

import com.amplifyframework.core.Amplify;
import com.amplifyframework.storage.StorageCategory;
import com.amplifyframework.storage.StorageException;
import com.amplifyframework.storage.options.StorageDownloadFileOptions;
import com.amplifyframework.storage.options.StorageListOptions;
import com.amplifyframework.storage.options.StorageRemoveOptions;
import com.amplifyframework.storage.options.StorageUploadFileOptions;
import com.amplifyframework.storage.result.StorageDownloadFileResult;
import com.amplifyframework.storage.result.StorageListResult;
import com.amplifyframework.storage.result.StorageRemoveResult;
import com.amplifyframework.storage.result.StorageUploadFileResult;

import java.util.concurrent.TimeUnit;

/**
 * A utility to perform synchronous calls to the {@link StorageCategory}.
 * This code is not well suited for production use, but is useful in test
 * code, where we want to make a series of sequential assertions after
 * performing various operations.
 */
public final class SynchronousStorage {
    // 5 seconds seemed to be insufficient to reliably cover both initial auth calls + storage network ops
    private static final long STORAGE_OPERATION_TIMEOUT_MS = TimeUnit.SECONDS.toMillis(10);

    private static SynchronousStorage singleton = null;

    @SuppressWarnings("checkstyle:all") private SynchronousStorage() {}

    /**
     * Gets a singleton instance of the Synchronous Storage utility.
     * @return Singleton instance of Synchronous Storage
     */
    @NonNull
    public static synchronized SynchronousStorage singleton() {
        if (SynchronousStorage.singleton == null) {
            SynchronousStorage.singleton = new SynchronousStorage();
        }
        return SynchronousStorage.singleton;
    }

    /**
     * Download a file synchronously and return the result of operation.
     * @param key Key to uniquely identify the file
     * @param local Path to save downloaded file to
     * @param options Download options
     * @return Download operation result containing downloaded file
     * @throws StorageException if download fails or times out
     */
    @NonNull
    public StorageDownloadFileResult downloadFile(
            @NonNull String key,
            @NonNull String local,
            @NonNull StorageDownloadFileOptions options
    ) throws StorageException {
        return downloadFile(key, local, options, STORAGE_OPERATION_TIMEOUT_MS);
    }

    /**
     * Download a file synchronously and return the result of operation.
     * @param key Key to uniquely identify the file
     * @param local Path to save downloaded file to
     * @param options Download options
     * @param timeout Custom time-out duration in milliseconds
     * @return Download operation result containing downloaded file
     * @throws StorageException if download fails or times out
     */
    @NonNull
    public StorageDownloadFileResult downloadFile(
            @NonNull String key,
            @NonNull String local,
            @NonNull StorageDownloadFileOptions options,
            long timeout
    ) throws StorageException {
        return Await.<StorageDownloadFileResult, StorageException>result(timeout, (onResult, onError) ->
                Amplify.Storage.downloadFile(key, local, options, onResult, onError)
        );
    }

    /**
     * Upload a file synchronously and return the result of operation.
     * @param key Key to uniquely identify the file
     * @param local Path of the file being uploaded
     * @param options Upload options
     * @return Upload operation result
     * @throws StorageException if upload fails or times out
     */
    @NonNull
    public StorageUploadFileResult uploadFile(
            @NonNull String key,
            @NonNull String local,
            @NonNull StorageUploadFileOptions options
    ) throws StorageException {
        return uploadFile(key, local, options, STORAGE_OPERATION_TIMEOUT_MS);
    }

    /**
     * Upload a file synchronously and return the result of operation.
     * @param key Key to uniquely identify the file
     * @param local Path of the file being uploaded
     * @param options Upload options
     * @param timeout Custom time-out duration in milliseconds
     * @return Upload operation result
     * @throws StorageException if upload fails or times out
     */
    @NonNull
    public StorageUploadFileResult uploadFile(
            @NonNull String key,
            @NonNull String local,
            @NonNull StorageUploadFileOptions options,
            long timeout
    ) throws StorageException {
        return Await.<StorageUploadFileResult, StorageException>result(timeout, (onResult, onError) ->
                Amplify.Storage.uploadFile(key, local, options, onResult, onError)
        );
    }

    /**
     * Remove a file from S3 bucket synchronously.
     * @param key Key to uniquely identify the file
     * @param options Remove options
     * @return Remove operation result containing name of the removed file
     * @throws StorageException if removal fails or times out
     */
    @NonNull
    public StorageRemoveResult remove(
            @NonNull String key,
            @NonNull StorageRemoveOptions options
    ) throws StorageException {
        return remove(key, options, STORAGE_OPERATION_TIMEOUT_MS);
    }

    /**
     * Remove a file from S3 bucket synchronously.
     * @param key Key to uniquely identify the file
     * @param options Remove options
     * @param timeout Custom time-out duration in milliseconds
     * @return Remove operation result containing name of the removed file
     * @throws StorageException if removal fails or times out
     */
    @NonNull
    public StorageRemoveResult remove(
            @NonNull String key,
            @NonNull StorageRemoveOptions options,
            long timeout
    ) throws StorageException {
        return Await.<StorageRemoveResult, StorageException>result(timeout, (onResult, onError) ->
                Amplify.Storage.remove(key, options, onResult, onError)
        );
    }

    /**
     * List the files in S3 bucket synchronously.
     * @param path Path inside S3 bucket to list files from
     * @param options List options
     * @return List operation result containing list of stored objects
     * @throws StorageException if list fails or times out
     */
    @NonNull
    public StorageListResult list(
            @NonNull String path,
            @NonNull StorageListOptions options
    ) throws StorageException {
        return list(path, options, STORAGE_OPERATION_TIMEOUT_MS);
    }

    /**
     * List the files in S3 bucket synchronously.
     * @param path Path inside S3 bucket to list files from
     * @param options List options
     * @param timeout Custom time-out duration in milliseconds
     * @return List operation result containing list of stored objects
     * @throws StorageException if list fails or times out
     */
    @NonNull
    public StorageListResult list(
            @NonNull String path,
            @NonNull StorageListOptions options,
            long timeout
    ) throws StorageException {
        return Await.<StorageListResult, StorageException>result(timeout, (onResult, onError) ->
                Amplify.Storage.list(path, options, onResult, onError)
        );
    }
}
