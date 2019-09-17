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

import com.amplifyframework.core.Amplify;
import com.amplifyframework.core.plugin.CategoryType;
import com.amplifyframework.core.plugin.CategoryPlugin;
import com.amplifyframework.storage.exception.*;
import com.amplifyframework.storage.operation.*;
import com.amplifyframework.storage.option.*;

import java.io.File;

public class StorageCategory extends Amplify implements StorageCategoryClientBehavior {

    private static CategoryType categoryType = CategoryType.STORAGE;

    @Override
    public StorageGetOperation get(@NonNull String key, StorageGetOption option) throws StorageGetException {
        CategoryPlugin storageCategoryPlugin = Amplify.getPluginForCategory(categoryType);
        if (storageCategoryPlugin instanceof StorageCategoryPlugin) {
            return ((StorageCategoryPlugin) storageCategoryPlugin).get(key, option);
        } else {
            throw new StorageGetException("Failed to get stored data.");
        }
    }

    @Override
    public StoragePutOperation put(@NonNull String key, @NonNull File file, StoragePutOption option) throws StoragePutException {
        CategoryPlugin storageCategoryPlugin = Amplify.getPluginForCategory(categoryType);
        if (storageCategoryPlugin instanceof StorageCategoryPlugin) {
            return ((StorageCategoryPlugin) storageCategoryPlugin).put(key, file, option);
        } else {
            throw new StoragePutException("Failed to store data.");
        }
    }

    @Override
    public StoragePutOperation put(@NonNull String key, @NonNull String path, StoragePutOption option) throws StoragePutException {
        CategoryPlugin storageCategoryPlugin = Amplify.getPluginForCategory(categoryType);
        if (storageCategoryPlugin instanceof StorageCategoryPlugin) {
            return ((StorageCategoryPlugin) storageCategoryPlugin).put(key, path, option);
        } else {
            throw new StoragePutException("Failed to store data.");
        }
    }

    @Override
    public StorageListOperation list(StorageListOption option) throws StorageListException {
        CategoryPlugin storageCategoryPlugin = Amplify.getPluginForCategory(categoryType);
        if (storageCategoryPlugin instanceof StorageCategoryPlugin) {
            return ((StorageCategoryPlugin) storageCategoryPlugin).list(option);
        } else {
            throw new StorageListException("Failed to list data.");
        }
    }

    @Override
    public StorageRemoveOperation remove(@NonNull String key, StorageRemoveOption option) throws StorageRemoveException {
        CategoryPlugin storageCategoryPlugin = Amplify.getPluginForCategory(categoryType);
        if (storageCategoryPlugin instanceof StorageCategoryPlugin) {
            return ((StorageCategoryPlugin) storageCategoryPlugin).remove(key, option);
        } else {
            throw new StorageRemoveException("Failed to remove data.");
        }
    }
}
