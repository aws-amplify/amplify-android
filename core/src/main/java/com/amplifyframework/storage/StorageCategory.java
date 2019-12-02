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
import com.amplifyframework.core.category.Category;
import com.amplifyframework.core.category.CategoryType;
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
 * Defines the Client API consumed by the application.
 * Internally routes the calls to the Storage Category
 * plugins registered.
 */
public final class StorageCategory extends Category<StoragePlugin<?>> implements StorageCategoryBehavior {

    @Override
    public CategoryType getCategoryType() {
        return CategoryType.STORAGE;
    }

    @Override
    public StorageDownloadFileOperation<?> downloadFile(
            @NonNull String key,
            @NonNull String local
    ) {
        return getSelectedPlugin().downloadFile(key, local);
    }

    @Override
    public StorageDownloadFileOperation<?> downloadFile(
            @NonNull String key,
            @NonNull String local,
            StorageDownloadFileOptions options)  {
        return getSelectedPlugin().downloadFile(key, local, options);
    }

    @Override
    public StorageDownloadFileOperation<?> downloadFile(
            @NonNull String key,
            @NonNull String local,
            ResultListener<StorageDownloadFileResult> resultListener)  {
        return getSelectedPlugin().downloadFile(key, local, resultListener);
    }

    @Override
    public StorageDownloadFileOperation<?> downloadFile(
            @NonNull String key,
            @NonNull String local,
            StorageDownloadFileOptions options,
            ResultListener<StorageDownloadFileResult> resultListener)  {
        return getSelectedPlugin().downloadFile(key, local, options, resultListener);
    }

    @Override
    public StorageUploadFileOperation<?> uploadFile(
            @NonNull String key,
            @NonNull String local)  {
        return getSelectedPlugin().uploadFile(key, local);
    }

    @Override
    public StorageUploadFileOperation<?> uploadFile(
            @NonNull String key,
            @NonNull String local,
            StorageUploadFileOptions options)  {
        return getSelectedPlugin().uploadFile(key, local, options);
    }

    @Override
    public StorageUploadFileOperation<?> uploadFile(
            @NonNull String key,
            @NonNull String local,
            ResultListener<StorageUploadFileResult> resultListener)  {
        return getSelectedPlugin().uploadFile(key, local, resultListener);
    }

    @Override
    public StorageUploadFileOperation<?> uploadFile(
            @NonNull String key,
            @NonNull String local,
            StorageUploadFileOptions options,
            ResultListener<StorageUploadFileResult> resultListener)  {
        return getSelectedPlugin().uploadFile(key, local, options, resultListener);
    }

    @Override
    public StorageRemoveOperation<?> remove(
            @NonNull String key
    )  {
        return getSelectedPlugin().remove(key);
    }

    @Override
    public StorageRemoveOperation<?> remove(
            @NonNull String key,
            StorageRemoveOptions options
    )  {
        return getSelectedPlugin().remove(key, options);
    }

    @Override
    public StorageRemoveOperation<?> remove(
            @NonNull String key,
            ResultListener<StorageRemoveResult> resultListener
    )  {
        return getSelectedPlugin().remove(key, resultListener);
    }

    @Override
    public StorageRemoveOperation<?> remove(
            @NonNull String key,
            StorageRemoveOptions options,
            ResultListener<StorageRemoveResult> resultListener)  {
        return getSelectedPlugin().remove(key, options, resultListener);
    }

    @Override
    public StorageListOperation<?> list(@NonNull String path)  {
        return getSelectedPlugin().list(path);
    }

    @Override
    public StorageListOperation<?> list(@NonNull String path, StorageListOptions options)  {
        return getSelectedPlugin().list(path, options);
    }

    @Override
    public StorageListOperation<?> list(@NonNull String path, ResultListener<StorageListResult> resultListener)
             {
        return getSelectedPlugin().list(path, resultListener);
    }

    @Override
    public StorageListOperation<?> list(
            @NonNull String path,
            StorageListOptions options,
            ResultListener<StorageListResult> resultListener)  {
        return getSelectedPlugin().list(path, options, resultListener);
    }
}

