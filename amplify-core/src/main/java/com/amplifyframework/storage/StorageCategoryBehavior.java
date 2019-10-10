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

import com.amplifyframework.core.async.Listener;
import com.amplifyframework.storage.exception.StorageGetException;
import com.amplifyframework.storage.exception.StorageListException;
import com.amplifyframework.storage.exception.StoragePutException;
import com.amplifyframework.storage.exception.StorageRemoveException;
import com.amplifyframework.storage.operation.StorageGetOperation;
import com.amplifyframework.storage.operation.StorageListOperation;
import com.amplifyframework.storage.operation.StoragePutOperation;
import com.amplifyframework.storage.operation.StorageRemoveOperation;
import com.amplifyframework.storage.options.StorageGetOptions;
import com.amplifyframework.storage.options.StorageListOptions;
import com.amplifyframework.storage.options.StoragePutOptions;
import com.amplifyframework.storage.options.StorageRemoveOptions;
import com.amplifyframework.storage.result.StorageGetResult;
import com.amplifyframework.storage.result.StorageListResult;
import com.amplifyframework.storage.result.StoragePutResult;
import com.amplifyframework.storage.result.StorageRemoveResult;

/**
 * Defines the behavior of the Storage category that clients will use.
 */
public interface StorageCategoryBehavior {

    /**
     * Download object to memory from storage. Specify in the
     * options to download to local file or retrieve remote URL.
     * @param key the unique identifier for the object in storage
     * @return an operation object that provides notifications and
     *         actions related to the execution of the work
     * @throws StorageGetException
     *         On failure to obtain the requested object from storage.
     *         This could occur for a variety of reasons, including if
     *         {@see key} is not known in storage.
     */
    StorageGetOperation get(@NonNull String key) throws StorageGetException;

    /**
     * Download object to memory from storage. Specify in the
     * options to download to local file or retrieve remote URL.
     * @param key the unique identifier for the object in storage
     * @param options parameters specific to plugin behavior
     * @return an operation object that provides notifications and
     *         actions related to the execution of the work
     * @throws StorageGetException
     *         On failure to obtain the requested object from storage.
     *         This could occur for a variet of reasons, including if {@see key}
     *         is now known in storage, or if bad {@see options} are provided.
     */
    StorageGetOperation get(@NonNull String key,
                            StorageGetOptions options) throws StorageGetException;

    /**
     * Download object to memory from storage. Specify in the
     * options to download to local file or retrieve remote URL.
     * Register a callback to observe progress.
     * @param key the unique identifier for the object in storage
     * @param options parameters specific to plugin behavior
     * @param callback triggered when event occurs
     * @return an operation object that provides notifications and
     *         actions related to the execution of the work
     * @throws StorageGetException
     *         If a failure occurs before asynchronous operation begins.
     *         After that time, errors are communicated via the {@see callback}.
     */
    StorageGetOperation get(@NonNull String key,
                            StorageGetOptions options,
                            Listener<StorageGetResult> callback) throws StorageGetException;

    /**
     * Upload local file on given path to storage.
     * @param key the unique identifier of the object in storage
     * @param local the path to a local file
     * @return an operation object that provides notifications and
     *         actions related to the execution of the work
     * @throws StoragePutException
     *         On failure to put an object into storage. This could
     *         occur for a variety of reasons, including a bad
     *         {@see key}, a bad {@see local} file path, or other reasons.
     */
    StoragePutOperation put(@NonNull String key,
                            @NonNull String local) throws StoragePutException;

    /**
     * Upload local file on given path to storage.
     * @param key the unique identifier of the object in storage
     * @param local the path to a local file
     * @param options parameters specific to plugin behavior
     * @return an operation object that provides notifications and
     *         actions related to the execution of the work
     * @throws StoragePutException
     *         On failure to put an object into storage. This could occur
     *         for a variety of reasons, including a bad {@see key},
     *         a bad {@see local} file path, or bad {@see options}.
     */
    StoragePutOperation put(@NonNull String key,
                            @NonNull String local,
                            StoragePutOptions options) throws StoragePutException;

    /**
     * Upload local file on given path to storage.
     * Register a callback to observe progress
     *
     * @param key the unique identifier of the object in storage
     * @param local the path to a local file
     * @param options parameters specific to plugin behavior
     * @param callback triggered when event occurs
     * @return an operation object that provides notifications and
     *         actions related to the execution of the work
     * @throws StoragePutException
     *         If an error occurs before asynchronous operation begins.
     *         After that, errors are communicated via the {@see callback}.
     */
    StoragePutOperation put(@NonNull String key,
                            @NonNull String local,
                            StoragePutOptions options,
                            Listener<StoragePutResult> callback) throws StoragePutException;

    /**
     * Delete object from storage.
     * @param key the unique identifier of the object in storage
     * @return an operation object that provides notifications and
     *        actions related to the execution of the work
     * @throws StorageRemoveException
     *         On error to remove an object from storage.
     *         This could occur for a variety of reasons, including
     *         if the {@see key} does not refer to an object currently in storage.
     */
    StorageRemoveOperation remove(@NonNull String key) throws StorageRemoveException;

    /**
     * Delete object from storage.
     * @param key the unique identifier of the object in storage
     * @param options parameters specific to plugin behavior
     * @return an operation object that provides notifications and
     *        actions related to the execution of the work
     * @throws StorageRemoveException
     *         On failure to remove an object from storage. This could
     *         occur for a variety of reasons, including if {@see key}
     *         does not refer to an object in storage, or if the
     *         provided {@see options} are invalid.
     *
     */
    StorageRemoveOperation remove(@NonNull String key,
                                  StorageRemoveOptions options) throws StorageRemoveException;

    /**
     * Delete object from storage.
     * Register a callback to observe progress
     *
     * @param key the unique identifier of the object in storage
     * @param options parameters specific to plugin behavior
     * @param callback triggered when event occurs
     * @return an operation object that provides notifications and
     *        actions related to the execution of the work
     * @throws StorageRemoveException
     *         If removal of an object from storage fails before the
     *         asynchronous operation begins. Otherwise, failures
     *         will be reported via the {@see callback}.
     */
    StorageRemoveOperation remove(@NonNull String key,
                                  StorageRemoveOptions options,
                                  Listener<StorageRemoveResult> callback) throws StorageRemoveException;

    /**
     * List the object identifiers under the hierarchy specified
     * by the path, relative to access level, from storage.
     * @return an operation object that provides notifications and
     *         actions related to the execution of the work
     * @throws StorageListException
     *         On failure to list items in storage. This can occur for
     *         a variety or reasons, such as if the storage system is not
     *         currently accessible.
     */
    StorageListOperation list() throws StorageListException;

    /**
     * List the object identifiers under the hierarchy specified
     * by the path, relative to access level, from storage.
     * @param options parameters specific to plugin behavior
     * @return an operation object that provides notifications and
     *         actions related to the execution of the work
     * @throws StorageListException
     *         On failure to list items in storage. This can happen
     *         for a variety of reasons, such as if the provided {@see options}
     *         are invalid.
     */
    StorageListOperation list(StorageListOptions options) throws StorageListException;

    /**
     * List the object identifiers under the hierarchy specified
     * by the path, relative to access level, from storage.
     * Register a callback to observe progress.
     * @param options parameters specific to plugin behavior
     * @param callback triggered when event occurs
     * @return an operation object that provides notifications and
     *         actions related to the execution of the work
     * @throws StorageListException
     *         If a failure to list items in storage occurs before
     *         asynchronous operation begins. Otherwise, failures will
     *         be reported via the {@see callback}.
     */
    StorageListOperation list(StorageListOptions options,
                              Listener<StorageListResult> callback) throws StorageListException;
}
