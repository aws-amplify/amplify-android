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

import android.support.annotation.NonNull;

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
 * Defines the behavior of the Storage category that clients will use
 */
public interface StorageCategoryBehavior {
    /**
     * Download object to memory from storage. Specify in the
     * options to download to local file or retrieve remote URL
     *
     * @param key the unique identifier for the object in storage
     * @return an operation object that provides notifications and
     *         actions related to the execution of the work
     * @throws StorageGetException
     */
    StorageGetOperation get(@NonNull String key) throws StorageGetException;

    /**
     * Download object to memory from storage. Specify in the
     * options to download to local file or retrieve remote URL
     *
     * @param key the unique identifier for the object in storage
     * @param options parameters specific to plugin behavior
     * @return an operation object that provides notifications and
     *         actions related to the execution of the work
     * @throws StorageGetException
     */
    StorageGetOperation get(@NonNull String key,
                            StorageGetOptions options) throws StorageGetException;

    /**
     * Download object to memory from storage. Specify in the
     * options to download to local file or retrieve remote URL.
     * Register a callback to observe progress
     *
     * @param key the unique identifier for the object in storage
     * @param options parameters specific to plugin behavior
     * @param callback triggered when event occurs
     * @return an operation object that provides notifications and
     *         actions related to the execution of the work
     * @throws StorageGetException
     */
    StorageGetOperation get(@NonNull String key,
                            StorageGetOptions options,
                            Listener<StorageGetResult> callback) throws StorageGetException;

    /**
     * Upload local file on given path to storage
     *
     * @param key the unique identifier of the object in storage
     * @param local the path to a local file
     * @return an operation object that provides notifications and
     *         actions related to the execution of the work
     * @throws StoragePutException
     */
    StoragePutOperation put(@NonNull String key,
                            @NonNull String local) throws StoragePutException;

    /**
     * Upload local file on given path to storage
     *
     * @param key the unique identifier of the object in storage
     * @param local the path to a local file
     * @param options parameters specific to plugin behavior
     * @return an operation object that provides notifications and
     *         actions related to the execution of the work
     * @throws StoragePutException
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
     */
    StoragePutOperation put(@NonNull String key,
                            @NonNull String local,
                            StoragePutOptions options,
                            Listener<StoragePutResult> callback) throws StoragePutException;

    /**
     * Delete object from storage
     *
     * @param key the unique identifier of the object in storage
     * @return an operation object that provides notifications and
     *        actions related to the execution of the work
     * @throws StorageRemoveException
     */
    StorageRemoveOperation remove(@NonNull String key) throws StorageRemoveException;

    /**
     * Delete object from storage
     *
     * @param key the unique identifier of the object in storage
     * @param options parameters specific to plugin behavior
     * @return an operation object that provides notifications and
     *        actions related to the execution of the work
     * @throws StorageRemoveException
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
     */
    StorageRemoveOperation remove(@NonNull String key,
                                  StorageRemoveOptions options,
                                  Listener<StorageRemoveResult> callback) throws StorageRemoveException;

    /**
     * List the object identifiers under the hierarchy specified
     * by the path, relative to access level, from storage
     *
     * @return an operation object that provides notifications and
     *         actions related to the execution of the work
     * @throws StorageListException
     */
    StorageListOperation list() throws StorageListException;

    /**
     * List the object identifiers under the hierarchy specified
     * by the path, relative to access level, from storage
     *
     * @param options parameters specific to plugin behavior
     * @return an operation object that provides notifications and
     *         actions related to the execution of the work
     * @throws StorageListException
     */
    StorageListOperation list(StorageListOptions options) throws StorageListException;

    /**
     * List the object identifiers under the hierarchy specified
     * by the path, relative to access level, from storage.
     * Register a callback to observe progress
     *
     * @param options parameters specific to plugin behavior
     * @param callback triggered when event occurs
     * @return an operation object that provides notifications and
     *         actions related to the execution of the work
     * @throws StorageListException
     */
    StorageListOperation list(StorageListOptions options,
                              Listener<StorageListResult> callback) throws StorageListException;
}
