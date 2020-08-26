/*
 * Copyright 2020 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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
import androidx.annotation.VisibleForTesting;

import com.amplifyframework.core.Amplify;
import com.amplifyframework.core.async.NoOpCancelable;
import com.amplifyframework.storage.StorageCategory;
import com.amplifyframework.storage.StorageCategoryBehavior;
import com.amplifyframework.storage.StorageException;
import com.amplifyframework.storage.options.StorageDownloadFileOptions;
import com.amplifyframework.storage.options.StorageListOptions;
import com.amplifyframework.storage.options.StorageRemoveOptions;
import com.amplifyframework.storage.options.StorageUploadFileOptions;
import com.amplifyframework.storage.result.StorageDownloadFileResult;
import com.amplifyframework.storage.result.StorageListResult;
import com.amplifyframework.storage.result.StorageRemoveResult;
import com.amplifyframework.storage.result.StorageTransferProgress;
import com.amplifyframework.storage.result.StorageUploadFileResult;

import java.io.File;

import io.reactivex.rxjava3.core.Single;

final class RxStorageBinding implements RxStorageCategoryBehavior {
    private final StorageCategoryBehavior storage;

    RxStorageBinding() {
        this(Amplify.Storage);
    }

    @VisibleForTesting
    RxStorageBinding(StorageCategory storage) {
        this.storage = storage;
    }

    @NonNull
    @Override
    public RxStorageDownloadOperation downloadFile(@NonNull String key, @NonNull File local) {
        return downloadFile(key, local, StorageDownloadFileOptions.defaultInstance());
    }

    @NonNull
    @Override
    public RxStorageDownloadOperation downloadFile(
            @NonNull String key, @NonNull File local, @NonNull StorageDownloadFileOptions options) {
        return new RxStorageDownloadOperation((onProgress, onResult, onError) -> {
            return storage.downloadFile(key, local, options, onProgress, onResult, onError);
        });
    }

    @NonNull
    @Override
    public Single<StorageUploadFileResult> uploadFile(@NonNull String key, @NonNull File local) {
        return toSingle((onResult, onError) -> storage.uploadFile(key, local, onResult, onError));
    }

    @NonNull
    @Override
    public Single<StorageUploadFileResult> uploadFile(
            @NonNull String key, @NonNull File local, @NonNull StorageUploadFileOptions options) {
        return toSingle((onResult, onError) -> storage.uploadFile(key, local, options, onResult, onError));
    }

    @NonNull
    @Override
    public Single<StorageRemoveResult> remove(@NonNull String key) {
        return toSingle((onResult, onError) -> {
            storage.remove(key, onResult, onError);
            return new NoOpCancelable(); // StorageRemoveOperation (above) is not Cancelable right now!
        });
    }

    @NonNull
    @Override
    public Single<StorageRemoveResult> remove(@NonNull String key, @NonNull StorageRemoveOptions options) {
        return toSingle((onResult, onError) -> {
            storage.remove(key, options, onResult, onError);
            return new NoOpCancelable(); // StorageRemoveOperation is not Cancelable at the moment!
        });
    }

    @NonNull
    @Override
    public Single<StorageListResult> list(@NonNull String path) {
        return toSingle((onResult, onError) -> {
            storage.list(path, onResult, onError);
            return new NoOpCancelable(); // StorageListOperation is not Cancelable at the moment!
        });
    }

    @NonNull
    @Override
    public Single<StorageListResult> list(@NonNull String path, @NonNull StorageListOptions options) {
        return toSingle((onResult, onError) -> {
            storage.list(path, options, onResult, onError);
            return new NoOpCancelable(); // StorageListOperation is not Cancelable at the moment!
        });
    }

    private <T> Single<T> toSingle(RxAdapters.CancelableResultEmitter<T, StorageException> method) {
        return RxAdapters.toSingle(method);
    }

    /**
     * Defines the parameters of the download operation by
     * supplying the generic types required by {@link RxAdapters.RxProgressAwareSingle}.
     */
    static final class RxStorageDownloadOperation extends
        RxAdapters.RxProgressAwareSingle<StorageDownloadFileResult, StorageTransferProgress> {
        RxStorageDownloadOperation(RxStorageDownloadCallbackMapper callbacks) {
            super(callbacks);
        }
    }

    /**
     * Type alias that defines the generic parameters for a download operation.
     * @see RxAdapters.RxProgressAwareCallbackMapper
     */
    interface RxStorageDownloadCallbackMapper
        extends RxAdapters.RxProgressAwareCallbackMapper<StorageDownloadFileResult,
                                                         StorageTransferProgress,
                                                         StorageException> {
    }
}
