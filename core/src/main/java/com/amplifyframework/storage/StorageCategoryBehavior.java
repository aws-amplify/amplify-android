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
import com.amplifyframework.storage.operation.StorageUploadFileOperation;
import com.amplifyframework.storage.options.StorageDownloadFileOptions;
import com.amplifyframework.storage.options.StorageGetUrlOptions;
import com.amplifyframework.storage.options.StorageListOptions;
import com.amplifyframework.storage.options.StorageRemoveOptions;
import com.amplifyframework.storage.options.StorageUploadFileOptions;
import com.amplifyframework.storage.result.StorageDownloadFileResult;
import com.amplifyframework.storage.result.StorageGetUrlResult;
import com.amplifyframework.storage.result.StorageListResult;
import com.amplifyframework.storage.result.StorageRemoveResult;
import com.amplifyframework.storage.result.StorageUploadFileResult;

/**
 * Defines the behavior of the Storage category that clients will use.
 */
@SuppressWarnings("unused")
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
     * Download object to file from storage.
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
            @NonNull String local,
            @NonNull Consumer<StorageDownloadFileResult> onSuccess,
            @NonNull Consumer<StorageException> onError);

    /**
     * Download object to memory from storage.
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
            @NonNull String local,
            @NonNull StorageDownloadFileOptions options,
            @NonNull Consumer<StorageDownloadFileResult> onSuccess,
            @NonNull Consumer<StorageException> onError);

    /**
     * Upload local file on given path to storage.
     * Register consumers to obtain the results of the upload.
     * @param key the unique identifier of the object in storage
     * @param local the path to a local file
     * @param onSuccess Called if operation completed successfully and furnishes a result
     * @param onError Called if an error occurs during operation
     * @return an operation object that provides notifications and
     *         actions related to the execution of the work
     */
    @NonNull
    StorageUploadFileOperation<?> uploadFile(
            @NonNull String key,
            @NonNull String local,
            @NonNull Consumer<StorageUploadFileResult> onSuccess,
            @NonNull Consumer<StorageException> onError);

    /**
     * Upload local file on given path to storage.
     * Specify options such as the access level the file should have.
     * Register consumers to observe results of upload request.
     * @param key the unique identifier of the object in storage
     * @param local the path to a local file
     * @param options parameters specific to plugin behavior
     * @param onSuccess Called if operation completed successfully and furnishes a result
     * @param onError Called if an error occurs during operation
     * @return an operation object that provides notifications and
     *         actions related to the execution of the work
     */
    @NonNull
    StorageUploadFileOperation<?> uploadFile(
            @NonNull String key,
            @NonNull String local,
            @NonNull StorageUploadFileOptions options,
            @NonNull Consumer<StorageUploadFileResult> onSuccess,
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
     */
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
     */
    @NonNull
    StorageListOperation<?> list(
            @NonNull String path,
            @NonNull StorageListOptions options,
            @NonNull Consumer<StorageListResult> onSuccess,
            @NonNull Consumer<StorageException> onError);
}

