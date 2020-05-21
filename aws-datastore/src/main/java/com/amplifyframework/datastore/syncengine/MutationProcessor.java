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

import com.amplifyframework.api.graphql.GraphQLResponse;
import com.amplifyframework.core.Amplify;
import com.amplifyframework.core.Consumer;
import com.amplifyframework.core.model.Model;
import com.amplifyframework.datastore.DataStoreChannelEventName;
import com.amplifyframework.datastore.DataStoreException;
import com.amplifyframework.datastore.appsync.AppSync;
import com.amplifyframework.datastore.appsync.ModelWithMetadata;
import com.amplifyframework.hub.HubChannel;
import com.amplifyframework.hub.HubEvent;
import com.amplifyframework.logging.Logger;

import java.util.Objects;
import java.util.concurrent.TimeUnit;

import io.reactivex.Completable;
import io.reactivex.Single;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;

/**
 * The {@link MutationProcessor} observes the {@link MutationOutbox}, and publishes its items to an
 * {@link AppSync}. The responses to these mutations are themselves forwarded to the Merger,
 * which may again modify the store.
 */
final class MutationProcessor {
    private static final Logger LOG = Amplify.Logging.forNamespace("amplify:aws-datastore");
    private static final long ITEM_PROCESSING_TIMEOUT_MS = TimeUnit.SECONDS.toMillis(10);

    private final VersionRepository versionRepository;
    private final Merger merger;
    private final AppSync appSync;
    private final MutationOutbox mutationOutbox;
    private final CompositeDisposable ongoingOperationsDisposable;

    MutationProcessor(
            @NonNull Merger merger,
            @NonNull VersionRepository versionRepository,
            @NonNull MutationOutbox mutationOutbox,
            @NonNull AppSync appSync) {
        this.merger = Objects.requireNonNull(merger);
        this.versionRepository = Objects.requireNonNull(versionRepository);
        this.appSync = Objects.requireNonNull(appSync);
        this.mutationOutbox = Objects.requireNonNull(mutationOutbox);
        this.ongoingOperationsDisposable = new CompositeDisposable();
    }

    /**
     * Start observing the mutation outbox for locally-initiated changes.
     *
     * To process a pending mutation, we try to publish it to the remote GraphQL
     * API. If that succeeds, then we can remove it from the outbox. Otherwise,
     * we have to keep the mutation in the outbox, so that we can try to publish
     * it again later, when network conditions become favorable again.
     */
    Disposable startDrainingMutationOutbox() {
        ongoingOperationsDisposable.add(mutationOutbox.events()
            .doOnSubscribe(disposable ->
                LOG.info(
                    "Started processing the mutation outbox. " +
                        "Pending mutations will be published to the cloud."
                )
            )
            .startWith(MutationOutbox.OutboxEvent.CONTENT_AVAILABLE) // To start draining immediately
            .subscribeOn(Schedulers.single())
            .observeOn(Schedulers.single())
            .flatMapCompletable(event -> drainMutationOutbox())
            .subscribe(
                () -> LOG.warn("Observation of mutation outbox was completed."),
                error -> {
                    LOG.warn("Error ended observation of mutation outbox: ", error);
                    Amplify.Hub.publish(HubChannel.DATASTORE,
                        HubEvent.create(DataStoreChannelEventName.LOST_CONNECTION)
                    );
                }
            )
        );
        return ongoingOperationsDisposable;
    }

    /**
     * Stop draining the mutation outbox.
     */
    void stopDrainingMutationOutbox() {
        ongoingOperationsDisposable.clear();
    }

    Completable drainMutationOutbox() {
        PendingMutation<? extends Model> next;
        do {
            next = mutationOutbox.peek();
            if (next == null) {
                return Completable.complete();
            }
            boolean itemFailedToProcess = !processOutboxItem(next)
                .blockingAwait(ITEM_PROCESSING_TIMEOUT_MS, TimeUnit.MILLISECONDS);
            if (itemFailedToProcess) {
                return Completable.error(new DataStoreException(
                    "Failed to process " + next, "Check your internet connection."
                ));
            }
        } while (true);
    }

    /**
     * Process an item in the mutation outbox.
     * @param mutationOutboxItem An item in the mutation outbox
     * @param <T> Type of model
     * @return A Completable that emits success when the item is processed, emits failure, otherwise
     */
    private <T extends Model> Completable processOutboxItem(PendingMutation<T> mutationOutboxItem) {
        // First, mark the item as in-flight.
        return mutationOutbox.markInFlight(mutationOutboxItem.getMutationId())
            // Then, put it "into flight"
            .andThen(publishToNetwork(mutationOutboxItem)
                .flatMapCompletable(modelWithMetadata ->
                    // Once the server knows about it, it's safe to remove from the outbox.
                    // This is done before merging, because the merger will refuse to merge
                    // if there are outstanding mutations in the outbox.
                    mutationOutbox.remove(mutationOutboxItem.getMutationId())
                        .andThen(merger.merge(modelWithMetadata))
                )
            )
            .doOnComplete(() -> {
                LOG.debug(
                    "Pending mutation was published to cloud successfully, " +
                        "and removed from the mutation outbox: " + mutationOutboxItem
                );
                announceSuccessfulPublication(mutationOutboxItem);
            })
            .doOnError(error -> LOG.warn("Failed to publish a local change = " + mutationOutboxItem, error));
    }

    /**
     * Publish a successfully processed pending mutation to hub.
     * @param processedMutation A mutation that has been successfully processed and removed from outbox
     * @param <T> Type of model
     */
    private <T extends Model> void announceSuccessfulPublication(PendingMutation<T> processedMutation) {
        Amplify.Hub.publish(
            HubChannel.DATASTORE,
            HubEvent.create(DataStoreChannelEventName.PUBLISHED_TO_CLOUD, processedMutation)
        );
    }

    /**
     * Attempt to publish a mutation (update, delete, creation) over the network.
     * @param pendingMutation A pending mutation, waiting to be published to remote API
     * @param <T> Type of model
     * @return A single which completes with the successfully published item, or emits error
     *         if the publication fails
     */
    private <T extends Model> Single<ModelWithMetadata<T>> publishToNetwork(PendingMutation<T> pendingMutation) {
        switch (pendingMutation.getMutationType()) {
            case UPDATE:
                return update(pendingMutation);
            case CREATE:
                return create(pendingMutation);
            case DELETE:
                return delete(pendingMutation);
            default:
                return Single.error(new DataStoreException(
                   "Unknown mutation type in storage = " + pendingMutation.getMutationType(),
                   "This is likely a bug. Please file a ticket with AWS."
                ));
        }
    }

    // For an item in the outbox, dispatch an update mutation
    private <T extends Model> Single<ModelWithMetadata<T>> update(PendingMutation<T> mutation) {
        final T updatedItem = mutation.getMutatedItem();
        return versionRepository.findModelVersion(updatedItem).flatMap(version ->
            publishWithStrategy(mutation, (model, onSuccess, onError) ->
                appSync.update(model, version, onSuccess, onError)
            )
        );
    }

    // For an item in the outbox, dispatch a create mutation
    private <T extends Model> Single<ModelWithMetadata<T>> create(PendingMutation<T> mutation) {
        return publishWithStrategy(mutation, appSync::create);
    }

    // For an item in the outbox, dispatch a delete mutation
    private <T extends Model> Single<ModelWithMetadata<T>> delete(PendingMutation<T> mutation) {
        final T deletedItem = mutation.getMutatedItem();
        final Class<T> deletedItemClass = mutation.getClassOfMutatedItem();
        return versionRepository.findModelVersion(deletedItem).flatMap(version ->
            publishWithStrategy(mutation, (model, onSuccess, onError) ->
                appSync.delete(deletedItemClass, deletedItem.getId(), version, onSuccess, onError)
            )
        );
    }

    /**
     * For an pending mutation, publish mutated item using a publication strategy.
     * @param mutation A mutation that is waiting to be published
     * @param publicationStrategy A strategy to publish the mutated item
     * @param <T> The model type of the item
     * @return A single which emits the model with its metadata, upon success; emits
     *         a failure, if publication does not succeed
     */
    private <T extends Model> Single<ModelWithMetadata<T>> publishWithStrategy(
            PendingMutation<T> mutation, PublicationStrategy<T> publicationStrategy) {
        T mutatedItem = mutation.getMutatedItem();
        String modelClassName = mutation.getClassOfMutatedItem().getSimpleName();
        return Single.defer(() -> Single.create(subscriber ->
            publicationStrategy.publish(
                mutatedItem,
                result -> {
                    if (!result.hasErrors() && result.hasData()) {
                        subscriber.onSuccess(result.getData());
                        return;
                    }
                    subscriber.onError(new DataStoreException(
                        "Mutation failed. Failed mutation = " + mutation + ". " +
                            "AppSync response contained errors = " + result.getErrors(),
                        "Verify that your AppSync endpoint is able to store " + modelClassName + " models."
                    ));
                },
                subscriber::onError
            )
        ));
    }

    /**
     * A strategy to publish an item over the network.
     * @param <T> Type of model being published
     */
    interface PublicationStrategy<T extends Model> {
        /**
         * Publish a pending mutation, over the network.
         * @param item An item to publish
         * @param onSuccess Called when publication succeeds
         * @param onFailure Called when publication fails
         */
        void publish(
            T item,
            Consumer<GraphQLResponse<ModelWithMetadata<T>>> onSuccess,
            Consumer<DataStoreException> onFailure
        );
    }
}
