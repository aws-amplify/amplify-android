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
import com.amplifyframework.datastore.appsync.AppSyncConflictUnhandledError;
import com.amplifyframework.datastore.appsync.ModelWithMetadata;
import com.amplifyframework.datastore.events.OutboxStatusEvent;
import com.amplifyframework.hub.HubChannel;
import com.amplifyframework.hub.HubEvent;
import com.amplifyframework.logging.Logger;

import java.util.List;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

import io.reactivex.rxjava3.core.Completable;
import io.reactivex.rxjava3.core.Single;
import io.reactivex.rxjava3.disposables.CompositeDisposable;
import io.reactivex.rxjava3.schedulers.Schedulers;

/**
 * The {@link MutationProcessor} observes the {@link MutationOutbox}, and publishes its items to an
 * {@link AppSync}. The responses to these mutations are themselves forwarded to the Merger,
 * which may again modify the store.
 */
final class MutationProcessor {
    private static final Logger LOG = Amplify.Logging.forNamespace("amplify:aws-datastore");
    private static final long ITEM_PROCESSING_TIMEOUT_MS = TimeUnit.SECONDS.toMillis(10);

    private final Merger merger;
    private final VersionRepository versionRepository;
    private final MutationOutbox mutationOutbox;
    private final AppSync appSync;
    private final ConflictResolver conflictResolver;
    private final CompositeDisposable ongoingOperationsDisposable;

    private MutationProcessor(Builder builder) {
        this.merger = Objects.requireNonNull(builder.merger);
        this.versionRepository = Objects.requireNonNull(builder.versionRepository);
        this.mutationOutbox = Objects.requireNonNull(builder.mutationOutbox);
        this.appSync = Objects.requireNonNull(builder.appSync);
        this.conflictResolver = Objects.requireNonNull(builder.conflictResolver);
        this.ongoingOperationsDisposable = new CompositeDisposable();
    }

    /**
     * Returns a step builder to begin construction of a new
     * {@link MutationProcessor} instance.
     * @return The first step in a sequence of steps to build an instance
     *          of the mutation processor
     */
    public static BuilderSteps.MergerStep builder() {
        return new Builder();
    }

    /**
     * Start observing the mutation outbox for locally-initiated changes.
     *
     * To process a pending mutation, we try to publish it to the remote GraphQL
     * API. If that succeeds, then we can remove it from the outbox. Otherwise,
     * we have to keep the mutation in the outbox, so that we can try to publish
     * it again later, when network conditions become favorable again.
     */
    void startDrainingMutationOutbox() {
        ongoingOperationsDisposable.add(mutationOutbox.events()
            .doOnSubscribe(disposable ->
                LOG.info(
                    "Started processing the mutation outbox. " +
                        "Pending mutations will be published to the cloud."
                )
            )
            .startWithItem(MutationOutbox.OutboxEvent.CONTENT_AVAILABLE) // To start draining immediately
            .subscribeOn(Schedulers.single())
            .observeOn(Schedulers.single())
            .flatMapCompletable(event -> drainMutationOutbox())
            .subscribe(
                () -> LOG.warn("Observation of mutation outbox was completed."),
                error -> LOG.warn("Error ended observation of mutation outbox: ", error)
            )
        );
    }

    private Completable drainMutationOutbox() {
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
                        .doOnComplete(() -> announceMutationProcessed(modelWithMetadata))
                )
            )
            .doOnComplete(() -> {
                LOG.debug(
                    "Pending mutation was published to cloud successfully, " +
                        "and removed from the mutation outbox: " + mutationOutboxItem
                );
                publishCurrentOutboxStatus();
            })
            .doOnError(error -> LOG.warn("Failed to publish a local change = " + mutationOutboxItem, error));
    }

    /**
     * Publish a successfully mutated model and its metadata to hub.
     * @param modelWithMetadata A model that was successfully mutated and its sync metadata
     * @param <T> Type of model
     */
    private <T extends Model> void announceMutationProcessed(ModelWithMetadata<T> modelWithMetadata) {
        OutboxMutationEvent<T> mutationEvent =
            OutboxMutationEvent.fromModelWithMetadata(modelWithMetadata);
        HubEvent<OutboxMutationEvent<T>> hubEvent =
            HubEvent.create(DataStoreChannelEventName.OUTBOX_MUTATION_PROCESSED, mutationEvent);
        Amplify.Hub.publish(HubChannel.DATASTORE, hubEvent);
    }

    /**
     * Publish current outbox status to hub.
     */
    private void publishCurrentOutboxStatus() {
        HubEvent<OutboxStatusEvent> hubEvent =
            new OutboxStatusEvent(mutationOutbox.peek() == null).toHubEvent();
        Amplify.Hub.publish(HubChannel.DATASTORE, hubEvent);
    }

    /**
     * Don't process any more mutations.
     */
    void stopDrainingMutationOutbox() {
        // Calling clear on ongoingOperationsDisposable triggers dispose method
        // to anything that was added to ongoingOperationsDisposable
        ongoingOperationsDisposable.clear();
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
                appSync.update(model, version, mutation.getPredicate(), onSuccess, onError)
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
                appSync.delete(
                    deletedItemClass, deletedItem.getId(), version, mutation.getPredicate(), onSuccess, onError
                )
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
    @NonNull
    private <T extends Model> Single<ModelWithMetadata<T>> publishWithStrategy(
            @NonNull PendingMutation<T> mutation,
            @NonNull PublicationStrategy<T> publicationStrategy) {
        return Single
            .<GraphQLResponse<ModelWithMetadata<T>>>create(subscriber ->
                publicationStrategy.publish(mutation.getMutatedItem(), subscriber::onSuccess, subscriber::onError)
            )
            .flatMap(response -> {
                // If there are no errors, and the response has data, just return.
                if (!response.hasErrors() && response.hasData()) {
                    return Single.just(response.getData());
                } else {
                    return handleResponseErrors(mutation, response.getErrors());
                }
            });
    }

    /**
     * Handle errors that come back from AppSync while attempting to publish a mutation.
     * @param <T> Type of model for which a publication had response errors
     * @return A ModelWithMetadata representing the data as AppSync understands it;
     *         the MutationProcessor should apply this data into the local store,
     *         in a later step.
     */
    private <T extends Model> Single<ModelWithMetadata<T>> handleResponseErrors(
            PendingMutation<T> pendingMutation,
            List<GraphQLResponse.Error> errors) {
        // At this point, we know something wrong. Check if the mutation failed
        // due to ConflictUnhandled. If so, invoke our user-provided handler
        // to try and recover. We don't know how to resolve other types of errors,
        // so we just bubble those out in a DataStoreException.
        Class<T> modelClazz = pendingMutation.getClassOfMutatedItem();
        AppSyncConflictUnhandledError<T> unhandledConflict =
            AppSyncConflictUnhandledError.findFirst(modelClazz, errors);
        if (unhandledConflict == null) {
            return Single.error(new DataStoreException(
                "Mutation failed. Failed mutation = " + pendingMutation + ". " +
                    "AppSync response contained errors = " + errors,
                "Verify that your AppSync endpoint is able to store " + modelClazz + " models."
            ));
        }

        return conflictResolver.resolve(pendingMutation, unhandledConflict);
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

    static final class Builder implements
            BuilderSteps.MergerStep,
            BuilderSteps.VersionRepositoryStep,
            BuilderSteps.MutationOutboxStep,
            BuilderSteps.AppSyncStep,
            BuilderSteps.ConflictResolverStep,
            BuilderSteps.BuildStep {
        private Merger merger;
        private VersionRepository versionRepository;
        private MutationOutbox mutationOutbox;
        private AppSync appSync;
        private ConflictResolver conflictResolver;

        @NonNull
        @Override
        public BuilderSteps.VersionRepositoryStep merger(@NonNull Merger merger) {
            Builder.this.merger = Objects.requireNonNull(merger);
            return Builder.this;
        }

        @NonNull
        @Override
        public BuilderSteps.MutationOutboxStep versionRepository(@NonNull VersionRepository versionRepository) {
            Builder.this.versionRepository = Objects.requireNonNull(versionRepository);
            return Builder.this;
        }

        @NonNull
        @Override
        public BuilderSteps.AppSyncStep mutationOutbox(@NonNull MutationOutbox mutationOutbox) {
            Builder.this.mutationOutbox = Objects.requireNonNull(mutationOutbox);
            return Builder.this;
        }

        @NonNull
        @Override
        public BuilderSteps.ConflictResolverStep appSync(@NonNull AppSync appSync) {
            Builder.this.appSync = Objects.requireNonNull(appSync);
            return Builder.this;
        }

        @NonNull
        @Override
        public BuilderSteps.BuildStep conflictResolver(@NonNull ConflictResolver conflictResolver) {
            this.conflictResolver = Objects.requireNonNull(conflictResolver);
            return Builder.this;
        }

        @NonNull
        @Override
        public MutationProcessor build() {
            return new MutationProcessor(Builder.this);
        }
    }

    interface BuilderSteps {
        interface MergerStep {
            @NonNull
            VersionRepositoryStep merger(@NonNull Merger merger);
        }

        interface VersionRepositoryStep {
            @NonNull
            MutationOutboxStep versionRepository(@NonNull VersionRepository versionRepository);
        }

        interface MutationOutboxStep {
            @NonNull
            AppSyncStep mutationOutbox(@NonNull MutationOutbox mutationOutbox);
        }

        interface AppSyncStep {
            @NonNull
            ConflictResolverStep appSync(@NonNull AppSync appSync);
        }

        interface ConflictResolverStep {
            @NonNull
            BuildStep conflictResolver(@NonNull ConflictResolver conflictResolver);
        }

        interface BuildStep {
            @NonNull
            MutationProcessor build();
        }
    }
}
