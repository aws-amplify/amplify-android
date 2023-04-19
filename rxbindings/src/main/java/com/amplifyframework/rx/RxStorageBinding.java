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
import com.amplifyframework.core.Consumer;
import com.amplifyframework.core.async.NoOpCancelable;
import com.amplifyframework.core.async.Resumable;
import com.amplifyframework.rx.RxAdapters.CancelableBehaviors;
import com.amplifyframework.storage.StorageCategory;
import com.amplifyframework.storage.StorageCategoryBehavior;
import com.amplifyframework.storage.StorageException;
import com.amplifyframework.storage.operation.StorageTransferOperation;
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

import io.reactivex.rxjava3.core.Observable;
import io.reactivex.rxjava3.core.Single;
import io.reactivex.rxjava3.subjects.PublishSubject;
import io.reactivex.rxjava3.subjects.ReplaySubject;

/**
 * Rx binding for Amplify's storage category.
 */
public final class RxStorageBinding implements RxStorageCategoryBehavior {
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
    public Single<StorageGetUrlResult> getUrl(String key) {
        return toSingle((onResult, onError) -> {
            storage.getUrl(key, onResult, onError);
            return new NoOpCancelable();
        });
    }

    @NonNull
    @Override
    public Single<StorageGetUrlResult> getUrl(@NonNull String key, @NonNull StorageGetUrlOptions options) {
        return toSingle((onResult, onError) -> {
            storage.getUrl(key, options, onResult, onError);
            return new NoOpCancelable();
        });
    }

    @NonNull
    @Override
    public RxProgressAwareSingleOperation<StorageDownloadFileResult> downloadFile(@NonNull String key,
                                                                                  @NonNull File local) {
        return downloadFile(key, local, StorageDownloadFileOptions.defaultInstance());
    }

    @NonNull
    @Override
    public RxProgressAwareSingleOperation<StorageDownloadFileResult> downloadFile(
            @NonNull String key, @NonNull File local, @NonNull StorageDownloadFileOptions options) {
        return new RxProgressAwareSingleOperation<>((onProgress, onResult, onError) ->
            storage.downloadFile(key, local, options, onProgress, onResult, onError)
        );
    }

    @NonNull
    @Override
    public RxProgressAwareSingleOperation<StorageUploadFileResult> uploadFile(@NonNull String key,
                                                                              @NonNull File local) {
        return uploadFile(key, local, StorageUploadFileOptions.defaultInstance());
    }

    @NonNull
    @Override
    public RxProgressAwareSingleOperation<StorageUploadFileResult> uploadFile(
            @NonNull String key, @NonNull File local, @NonNull StorageUploadFileOptions options) {
        return new RxProgressAwareSingleOperation<>((onProgress, onResult, onError) ->
            storage.uploadFile(key, local, options, onProgress, onResult, onError)
        );
    }

    @NonNull
    @Override
    public RxProgressAwareSingleOperation<StorageUploadInputStreamResult> uploadInputStream(
            @NonNull String key, @NonNull InputStream local) {
        return uploadInputStream(key, local, StorageUploadInputStreamOptions.defaultInstance());
    }

    @NonNull
    @Override
    public RxProgressAwareSingleOperation<StorageUploadInputStreamResult> uploadInputStream(
            @NonNull String key, @NonNull InputStream local, @NonNull StorageUploadInputStreamOptions options) {
        return new RxProgressAwareSingleOperation<>((onProgress, onResult, onError) ->
            storage.uploadInputStream(key, local, options, onProgress, onResult, onError)
        );
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
    @SuppressWarnings("deprecation")
    public Single<StorageListResult> list(@NonNull String path) {
        return toSingle((onResult, onError) -> {
            storage.list(path, onResult, onError);
            return new NoOpCancelable(); // StorageListOperation is not Cancelable at the moment!
        });
    }

    @NonNull
    @Override
    @SuppressWarnings("deprecation")
    public Single<StorageListResult> list(@NonNull String path, @NonNull StorageListOptions options) {
        return toSingle((onResult, onError) -> {
            storage.list(path, options, onResult, onError);
            return new NoOpCancelable(); // StorageListOperation is not Cancelable at the moment!
        });
    }

    @NonNull
    @Override
    public Single<StorageListResult> list(@NonNull String path, @NonNull StoragePagedListOptions options) {
        return toSingle((onResult, onError) -> {
            storage.list(path, options, onResult, onError);
            return new NoOpCancelable(); // StorageListOperation is not Cancelable at the moment!
        });
    }

    @NonNull
    @Override
    public Single<StorageTransferOperation<?, ? extends StorageTransferResult>> getTransfer(
        @NonNull String transferId) {
        return toSingle(((onResult, onError) -> {
            storage.getTransfer(transferId, onResult, onError);
            return new NoOpCancelable();
        }));
    }

    private <T> Single<T> toSingle(CancelableBehaviors.ResultEmitter<T, StorageException> method) {
        return CancelableBehaviors.toSingle(method);
    }

    /**
     * A generic implementation of an operation that emits
     * progress information and returns a single.
     * @param <T> The type that represents the result of a given operation.
     */
    public static final class RxProgressAwareSingleOperation<T> implements RxAdapters.RxSingleOperation<T>, Resumable {
        private final PublishSubject<StorageTransferProgress> progressSubject;
        private final ReplaySubject<T> resultSubject;
        private final StorageTransferOperation<?, ?> amplifyOperation;

        RxProgressAwareSingleOperation(RxStorageTransferCallbackMapper<T> callbacks) {
            progressSubject = PublishSubject.create();
            resultSubject = ReplaySubject.create();
            amplifyOperation = callbacks.emitTo(progressSubject::onNext,
                                                resultSubject::onNext,
                                                resultSubject::onError);
        }

        /**
         * Return the transfer ID for this operation.
         *
         * @return unique transferId for this operation
         */
        @NonNull
        public String getTransferId() {
            return amplifyOperation.getTransferId();
        }

        @Override
        public void resume() {
            amplifyOperation.resume();
        }

        @Override
        public void pause() {
            amplifyOperation.pause();
        }

        @Override
        public void cancel() {
            amplifyOperation.cancel();
            resultSubject.onComplete();
            progressSubject.onComplete();
        }

        /**
         * Returns a {@link Single} which consumers can use to capture
         * the result of the operation.
         * @return Instance of the {@link Single} for the operation result.
         */
        @Override
        public Single<T> observeResult() {
            return Single.create(emitter -> {
                emitter.setDisposable(
                    resultSubject.subscribe(emitter::onSuccess, emitter::tryOnError)
                );
            });
        }

        /**
         * Returns an {@link Observable} which consumers can use to get
         * progress notifications related to the operation.
         * @return Reference to the {@link Observable} for progress notifications.
         */
        public Observable<StorageTransferProgress> observeProgress() {
            return progressSubject;
        }
    }

    /**
     * Type alias that defines the generic parameters for a download operation.
     * @param <T> The type that represents the result of a given operation.
     */
    interface RxStorageTransferCallbackMapper<T> {
        StorageTransferOperation<?, ?> emitTo(
                Consumer<StorageTransferProgress> onProgress,
                Consumer<T> onItem,
                Consumer<StorageException> onError
        );
    }
}
