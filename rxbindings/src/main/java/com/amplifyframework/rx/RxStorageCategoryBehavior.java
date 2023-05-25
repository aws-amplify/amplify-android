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

package com.amplifyframework.rx;

import androidx.annotation.NonNull;

import com.amplifyframework.storage.StorageCategoryBehavior;
import com.amplifyframework.storage.StorageException;
import com.amplifyframework.storage.operation.StorageTransferOperation;
import com.amplifyframework.storage.options.StorageDownloadFileOptions;
import com.amplifyframework.storage.options.StorageGetUrlOptions;
import com.amplifyframework.storage.options.StorageListOptions;
import com.amplifyframework.storage.options.StoragePagedListOptions;
import com.amplifyframework.storage.options.StorageRemoveOptions;
import com.amplifyframework.storage.options.StorageUploadFileOptions;
import com.amplifyframework.storage.options.StorageUploadInputStreamOptions;
import com.amplifyframework.storage.result.StorageDownloadFileResult;
import com.amplifyframework.storage.result.StorageGetUrlResult;
import com.amplifyframework.storage.result.StorageListResult;
import com.amplifyframework.storage.result.StorageRemoveResult;
import com.amplifyframework.storage.result.StorageTransferResult;
import com.amplifyframework.storage.result.StorageUploadFileResult;
import com.amplifyframework.storage.result.StorageUploadInputStreamResult;

import java.io.File;
import java.io.InputStream;

import io.reactivex.rxjava3.core.Single;

/**
 * An Rx idiomatic expression of the facilities in Amplify's {@link StorageCategoryBehavior}.
 */
public interface RxStorageCategoryBehavior {

    /**
     * Retrieve the remote URL for the object from storage.
     * Provide callbacks to obtain the URL retrieval results.
     *
     * @param key the unique identifier for the object in storage
     * @return A Single which emits a result on success, or an {@link StorageException}
     * on failure to get the URL for the requested key
     */
    @NonNull
    Single<StorageGetUrlResult> getUrl(String key);

    /**
     * Retrieve the remote URL for the object from storage.
     * Set advanced options such as the access level of the object
     * or the expiration details of the URL.
     * Provide callbacks to obtain the URL retrieval results.
     *
     * @param key the unique identifier for the object in storage
     * @param options parameters specific to plugin behavior
     * @return A Single which emits a result on success, or an {@link StorageException}
     * if not able to get a url for the requested key
     */
    @NonNull
    Single<StorageGetUrlResult> getUrl(@NonNull String key, @NonNull StorageGetUrlOptions options);

    /**
     * Download a file.
     *
     * @param key   Remote key of file
     * @param local Local file to which to save
     * @return An instance of {@link RxStorageBinding.RxProgressAwareSingleOperation}
     * which emits a download result or failure as a {@link Single}
     */
    @NonNull
    RxStorageBinding.RxProgressAwareSingleOperation<StorageDownloadFileResult> downloadFile(
        @NonNull String key,
        @NonNull File local
    );

    /**
     * Download a file.
     *
     * @param key     Remote key of file
     * @param local   Local file to which to save
     * @param options Additional download options
     * @return An instance of {@link RxStorageBinding.RxProgressAwareSingleOperation}
     * which emits a download result or failure as a {@link Single}. It also
     * provides progress information when the caller subscribes to
     * {@link RxStorageBinding.RxProgressAwareSingleOperation#observeProgress()}.
     * The download does not begin until subscription. You can cancel the download
     * by disposing the single subscription.
     */
    @NonNull
    RxStorageBinding.RxProgressAwareSingleOperation<StorageDownloadFileResult> downloadFile(
        @NonNull String key,
        @NonNull File local,
        @NonNull StorageDownloadFileOptions options
    );

    /**
     * Upload a file.
     *
     * @param key   Remote key of file
     * @param local Local file from which to read contents
     * @return A single which emits an upload result on success, or an error on failure.
     * The upload does not begin until subscription. You can cancel the upload
     * by disposing the single subscription.
     */
    @NonNull
    RxStorageBinding.RxProgressAwareSingleOperation<StorageUploadFileResult> uploadFile(
        @NonNull String key,
        @NonNull File local
    );

    /**
     * Upload a file.
     *
     * @param key     Remote key of file
     * @param local   Local file from which to read contents
     * @param options Additional upload options
     * @return A single which emits an upload result on success, or an error on failure.
     * The upload does not begin until subscription. You can cancel the upload
     * by disposing the single subscription.
     */
    @NonNull
    RxStorageBinding.RxProgressAwareSingleOperation<StorageUploadFileResult> uploadFile(
        @NonNull String key,
        @NonNull File local,
        @NonNull StorageUploadFileOptions options
    );

    /**
     * Upload an InputStream.
     *
     * @param key   Remote key of the file containing the InputStream content
     * @param local Local InputStream from which to read contents
     * @return A single which emits an upload result on success, or an error on failure.
     * The upload does not begin until subscription. You can cancel the upload
     * by disposing the single subscription.
     */
    @NonNull
    RxStorageBinding.RxProgressAwareSingleOperation<StorageUploadInputStreamResult> uploadInputStream(
        @NonNull String key,
        @NonNull InputStream local
    );

    /**
     * Upload an InputStream.
     *
     * @param key     Remote key of the file containing the InputStream content
     * @param local   Local InputStream from which to read contents
     * @param options Additional upload options
     * @return A single which emits an upload result on success, or an error on failure.
     * The upload does not begin until subscription. You can cancel the upload
     * by disposing the single subscription.
     */
    @NonNull
    RxStorageBinding.RxProgressAwareSingleOperation<StorageUploadInputStreamResult> uploadInputStream(
        @NonNull String key,
        @NonNull InputStream local,
        @NonNull StorageUploadInputStreamOptions options
    );

    /**
     * Removes a remote file.
     *
     * @param key Key to remote file
     * @return A single which emits a remove result on success, or an error on failure.
     * The remove operation does not begin until subscription. You can cancel the remove
     * by disposing the single subscription.
     */
    @NonNull
    Single<StorageRemoveResult> remove(
        @NonNull String key
    );

    /**
     * Removes a remote file.
     *
     * @param key     Key to remote file
     * @param options Remove options
     * @return A single which emits a remove result on success, or an error on failure.
     * The remove operation does not begin until subscription. You can cancel the remove
     * by disposing the single subscription.
     */
    @NonNull
    Single<StorageRemoveResult> remove(
        @NonNull String key,
        @NonNull StorageRemoveOptions options
    );

    /**
     * Lists remote files.
     *
     * @param path Remote path where files are found
     * @return A single which emits a list result on success, or an error on failure.
     * The list operation does not begin until subscription. You can cancel the listing
     * by disposing the single subscription.
     * @deprecated use the {@link #list(String, StoragePagedListOptions)} api instead.
     */
    @NonNull
    @Deprecated
    Single<StorageListResult> list(
        @NonNull String path
    );

    /**
     * Lists remote files.
     *
     * @param path    Remote path where files are found
     * @param options Storate listing options
     * @return A single which emits a list result on success, or an error on failure.
     * The list operation does not begin until subscription. You can cancel the listing
     * by disposing the single subscription.
     * @deprecated use the {@link #list(String, StoragePagedListOptions)} api instead.
     */
    @NonNull
    @Deprecated
    Single<StorageListResult> list(
        @NonNull String path,
        @NonNull StorageListOptions options
    );

    /**
     * Lists remote files.
     *
     * @param path    Remote path where files are found
     * @param options Storate listing options
     * @return A single which emits a list result on success, or an error on failure.
     * The list operation does not begin until subscription. You can cancel the listing
     * by disposing the single subscription.
     */
    @NonNull
    Single<StorageListResult> list(
        @NonNull String path,
        @NonNull StoragePagedListOptions options
    );

    /**
     * Queries the transfer form local db.
     *
     * @param transferId ID of the transferOperation
     * @return A single which emits a StorageTransferOperation on success, or an error on failure.
     * The getTransfer operation does not begin until subscription. You can cancel the listing
     * by disposing the single subscription.
     */
    @NonNull
    Single<StorageTransferOperation<?, ? extends StorageTransferResult>> getTransfer(
        @NonNull String transferId
    );
}
