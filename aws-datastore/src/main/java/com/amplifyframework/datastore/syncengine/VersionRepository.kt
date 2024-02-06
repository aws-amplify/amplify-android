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

import androidx.annotation.VisibleForTesting
import com.amplifyframework.core.Amplify
import com.amplifyframework.core.category.CategoryType
import com.amplifyframework.core.model.Model
import com.amplifyframework.core.model.ModelSchema
import com.amplifyframework.core.model.query.Where
import com.amplifyframework.core.model.query.predicate.QueryField
import com.amplifyframework.core.model.query.predicate.QueryPredicateGroup
import com.amplifyframework.datastore.DataStoreException
import com.amplifyframework.datastore.appsync.ModelMetadata
import com.amplifyframework.datastore.appsync.ModelWithMetadata
import com.amplifyframework.datastore.extensions.getMetadataSQLitePrimaryKey
import com.amplifyframework.datastore.storage.LocalStorageAdapter
import io.reactivex.rxjava3.core.Single
import io.reactivex.rxjava3.core.SingleEmitter
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine

/**
 * The VersionRepository provides a higher-level facade to lookup the version
 * of the various models in the local storage.
 */
internal class VersionRepository(private val localStorageAdapter: LocalStorageAdapter) {

    @VisibleForTesting
    internal var chunkSize = DEFAULT_CHUNK_SIZE

    /**
     * Find the current version of a model, that we have in the local store.
     * @param model A model
     * @param <T> Type of model
     * @return Current version known locally
     </T> */
    fun <T : Model> findModelVersion(model: T): Single<Int> {
        return Single.create { emitter: SingleEmitter<Int> ->
            // The ModelMetadata for the model uses the same ID as an identifier.
            localStorageAdapter.query(
                ModelMetadata::class.java,
                Where.identifier(
                    ModelMetadata::class.java,
                    model.getMetadataSQLitePrimaryKey()
                ),
                { iterableResults: Iterator<ModelMetadata> ->
                    try {
                        emitter.onSuccess(extractVersion(model, iterableResults))
                    } catch (badVersionFailure: DataStoreException) {
                        emitter.onError(badVersionFailure)
                    }
                },
                { t: DataStoreException ->
                    emitter.onError(t)
                }
            )
        }
    }

    suspend fun <T : Model> fetchModelVersions(modelsWithMetadata: List<ModelWithMetadata<T>>): Map<String, Int> {
        // Create query field once, to be used for building QueryOptions
        val idField = QueryField.field(
            ModelMetadata::class.java.simpleName,
            ModelSchema.fromModelClass(ModelMetadata::class.java).primaryKeyName
        )

        // We chunk sql query to prevent hitting 1k sqlite predicate limit
        // Improvement would be to use IN, but not currently supported in our query builders
        return modelsWithMetadata.chunked(chunkSize).fold(mutableMapOf()) { acc, chunk ->
            val queryOptions = Where.matches(
                QueryPredicateGroup(
                    QueryPredicateGroup.Type.OR,
                    chunk.map {
                        idField.eq("${it.model.modelName}|${it.model.primaryKeyString}")
                    }
                )
            )

            // Fetch chunk list of sqlite results
            val chunkResult = suspendCoroutine { continuation ->
                localStorageAdapter.query(
                    ModelMetadata::class.java,
                    queryOptions,
                    { continuation.resume(it) },
                    { continuation.resumeWithException(it) }
                )
            }

            // Add version of each ModelMetadata item to accumulator map
            chunkResult.forEach {
                val version = if (it.version == null) {
                    LOG.warn(
                        "Metadata for item with id = ${it.primaryKeyString} had null version. " +
                            "This is likely a bug. Please report to AWS."
                    )
                    -1
                } else if (acc.contains(it.primaryKeyString)) {
                    LOG.warn(
                        "Fetched more than 1 metadata for item with id = ${it.primaryKeyString}. " +
                            "This is likely a bug. please report to AWS."
                    )
                    -1
                } else {
                    it.version ?: -1
                }
                acc[it.primaryKeyString] = version
            }
            acc
        }
    }

    /**
     * Extract a model version from an metadata iterator.
     * @param model The model for which metadata is being interrogated, used only for creating error messages.
     * @param metadataIterator An iterator of ModelMetadata; the metadata is associated with the provided model
     * @param <T> The type of model
     * @return The version of the model, if available
     * @throws DataStoreException If there is no version for the model, or if the version cannot be obtained
     </T> */
    @Throws(DataStoreException::class)
    private fun <T : Model> extractVersion(
        model: T,
        metadataIterator: Iterator<ModelMetadata>
    ): Int {
        val results: MutableList<ModelMetadata> =
            ArrayList()
        while (metadataIterator.hasNext()) {
            results.add(metadataIterator.next())
        }

        // There should be only one metadata for the model....
        if (results.size != 1) {
            throw DataStoreException(
                "Wanted 1 metadata for item with id = " + model.primaryKeyString + ", but had " + results.size +
                    ".",
                "This is likely a bug. please report to AWS."
            )
        }
        return results[0].version
            ?: throw DataStoreException(
                "Metadata for item with id = " + model.primaryKeyString + " had null version.",
                "This is likely a bug. Please report to AWS."
            )
    }

    companion object {
        private val LOG = Amplify.Logging.logger(CategoryType.DATASTORE, "amplify:aws-datastore")
        // Keep safely below 1k to prevent SQLite issues
        private const val DEFAULT_CHUNK_SIZE = 950
    }
}
