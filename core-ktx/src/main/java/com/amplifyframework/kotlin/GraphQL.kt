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

package com.amplifyframework.kotlin

import com.amplifyframework.api.ApiException
import com.amplifyframework.api.graphql.GraphQLRequest
import com.amplifyframework.api.graphql.GraphQLResponse
import com.amplifyframework.core.async.Cancelable
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flattenMerge
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach

interface GraphQL {
    /**
     * Query a GraphQL API.
     * @param request Query request
     * @param apiName The name of an API as configured in your configuration file;
     *                if not provided, the first GraphQL API in your config is used
     * @return Response
     */
    @Throws(ApiException::class)
    suspend fun <T> query(request: GraphQLRequest<T>, apiName: String? = null): GraphQLResponse<T>

    /**
     * Run a mutation against a GraphQL API.
     * @param request Mutation request
     * @param apiName The name of an API as configured in your configuration file;
     *                if not provided, the first GraphQL API in your config is used
     * @return Response
     */
    @Throws(ApiException::class)
    suspend fun <T> mutate(request: GraphQLRequest<T>, apiName: String? = null): GraphQLResponse<T>

    /**
     * Subscribe to realtime events observed on a GraphQL API.
     * @param request Subscription request
     * @param apiName The name of an API as configured in your configuration file;
     *                if not provided, the first GraphQL API in your config is used
     * @return A subscription object. Inspect its connectionState and subscriptionData.
     */
    @ExperimentalCoroutinesApi
    @FlowPreview
    fun <T> subscribe(request: GraphQLRequest<T>, apiName: String? = null): Subscription<T>

    /**
     * Models an ongoing subscription to a GraphQL API.
     */
    @FlowPreview
    data class Subscription<T>(
        private val subscriptionData: SharedFlow<GraphQLResponse<T>>,
        private val connectionState: StateFlow<ConnectionState>,
        private val errors: SharedFlow<ApiException>,
        private val cancelDelegate: Cancelable
    ) : Cancelable {
        override fun cancel() = cancelDelegate.cancel()

        fun connectionState(): Flow<ConnectionState> {
            return connectionState
        }

        @Suppress("UNCHECKED_CAST")
        fun subscriptionData(): Flow<GraphQLResponse<T>> {
            return flowOf(subscriptionData, errors)
                .flattenMerge()
                .onEach {
                    if (it is ApiException) {
                        throw it
                    }
                }.map { it as GraphQLResponse<T> }
        }
    }

    /**
     * The various connection states modeled by the GraphQL subscription.
     */
    enum class ConnectionState {
        CONNECTING,
        CONNECTED,
        DISCONNECTED
    }
}
