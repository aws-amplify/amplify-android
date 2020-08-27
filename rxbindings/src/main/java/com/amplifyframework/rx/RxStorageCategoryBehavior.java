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

import com.amplifyframework.core.Consumer;
import com.amplifyframework.core.async.Cancelable;
import com.amplifyframework.storage.StorageCategoryBehavior;
import com.amplifyframework.storage.StorageException;
import com.amplifyframework.storage.options.StorageDownloadFileOptions;
import com.amplifyframework.storage.options.StorageListOptions;
import com.amplifyframework.storage.options.StorageRemoveOptions;
import com.amplifyframework.storage.options.StorageUploadFileOptions;
import com.amplifyframework.storage.result.StorageDownloadFileResult;
import com.amplifyframework.storage.result.StorageListResult;
import com.amplifyframework.storage.result.StorageRemoveResult;
import com.amplifyframework.storage.result.StorageTransferProgress;
import com.amplifyframework.storage.result.StorageUploadFileResult;

import java.io.File;

import io.reactivex.rxjava3.core.Single;

/**
 * An Rx idiomatic expression of the facilities in Amplify's {@link StorageCategoryBehavior}.
 */
public interface RxStorageCategoryBehavior {

    /**
     * Download a file.
     * @param key Remote key of file
     * @param local Local file to which to save
     * @return An instance of {@link RxStorageBinding.RxStorageDownloadOperation} which emits a
     *         download result or failure as a {@link Single}
     */
    @NonNull
    RxStorageBinding.RxProgressAwareSingleOperation<StorageDownloadFileResult> downloadFile(
            @NonNull String key,
            @NonNull File local
    );

    /**
     * Download a file.
     * @param key Remote key of file
     * @param local Local file to which to save
     * @param options Additional download options
     * @return An instance of {@link RxStorageBinding.RxStorageDownloadOperation} which emits a
     *         download result or failure as a {@link Single}. It also
     *         provides progress information when the caller subscribes to
     *         {@link RxStorageBinding.RxStorageDownloadOperation#observeProgress()}.
     *         The download does not begin until subscription. You can cancel the download
     *         by disposing the single subscription.
     */
    @NonNull
    RxStorageBinding.RxProgressAwareSingleOperation<StorageDownloadFileResult> downloadFile(
            @NonNull String key,
            @NonNull File local,
            @NonNull StorageDownloadFileOptions options
    );

    /**
     * Upload a file.
     * @param key Remote key of file
     * @param local Local file from which to read contents
     * @return A single which emits an upload result on success, or an error on failure.
     *         The upload does not begin until subscription. You can cancel the upload
     *         by disposing the single subscription.
     */
    @NonNull
    RxStorageBinding.RxProgressAwareSingleOperation<StorageUploadFileResult> uploadFile(
            @NonNull String key,
            @NonNull File local
    );

    /**
     * Upload a file.
     * @param key Remote key of file
     * @param local Local file from which to read contents
     * @param options Additional upload options
     * @return A single which emits an upload result on success, or an error on failure.
     *         The upload does not begin until subscription. You can cancel the upload
     *         by disposing the single subscription.
     */
    @NonNull
    RxStorageBinding.RxProgressAwareSingleOperation<StorageUploadFileResult> uploadFile(
            @NonNull String key,
            @NonNull File local,
            @NonNull StorageUploadFileOptions options
    );

    /**
     * Removes a remote file.
     * @param key Key to remote file
     * @return A single which emits a remove result on success, or an error on failure.
     *         The remove operation does not begin until subscription. You can cancel the remove
     *         by disposing the single subscription.
     */
    @NonNull
    Single<StorageRemoveResult> remove(
            @NonNull String key
    );

    /**
     * Removes a remote file.
     * @param key Key to remote file
     * @param options Remove options
     * @return A single which emits a remove result on success, or an error on failure.
     *         The remove operation does not begin until subscription. You can cancel the remove
     *         by disposing the single subscription.
     */
    @NonNull
    Single<StorageRemoveResult> remove(
            @NonNull String key,
            @NonNull StorageRemoveOptions options
    );

    /**
     * Lists remote files.
     * @param path Remote path where files are found
     * @return A single which emits a list result on success, or an error on failure.
     *         The list operation does not begin until subscription. You can cancel the listing
     *         by disposing the single subscription.
     */
    @NonNull
    Single<StorageListResult> list(
            @NonNull String path
    );

    /**
     * Lists remote files.
     * @param path Remote path where files are found
     * @param options Storate listing options
     * @return A single which emits a list result on success, or an error on failure.
     *         The list operation does not begin until subscription. You can cancel the listing
     *         by disposing the single subscription.
     */
    @NonNull
    Single<StorageListResult> list(
            @NonNull String path,
            @NonNull StorageListOptions options
    );

    /**
     * Type alias that defines the generic parameters for a download operation.
     * @param <T> The type that represents the result of a given operation.
     * @see RxAdapters.RxProgressAwareCallbackMapper
     */
    interface RxStorageTransferCallbackMapper<T> {
        Cancelable emitTo(Consumer<StorageTransferProgress> onProgress,
                          Consumer<T> onItem,
                          Consumer<StorageException> onError);
    }
}
