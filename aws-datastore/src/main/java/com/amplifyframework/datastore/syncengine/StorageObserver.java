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

import com.amplifyframework.core.Action;
import com.amplifyframework.core.Amplify;
import com.amplifyframework.core.category.CategoryType;
import com.amplifyframework.core.model.Model;
import com.amplifyframework.core.model.SerializedModel;
import com.amplifyframework.datastore.storage.LocalStorageAdapter;
import com.amplifyframework.datastore.storage.StorageItemChange;
import com.amplifyframework.logging.Logger;

import java.util.Objects;

import io.reactivex.rxjava3.core.Observable;
import io.reactivex.rxjava3.disposables.CompositeDisposable;
import io.reactivex.rxjava3.schedulers.Schedulers;

/**
 * Observes a {@link LocalStorageAdapter} for its {@link StorageItemChange}s.
 * When such a change is observed, build an {@link PendingMutation}, and write
 * it onto a {@link MutationOutbox}.
 */
final class StorageObserver {
    private static final Logger LOG = Amplify.Logging.logger(CategoryType.DATASTORE, "amplify:aws-datastore");

    private final LocalStorageAdapter localStorageAdapter;
    private final MutationOutbox mutationOutbox;
    private final CompositeDisposable ongoingOperationsDisposable;

    StorageObserver(
            @NonNull LocalStorageAdapter localStorageAdapter,
            @NonNull MutationOutbox mutationOutbox) {
        this.localStorageAdapter = Objects.requireNonNull(localStorageAdapter);
        this.mutationOutbox = Objects.requireNonNull(mutationOutbox);
        this.ongoingOperationsDisposable = new CompositeDisposable();
    }

    /**
     * When a change is observed on the storage adapter, and that change wasn't caused
     * by the sync engine, then place that change into the mutation outbox.
     */
    void startObservingStorageChanges(Action onStarted) {
        ongoingOperationsDisposable.add(
            Observable.<StorageItemChange<? extends Model>>create(emitter -> {
                localStorageAdapter.observe(emitter::onNext, emitter::onError, emitter::onComplete);
                onStarted.call();
            })
            .subscribeOn(Schedulers.single())
            .observeOn(Schedulers.single())
            .doOnSubscribe(disposable ->
                LOG.info("Now observing local storage. Local changes will be enqueued to mutation outbox.")
            )
            .filter(possiblySystemChange -> {
                // Only enqueue mutations for USER models
                return Model.Type.USER.equals(possiblySystemChange.modelSchema().getModelType());
            })
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

    private PendingMutation<SerializedModel> toPendingMutation(StorageItemChange<? extends Model> change) {
        switch (change.type()) {
            case CREATE:
                return PendingMutation.creation(change.patchItem(), change.modelSchema());
            case UPDATE:
                return PendingMutation.update(change.patchItem(), change.modelSchema(), change.predicate());
            case DELETE:
                return PendingMutation.deletion(change.patchItem(), change.modelSchema(), change.predicate());
            default:
                throw new IllegalStateException("Unknown mutation type = " + change.type());
        }
    }

    /**
     * Stop observing changes in the storage adapter.
     */
    void stopObservingStorageChanges() {
        ongoingOperationsDisposable.clear();
    }
}
