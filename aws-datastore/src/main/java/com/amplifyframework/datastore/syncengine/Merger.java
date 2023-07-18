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

package com.amplifyframework.datastore.syncengine;

import android.database.sqlite.SQLiteConstraintException;
import androidx.annotation.NonNull;

import com.amplifyframework.core.Amplify;
import com.amplifyframework.core.Consumer;
import com.amplifyframework.core.NoOpConsumer;
import com.amplifyframework.core.category.CategoryType;
import com.amplifyframework.core.model.Model;
import com.amplifyframework.core.model.query.predicate.QueryPredicates;
import com.amplifyframework.datastore.DataStoreChannelEventName;
import com.amplifyframework.datastore.appsync.ModelMetadata;
import com.amplifyframework.datastore.appsync.ModelWithMetadata;
import com.amplifyframework.datastore.storage.LocalStorageAdapter;
import com.amplifyframework.datastore.storage.StorageItemChange;
import com.amplifyframework.datastore.utils.ErrorInspector;
import com.amplifyframework.hub.HubChannel;
import com.amplifyframework.hub.HubEvent;
import com.amplifyframework.logging.Logger;

import java.util.Objects;
import java.util.concurrent.atomic.AtomicReference;

import io.reactivex.rxjava3.core.Completable;

/**
 * The merger is responsible for merging cloud data back into the local store.
 * Subscriptions, Mutations, and Syncs, all generate cloud-side data models.
 * All of these sources must be rectified with the current state of the local storage.
 * This is the purpose of the merger.
 */
final class Merger {
    private static final Logger LOG = Amplify.Logging.logger(CategoryType.DATASTORE, "amplify:aws-datastore");
    private final MutationOutbox mutationOutbox;
    private final VersionRepository versionRepository;
    private final LocalStorageAdapter localStorageAdapter;

    /**
     * Constructs a Merger.
     * @param localStorageAdapter A local storage adapter
     */
    Merger(
            @NonNull MutationOutbox mutationOutbox,
            @NonNull VersionRepository versionRepository,
            @NonNull LocalStorageAdapter localStorageAdapter) {
        this.mutationOutbox = Objects.requireNonNull(mutationOutbox);
        this.versionRepository = Objects.requireNonNull(versionRepository);
        this.localStorageAdapter = Objects.requireNonNull(localStorageAdapter);
    }

    /**
     * Merge an item back into the local store, using a default strategy.
     * @param modelWithMetadata A model, combined with metadata about it
     * @param <T> Type of model
     * @return A completable operation to merge the model
     */
    <T extends Model> Completable merge(ModelWithMetadata<T> modelWithMetadata) {
        return merge(modelWithMetadata, NoOpConsumer.create());
    }

    /**
     * Merge an item back into the local store, using a default strategy.
     * TODO: Change this method to return a Maybe, and remove the Consumer argument.
     * @param modelWithMetadata A model, combined with metadata about it
     * @param changeTypeConsumer A callback invoked when the merge method saves or deletes the model.
     * @param <T> Type of model
     * @return A completable operation to merge the model
     */
    <T extends Model> Completable merge(
            ModelWithMetadata<T> modelWithMetadata, Consumer<StorageItemChange.Type> changeTypeConsumer) {
        AtomicReference<Long> startTime = new AtomicReference<>();
        return Completable.defer(() -> {
            ModelMetadata metadata = modelWithMetadata.getSyncMetadata();
            boolean isDelete = Boolean.TRUE.equals(metadata.isDeleted());
            int incomingVersion = metadata.getVersion() == null ? -1 : metadata.getVersion();
            T model = modelWithMetadata.getModel();

            return versionRepository.findModelVersion(model)
                .onErrorReturnItem(-1)
                // If the incoming version is strictly less than the current version, it's "out of date,"
                // so don't merge it.
                // If the incoming version is exactly equal, it might clobber our local changes. So we
                // *still* won't merge it. Instead, the MutationProcessor would publish the current content,
                // and the version would get bumped up.
                .filter(currentVersion -> currentVersion == -1 || incomingVersion > currentVersion)
                // If we should merge, then do so now, starting with the model data.
                .flatMapCompletable(shouldMerge -> {
                    Completable firstStep;
                    if (mutationOutbox.hasPendingMutation(model.getPrimaryKeyString())) {
                        LOG.info("Mutation outbox has pending mutation for " + model.resolveIdentifier()
                            + ". Saving the metadata, but not model itself.");
                        firstStep = Completable.complete();
                    } else {
                        firstStep = (isDelete ? delete(model, changeTypeConsumer) : save(model, changeTypeConsumer));
                    }
                    return firstStep.andThen(save(metadata, NoOpConsumer.create()));
                })
                // Let the world know that we've done a good thing.
                .doOnComplete(() -> {
                    announceSuccessfulMerge(modelWithMetadata);
                    LOG.debug("Remote model update was sync'd down into local storage: " + modelWithMetadata);
                })
                // Remote store may not always respect the foreign key constraint, so
                // swallow any error caused by foreign key constraint violation.
                .onErrorComplete(failure -> {
                    if (!ErrorInspector.contains(failure, SQLiteConstraintException.class)) {
                        return false;
                    }
                    LOG.warn("Sync failed: foreign key constraint violation: " + modelWithMetadata, failure);
                    return true;
                })
                .doOnError(failure ->
                    LOG.warn("Failed to sync remote model into local storage: " + modelWithMetadata, failure)
                );
        })
        .doOnSubscribe(disposable -> startTime.set(System.currentTimeMillis()))
        .doOnTerminate(() -> {
            long duration = System.currentTimeMillis() - startTime.get();
            LOG.verbose("Merged a single item in " + duration + " ms.");
        });
    }

    /**
     * Announce a successful merge over Hub.
     * @param modelWithMetadata Model with metadata that was successfully merged
     * @param <T> Type of model
     */
    private <T extends Model> void announceSuccessfulMerge(ModelWithMetadata<T> modelWithMetadata) {
        Amplify.Hub.publish(HubChannel.DATASTORE,
            HubEvent.create(DataStoreChannelEventName.SUBSCRIPTION_DATA_PROCESSED, modelWithMetadata)
        );
    }

    // Delete a model.
    private <T extends Model> Completable delete(T model, Consumer<StorageItemChange.Type> changeTypeConsumer) {
        return Completable.create(emitter ->
            localStorageAdapter.delete(model, StorageItemChange.Initiator.SYNC_ENGINE, QueryPredicates.all(),
                storageItemChange -> {
                    changeTypeConsumer.accept(storageItemChange.type());
                    emitter.onComplete();
                },
                failure -> {
                    LOG.verbose(
                        "Failed to delete a model while merging. Perhaps it was already gone? "
                        + android.util.Log.getStackTraceString(failure)
                    );
                    changeTypeConsumer.accept(StorageItemChange.Type.DELETE);
                    emitter.onComplete();
                }
            )
        );
    }

    // Create or update a model.
    private <T extends Model> Completable save(T model, Consumer<StorageItemChange.Type> changeTypeConsumer) {
        return Completable.create(emitter ->
            localStorageAdapter.save(model, StorageItemChange.Initiator.SYNC_ENGINE, QueryPredicates.all(),
                storageItemChange -> {
                    changeTypeConsumer.accept(storageItemChange.type());
                    emitter.onComplete();
                },
                emitter::onError
            )
        );
    }
}
