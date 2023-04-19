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

package com.amplifyframework.storage;

import androidx.annotation.NonNull;

import com.amplifyframework.core.Consumer;
import com.amplifyframework.storage.operation.StorageDownloadFileOperation;
import com.amplifyframework.storage.operation.StorageGetUrlOperation;
import com.amplifyframework.storage.operation.StorageListOperation;
import com.amplifyframework.storage.operation.StorageRemoveOperation;
import com.amplifyframework.storage.operation.StorageTransferOperation;
import com.amplifyframework.storage.operation.StorageUploadFileOperation;
import com.amplifyframework.storage.operation.StorageUploadInputStreamOperation;
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
import com.amplifyframework.storage.result.StorageTransferProgress;
import com.amplifyframework.storage.result.StorageTransferResult;
import com.amplifyframework.storage.result.StorageUploadFileResult;
import com.amplifyframework.storage.result.StorageUploadInputStreamResult;

import java.io.File;
import java.io.InputStream;

/**
 * Defines the behavior of the Storage category that clients will use.
 */
public interface StorageCategoryBehavior {

    /**
     * Retrieve the remote URL for the object from storage.
     * Provide callbacks to obtain the URL retrieval results.
     * @param key the unique identifier for the object in storage
     * @param onSuccess Called if operation completed successfully and furnishes a result
     * @param onError Called if an error occurs during operation
     * @return an operation object that provides notifications and
     *         actions related to the execution of the work
     */
    @NonNull
    StorageGetUrlOperation<?> getUrl(
            @NonNull String key,
            @NonNull Consumer<StorageGetUrlResult> onSuccess,
            @NonNull Consumer<StorageException> onError);

    /**
     * Retrieve the remote URL for the object from storage.
     * Set advanced options such as the access level of the object
     * or the expiration details of the URL.
     * Provide callbacks to obtain the URL retrieval results.
     * @param key the unique identifier for the object in storage
     * @param options parameters specific to plugin behavior
     * @param onSuccess Called if operation completed successfully and furnishes a result
     * @param onError Called if an error occurs during operation
     * @return an operation object that provides notifications and
     *         actions related to the execution of the work
     */
    @NonNull
    StorageGetUrlOperation<?> getUrl(
            @NonNull String key,
            @NonNull StorageGetUrlOptions options,
            @NonNull Consumer<StorageGetUrlResult> onSuccess,
            @NonNull Consumer<StorageException> onError);

    /**
     * Download a remote resource and store it as a local file.
     * Provide callbacks to obtain the download results.
     * @param key the unique identifier for the object in storage
     * @param local the path to a local file
     * @param onSuccess Called if operation completed successfully and furnishes a result
     * @param onError Called if an error occurs during operation
     * @return an operation object that provides notifications and
     *         actions related to the execution of the work
     */
    @NonNull
    StorageDownloadFileOperation<?> downloadFile(
            @NonNull String key,
            @NonNull File local,
            @NonNull Consumer<StorageDownloadFileResult> onSuccess,
            @NonNull Consumer<StorageException> onError);

    /**
     * Download a remote resource and store it as a local file.
     * Set advanced options such as the access level of the object
     * you want to retrieve (you can have different objects with
     * the same name under different access levels).
     * Provide callbacks to obtain the results of the download.
     * @param key the unique identifier for the object in storage
     * @param local the path to a local file
     * @param options parameters specific to plugin behavior
     * @param onSuccess Called if operation completed successfully and furnishes a result
     * @param onError Called if an error occurs during operation
     * @return an operation object that provides notifications and
     *         actions related to the execution of the work
     */
    @NonNull
    StorageDownloadFileOperation<?> downloadFile(
            @NonNull String key,
            @NonNull File local,
            @NonNull StorageDownloadFileOptions options,
            @NonNull Consumer<StorageDownloadFileResult> onSuccess,
            @NonNull Consumer<StorageException> onError);

    /**
     * Download a remote resource and store it as a local file.
     * Set advanced options such as the access level of the object
     * you want to retrieve (you can have different objects with
     * the same name under different access levels).
     * Provide callbacks to obtain the results of the download, as
     * well as intermediary progress updates.
     * @param key the unique identifier for the object in storage
     * @param local the path to a local file
     * @param options parameters specific to plugin behavior
     * @param onProgress Called periodically to provides updates on download progress
     * @param onSuccess Called if operation completed successfully and furnishes a result
     * @param onError Called if an error occurs during operation
     * @return an operation object that provides notifications and
     *         actions related to the execution of the work
     */
    @NonNull
    StorageDownloadFileOperation<?> downloadFile(
        @NonNull String key,
        @NonNull File local,
        @NonNull StorageDownloadFileOptions options,
        @NonNull Consumer<StorageTransferProgress> onProgress,
        @NonNull Consumer<StorageDownloadFileResult> onSuccess,
        @NonNull Consumer<StorageException> onError);

    /**
     * Upload a local File, storing it as a remote resource.
     * Register consumers to obtain the results of the upload.
     * @param key the unique identifier of the object in storage
     * @param local the local file
     * @param onSuccess Called if operation completed successfully and furnishes a result
     * @param onError Called if an error occurs during operation
     * @return an operation object that provides notifications and
     *         actions related to the execution of the work
     */
    @NonNull
    StorageUploadFileOperation<?> uploadFile(
            @NonNull String key,
            @NonNull File local,
            @NonNull Consumer<StorageUploadFileResult> onSuccess,
            @NonNull Consumer<StorageException> onError);

    /**
     * Upload a local File, storing it as a remote resource.
     * Specify options such as the access level the file should have.
     * Register consumers to observe results of upload request.
     * @param key the unique identifier of the object in storage
     * @param local the local file
     * @param options parameters specific to plugin behavior
     * @param onProgress Called periodically to provides updates on upload progress
     * @param onSuccess Called if operation completed successfully and furnishes a result
     * @param onError Called if an error occurs during operation
     * @return an operation object that provides notifications and
     *         actions related to the execution of the work
     */
    @NonNull
    StorageUploadFileOperation<?> uploadFile(
            @NonNull String key,
            @NonNull File local,
            @NonNull StorageUploadFileOptions options,
            @NonNull Consumer<StorageTransferProgress> onProgress,
            @NonNull Consumer<StorageUploadFileResult> onSuccess,
            @NonNull Consumer<StorageException> onError);

    /**
     * Upload a local File, storing it as a remote resource.
     * Specify options such as the access level the file should have.
     * Register consumers to observe results of upload request,
     * as well as intermediary progress updates.
     * @param key the unique identifier of the object in storage
     * @param local the local file
     * @param options parameters specific to plugin behavior
     * @param onSuccess Called if operation completed successfully and furnishes a result
     * @param onError Called if an error occurs during operation
     * @return an operation object that provides notifications and
     *         actions related to the execution of the work
     */
    @NonNull
    StorageUploadFileOperation<?> uploadFile(
        @NonNull String key,
        @NonNull File local,
        @NonNull StorageUploadFileOptions options,
        @NonNull Consumer<StorageUploadFileResult> onSuccess,
        @NonNull Consumer<StorageException> onError);

    /**
     * Upload a local InputStream, storing it as a remote resource.
     * Register consumers to obtain the results of the upload.
     * @param key the unique identifier of the object in storage
     * @param local the local InputStream
     * @param onSuccess Called if operation completed successfully and furnishes a result
     * @param onError Called if an error occurs during operation
     * @return an operation object that provides notifications and
     *         actions related to the execution of the work
     */
    @NonNull
    StorageUploadInputStreamOperation<?> uploadInputStream(
        @NonNull String key,
        @NonNull InputStream local,
        @NonNull Consumer<StorageUploadInputStreamResult> onSuccess,
        @NonNull Consumer<StorageException> onError);

    /**
     * Upload a local InputStream, storing it as a remote resource.
     * Specify options such as the access level the file should have.
     * Register consumers to observe results of upload request,
     * as well as intermediary progress updates.
     * @param key the unique identifier of the object in storage
     * @param local the local InputStream
     * @param options parameters specific to plugin behavior
     * @param onSuccess Called if operation completed successfully and furnishes a result
     * @param onError Called if an error occurs during operation
     * @return an operation object that provides notifications and
     *         actions related to the execution of the work
     */
    StorageUploadInputStreamOperation<?> uploadInputStream(
        @NonNull String key,
        @NonNull InputStream local,
        @NonNull StorageUploadInputStreamOptions options,
        @NonNull Consumer<StorageUploadInputStreamResult> onSuccess,
        @NonNull Consumer<StorageException> onError);

    /**
     * Upload a local InputStream, storing it as a remote resource.
     * Specify options such as the access level the file should have.
     * Register consumers to observe results of upload request.
     * @param key the unique identifier of the object in storage
     * @param local the local InputStream
     * @param options parameters specific to plugin behavior
     * @param onProgress Called periodically to provides updates on upload progress
     * @param onSuccess Called if operation completed successfully and furnishes a result
     * @param onError Called if an error occurs during operation
     * @return an operation object that provides notifications and
     *         actions related to the execution of the work
     */
    @NonNull
    StorageUploadInputStreamOperation<?> uploadInputStream(
            @NonNull String key,
            @NonNull InputStream local,
            @NonNull StorageUploadInputStreamOptions options,
            @NonNull Consumer<StorageTransferProgress> onProgress,
            @NonNull Consumer<StorageUploadInputStreamResult> onSuccess,
            @NonNull Consumer<StorageException> onError);

    /**
     * Gets an existing transfer in the local device queue.
     * Note: Successfully completed transfers are deleted from the local database and cannot be queried.
     * Register consumer to observe result of transfer lookup.
     * @param transferId the unique identifier of the object in storage
     * @param onReceived Called if operation completed successfully and furnishes an operation
     * @param onError Called if an error occurs during lookup
     */
    void getTransfer(
            @NonNull String transferId,
            @NonNull Consumer<StorageTransferOperation<?, ? extends StorageTransferResult>> onReceived,
            @NonNull Consumer<StorageException> onError);

    /**
     * Delete object from storage.
     * @param key the unique identifier of the object in storage
     * @param onSuccess Called if operation completed successfully and furnishes a result
     * @param onError Called if an error occurs during operation
     * @return an operation object that provides notifications and
     *        actions related to the execution of the work
     */
    @NonNull
    StorageRemoveOperation<?> remove(
            @NonNull String key,
            @NonNull Consumer<StorageRemoveResult> onSuccess,
            @NonNull Consumer<StorageException> onError);

    /**
     * Delete object from storage.
     * Register consumers to get results of remove operation.
     * @param key the unique identifier of the object in storage
     * @param options parameters specific to plugin behavior
     * @param onSuccess Called if operation completed successfully and furnishes a result
     * @param onError Called if an error occurs during operation
     * @return an operation object that provides notifications and
     *        actions related to the execution of the work
     */
    @NonNull
    StorageRemoveOperation<?> remove(
            @NonNull String key,
            @NonNull StorageRemoveOptions options,
            @NonNull Consumer<StorageRemoveResult> onSuccess,
            @NonNull Consumer<StorageException> onError);

    /**
     * List the object identifiers under the hierarchy specified
     * by the path, relative to access level, from storage.
     * @param path The path in storage to list items from
     * @param onSuccess Called if operation completed successfully and furnishes a result
     * @param onError Called if an error occurs during operation
     * @return an operation object that provides notifications and
     *         actions related to the execution of the work
     * @deprecated use the {@link #list(String, StoragePagedListOptions, Consumer, Consumer)} api instead.
     */
    @Deprecated
    @NonNull
    StorageListOperation<?> list(
            @NonNull String path,
            @NonNull Consumer<StorageListResult> onSuccess,
            @NonNull Consumer<StorageException> onError);

    /**
     * List the object identifiers under the hierarchy specified
     * by the path, relative to access level, from storage.
     * Register consumers to observe progress.
     * @param path The path in storage to list items from
     * @param options parameters specific to plugin behavior
     * @param onSuccess Called if operation completed successfully and furnishes a result
     * @param onError Called if an error occurs during operation
     * @return an operation object that provides notifications and
     *         actions related to the execution of the work
     * @deprecated use the {@link #list(String, StoragePagedListOptions, Consumer, Consumer)} api instead.
     */
    @Deprecated
    @NonNull
    StorageListOperation<?> list(
            @NonNull String path,
            @NonNull StorageListOptions options,
            @NonNull Consumer<StorageListResult> onSuccess,
            @NonNull Consumer<StorageException> onError);

    /**
     * List the object identifiers under the hierarchy specified
     * by the path, relative to access level, from storage.
     * Register consumers to observe progress.
     * @param path The path in storage to list items from
     * @param options parameters specific to plugin behavior
     * @param onSuccess Called if operation completed successfully and furnishes a result
     * @param onError Called if an error occurs during operation
     * @return an operation object that provides notifications and
     *         actions related to the execution of the work
     */
    StorageListOperation<?> list(
        @NonNull String path,
        @NonNull StoragePagedListOptions options,
        @NonNull Consumer<StorageListResult> onSuccess,
        @NonNull Consumer<StorageException> onError);

}

