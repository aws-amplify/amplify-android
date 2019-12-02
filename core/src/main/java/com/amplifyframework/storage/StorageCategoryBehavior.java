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

import com.amplifyframework.core.ResultListener;
import com.amplifyframework.storage.operation.StorageDownloadFileOperation;
import com.amplifyframework.storage.operation.StorageListOperation;
import com.amplifyframework.storage.operation.StorageRemoveOperation;
import com.amplifyframework.storage.operation.StorageUploadFileOperation;
import com.amplifyframework.storage.options.StorageDownloadFileOptions;
import com.amplifyframework.storage.options.StorageListOptions;
import com.amplifyframework.storage.options.StorageRemoveOptions;
import com.amplifyframework.storage.options.StorageUploadFileOptions;
import com.amplifyframework.storage.result.StorageDownloadFileResult;
import com.amplifyframework.storage.result.StorageListResult;
import com.amplifyframework.storage.result.StorageRemoveResult;
import com.amplifyframework.storage.result.StorageUploadFileResult;

/**
 * Defines the behavior of the Storage category that clients will use.
 */
public interface StorageCategoryBehavior {

    /**
     * Download object to file from storage.
     * Register a listener to get the download results.
     * @param key the unique identifier for the object in storage
     * @param local the path to a local file
     * @param resultListener Listens for the results of the download
     * @return an operation object that provides notifications and
     *         actions related to the execution of the work
     */
    StorageDownloadFileOperation<?> downloadFile(@NonNull String key,
                                  @NonNull String local,
                                  @NonNull ResultListener<StorageDownloadFileResult> resultListener);

    /**
     * Download object to memory from storage.
     * Set advanced options such as the access level of the object
     * you want to retrieve (you can have different objects with
     * the same name under different access levels).
     * Register a listener to get the results of the download.
     * @param key the unique identifier for the object in storage
     * @param local the path to a local file
     * @param options parameters specific to plugin behavior
     * @param resultListener Listens for the results of the download
     * @return an operation object that provides notifications and
     *         actions related to the execution of the work
     */
    StorageDownloadFileOperation<?> downloadFile(@NonNull String key,
                                  @NonNull String local,
                                  @NonNull StorageDownloadFileOptions options,
                                  @NonNull ResultListener<StorageDownloadFileResult> resultListener);

    /**
     * Upload local file on given path to storage.
     * Register a listener to obtain the results of the upload.
     * @param key the unique identifier of the object in storage
     * @param local the path to a local file
     * @param resultListener Listens for results of upload request
     * @return an operation object that provides notifications and
     *         actions related to the execution of the work
     */
    StorageUploadFileOperation<?> uploadFile(@NonNull String key,
                                @NonNull String local,
                                @NonNull ResultListener<StorageUploadFileResult> resultListener);

    /**
     * Upload local file on given path to storage.
     * Specify options such as the access level the file should have.
     * Register a listener to observe results of upload request.
     * @param key the unique identifier of the object in storage
     * @param local the path to a local file
     * @param options parameters specific to plugin behavior
     * @param resultListener Listens to results of upload request
     * @return an operation object that provides notifications and
     *         actions related to the execution of the work
     */
    StorageUploadFileOperation<?> uploadFile(@NonNull String key,
                                @NonNull String local,
                                @NonNull StorageUploadFileOptions options,
                                @NonNull ResultListener<StorageUploadFileResult> resultListener);

    /**
     * Delete object from storage.
     * @param key the unique identifier of the object in storage
     * @param resultListener Listens to results of remove operation
     * @return an operation object that provides notifications and
     *        actions related to the execution of the work
     *
     */
    StorageRemoveOperation<?> remove(@NonNull String key,
                            @NonNull ResultListener<StorageRemoveResult> resultListener);

    /**
     * Delete object from storage.
     * Register a listener to get results of remove operation.
     * @param key the unique identifier of the object in storage
     * @param options parameters specific to plugin behavior
     * @param resultListener Listens for results of remove request.
     * @return an operation object that provides notifications and
     *        actions related to the execution of the work
     */
    StorageRemoveOperation<?> remove(@NonNull String key,
                            @NonNull StorageRemoveOptions options,
                            @NonNull ResultListener<StorageRemoveResult> resultListener);

    /**
     * List the object identifiers under the hierarchy specified
     * by the path, relative to access level, from storage.
     * @param path The path in storage to list items from
     * @param resultListener Listens for results of list operation
     * @return an operation object that provides notifications and
     *         actions related to the execution of the work
     */
    StorageListOperation<?> list(@NonNull String path,
                            @NonNull ResultListener<StorageListResult> resultListener);

    /**
     * List the object identifiers under the hierarchy specified
     * by the path, relative to access level, from storage.
     * Register a listener to observe progress.
     * @param path The path in storage to list items from
     * @param options parameters specific to plugin behavior
     * @param resultListener listens to results of list operation
     * @return an operation object that provides notifications and
     *         actions related to the execution of the work
     */
    StorageListOperation<?> list(@NonNull String path,
                            @NonNull StorageListOptions options,
                            @NonNull ResultListener<StorageListResult> resultListener);
}
