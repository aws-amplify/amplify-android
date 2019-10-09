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

import com.amplifyframework.core.async.Callback;
import com.amplifyframework.core.category.Category;
import com.amplifyframework.core.category.CategoryType;
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
 * Defines the Client API consumed by the application.
 * Internally routes the calls to the Storage Category
 * plugins registered.
 */

public class StorageCategory extends Category<StoragePlugin<?>> implements StorageCategoryBehavior {
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
                                   Callback<StorageGetResult> callback) throws StorageGetException {
        return getSelectedPlugin().get(key, options, callback);
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
                                   Callback<StoragePutResult> callback) throws StoragePutException {
        return getSelectedPlugin().put(key, local, options, callback);
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
                                     Callback<StorageListResult> callback) throws StorageListException {
        return getSelectedPlugin().list(options, callback);
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
                                         Callback<StorageRemoveResult> callback) throws StorageRemoveException {
        return getSelectedPlugin().remove(key, options, callback);
    }
}
