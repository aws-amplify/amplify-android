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
import com.amplifyframework.core.category.Category;
import com.amplifyframework.core.category.CategoryType;
import com.amplifyframework.storage.operation.StorageDownloadFileOperation;
import com.amplifyframework.storage.operation.StorageGetUrlOperation;
import com.amplifyframework.storage.operation.StorageListOperation;
import com.amplifyframework.storage.operation.StorageRemoveOperation;
import com.amplifyframework.storage.operation.StorageTransferOperation;
import com.amplifyframework.storage.operation.StorageUploadFileOperation;
import com.amplifyframework.storage.operation.StorageUploadInputStreamOperation;
import com.amplifyframework.storage.options.StorageDownloadFileOptions;
import com.amplifyframework.storage.options.StorageGetUrlOptions;
import com.amplifyframework.storage.options.StorageListOptions;
import com.amplifyframework.storage.options.StoragePagedListOptions;
import com.amplifyframework.storage.options.StorageRemoveOptions;
import com.amplifyframework.storage.options.StorageUploadFileOptions;
import com.amplifyframework.storage.options.StorageUploadInputStreamOptions;
import com.amplifyframework.storage.result.StorageDownloadFileResult;
import com.amplifyframework.storage.result.StorageGetUrlResult;
import com.amplifyframework.storage.result.StorageListResult;
import com.amplifyframework.storage.result.StorageRemoveResult;
import com.amplifyframework.storage.result.StorageTransferProgress;
import com.amplifyframework.storage.result.StorageTransferResult;
import com.amplifyframework.storage.result.StorageUploadFileResult;
import com.amplifyframework.storage.result.StorageUploadInputStreamResult;

import java.io.File;
import java.io.InputStream;

/**
 * Defines the Client API consumed by the application.
 * Internally routes the calls to the Storage Category
 * plugins registered.
 */
public final class StorageCategory extends Category<StoragePlugin<?>> implements StorageCategoryBehavior {

    @NonNull
    @Override
    public CategoryType getCategoryType() {
        return CategoryType.STORAGE;
    }

    @NonNull
    @Override
    public StorageGetUrlOperation<?> getUrl(
            @NonNull String key,
            @NonNull Consumer<StorageGetUrlResult> onSuccess,
            @NonNull Consumer<StorageException> onError) {
        return getSelectedPlugin().getUrl(key, onSuccess, onError);
    }

    @NonNull
    @Override
    public StorageGetUrlOperation<?> getUrl(
            @NonNull String key,
            @NonNull StorageGetUrlOptions options,
            @NonNull Consumer<StorageGetUrlResult> onSuccess,
            @NonNull Consumer<StorageException> onError) {
        return getSelectedPlugin().getUrl(key, options, onSuccess, onError);
    }

    @NonNull
    @Override
    public StorageDownloadFileOperation<?> downloadFile(
            @NonNull String key,
            @NonNull File local,
            @NonNull Consumer<StorageDownloadFileResult> onSuccess,
            @NonNull Consumer<StorageException> onError
    ) {
        return getSelectedPlugin().downloadFile(key, local, onSuccess, onError);
    }

    @NonNull
    @Override
    public StorageDownloadFileOperation<?> downloadFile(
            @NonNull String key,
            @NonNull File local,
            @NonNull StorageDownloadFileOptions options,
            @NonNull Consumer<StorageDownloadFileResult> onSuccess,
            @NonNull Consumer<StorageException> onError
    ) {
        return getSelectedPlugin().downloadFile(key, local, options, onSuccess, onError);
    }

    @NonNull
    @Override
    public StorageDownloadFileOperation<?> downloadFile(
            @NonNull String key,
            @NonNull File local,
            @NonNull StorageDownloadFileOptions options,
            @NonNull Consumer<StorageTransferProgress> onProgress,
            @NonNull Consumer<StorageDownloadFileResult> onSuccess,
            @NonNull Consumer<StorageException> onError
    ) {
        return getSelectedPlugin().downloadFile(key, local, options, onProgress, onSuccess, onError);
    }

    @NonNull
    @Override
    public StorageUploadFileOperation<?> uploadFile(
            @NonNull String key,
            @NonNull File local,
            @NonNull Consumer<StorageUploadFileResult> onSuccess,
            @NonNull Consumer<StorageException> onError
    ) {
        return getSelectedPlugin().uploadFile(key, local, onSuccess, onError);
    }

    @NonNull
    @Override
    public StorageUploadFileOperation<?> uploadFile(
            @NonNull String key,
            @NonNull File local,
            @NonNull StorageUploadFileOptions options,
            @NonNull Consumer<StorageUploadFileResult> onSuccess,
            @NonNull Consumer<StorageException> onError
    ) {
        return getSelectedPlugin().uploadFile(key, local, options, onSuccess, onError);
    }

    @NonNull
    @Override
    public StorageUploadFileOperation<?> uploadFile(
            @NonNull String key,
            @NonNull File local,
            @NonNull StorageUploadFileOptions options,
            @NonNull Consumer<StorageTransferProgress> onProgress,
            @NonNull Consumer<StorageUploadFileResult> onSuccess,
            @NonNull Consumer<StorageException> onError
    ) {
        return getSelectedPlugin().uploadFile(key, local, options, onProgress, onSuccess, onError);
    }

    @NonNull
    @Override
    public StorageUploadInputStreamOperation<?> uploadInputStream(
            @NonNull String key,
            @NonNull InputStream local,
            @NonNull Consumer<StorageUploadInputStreamResult> onSuccess,
            @NonNull Consumer<StorageException> onError
    ) {
        return getSelectedPlugin().uploadInputStream(key, local, onSuccess, onError);
    }

    @NonNull
    @Override
    public StorageUploadInputStreamOperation<?> uploadInputStream(
            @NonNull String key,
            @NonNull InputStream local,
            @NonNull StorageUploadInputStreamOptions options,
            @NonNull Consumer<StorageUploadInputStreamResult> onSuccess,
            @NonNull Consumer<StorageException> onError
    ) {
        return getSelectedPlugin().uploadInputStream(key, local, options, onSuccess, onError);
    }

    @NonNull
    @Override
    public StorageUploadInputStreamOperation<?> uploadInputStream(
            @NonNull String key,
            @NonNull InputStream local,
            @NonNull StorageUploadInputStreamOptions options,
            @NonNull Consumer<StorageTransferProgress> onProgress,
            @NonNull Consumer<StorageUploadInputStreamResult> onSuccess,
            @NonNull Consumer<StorageException> onError
    ) {
        return getSelectedPlugin().uploadInputStream(key, local, options, onProgress, onSuccess, onError);
    }

    @Override
    public void getTransfer(
            @NonNull String transferId,
            @NonNull Consumer<StorageTransferOperation<?, ? extends StorageTransferResult>> onReceived,
            @NonNull Consumer<StorageException> onError
    ) {
        getSelectedPlugin().getTransfer(transferId, onReceived, onError);
    }

    @NonNull
    @Override
    public StorageRemoveOperation<?> remove(
            @NonNull String key,
            @NonNull Consumer<StorageRemoveResult> onSuccess,
            @NonNull Consumer<StorageException> onError
    ) {
        return getSelectedPlugin().remove(key, onSuccess, onError);
    }

    @NonNull
    @Override
    public StorageRemoveOperation<?> remove(
            @NonNull String key,
            @NonNull StorageRemoveOptions options,
            @NonNull Consumer<StorageRemoveResult> onSuccess,
            @NonNull Consumer<StorageException> onError
    ) {
        return getSelectedPlugin().remove(key, options, onSuccess, onError);
    }

    @NonNull
    @Override
    @Deprecated
    @SuppressWarnings("deprecation")
    public StorageListOperation<?> list(
            @NonNull String path,
            @NonNull Consumer<StorageListResult> onSuccess,
            @NonNull Consumer<StorageException> onError
    ) {
        return getSelectedPlugin().list(path, onSuccess, onError);
    }

    @NonNull
    @Override
    @Deprecated
    @SuppressWarnings("deprecation")
    public StorageListOperation<?> list(
            @NonNull String path,
            @NonNull StorageListOptions options,
            @NonNull Consumer<StorageListResult> onSuccess,
            @NonNull Consumer<StorageException> onError
    ) {
        return getSelectedPlugin().list(path, options, onSuccess, onError);
    }

    @Override
    public StorageListOperation<?> list(@NonNull String path,
                                     @NonNull StoragePagedListOptions options,
                                     @NonNull Consumer<StorageListResult> onSuccess,
                                     @NonNull Consumer<StorageException> onError) {
        return getSelectedPlugin().list(path, options, onSuccess, onError);
    }
}

