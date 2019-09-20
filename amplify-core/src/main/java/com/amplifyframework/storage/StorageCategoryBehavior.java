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

import com.amplifyframework.storage.exception.*;
import com.amplifyframework.storage.operation.*;
import com.amplifyframework.storage.options.*;

import java.io.File;

/**
 * Defines the behavior of the Storage category that clients will use
 */
public interface StorageCategoryBehavior {
    /**
     * Download object to memory from storage. Specify in the
     * options to download to local file or retrieve remote URL
     *
     * @param key the unique identifier for the object in storage
     * @param option parameters specific to plugin behavior
     * @return an operation object that provides notifications and
     *         actions related to the execution of the work
     * @throws StorageGetException
     */
    StorageGetOperation get(@NonNull String key, StorageGetOptions option) throws StorageGetException;

    /**
     * Upload local file on given path to storage
     *
     * @param key the unique identifier of the object in storage
     * @param local the path to a local file
     * @param option parameters specific to plugin behavior
     * @return an operation object that provides notifications and
     *         actions related to the execution of the work
     * @throws StoragePutException
     */
    StoragePutOperation put(@NonNull String key, @NonNull String local, StoragePutOptions option) throws StoragePutException;

    /**
     * Delete object from storage
     *
     * @param key the unique identifier of the object in storage
     * @param option parameters specific to plugin behavior
     * @return an operation object that provides notifications and
     *        actions related to the execution of the work
     * @throws StorageRemoveException
     */
    StorageRemoveOperation remove(@NonNull String key, StorageRemoveOptions option) throws StorageRemoveException;

    /**
     * List the object identifiers under the hierarchy specified
     * by the path, relative to access level, from storage
     *
     * @param option parameters specific to plugin behavior
     * @return an operation object that provides notifications and
     *         actions related to the execution of the work
     * @throws StorageListException
     */
    StorageListOperation list(StorageListOptions option) throws StorageListException;
}
