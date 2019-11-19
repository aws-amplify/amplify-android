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
import com.amplifyframework.storage.exception.StorageException;
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
     * @param key the unique identifier for the object in storage
     * @param local the path to a local file
     * @return an operation object that provides notifications and
     *         actions related to the execution of the work
     * @throws StorageException
     *         On failure to obtain the requested object from storage.
     *         This could occur for a variety of reasons, including if
     *         {@see key} is not known in storage.
     */
    StorageDownloadFileOperation<?> downloadFile(@NonNull String key,
                                              @NonNull String local) throws StorageException;

    /**
     * Download object to file from storage.
     * Set advanced options such as the access level of the object
     * you want to retrieve (you can have different objects with
     * the same name under different access levels).
     * @param key the unique identifier for the object in storage
     * @param local the path to a local file
     * @param options parameters specific to plugin behavior
     * @return an operation object that provides notifications and
     *         actions related to the execution of the work
     * @throws StorageException
     *         On failure to obtain the requested object from storage.
     *         This could occur for a variety of reasons, including if {@see key}
     *         is now known in storage, or if bad {@see options} are provided.
     */
    StorageDownloadFileOperation<?> downloadFile(@NonNull String key,
                                  @NonNull String local,
                                  StorageDownloadFileOptions options) throws StorageException;

    /**
     * Download object to file from storage.
     * Register a listener to get the download results.
     * @param key the unique identifier for the object in storage
     * @param local the path to a local file
     * @param resultListener Listens for the results of the download
     * @return an operation object that provides notifications and
     *         actions related to the execution of the work
     * @throws StorageException
     *         On failure to obtain the requested object from storage.
     *         This could occur for a variety of reasons, including if
     *         {@see key} is not known in storage.
     */
    StorageDownloadFileOperation<?> downloadFile(@NonNull String key,
                                  @NonNull String local,
                                  ResultListener<StorageDownloadFileResult> resultListener) throws StorageException;

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
     * @throws StorageException
     *         If a failure occurs before asynchronous operation begins.
     *         After that time, errors are communicated via the
     *         {@see resultListener}.
     */
    StorageDownloadFileOperation<?> downloadFile(@NonNull String key,
                                  @NonNull String local,
                                  StorageDownloadFileOptions options,
                                  ResultListener<StorageDownloadFileResult> resultListener) throws StorageException;

    /**
     * Upload local file on given path to storage.
     * @param key the unique identifier of the object in storage
     * @param local the path to a local file
     * @return an operation object that provides notifications and
     *         actions related to the execution of the work
     * @throws StorageException
     *         On failure to put an object into storage. This could
     *         occur for a variety of reasons, including a bad
     *         {@see key}, a bad {@see local} file path, or other reasons.
     */
    StorageUploadFileOperation<?> uploadFile(@NonNull String key,
                                          @NonNull String local) throws StorageException;

    /**
     * Upload local file on given path to storage.
     * Specify options such as the access level the file should have.
     * @param key the unique identifier of the object in storage
     * @param local the path to a local file
     * @param options parameters specific to plugin behavior
     * @return an operation object that provides notifications and
     *         actions related to the execution of the work
     * @throws StorageException
     *         On failure to put an object into storage. This could occur
     *         for a variety of reasons, including a bad {@see key},
     *         a bad {@see local} file path, or bad {@see options}.
     */
    StorageUploadFileOperation<?> uploadFile(@NonNull String key,
                                @NonNull String local,
                                StorageUploadFileOptions options) throws StorageException;

    /**
     * Upload local file on given path to storage.
     * Register a listener to obtain the results of the upload.
     * @param key the unique identifier of the object in storage
     * @param local the path to a local file
     * @param resultListener Listens for results of upload request
     * @return an operation object that provides notifications and
     *         actions related to the execution of the work
     * @throws StorageException
     *         If a failure to upload a file occurs before asynchronous operation
     *         is attempted; otherwise, errors are reported via the
     *         result listener.
     */
    StorageUploadFileOperation<?> uploadFile(@NonNull String key,
                                @NonNull String local,
                                ResultListener<StorageUploadFileResult> resultListener) throws StorageException;

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
     * @throws StorageException
     *         If an error occurs before asynchronous operation begins.
     *         After that, errors are communicated via the
     *         {@see resultListener}.
     */
    StorageUploadFileOperation<?> uploadFile(@NonNull String key,
                                @NonNull String local,
                                StorageUploadFileOptions options,
                                ResultListener<StorageUploadFileResult> resultListener) throws StorageException;

    /**
     * Delete object from storage.
     * @param key the unique identifier of the object in storage
     * @return an operation object that provides notifications and
     *        actions related to the execution of the work
     * @throws StorageException
     *         On error to remove an object from storage.
     *         This could occur for a variety of reasons, including
     *         if the {@see key} does not refer to an object currently in storage.
     */
    StorageRemoveOperation<?> remove(@NonNull String key) throws StorageException;

    /**
     * Delete object from storage.
     * @param key the unique identifier of the object in storage
     * @param options parameters specific to plugin behavior
     * @return an operation object that provides notifications and
     *        actions related to the execution of the work
     * @throws StorageException
     *         On failure to remove an object from storage. This could
     *         occur for a variety of reasons, including if {@see key}
     *         does not refer to an object in storage, or if the
     *         provided {@see options} are invalid.
     *
     */
    StorageRemoveOperation<?> remove(@NonNull String key,
                            StorageRemoveOptions options) throws StorageException;

    /**
     * Delete object from storage.
     * @param key the unique identifier of the object in storage
     * @param resultListener Listens to results of remove operation
     * @return an operation object that provides notifications and
     *        actions related to the execution of the work
     * @throws StorageException
     *         On failure to remove an object from storage. This could
     *         occur for a variety of reasons, including if {@see key}
     *         does not refer to an object in storage, or if the
     *         provided {@see options} are invalid.
     *
     */
    StorageRemoveOperation<?> remove(@NonNull String key,
                            ResultListener<StorageRemoveResult> resultListener) throws StorageException;

    /**
     * Delete object from storage.
     * Register a listener to get results of remove operation.
     * @param key the unique identifier of the object in storage
     * @param options parameters specific to plugin behavior
     * @param resultListener Listens for results of remove request.
     * @return an operation object that provides notifications and
     *        actions related to the execution of the work
     * @throws StorageException
     *         If removal of an object from storage fails before the
     *         asynchronous operation begins. Otherwise, failures
     *         will be reported via the {@see resultListener}.
     */
    StorageRemoveOperation<?> remove(@NonNull String key,
                            StorageRemoveOptions options,
                            ResultListener<StorageRemoveResult> resultListener) throws StorageException;

    /**
     * List the object identifiers under the hierarchy specified
     * by the path, relative to access level, from storage.
     * @param path The path in storage to set items from
     * @return an operation object that provides notifications and
     *         actions related to the execution of the work
     * @throws StorageException
     *         On failure to set items in storage. This can occur for
     *         a variety or reasons, such as if the storage system is not
     *         currently accessible.
     */
    StorageListOperation<?> list(@NonNull String path) throws StorageException;

    /**
     * List the object identifiers under the hierarchy specified
     * by the path, relative to access level, from storage.
     * @param path The path in storage to set items from
     * @param options parameters specific to plugin behavior
     * @return an operation object that provides notifications and
     *         actions related to the execution of the work
     * @throws StorageException
     *         On failure to set items in storage. This can happen
     *         for a variety of reasons, such as if the provided {@see options}
     *         are invalid.
     */
    StorageListOperation<?> list(@NonNull String path, StorageListOptions options) throws StorageException;

    /**
     * List the object identifiers under the hierarchy specified
     * by the path, relative to access level, from storage.
     * @param path The path in storage to set items from
     * @param resultListener Listens for results of set operation
     * @return an operation object that provides notifications and
     *         actions related to the execution of the work
     * @throws StorageException
     *         On failure to set items in storage. This can happen
     *         for a variety of reasons, such as if the provided {@see options}
     *         are invalid.
     */
    StorageListOperation<?> list(@NonNull String path,
                            ResultListener<StorageListResult> resultListener) throws StorageException;

    /**
     * List the object identifiers under the hierarchy specified
     * by the path, relative to access level, from storage.
     * Register a listener to observe progress.
     * @param path The path in storage to set items from
     * @param options parameters specific to plugin behavior
     * @param resultListener listens to results of set operation
     * @return an operation object that provides notifications and
     *         actions related to the execution of the work
     * @throws StorageException
     *         If a failure to set items in storage occurs before
     *         asynchronous operation begins. Otherwise, failures will
     *         be reported via the {@see resultListener}.
     */
    StorageListOperation<?> list(@NonNull String path,
                            StorageListOptions options,
                            ResultListener<StorageListResult> resultListener) throws StorageException;
}
