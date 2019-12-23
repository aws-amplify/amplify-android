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

package com.amplifyframework.rx;

import androidx.annotation.NonNull;

import com.amplifyframework.core.Amplify;
import com.amplifyframework.storage.StorageCategoryBehavior;
import com.amplifyframework.storage.options.StorageDownloadFileOptions;
import com.amplifyframework.storage.options.StorageListOptions;
import com.amplifyframework.storage.options.StorageRemoveOptions;
import com.amplifyframework.storage.options.StorageUploadFileOptions;
import com.amplifyframework.storage.result.StorageDownloadFileResult;
import com.amplifyframework.storage.result.StorageListResult;
import com.amplifyframework.storage.result.StorageRemoveResult;
import com.amplifyframework.storage.result.StorageUploadFileResult;

import io.reactivex.Single;

/* package-local */ @SuppressWarnings({"WeakerAccess", "WhitespaceAround"})
final class RxStorageBinding implements RxStorage {
    private final StorageCategoryBehavior storage;

    RxStorageBinding() {
        this(Amplify.Storage);
    }

    RxStorageBinding(StorageCategoryBehavior storage) {
        this.storage = storage;
    }

    @NonNull
    @Override
    public Single<StorageDownloadFileResult> downloadFile(@NonNull String key, @NonNull String local) {
        return RxAdapters.toSingle(listener -> storage.downloadFile(key, local, listener));
    }

    @NonNull
    @Override
    public Single<StorageDownloadFileResult> downloadFile(
            @NonNull String key, @NonNull String local, @NonNull StorageDownloadFileOptions options) {
        return RxAdapters.toSingle(listener -> storage.downloadFile(key, local, options, listener));
    }

    @NonNull
    @Override
    public Single<StorageUploadFileResult> uploadFile(@NonNull String key, @NonNull String local) {
        return RxAdapters.toSingle(listener -> storage.uploadFile(key, local, listener));
    }

    @NonNull
    @Override
    public Single<StorageUploadFileResult> uploadFile(
            @NonNull String key, @NonNull String local, @NonNull StorageUploadFileOptions options) {
        return RxAdapters.toSingle(listener -> storage.uploadFile(key, local, options, listener));
    }

    @NonNull
    @Override
    public Single<StorageRemoveResult> remove(@NonNull String key) {
        return RxAdapters.toSingle(listener -> {
            storage.remove(key, listener);
            return () -> {}; // StorageRemoveOperation is not Cancelable at the moment!
        });
    }

    @NonNull
    @Override
    public Single<StorageRemoveResult> remove(@NonNull String key, @NonNull StorageRemoveOptions options) {
        return RxAdapters.toSingle(listener -> {
            storage.remove(key, options, listener);
            return () -> {}; // StorageRemoveOperation is not Cancelable at the moment!
        });
    }

    @NonNull
    @Override
    public Single<StorageListResult> list(@NonNull String path) {
        return RxAdapters.toSingle(listener -> {
            storage.list(path, listener);
            return () -> {}; // StorageListOperation is not Cancelable at the moment!
        });
    }

    @NonNull
    @Override
    public Single<StorageListResult> list(@NonNull String path, @NonNull StorageListOptions options) {
        return RxAdapters.toSingle(listener -> {
            storage.list(path, options, listener);
            return () -> {}; // StorageListOperation is not Cancelable at the moment!
        });
    }
}
