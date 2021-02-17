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

package com.amplifyframework.kotlin.facades

import com.amplifyframework.core.Amplify
import com.amplifyframework.core.async.Cancelable
import com.amplifyframework.core.model.Model
import com.amplifyframework.core.model.query.QueryOptions
import com.amplifyframework.core.model.query.predicate.QueryPredicate
import com.amplifyframework.datastore.DataStoreCategoryBehavior as Delegate
import com.amplifyframework.datastore.DataStoreException
import com.amplifyframework.datastore.DataStoreItemChange
import com.amplifyframework.kotlin.DataStore
import java.util.concurrent.atomic.AtomicReference
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.channels.sendBlocking
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

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

    @ExperimentalCoroutinesApi
    @Throws(DataStoreException::class)
    override fun <T : Model> query(itemClass: Class<T>, options: QueryOptions): Flow<T> {
        return callbackFlow {
            delegate.query(
                itemClass,
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

    @ExperimentalCoroutinesApi
    @Throws(DataStoreException::class)
    override fun observe(): Flow<DataStoreItemChange<out Model>> {
        return callbackFlow {
            val cancelable = AtomicReference<Cancelable?>()
            delegate.observe(
                { cancelable.set(it) },
                { change -> sendBlocking(change) },
                { failure -> close(failure) },
                { close() }
            )
            awaitClose { cancelable.get()?.cancel() }
        }
    }

    @ExperimentalCoroutinesApi
    @Throws(DataStoreException::class)
    override fun <T : Model> observe(itemClass: Class<T>, uniqueId: String):
        Flow<DataStoreItemChange<T>> {
            return callbackFlow {
                val cancelable = AtomicReference<Cancelable?>()
                delegate.observe(
                    itemClass,
                    uniqueId,
                    { cancelable.set(it) },
                    { change -> sendBlocking(change) },
                    { failure -> close(failure) },
                    { close() }
                )
                awaitClose { cancelable.get()?.cancel() }
            }
        }

    @ExperimentalCoroutinesApi
    @Throws(DataStoreException::class)
    override fun <T : Model> observe(itemClass: Class<T>, selectionCriteria: QueryPredicate):
        Flow<DataStoreItemChange<T>> {
            return callbackFlow {
                val cancelable = AtomicReference<Cancelable?>()
                delegate.observe(
                    itemClass,
                    selectionCriteria,
                    { cancelable.set(it) },
                    { change -> sendBlocking(change) },
                    { failure -> close(failure) },
                    { close() }
                )
                awaitClose { cancelable.get()?.cancel() }
            }
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
}
