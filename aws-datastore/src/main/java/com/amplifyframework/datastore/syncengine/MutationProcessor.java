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
import com.amplifyframework.core.model.query.predicate.QueryField;
import com.amplifyframework.core.model.query.predicate.QueryPredicate;
import com.amplifyframework.datastore.DataStoreChannelEventName;
import com.amplifyframework.datastore.DataStoreException;
import com.amplifyframework.datastore.appsync.AppSync;
import com.amplifyframework.datastore.appsync.ModelMetadata;
import com.amplifyframework.datastore.appsync.ModelWithMetadata;
import com.amplifyframework.datastore.storage.LocalStorageAdapter;
import com.amplifyframework.datastore.storage.StorageItemChange;
import com.amplifyframework.hub.HubChannel;
import com.amplifyframework.hub.HubEvent;
import com.amplifyframework.logging.Logger;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import io.reactivex.Completable;
import io.reactivex.Single;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.schedulers.Schedulers;

/**
 * The {@link MutationProcessor} observes the {@link MutationOutbox}, and publishes its items to an
 * {@link AppSync}.
 *
 * The responses to these mutations are themselves forwarded to the Merger (TODO: write a merger.)
 */
@SuppressWarnings("CodeBlock2Expr")
final class MutationProcessor {
    private static final Logger LOG = Amplify.Logging.forNamespace("amplify:aws-datastore");

    private final LocalStorageAdapter localStorageAdapter;
    private final AppSync appSync;
    private final MutationOutbox mutationOutbox;
    private final CompositeDisposable disposable;

    MutationProcessor(
            @NonNull LocalStorageAdapter localStorageAdapter,
            @NonNull MutationOutbox mutationOutbox,
            @NonNull AppSync appSync) {
        this.localStorageAdapter = localStorageAdapter;
        this.appSync = Objects.requireNonNull(appSync);
        this.mutationOutbox = Objects.requireNonNull(mutationOutbox);
        this.disposable = new CompositeDisposable();
    }

    /**
     * Start observing the mutation outbox for locally-initiated changes.
     *
     * To process a StorageItemChange, we try to publish it to the remote GraphQL
     * API. If that succeeds, then we can remove it from the outbox. Otherwise,
     * we have to keep the mutation in the outbox, so that we can try to publish
     * it again later, when network conditions become favorable again.
     */
    void startDrainingMutationOutbox() {
        disposable.add(
            mutationOutbox.observe()
                .subscribeOn(Schedulers.io())
                .observeOn(Schedulers.io())
                .flatMapSingle(this::processOutboxItem)
                .subscribe(
                    this::announceSuccessToHub,
                    error -> LOG.warn("Error ended observation of mutation outbox: ", error),
                    () -> LOG.warn("Observation of mutation outbox was completed.")
                )
        );
    }

    /**
     * Process an item in the mutation outbox.
     * @param mutationOutboxItem An item in the mutation outbox
     * @param <T> Type of model
     * @return A single echos back the processed change, upon success; emits failure, upon
     */
    private <T extends Model> Single<StorageItemChange<T>> processOutboxItem(StorageItemChange<T> mutationOutboxItem) {
        // First, publish the change over the network.
        return publishToNetwork(mutationOutboxItem)
            .flatMapCompletable(modelWithMetadata -> {
                // Then, save the RESPONSE of the network call, locally
                return saveModel(modelWithMetadata.getModel())
                    // If that succeeds, save the metadata, too.
                    .andThen(saveModel(modelWithMetadata.getSyncMetadata()));
            })
            // Lastly, remove the item from the outbox, so we don't process it again.
            .andThen(mutationOutbox.remove(mutationOutboxItem));
    }

    /**
     * Publish a successfully processed storage item change to hub.
     * @param processedChange A change that has been successfully processed and removed from outbox
     * @param <T> Type of model
     */
    private <T extends Model> void announceSuccessToHub(StorageItemChange<T> processedChange) {
        LOG.info("Change processed successfully! " + processedChange);
        HubEvent<StorageItemChange<? extends Model>> publishedToCloudEvent =
            HubEvent.create(DataStoreChannelEventName.PUBLISHED_TO_CLOUD, processedChange);
        Amplify.Hub.publish(HubChannel.DATASTORE, publishedToCloudEvent);
    }

    // This should go through the Merger, not directly into storage.
    private <T extends Model> Completable saveModel(T model) {
        return Completable.create(emitter ->
            localStorageAdapter.save(
                model,
                StorageItemChange.Initiator.SYNC_ENGINE,
                record -> emitter.onComplete(),
                emitter::onError
            )
        );
    }

    /**
     * Don't process any more mutations.
     */
    void stopDrainingMutationOutbox() {
        disposable.dispose();
    }

    /**
     * Attempt to publish a change (update, delete, creation) over the network.
     * @param storageItemChange A storage item change to be published to remote API
     * @param <T> Type of model
     * @return A single which completes with the successfully published item, or emits error
     *         if the publication fails
     */
    private <T extends Model> Single<ModelWithMetadata<T>> publishToNetwork(StorageItemChange<T> storageItemChange) {
        switch (storageItemChange.type()) {
            case UPDATE:
                return update(storageItemChange);
            case CREATE:
                return create(storageItemChange);
            case DELETE:
                return delete(storageItemChange);
            default:
                return Single.error(new DataStoreException(
                   "Unknown change type in storage = " + storageItemChange.type(),
                   "This is likely a bug. Please file a ticket with AWS."
                ));
        }
    }

    // For an item in the outbox, dispatch an update mutation
    private <T extends Model> Single<ModelWithMetadata<T>> update(StorageItemChange<T> storageItemChange) {
        return findModelVersion(storageItemChange.item()).flatMap(version -> {
            return publishWithStrategy(storageItemChange, (model, onSuccess, onError) -> {
                appSync.update(model, version, onSuccess, onError);
            });
        });
    }

    // For an item in the outbox, dispatch a create mutation
    private <T extends Model> Single<ModelWithMetadata<T>> create(StorageItemChange<T> storageItemChange) {
        return publishWithStrategy(storageItemChange, appSync::create);
    }

    // For an item in the outbox, dispatch a delete mutation
    private <T extends Model> Single<ModelWithMetadata<T>> delete(StorageItemChange<T> storageItemChange) {
        return findModelVersion(storageItemChange.item()).flatMap(version -> {
            return publishWithStrategy(storageItemChange, (model, onSuccess, onError) -> {
                final Class<T> modelClass = storageItemChange.itemClass();
                final String modelId = storageItemChange.item().getId();
                appSync.delete(modelClass, modelId, version, onSuccess, onError);
            });
        });
    }

    /**
     * Find the current version of a model, that we have in the local store.
     * @param model A model
     * @param <T> Type of model
     * @return Current version known locally
     */
    private <T extends Model> Single<Integer> findModelVersion(T model) {
        // The ModelMetadata for the model uses the same ID as an identifier.
        final QueryPredicate hasMatchingId = QueryField.field("id").eq(model.getId());
        return Single.create(emitter -> {
            localStorageAdapter.query(ModelMetadata.class, hasMatchingId, iterableResults -> {
                // Extract the results into a list.
                final List<ModelMetadata> results = new ArrayList<>();
                while (iterableResults.hasNext()) {
                    results.add(iterableResults.next());
                }
                // There should be only one metadata for the model....
                if (results.size() == 1) {
                    emitter.onSuccess(results.get(0).getVersion());
                } else {
                    emitter.onError(new DataStoreException(
                        "Wanted 1 metadata for item with id = " + model.getId() + ", but had " + results.size() + ".",
                        "This is likely a bug. please report to AWS."
                    ));
                }
            }, emitter::onError);
        });
    }

    /**
     * For an item in storage that has changed, publish the changed item using a publication strategy.
     * @param storageItemChange A change to an item in storage
     * @param publicationStrategy A strategy to publish the changed item
     * @param <T> The model type of the item
     * @return A single which emits the original storage item change, upon success; emits
     *         a failure, if publication does not succeed
     */
    private <T extends Model> Single<ModelWithMetadata<T>> publishWithStrategy(
            StorageItemChange<T> storageItemChange, PublicationStrategy<T> publicationStrategy) {
        return Single.defer(() -> Single.create(subscriber -> {
            publicationStrategy.publish(
                storageItemChange.item(),
                result -> {
                    if (!result.hasErrors() && result.hasData()) {
                        subscriber.onSuccess(result.getData());
                        return;
                    }
                    subscriber.onError(new DataStoreException(
                        "Failed to publish an item to the network. AppSync response contained errors: "
                            + result.getErrors(),
                        "Verify that your endpoint is configured to accept "
                            + storageItemChange.itemClass().getSimpleName() + " models."
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
         * Publish a storage item change, over the network.
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
