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

package com.amplifyframework.datastore.network;

import com.amplifyframework.core.ResultListener;
import com.amplifyframework.core.model.Model;
import com.amplifyframework.datastore.storage.LocalStorageAdapter;
import com.amplifyframework.datastore.storage.StorageItemChange;

import io.reactivex.Completable;
import io.reactivex.CompletableEmitter;
import io.reactivex.schedulers.Schedulers;

/**
 * "Hydrates" the local DataStore, using model metadata receive from the
 * {@link RemoteModelState}. Hydration refers to populating the local storage
 * with values from a remote system.
 */
@SuppressWarnings("unused")
final class DataHydration {
    private final RemoteModelState remoteModelState;
    private final LocalStorageAdapter localStorageAdapter;

    DataHydration(
            RemoteModelState remoteModelState,
            LocalStorageAdapter localStorageAdapter) {
        this.remoteModelState = remoteModelState;
        this.localStorageAdapter = localStorageAdapter;
    }

    /**
     * The task of hydrating the DataStore either succeeds (with no return value),
     * or it fails, with an explanation.
     * @return An Rx {@link Completable} which can be used to perform the operation.
     */
    Completable hydrate() {
        // Observe the remote model states,
        return remoteModelState.observe()
            .subscribeOn(Schedulers.io())
            .observeOn(Schedulers.io())
            // For each model state, provide it as an input to a completable operation
            .flatMapCompletable(modelWithMetadata ->
                // Save the metadata if the data save succeeds
                saveItemToStorage(modelWithMetadata)
                    .andThen(saveMetadataToStorage(modelWithMetadata))
            );
    }

    private <T extends Model> Completable saveItemToStorage(ModelWithMetadata<T> modelWithMetadata) {
        return Completable.create(emitter -> {
            // Save the model portion
            final ModelMetadata metadata = modelWithMetadata.getSyncMetadata();
            final T item = modelWithMetadata.getModel();
            final StorageItemChangeListener listener = new StorageItemChangeListener(emitter);
            if (Boolean.TRUE.equals(metadata.isDeleted())) {
                localStorageAdapter.delete(item, StorageItemChange.Initiator.SYNC_ENGINE, listener);
            } else {
                localStorageAdapter.save(item, StorageItemChange.Initiator.SYNC_ENGINE, listener);
            }
        });
    }

    private <T extends Model> Completable saveMetadataToStorage(ModelWithMetadata<T> modelWithMetadata) {
        return Completable.create(emitter -> {
            // Save the metadata portion
            // This is separate from the model save since they have two distinct completions.
            final ModelMetadata metadata = modelWithMetadata.getSyncMetadata();
            final StorageItemChangeListener metadataSaveListener = new StorageItemChangeListener(emitter);
            localStorageAdapter.save(metadata, StorageItemChange.Initiator.SYNC_ENGINE, metadataSaveListener);
        });
    }

    /**
     * Listens to change record on the {@link LocalStorageAdapter}, for
     * {@link LocalStorageAdapter#save(Model, StorageItemChange.Initiator, ResultListener)} and
     * {@link LocalStorageAdapter#delete(Model, StorageItemChange.Initiator, ResultListener)}.
     *  Publishes the values onto a {@link CompletableEmitter}, ignoring the result that is
     *  provided in {@link ResultListener#onResult(Object)}, if/when that is invoked.
     */
    static final class StorageItemChangeListener implements ResultListener<StorageItemChange.Record> {
        private final CompletableEmitter emitter;

        StorageItemChangeListener(CompletableEmitter emitter) {
            this.emitter = emitter;
        }

        @Override
        public void onResult(StorageItemChange.Record result) {
            emitter.onComplete();
        }

        @Override
        public void onError(Throwable error) {
            emitter.onError(error);
        }
    }
}
