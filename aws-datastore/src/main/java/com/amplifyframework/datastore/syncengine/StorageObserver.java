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

package com.amplifyframework.datastore.syncengine;

import androidx.annotation.NonNull;

import com.amplifyframework.core.Amplify;
import com.amplifyframework.core.model.Model;
import com.amplifyframework.datastore.storage.LocalStorageAdapter;
import com.amplifyframework.datastore.storage.StorageItemChange;
import com.amplifyframework.logging.Logger;

import java.util.Objects;

import io.reactivex.Observable;
import io.reactivex.disposables.CompositeDisposable;

/**
 * Observes a {@link LocalStorageAdapter} for its {@link StorageItemChange}s.
 * When such a change is observed, build an {@link PendingMutation}, and write
 * it onto a {@link MutationOutbox}.
 */
final class StorageObserver {
    private static final Logger LOG = Amplify.Logging.forNamespace("amplify:aws-datastore");

    private final LocalStorageAdapter localStorageAdapter;
    private final MutationOutbox mutationOutbox;
    private final CompositeDisposable disposable;

    StorageObserver(
            @NonNull LocalStorageAdapter localStorageAdapter,
            @NonNull MutationOutbox mutationOutbox) {
        this.localStorageAdapter = Objects.requireNonNull(localStorageAdapter);
        this.mutationOutbox = Objects.requireNonNull(mutationOutbox);
        this.disposable = new CompositeDisposable();
    }

    /**
     * When a change is observed on the storage adapter, and that change wasn't caused
     * by the sync engine, then place that change into the mutation outbox.
     */
    void startObservingStorageChanges() {
        disposable.add(streamOfStorageChanges()
            .filter(possiblyCyclicChange -> {
                // Don't continue if the storage change was caused by the sync engine itself
                return !StorageItemChange.Initiator.SYNC_ENGINE.equals(possiblyCyclicChange.initiator());
            })
            .map(this::toPendingMutation)
            .flatMapCompletable(mutationOutbox::enqueue)
            .subscribe(
                () -> LOG.warn("Storage adapter subscription terminated with completion."),
                error -> LOG.warn("Storage adapter subscription ended in error", error)
            )
        );
    }

    private <T extends Model> PendingMutation<T> toPendingMutation(StorageItemChange<T> change) {
        switch (change.type()) {
            case CREATE:
                return PendingMutation.creation(change.item(), change.itemClass());
            case UPDATE:
                return PendingMutation.update(change.item(), change.itemClass());
            case DELETE:
                return PendingMutation.deletion(change.item(), change.itemClass());
            default:
                throw new IllegalStateException("Unknown mutation type = " + change.type());
        }
    }

    /**
     * Stop observing changes in the storage adapter.
     */
    void stopObservingStorageChanges() {
        disposable.clear();
    }

    private Observable<StorageItemChange<? extends Model>> streamOfStorageChanges() {
        return Observable.create(emitter ->
            localStorageAdapter.observe(emitter::onNext, emitter::onError, emitter::onComplete)
        );
    }
}
