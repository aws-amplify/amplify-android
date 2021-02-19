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

package com.amplifyframework.kotlin.api

import com.amplifyframework.api.ApiCategoryBehavior as Delegate
import com.amplifyframework.api.ApiException
import com.amplifyframework.api.graphql.GraphQLRequest
import com.amplifyframework.api.graphql.GraphQLResponse
import com.amplifyframework.api.rest.RestOptions
import com.amplifyframework.api.rest.RestResponse
import com.amplifyframework.core.Amplify
import com.amplifyframework.core.async.Cancelable
import com.amplifyframework.core.async.NoOpCancelable
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flattenMerge
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.flow.takeWhile
import kotlinx.coroutines.suspendCancellableCoroutine

class KotlinApiFacade(private val delegate: Delegate = Amplify.API) : Api {
    @Throws(ApiException::class)
    override suspend fun <R> query(request: GraphQLRequest<R>, apiName: String?):
        GraphQLResponse<R> {
            return suspendCancellableCoroutine { continuation ->
                val operation = if (apiName != null) {
                    delegate.query(
                        apiName,
                        request,
                        { continuation.resume(it) },
                        { continuation.resumeWithException(it) }
                    )
                } else {
                    delegate.query(
                        request,
                        { continuation.resume(it) },
                        { continuation.resumeWithException(it) }
                    )
                }
                continuation.invokeOnCancellation { operation?.cancel() }
            }
        }

    @Throws(ApiException::class)
    override suspend fun <T> mutate(request: GraphQLRequest<T>, apiName: String?):
        GraphQLResponse<T> {
            return suspendCancellableCoroutine { continuation ->
                val operation = if (apiName != null) {
                    delegate.mutate(
                        apiName,
                        request,
                        { continuation.resume(it) },
                        { continuation.resumeWithException(it) }
                    )
                } else {
                    delegate.mutate(
                        request,
                        { continuation.resume(it) },
                        { continuation.resumeWithException(it) }
                    )
                }
                continuation.invokeOnCancellation { operation?.cancel() }
            }
        }

    @ExperimentalCoroutinesApi
    @FlowPreview
    override suspend fun <T> subscribe(
        request: GraphQLRequest<T>,
        apiName: String?
    ): Flow<GraphQLResponse<T>> {
        val subscription = Subscription<GraphQLResponse<T>>()
        val operation = if (apiName != null) {
            delegate.subscribe(
                apiName,
                request,
                { subscription.starts.tryEmit(Unit) },
                { subscription.data.tryEmit(it) },
                { subscription.failures.tryEmit(it) },
                { subscription.completions.tryEmit(Unit) }
            )
        } else {
            delegate.subscribe(
                request,
                { subscription.starts.tryEmit(Unit) },
                { subscription.data.tryEmit(it) },
                { subscription.failures.tryEmit(it) },
                { subscription.completions.tryEmit(Unit) }
            )
        }
        subscription.cancelable = operation as Cancelable
        return subscription.awaitStart()
    }

    @Throws(ApiException::class)
    override suspend fun get(request: RestOptions, apiName: String?): RestResponse {
        return suspendCancellableCoroutine { continuation ->
            val operation = if (apiName != null) {
                delegate.get(
                    apiName,
                    request,
                    { continuation.resume(it) },
                    { continuation.resumeWithException(it) }
                )
            } else {
                delegate.get(
                    request,
                    { continuation.resume(it) },
                    { continuation.resumeWithException(it) }
                )
            }
            continuation.invokeOnCancellation { operation?.cancel() }
        }
    }

    @Throws(ApiException::class)
    override suspend fun put(request: RestOptions, apiName: String?): RestResponse {
        return suspendCancellableCoroutine { continuation ->
            val operation = if (apiName != null) {
                delegate.put(
                    apiName,
                    request,
                    { continuation.resume(it) },
                    { continuation.resumeWithException(it) }
                )
            } else {
                delegate.put(
                    request,
                    { continuation.resume(it) },
                    { continuation.resumeWithException(it) }
                )
            }
            continuation.invokeOnCancellation { operation?.cancel() }
        }
    }

    @Throws(ApiException::class)
    override suspend fun post(request: RestOptions, apiName: String?): RestResponse {
        return suspendCancellableCoroutine { continuation ->
            val operation = if (apiName != null) {
                delegate.post(
                    apiName,
                    request,
                    { continuation.resume(it) },
                    { continuation.resumeWithException(it) }
                )
            } else {
                delegate.post(
                    request,
                    { continuation.resume(it) },
                    { continuation.resumeWithException(it) }
                )
            }
            continuation.invokeOnCancellation { operation?.cancel() }
        }
    }

    @Throws(ApiException::class)
    override suspend fun delete(request: RestOptions, apiName: String?): RestResponse {
        return suspendCancellableCoroutine { continuation ->
            val operation = if (apiName != null) {
                delegate.delete(
                    apiName,
                    request,
                    { continuation.resume(it) },
                    { continuation.resumeWithException(it) }
                )
            } else {
                delegate.delete(
                    request,
                    { continuation.resume(it) },
                    { continuation.resumeWithException(it) }
                )
            }
            continuation.invokeOnCancellation { operation?.cancel() }
        }
    }

    @Throws(ApiException::class)
    override suspend fun head(request: RestOptions, apiName: String?): RestResponse {
        return suspendCancellableCoroutine { continuation ->
            val operation = if (apiName != null) {
                delegate.head(
                    apiName,
                    request,
                    { continuation.resume(it) },
                    { continuation.resumeWithException(it) }
                )
            } else {
                delegate.head(
                    request,
                    { continuation.resume(it) },
                    { continuation.resumeWithException(it) }
                )
            }
            continuation.invokeOnCancellation { operation?.cancel() }
        }
    }

    @Throws(ApiException::class)
    override suspend fun patch(request: RestOptions, apiName: String?): RestResponse {
        return suspendCancellableCoroutine { continuation ->
            val operation = if (apiName != null) {
                delegate.patch(
                    apiName,
                    request,
                    { continuation.resume(it) },
                    { continuation.resumeWithException(it) }
                )
            } else {
                delegate.patch(
                    request,
                    { continuation.resume(it) },
                    { continuation.resumeWithException(it) }
                )
            }
            continuation.invokeOnCancellation { operation?.cancel() }
        }
    }

    /**
     * Models an ongoing subscription to a GraphQL API.
     */
    @FlowPreview
    internal class Subscription<T>(
        internal val starts: MutableSharedFlow<Unit> = MutableSharedFlow(replay = 1),
        internal val data: MutableSharedFlow<T> = MutableSharedFlow(replay = 1),
        internal val failures: MutableSharedFlow<ApiException> = MutableSharedFlow(replay = 1),
        internal val completions: MutableSharedFlow<Unit> = MutableSharedFlow(replay = 1),
        internal var cancelable: Cancelable = NoOpCancelable()
    ) {
        @Suppress("UNCHECKED_CAST")
        internal suspend fun awaitStart(): Flow<T> {
            // Wait for a start signal (or a failure to start)
            flowOf(starts, failures)
                .flattenMerge()
                .map {
                    if (it is ApiException) {
                        throw it
                    } else {
                        it as Unit
                    }
                }
                .first()
            return flowOf(data, failures, completions)
                .flattenMerge()
                .takeWhile { it !is Unit }
                .map {
                    if (it is ApiException) {
                        throw it
                    } else {
                        it as T
                    }
                }
                .onCompletion { cancelable.cancel() }
        }
    }
}
