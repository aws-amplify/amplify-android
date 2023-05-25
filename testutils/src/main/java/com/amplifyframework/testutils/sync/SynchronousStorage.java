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

package com.amplifyframework.testutils.sync;

import androidx.annotation.NonNull;

import com.amplifyframework.core.Amplify;
import com.amplifyframework.storage.StorageCategory;
import com.amplifyframework.storage.StorageCategoryBehavior;
import com.amplifyframework.storage.StorageException;
import com.amplifyframework.storage.options.StorageDownloadFileOptions;
import com.amplifyframework.storage.options.StorageListOptions;
import com.amplifyframework.storage.options.StoragePagedListOptions;
import com.amplifyframework.storage.options.StorageRemoveOptions;
import com.amplifyframework.storage.options.StorageUploadFileOptions;
import com.amplifyframework.storage.options.StorageUploadInputStreamOptions;
import com.amplifyframework.storage.result.StorageDownloadFileResult;
import com.amplifyframework.storage.result.StorageListResult;
import com.amplifyframework.storage.result.StorageRemoveResult;
import com.amplifyframework.storage.result.StorageUploadFileResult;
import com.amplifyframework.storage.result.StorageUploadInputStreamResult;
import com.amplifyframework.testutils.Await;

import java.io.File;
import java.io.InputStream;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

/**
 * A utility to perform synchronous calls to the {@link StorageCategory}.
 * This code is not well suited for production use, but is useful in test
 * code, where we want to make a series of sequential assertions after
 * performing various operations.
 */
public final class SynchronousStorage {
    private static final long STORAGE_OPERATION_TIMEOUT_MS = TimeUnit.SECONDS.toMillis(50);
    private final StorageCategoryBehavior asyncDelegate;

    private SynchronousStorage(StorageCategoryBehavior asyncDelegate) {
        this.asyncDelegate = asyncDelegate;
    }

    /**
     * Creates a synchronous storage wrapper which delegates calls to the provided storage
     * category behavior.
     *
     * @param asyncDelegate Performs the actual storage operations
     * @return A synchronous storage wrapper
     */
    @NonNull
    public static SynchronousStorage delegatingTo(@NonNull StorageCategoryBehavior asyncDelegate) {
        Objects.requireNonNull(asyncDelegate);
        return new SynchronousStorage(asyncDelegate);
    }

    /**
     * Creates a synchronous storage wrapper which delegates to the {@link Amplify#Storage} facade.
     *
     * @return A synchronous storage wrapper
     */
    @NonNull
    public static SynchronousStorage delegatingToAmplify() {
        return new SynchronousStorage(Amplify.Storage);
    }

    /**
     * Download a file synchronously and return the result of operation.
     *
     * @param key     Key to uniquely identify the file
     * @param local   File to save downloaded object to
     * @param options Download options
     * @return Download operation result containing downloaded file
     * @throws StorageException if download fails or times out
     */
    @NonNull
    public StorageDownloadFileResult downloadFile(
            @NonNull String key,
            @NonNull File local,
            @NonNull StorageDownloadFileOptions options
    ) throws StorageException {
        return downloadFile(key, local, options, STORAGE_OPERATION_TIMEOUT_MS);
    }

    /**
     * Download a file synchronously and return the result of operation.
     *
     * @param key       Key to uniquely identify the file
     * @param local     File to save downloaded object to
     * @param options   Download options
     * @param timeoutMs Custom time-out duration in milliseconds
     * @return Download operation result containing downloaded file
     * @throws StorageException if download fails or times out
     */
    @NonNull
    public StorageDownloadFileResult downloadFile(
            @NonNull String key,
            @NonNull File local,
            @NonNull StorageDownloadFileOptions options,
            long timeoutMs
    ) throws StorageException {
        return Await.<StorageDownloadFileResult, StorageException>result(timeoutMs, (onResult, onError) ->
                asyncDelegate.downloadFile(key, local, options, onResult, onError)
        );
    }

    /**
     * Upload a file synchronously and return the result of operation.
     *
     * @param key     Key to uniquely identify the file
     * @param local   File to upload
     * @param options Upload options
     * @return Upload operation result
     * @throws StorageException if upload fails or times out
     */
    @NonNull
    public StorageUploadFileResult uploadFile(
            @NonNull String key,
            @NonNull File local,
            @NonNull StorageUploadFileOptions options
    ) throws StorageException {
        return uploadFile(key, local, options, STORAGE_OPERATION_TIMEOUT_MS);
    }

    /**
     * Upload a file synchronously and return the result of operation.
     *
     * @param key       Key to uniquely identify the file
     * @param local     File to upload
     * @param options   Upload options
     * @param timeoutMs Custom time-out duration in milliseconds
     * @return Upload operation result
     * @throws StorageException if upload fails or times out
     */
    @NonNull
    public StorageUploadFileResult uploadFile(
            @NonNull String key,
            @NonNull File local,
            @NonNull StorageUploadFileOptions options,
            long timeoutMs
    ) throws StorageException {
        return Await.<StorageUploadFileResult, StorageException>result(timeoutMs, (onResult, onError) ->
                asyncDelegate.uploadFile(key, local, options, onResult, onError)
        );
    }

    /**
     * Upload an InputStream synchronously and return the result of operation.
     *
     * @param key     Key to uniquely identify the InputStream
     * @param local   InputStream to upload
     * @param options Upload options
     * @return Upload operation result
     * @throws StorageException if upload fails or times out
     */
    @NonNull
    public StorageUploadInputStreamResult uploadInputStream(
            @NonNull String key,
            @NonNull InputStream local,
            @NonNull StorageUploadInputStreamOptions options
    ) throws StorageException {
        return uploadInputStream(key, local, options, STORAGE_OPERATION_TIMEOUT_MS);
    }

    /**
     * Upload an InputStream synchronously and return the result of operation.
     *
     * @param key       Key to uniquely identify the InputStream
     * @param local     InputStream to upload
     * @param options   Upload options
     * @param timeoutMs Custom time-out duration in milliseconds
     * @return Upload operation result
     * @throws StorageException if upload fails or times out
     */
    @NonNull
    public StorageUploadInputStreamResult uploadInputStream(
            @NonNull String key,
            @NonNull InputStream local,
            @NonNull StorageUploadInputStreamOptions options,
            long timeoutMs
    ) throws StorageException {
        return Await.<StorageUploadInputStreamResult, StorageException>result(timeoutMs, (onResult, onError) ->
                asyncDelegate.uploadInputStream(key, local, options, onResult, onError)
        );
    }

    /**
     * Remove a file from S3 bucket synchronously.
     *
     * @param key     Key to uniquely identify the file
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
     *
     * @param key       Key to uniquely identify the file
     * @param options   Remove options
     * @param timeoutMs Custom time-out duration in milliseconds
     * @return Remove operation result containing name of the removed file
     * @throws StorageException if removal fails or times out
     */
    @NonNull
    public StorageRemoveResult remove(
            @NonNull String key,
            @NonNull StorageRemoveOptions options,
            long timeoutMs
    ) throws StorageException {
        return Await.<StorageRemoveResult, StorageException>result(timeoutMs, (onResult, onError) ->
                asyncDelegate.remove(key, options, onResult, onError)
        );
    }

    /**
     * List the files in S3 bucket synchronously.
     *
     * @param path    Path inside S3 bucket to list files from
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
     *
     * @param path      Path inside S3 bucket to list files from
     * @param options   List options
     * @param timeoutMs Custom time-out duration in milliseconds
     * @return List operation result containing list of stored objects
     * @throws StorageException if list fails or times out
     */
    @NonNull
    @SuppressWarnings("deprecation")
    public StorageListResult list(
            @NonNull String path,
            @NonNull StorageListOptions options,
            long timeoutMs
    ) throws StorageException {
        return Await.<StorageListResult, StorageException>result(timeoutMs, (onResult, onError) ->
                asyncDelegate.list(path, options, onResult, onError)
        );
    }

    /**
     * List the files in S3 bucket synchronously.
     *
     * @param path      Path inside S3 bucket to list files from
     * @param options   Paged list options
     * @param timeoutMs Custom time-out duration in milliseconds
     * @return List operation result containing list of stored objects
     * @throws StorageException if list fails or times out
     */
    @NonNull
    public StorageListResult list(
        @NonNull String path,
        @NonNull StoragePagedListOptions options,
        long timeoutMs
    ) throws StorageException {
        return Await.<StorageListResult, StorageException>result(timeoutMs, (onResult, onError) ->
            asyncDelegate.list(path, options, onResult, onError)
        );
    }
}
