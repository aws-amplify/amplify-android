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

import androidx.annotation.NonNull;

import com.amplifyframework.core.Amplify;
import com.amplifyframework.datastore.storage.GsonStorageItemChangeConverter;
import com.amplifyframework.datastore.storage.LocalStorageAdapter;
import com.amplifyframework.datastore.storage.StorageItemChange;
import com.amplifyframework.logging.Logger;

import java.util.Objects;

import io.reactivex.Observable;
import io.reactivex.disposables.CompositeDisposable;

/**
 * Observes a {@link LocalStorageAdapter} for changes. When a change occurs,
 * writes it onto a {@link MutationOutbox}.
 */
final class StorageObserver {
    private static final Logger LOG = Amplify.Logging.forNamespace("amplify:aws-datastore");

    private final LocalStorageAdapter localStorageAdapter;
    private final MutationOutbox mutationOutbox;
    private final StorageItemChange.StorageItemChangeFactory storageItemChangeConverter;
    private final CompositeDisposable disposable;

    StorageObserver(
            @NonNull LocalStorageAdapter localStorageAdapter,
            @NonNull MutationOutbox mutationOutbox) {
        this.localStorageAdapter = Objects.requireNonNull(localStorageAdapter);
        this.mutationOutbox = Objects.requireNonNull(mutationOutbox);
        this.storageItemChangeConverter = new GsonStorageItemChangeConverter();
        this.disposable = new CompositeDisposable();
    }

    /**
     * When a change is observed on the storage adapter, and that change wasn't caused
     * by the sync engine, then place that change into the mutation outbox.
     */
    void startObservingStorageChanges() {
        disposable.add(streamOfStorageChangeRecords()
            .map(storageItemChangeConverter::fromRecord)
            .filter(possiblyCyclicChange -> {
                // Don't continue if the storage change was caused by the sync engine itself
                return !StorageItemChange.Initiator.SYNC_ENGINE.equals(possiblyCyclicChange.initiator());
            })
            .flatMapSingle(mutationOutbox::enqueue)
            .subscribe(
                pendingChange -> LOG.info("Successfully enqueued " + pendingChange),
                error -> LOG.warn("Storage adapter subscription ended in error", error),
                () -> LOG.warn("Storage adapter subscription terminated with completion.")
            )
        );
    }

    /**
     * Stop observing changes in the storage adapter.
     */
    void stopObservingStorageChanges() {
        disposable.clear();
    }

    private Observable<StorageItemChange.Record> streamOfStorageChangeRecords() {
        return Observable.create(emitter ->
            localStorageAdapter.observe(emitter::onNext, emitter::onError, emitter::onComplete)
        );
    }
}
