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

import com.amplifyframework.api.ApiCategoryBehavior as Delegate
import com.amplifyframework.api.ApiException
import com.amplifyframework.api.graphql.GraphQLRequest
import com.amplifyframework.api.graphql.GraphQLResponse
import com.amplifyframework.api.rest.RestOptions
import com.amplifyframework.api.rest.RestResponse
import com.amplifyframework.core.Amplify
import com.amplifyframework.core.async.Cancelable
import com.amplifyframework.kotlin.Api
import com.amplifyframework.kotlin.GraphQL.ConnectionState.CONNECTED
import com.amplifyframework.kotlin.GraphQL.ConnectionState.CONNECTING
import com.amplifyframework.kotlin.GraphQL.ConnectionState.DISCONNECTED
import com.amplifyframework.kotlin.GraphQL.Subscription
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
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
    override fun <T> subscribe(request: GraphQLRequest<T>, apiName: String?): Subscription<T> {
        val subscriptionData = MutableSharedFlow<GraphQLResponse<T>>(replay = 1)
        val connectionState = MutableStateFlow(CONNECTING)
        val errors = MutableSharedFlow<ApiException>(replay = 1)

        val operation = if (apiName != null) {
            delegate.subscribe(
                apiName,
                request,
                { connectionState.tryEmit(CONNECTED) },
                { subscriptionData.tryEmit(it) },
                {
                    connectionState.tryEmit(DISCONNECTED)
                    errors.tryEmit(it)
                },
                { connectionState.tryEmit(DISCONNECTED) }
            )
        } else {
            delegate.subscribe(
                request,
                { connectionState.tryEmit(CONNECTED) },
                { subscriptionData.tryEmit(it) },
                {
                    connectionState.tryEmit(DISCONNECTED)
                    errors.tryEmit(it)
                },
                { connectionState.tryEmit(DISCONNECTED) }
            )
        }

        return Subscription(
            subscriptionData.asSharedFlow(),
            connectionState.asStateFlow(),
            errors.asSharedFlow(),
            operation as Cancelable
        )
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
}
