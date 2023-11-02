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
package com.amplifyframework.datastore.syncengine

import android.database.sqlite.SQLiteConstraintException
import android.util.Log
import com.amplifyframework.core.Amplify
import com.amplifyframework.core.Consumer
import com.amplifyframework.core.NoOpConsumer
import com.amplifyframework.core.category.CategoryType
import com.amplifyframework.core.model.Model
import com.amplifyframework.core.model.query.predicate.QueryPredicates
import com.amplifyframework.datastore.DataStoreChannelEventName
import com.amplifyframework.datastore.DataStoreException
import com.amplifyframework.datastore.appsync.ModelMetadata
import com.amplifyframework.datastore.appsync.ModelWithMetadata
import com.amplifyframework.datastore.storage.LocalStorageAdapter
import com.amplifyframework.datastore.storage.StorageItemChange
import com.amplifyframework.datastore.storage.StorageOperation
import com.amplifyframework.datastore.utils.ErrorInspector
import com.amplifyframework.hub.HubChannel
import com.amplifyframework.hub.HubEvent
import io.reactivex.rxjava3.core.Completable
import io.reactivex.rxjava3.core.CompletableEmitter
import java.lang.Boolean
import java.util.concurrent.atomic.AtomicReference
import kotlin.Int
import kotlin.Long
import kotlin.Throwable
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine
import kotlinx.coroutines.rx3.rxCompletable

/**
 * The merger is responsible for merging cloud data back into the local store.
 * Subscriptions, Mutations, and Syncs, all generate cloud-side data models.
 * All of these sources must be rectified with the current state of the local storage.
 * This is the purpose of the merger.
 */
internal class Merger(
    private val mutationOutbox: MutationOutbox,
    private val versionRepository: VersionRepository,
    private val localStorageAdapter: LocalStorageAdapter
) {

    /**
     * Merge an item back into the local store, using a default strategy.
     * @param modelWithMetadata A model, combined with metadata about it
     * @param <T> Type of model
     * @return A completable operation to merge the model
     </T> */
    fun <T : Model> merge(modelWithMetadata: ModelWithMetadata<T>): Completable {
        return merge(modelWithMetadata, NoOpConsumer.create())
    }

    /**
     * Merge an item back into the local store, using a default strategy.
     * TODO: Change this method to return a Maybe, and remove the Consumer argument.
     * @param modelWithMetadata A model, combined with metadata about it
     * @param changeTypeConsumer A callback invoked when the merge method saves or deletes the model.
     * @param <T> Type of model
     * @return A completable operation to merge the model
     </T> */
    fun <T : Model> merge(
        modelWithMetadata: ModelWithMetadata<T>,
        changeTypeConsumer: Consumer<StorageItemChange.Type>
    ): Completable {
        val startTime = AtomicReference<Long>()
        return Completable.defer {
            val metadata = modelWithMetadata.syncMetadata
            val isDelete = Boolean.TRUE == metadata.isDeleted
            val incomingVersion = if (metadata.version == null) -1 else metadata.version!!
            val model = modelWithMetadata.model
            versionRepository.findModelVersion(model)
                .onErrorReturnItem(-1) // If the incoming version is strictly less than the current version, it's "out of date,"
                // so don't merge it.
                // If the incoming version is exactly equal, it might clobber our local changes. So we
                // *still* won't merge it. Instead, the MutationProcessor would publish the current content,
                // and the version would get bumped up.
                .filter { currentVersion: Int -> currentVersion == -1 || incomingVersion > currentVersion } // If we should merge, then do so now, starting with the model data.
                .flatMapCompletable {
                    val firstStep: Completable = if (mutationOutbox.hasPendingMutation(
                            model.primaryKeyString,
                            model.javaClass.name
                        )
                    ) {
                        LOG.info(
                            "Mutation outbox has pending mutation for Model: " +
                                model.modelName + " with primary key: " + model.resolveIdentifier() +
                                ". Saving the metadata, but not model itself."
                        )
                        Completable.complete()
                    } else {
                        if (isDelete) delete(model, changeTypeConsumer) else save(
                            model,
                            changeTypeConsumer
                        )
                    }
                    firstStep.andThen(save(metadata, NoOpConsumer.create()))
                } // Let the world know that we've done a good thing.
                .doOnComplete {
                    announceSuccessfulMerge(modelWithMetadata)
                    LOG.debug("Remote model update was sync'd down into local storage: $modelWithMetadata")
                } // Remote store may not always respect the foreign key constraint, so
                // swallow any error caused by foreign key constraint violation.
                .onErrorComplete { failure: Throwable? ->
                    if (!ErrorInspector.contains(failure, SQLiteConstraintException::class.java)) {
                        return@onErrorComplete false
                    }
                    LOG.warn(
                        "Sync failed: foreign key constraint violation: $modelWithMetadata",
                        failure
                    )
                    true
                }
                .doOnError { failure: Throwable? ->
                    LOG.warn(
                        "Failed to sync remote model into local storage: $modelWithMetadata",
                        failure
                    )
                }
        }
            .doOnSubscribe { startTime.set(System.currentTimeMillis()) }
            .doOnTerminate {
                val duration = System.currentTimeMillis() - startTime.get()
                LOG.verbose("Merged a single item in $duration ms.")
            }
    }

    fun <T : Model> merge(
        modelsWithMetadata: List<ModelWithMetadata<T>>,
        changeTypeConsumer: Consumer<StorageItemChange.Type>
    ): Completable {
        return rxCompletable {
            // if no models to merge, return early
            if (modelsWithMetadata.isEmpty()) {
                return@rxCompletable
            }

            // create (key, model metadata) map for quick lookup to re-associate
            val modelMetadataMap = modelsWithMetadata.associateBy({ it.syncMetadata.primaryKeyString }, { it })

            // Consumer to announce Model merges
            val mergedConsumer = Consumer<ModelWithMetadata<T>> {
                announceSuccessfulMerge(it)
                LOG.debug("Remote model update was sync'd down into local storage: $it")
            }
            // Reusable consumer to pass into each operation, which will broadcast change type to mergedConsumer
            val modelChangeConsumer: Consumer<StorageItemChange<T>> = Consumer { changeTypeConsumer.accept(it.type()) }

            // After we receive a report of a successful metadata item process, we notify the merge consumer
            val metadataChangeConsumer: Consumer<StorageItemChange<ModelMetadata>> = Consumer { change ->
                modelMetadataMap[change.item().primaryKeyString]?.let {
                    mergedConsumer.accept(it)
                }
            }

            // fetch a Map of all model versions from Metadata table
            val localModelVersions = versionRepository.fetchModelVersions(modelsWithMetadata)

            // fetch a Set of all pending mutation ids for type T
            val pendingMutations = mutationOutbox.fetchPendingMutations(
                modelsWithMetadata.map { it.model },
                modelsWithMetadata.first().model.javaClass.name
            )

            // Construct a batch of operations to be executed in a single transactions
            val operations = modelsWithMetadata.mapNotNull {
                val incomingVersion = it.syncMetadata.version ?: -1
                val localVersion = localModelVersions.getOrDefault(
                    "${it.model.modelName}|${it.model.primaryKeyString}",
                    -1
                )
                // if local version unknown or lower than remote (incoming) version we will create operation(s)
                if (localVersion == -1 || (incomingVersion > localVersion)) {
                    val itemOperations = mutableListOf<StorageOperation<Model>>()
                    if (!pendingMutations.contains(it.model.primaryKeyString)) {
                        // If there are no pending mutations for the item,
                        // we will create/delete depending remote model syncMetadata deleted state
                        val modelOperation = if (it.syncMetadata.isDeleted == true) {
                            StorageOperation.Delete(it.model, modelChangeConsumer)
                        } else {
                            StorageOperation.Create(it.model, modelChangeConsumer)
                        }
                        itemOperations.add(modelOperation)
                    } else {
                        // If a pending mutation existed for model, we only update the sync metadata
                        LOG.info(
                            "Mutation outbox has pending mutation for Model: " +
                                it.model.modelName + " with primary key: " + it.model.resolveIdentifier() +
                                ". Saving the metadata, but not model itself."
                        )
                    }
                    // Save metadata
                    itemOperations.apply {
                        add(StorageOperation.Create(it.syncMetadata, metadataChangeConsumer))
                    }
                } else {
                    null
                }
            }.flatten() // flatten the list of a list of operations

            // Execute all batched operations in single transaction
            try {
                suspendCoroutine { continuation ->
                    localStorageAdapter.batchSyncOperations(
                        operations,
                        { continuation.resume(Unit) },
                        { continuation.resumeWithException(it) }
                    )
                }
            } catch (e: DataStoreException) {
                // Batch sync operation threw an unrecoverable exception.
                // Throw to announce sync failure
                throw e
            }
        }
    }

    /**
     * Announce a successful merge over Hub.
     * @param modelWithMetadata Model with metadata that was successfully merged
     * @param <T> Type of model
     </T> */
    private fun <T : Model> announceSuccessfulMerge(modelWithMetadata: ModelWithMetadata<T>) {
        Amplify.Hub.publish(
            HubChannel.DATASTORE,
            HubEvent.create(
                DataStoreChannelEventName.SUBSCRIPTION_DATA_PROCESSED,
                modelWithMetadata
            )
        )
    }

    // Delete a model.
    private fun <T : Model> delete(
        model: T,
        changeTypeConsumer: Consumer<StorageItemChange.Type>
    ): Completable {
        return Completable.create { emitter: CompletableEmitter ->
            localStorageAdapter.delete(
                model,
                StorageItemChange.Initiator.SYNC_ENGINE,
                QueryPredicates.all(),
                { storageItemChange: StorageItemChange<T> ->
                    changeTypeConsumer.accept(storageItemChange.type())
                    emitter.onComplete()
                }
            ) { failure: DataStoreException? ->
                LOG.verbose(
                    "Failed to delete a model while merging. Perhaps it was already gone? " +
                        Log.getStackTraceString(failure)
                )
                changeTypeConsumer.accept(StorageItemChange.Type.DELETE)
                emitter.onComplete()
            }
        }
    }

    // Create or update a model.
    private fun <T : Model> save(
        model: T,
        changeTypeConsumer: Consumer<StorageItemChange.Type>
    ): Completable {
        return Completable.create { emitter: CompletableEmitter ->
            localStorageAdapter.save(
                model,
                StorageItemChange.Initiator.SYNC_ENGINE,
                QueryPredicates.all(),
                { storageItemChange: StorageItemChange<T> ->
                    changeTypeConsumer.accept(storageItemChange.type())
                    emitter.onComplete()
                }
            ) { t: DataStoreException -> emitter.onError(t) }
        }
    }

    companion object {
        private val LOG = Amplify.Logging.logger(CategoryType.DATASTORE, "amplify:aws-datastore")
    }
}
