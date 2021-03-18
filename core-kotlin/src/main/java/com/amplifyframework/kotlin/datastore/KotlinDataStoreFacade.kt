/*
 * Copyright 2021 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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

package com.amplifyframework.kotlin.datastore

import com.amplifyframework.core.Amplify
import com.amplifyframework.core.async.Cancelable
import com.amplifyframework.core.model.Model
import com.amplifyframework.core.model.query.QueryOptions
import com.amplifyframework.core.model.query.predicate.QueryPredicate
import com.amplifyframework.datastore.DataStoreCategoryBehavior as Delegate
import com.amplifyframework.datastore.DataStoreException
import com.amplifyframework.datastore.DataStoreItemChange
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine
import kotlin.reflect.KClass
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.channels.sendBlocking
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flattenMerge
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.flow.takeWhile

class KotlinDataStoreFacade(private val delegate: Delegate = Amplify.DataStore) : DataStore {
    @Throws(DataStoreException::class)
    override suspend fun <T : Model> save(item: T, predicate: QueryPredicate) {
        return suspendCoroutine { continuation ->
            delegate.save(
                item,
                predicate,
                { continuation.resume(Unit) },
                { continuation.resumeWithException(it) }
            )
        }
    }

    @Throws(DataStoreException::class)
    override suspend fun <T : Model> delete(item: T, predicate: QueryPredicate) {
        return suspendCoroutine { continuation ->
            delegate.delete(
                item,
                predicate,
                { continuation.resume(Unit) },
                { continuation.resumeWithException(it) }
            )
        }
    }

    @Suppress("PARAMETER_NAME_CHANGED_ON_OVERRIDE")
    @Throws(DataStoreException::class)
    override suspend fun <T : Model> delete(itemClass: KClass<T>, filter: QueryPredicate) {
        return suspendCoroutine { continuation ->
            delegate.delete(
                itemClass.java,
                filter,
                { continuation.resume(Unit) },
                { continuation.resumeWithException(it) }
            )
        }
    }

    @ExperimentalCoroutinesApi
    @Throws(DataStoreException::class)
    override fun <T : Model> query(itemClass: KClass<T>, options: QueryOptions): Flow<T> {
        return callbackFlow {
            delegate.query(
                itemClass.java,
                options,
                {
                    while (it.hasNext()) {
                        sendBlocking(it.next())
                    }
                    close()
                },
                { close(it) }
            )
            awaitClose {}
        }
    }

    @FlowPreview
    @ExperimentalCoroutinesApi
    @Throws(DataStoreException::class)
    override suspend fun observe(): Flow<DataStoreItemChange<out Model>> {
        val observation = Observation<DataStoreItemChange<out Model>>()
        delegate.observe(
            { observation.starts.tryEmit(it) },
            { observation.changes.tryEmit(it) },
            { observation.failures.tryEmit(it) },
            { observation.completions.tryEmit(Unit) }
        )
        return observation.waitForStart()
    }

    @FlowPreview
    @ExperimentalCoroutinesApi
    @Throws(DataStoreException::class)
    override suspend fun <T : Model> observe(itemClass: KClass<T>, itemId: String):
        Flow<DataStoreItemChange<T>> {
            val observation = Observation<DataStoreItemChange<T>>()
            delegate.observe(
                itemClass.java,
                itemId,
                { observation.starts.tryEmit(it) },
                { observation.changes.tryEmit(it) },
                { observation.failures.tryEmit(it) },
                { observation.completions.tryEmit(Unit) }
            )
            return observation.waitForStart()
        }

    @FlowPreview
    @ExperimentalCoroutinesApi
    @Throws(DataStoreException::class)
    override suspend fun <T : Model> observe(
        itemClass: KClass<T>,
        selectionCriteria: QueryPredicate
    ): Flow<DataStoreItemChange<T>> {
        val observation = Observation<DataStoreItemChange<T>>()
        delegate.observe(
            itemClass.java,
            selectionCriteria,
            { observation.starts.tryEmit(it) },
            { observation.changes.tryEmit(it) },
            { observation.failures.tryEmit(it) },
            { observation.completions.tryEmit(Unit) }
        )
        return observation.waitForStart()
    }

    @Throws(DataStoreException::class)
    override suspend fun start() {
        return suspendCoroutine { continuation ->
            delegate.start(
                { continuation.resume(Unit) },
                { continuation.resumeWithException(it) }
            )
        }
    }

    @Throws(DataStoreException::class)
    override suspend fun stop() {
        return suspendCoroutine { continuation ->
            delegate.stop(
                { continuation.resume(Unit) },
                { continuation.resumeWithException(it) }
            )
        }
    }

    @Throws(DataStoreException::class)
    override suspend fun clear() {
        return suspendCoroutine { continuation ->
            delegate.clear(
                { continuation.resume(Unit) },
                { continuation.resumeWithException(it) }
            )
        }
    }

    internal class Observation<T>(
        internal val starts: MutableSharedFlow<Cancelable> = MutableSharedFlow(1),
        internal val changes: MutableSharedFlow<T> = MutableSharedFlow(1),
        internal val failures: MutableSharedFlow<DataStoreException> = MutableSharedFlow(1),
        internal val completions: MutableSharedFlow<Unit> = MutableSharedFlow(1)
    ) {
        @Suppress("UNCHECKED_CAST")
        @FlowPreview
        internal suspend fun waitForStart(): Flow<T> {
            // Observation either begins with signal from onError or onStart (with Cancelable token).
            val cancelable = flowOf(starts, failures)
                .flattenMerge()
                .map {
                    if (it is DataStoreException) {
                        throw it
                    } else {
                        it as Cancelable
                    }
                }
                .first()
            return flowOf(changes, failures, completions)
                .flattenMerge()
                .takeWhile { it !is Unit }
                .map {
                    if (it is DataStoreException) {
                        throw it
                    } else {
                        it as T
                    }
                }
                .onCompletion { cancelable.cancel() }
        }
    }
}
