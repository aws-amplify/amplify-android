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

import com.amplifyframework.AmplifyException;
import com.amplifyframework.core.Amplify;
import com.amplifyframework.core.Consumer;
import com.amplifyframework.core.model.Model;
import com.amplifyframework.core.model.ModelProvider;
import com.amplifyframework.datastore.DataStoreChannelEventName;
import com.amplifyframework.datastore.DataStoreException;
import com.amplifyframework.datastore.appsync.AppSync;
import com.amplifyframework.datastore.storage.LocalStorageAdapter;
import com.amplifyframework.datastore.storage.StorageItemChange;
import com.amplifyframework.hub.HubChannel;
import com.amplifyframework.hub.HubEvent;
import com.amplifyframework.logging.Logger;

import io.reactivex.Single;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.subjects.ReplaySubject;

/**
 * Starts subscriptions to AppSync. Applies data to local storage when it arrives.
 *
 * TODO: this component should save data via the merger, not directly through the
 * {@link LocalStorageAdapter}.
 */
final class SubscriptionProcessor {
    private static final Logger LOG = Amplify.Logging.forNamespace("amplify:aws-datastore");
    private final RemoteModelMutations remoteModelMutations;
    private final LocalStorageAdapter localStorageAdapter;
    private final CompositeDisposable disposable;
    private final ReplaySubject<Mutation<? extends Model>> buffer;

    SubscriptionProcessor(
            @NonNull LocalStorageAdapter localStorageAdapter,
            @NonNull AppSync appSync,
            @NonNull ModelProvider modelProvider) {
        this.localStorageAdapter = localStorageAdapter;
        this.remoteModelMutations = new RemoteModelMutations(appSync, modelProvider);
        this.disposable = new CompositeDisposable();
        this.buffer = ReplaySubject.create();
    }

    /**
     * Start subscribing to model mutations.
     */
    void startSubscriptions() {
        disposable.add(remoteModelMutations.observe()
            .subscribe(
                mutationOnSubscription -> {
                    buffer.onNext(mutationOnSubscription);
                    LOG.info("Successfully enqueued mutation from subscription: " + mutationOnSubscription);
                },
                error -> LOG.warn("Error enqueuing mutation from subscription.", error),
                () -> LOG.warn("Subscription to remote model mutations is completed.")
            )
        );
    }

    /**
     * Start draining mutations out of the mutation buffer.
     * This should be called after {@link #startSubscriptions()}.
     */
    void startDrainingMutationBuffer() {
        disposable.add(buffer
            .flatMapSingle(this::applyMutationToLocalStorage)
            .subscribe(
                savedMutation -> LOG.info("Saved a mutation from a subscription: " + savedMutation),
                failure -> LOG.warn("Reading subscriptions buffer has failed.", failure),
                () -> LOG.warn("Reading from subscriptions buffer is completed.")
            )
        );
    }

    /**
     * Stop any active subscriptions, and stop draining the mutation buffer.
     */
    void stopAllSubscriptionActivity() {
        disposable.clear();
    }

    private Single<Mutation<? extends Model>> applyMutationToLocalStorage(Mutation<? extends Model> mutation) {
        final StorageItemChange.Initiator initiator = StorageItemChange.Initiator.SYNC_ENGINE;
        return Single.defer(() -> Single.create(emitter -> {
            final Consumer<StorageItemChange.Record> onSuccess =
                result -> emitter.onSuccess(mutation);
            final Consumer<DataStoreException> onError = emitter::onError;

            switch (mutation.type()) {
                case UPDATE:
                case CREATE:
                    localStorageAdapter.save(mutation.model(), initiator, onSuccess, onError);
                    break;
                case DELETE:
                    localStorageAdapter.delete(mutation.model(), initiator, onSuccess, onError);
                    break;
                default:
                    throw new DataStoreException(
                        "Unknown mutation type = " + mutation.type(),
                        AmplifyException.TODO_RECOVERY_SUGGESTION
                    );
            }

            // Notify Hub that we just updated the local storage.
            HubEvent<? extends Model> receivedFromCloudEvent =
                HubEvent.create(DataStoreChannelEventName.RECEIVED_FROM_CLOUD, mutation.model());
            Amplify.Hub.publish(HubChannel.DATASTORE, receivedFromCloudEvent);
        }));
    }
}
