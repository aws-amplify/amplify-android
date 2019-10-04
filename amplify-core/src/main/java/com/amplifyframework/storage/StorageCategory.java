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
import com.amplifyframework.core.category.Category;
import com.amplifyframework.core.category.CategoryType;
import com.amplifyframework.storage.exception.*;
import com.amplifyframework.storage.operation.*;
import com.amplifyframework.storage.options.*;
import com.amplifyframework.storage.result.*;


/**
 * Defines the Client API consumed by the application.
 * Internally routes the calls to the Storage Category
 * plugins registered.
 */

public class StorageCategory extends Category<StoragePlugin> implements StorageCategoryBehavior {
    /**
     * Retrieve the Storage category type enum
     *
     * @return enum that represents Storage category
     */
    @Override
    public final CategoryType getCategoryType() {
        return CategoryType.STORAGE;
    }

    @Override
    public StorageGetOperation get(@NonNull String key) throws StorageGetException {
        return get(key, new StorageGetOptions(), null);
    }

    @Override
    public StorageGetOperation get(@NonNull String key,
                                   @NonNull StorageGetOptions options) throws StorageGetException {
        return get(key, options, null);
    }

    @Override
    public StorageGetOperation get(@NonNull String key,
                                   @NonNull StorageGetOptions options,
                                   Listener<StorageGetResult> listener) throws StorageGetException {
        return getSelectedPlugin().get(key, options, listener);
    }

    /**
     * Upload local file on given path to storage
     *
     * @param key   the unique identifier of the object in storage
     * @param local the path to a local file
     * @return an operation object that provides notifications and
     * actions related to the execution of the work
     * @throws StoragePutException
     */
    @Override
    public StoragePutOperation put(@NonNull String key, @NonNull String local) throws StoragePutException {
        return put(key, local, new StoragePutOptions(), null);
    }

    @Override
    public StoragePutOperation put(@NonNull String key,
                                   @NonNull String local,
                                   @NonNull StoragePutOptions options) throws StoragePutException {
        return put(key, local, options, null);
    }

    @Override
    public StoragePutOperation put(@NonNull String key,
                                   @NonNull String local,
                                   @NonNull StoragePutOptions options,
                                   Listener<StoragePutResult> listener) throws StoragePutException {
        return getSelectedPlugin().put(key, local, options, listener);
    }

    @Override
    public StorageListOperation list() throws StorageListException {
        return list(new StorageListOptions());
    }

    @Override
    public StorageListOperation list(@NonNull StorageListOptions options) throws StorageListException {
        return list(options, null);
    }

    @Override
    public StorageListOperation list(@NonNull StorageListOptions options,
                                     Listener<StorageListResult> listener) throws StorageListException {
        return getSelectedPlugin().list(options, listener);
    }

    @Override
    public StorageRemoveOperation remove(@NonNull String key) throws StorageRemoveException {
        return remove(key, new StorageRemoveOptions());
    }

    @Override
    public StorageRemoveOperation remove(@NonNull String key,
                                         StorageRemoveOptions options) throws StorageRemoveException {
        return remove(key, options, null);
    }

    @Override
    public StorageRemoveOperation remove(@NonNull String key,
                                         @NonNull StorageRemoveOptions options,
                                         Listener<StorageRemoveResult> listener) throws StorageRemoveException {
        return getSelectedPlugin().remove(key, options, listener);
    }
}