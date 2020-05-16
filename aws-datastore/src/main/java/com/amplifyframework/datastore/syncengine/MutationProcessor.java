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
import com.amplifyframework.core.category.CategoryType;
import com.amplifyframework.core.model.Model;
import com.amplifyframework.datastore.DataStoreChannelEventName;
import com.amplifyframework.datastore.DataStoreException;
import com.amplifyframework.datastore.appsync.AppSync;
import com.amplifyframework.datastore.appsync.ModelWithMetadata;
import com.amplifyframework.hub.HubChannel;
import com.amplifyframework.hub.HubEvent;
import com.amplifyframework.logging.Logger;

import java.util.Objects;

import io.reactivex.Completable;
import io.reactivex.Single;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.schedulers.Schedulers;

/**
 * The {@link MutationProcessor} observes the {@link MutationOutbox}, and publishes its items to an
 * {@link AppSync}. The responses to these mutations are themselves forwarded to the Merger,
 * which may again modify the store.
 */
@SuppressWarnings("CodeBlock2Expr")
final class MutationProcessor {
    private static final Logger LOG = Amplify.Logging.forCategory(CategoryType.DATASTORE);

    private final VersionRepository versionRepository;
    private final Merger merger;
    private final AppSync appSync;
    private final MutationOutbox mutationOutbox;
    private final CompositeDisposable disposable;

    MutationProcessor(
            @NonNull Merger merger,
            @NonNull VersionRepository versionRepository,
            @NonNull MutationOutbox mutationOutbox,
            @NonNull AppSync appSync) {
        this.merger = Objects.requireNonNull(merger);
        this.versionRepository = Objects.requireNonNull(versionRepository);
        this.appSync = Objects.requireNonNull(appSync);
        this.mutationOutbox = Objects.requireNonNull(mutationOutbox);
        this.disposable = new CompositeDisposable();
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
        disposable.add(
            mutationOutbox.observe()
                .subscribeOn(Schedulers.io())
                .observeOn(Schedulers.io())
                .flatMapCompletable(this::processOutboxItem)
                .subscribe(
                    () -> LOG.warn("Observation of mutation outbox was completed."),
                    error -> LOG.warn("Error ended observation of mutation outbox: ", error)
                )
        );
    }

    /**
     * Process an item in the mutation outbox.
     * @param mutationOutboxItem An item in the mutation outbox
     * @param <T> Type of model
     * @return A Completable that emits success when the item is processed, emits failure, otherwise
     */
    private <T extends Model> Completable processOutboxItem(PendingMutation<T> mutationOutboxItem) {
        // First, publish the mutation over the network.
        return publishToNetwork(mutationOutboxItem)
            // Merge the response back into the local store.
            .flatMapCompletable(merger::merge)
            // Lastly, remove the item from the outbox, so we don't process it again.
            .andThen(mutationOutbox.remove(mutationOutboxItem))
            .andThen(Completable.fromAction(() -> {
                LOG.info("Pending mutation was published up to Cloud: " + mutationOutboxItem);
                announceSuccessfulPublication(mutationOutboxItem);
            }));
    }

    /**
     * Publish a successfully processed pending mutation to hub.
     * @param processedMutation A mutation that has been successfully processed and removed from outbox
     * @param <T> Type of model
     */
    private <T extends Model> void announceSuccessfulPublication(PendingMutation<T> processedMutation) {
        HubEvent<PendingMutation<? extends Model>> publishedToCloudEvent =
            HubEvent.create(DataStoreChannelEventName.PUBLISHED_TO_CLOUD, processedMutation);
        Amplify.Hub.publish(HubChannel.DATASTORE, publishedToCloudEvent);
    }

    /**
     * Don't process any more mutations.
     */
    void stopDrainingMutationOutbox() {
        disposable.dispose();
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
        return versionRepository.findModelVersion(mutation.getMutatedItem()).flatMap(version -> {
            return publishWithStrategy(mutation, (model, onSuccess, onError) -> {
                appSync.update(model, version, onSuccess, onError);
            });
        });
    }

    // For an item in the outbox, dispatch a create mutation
    private <T extends Model> Single<ModelWithMetadata<T>> create(PendingMutation<T> mutation) {
        return publishWithStrategy(mutation, appSync::create);
    }

    // For an item in the outbox, dispatch a delete mutation
    private <T extends Model> Single<ModelWithMetadata<T>> delete(PendingMutation<T> mutation) {
        return versionRepository.findModelVersion(mutation.getMutatedItem()).flatMap(version -> {
            return publishWithStrategy(mutation, (model, onSuccess, onError) -> {
                final Class<T> modelClass = mutation.getClassOfMutatedItem();
                final String modelId = mutation.getMutatedItem().getId();
                appSync.delete(modelClass, modelId, version, onSuccess, onError);
            });
        });
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
        return Single.defer(() -> Single.create(subscriber -> {
            publicationStrategy.publish(
                mutation.getMutatedItem(),
                result -> {
                    if (!result.hasErrors() && result.hasData()) {
                        subscriber.onSuccess(result.getData());
                        return;
                    }
                    String modelName = mutation.getClassOfMutatedItem().getSimpleName();
                    subscriber.onError(new DataStoreException(
                        "Mutation failed. Failed mutation = " + mutation + ". " +
                            "AppSync response contained errors = " + result.getErrors(),
                        "Verify that your AppSync endpoint is able to store " + modelName + " models."
                    ));
                },
                subscriber::onError
            );
        }));
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
