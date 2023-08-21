/*
 * Copyright 2023 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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

package com.amplifyframework.api.aws

import com.amplifyframework.AmplifyException
import com.amplifyframework.annotations.InternalAmplifyApi
import com.amplifyframework.api.ApiException
import com.amplifyframework.api.graphql.GraphQLRequest
import com.amplifyframework.api.graphql.GraphQLResponse
import com.amplifyframework.api.graphql.PaginatedResult
import com.amplifyframework.core.Amplify
import com.amplifyframework.core.Consumer
import com.amplifyframework.core.NullableConsumer
import com.amplifyframework.core.model.LazyModel
import com.amplifyframework.core.model.Model
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine

@InternalAmplifyApi
class ApiLazyModel<M : Model> private constructor(
    private val clazz: Class<M>,
    private val keyMap: Map<String, Any>,
    private var loadedValue: Boolean = false,
    private var value: M? = null
) : LazyModel<M> {

    private val queryPredicate = AppSyncLazyQueryPredicate<M>().createPredicate(clazz, keyMap)

    override fun getValue(): M? {
        return value
    }

    override fun getIdentifier(): Map<String, Any> {
        return keyMap
    }

    override suspend fun getModel(): M? {
        if (loadedValue) {
            return value
        }

        try {
            val resultIterator = query(
                AppSyncGraphQLRequestFactory.buildQuery<PaginatedResult<M>, M>(
                    clazz,
                    queryPredicate
                )
            ).data.items.iterator()
            value = if (resultIterator.hasNext()) {
                resultIterator.next()
            } else {
                null
            }
            loadedValue = true
        } catch (error: ApiException) {
            throw AmplifyException("Error lazy loading the model.", error, error.message ?: "")
        }
        return value
    }

    override fun getModel(onSuccess: NullableConsumer<M?>, onError: Consumer<AmplifyException>) {
        if (loadedValue) {
            onSuccess.accept(value)
            return
        }
        val onQuerySuccess = Consumer<GraphQLResponse<PaginatedResult<M>>> {
            val resultIterator = it.data.items.iterator()
            value = if (resultIterator.hasNext()) {
                resultIterator.next()
            } else {
                null
            }
            loadedValue = true
            onSuccess.accept(value)
        }
        val onApiFailure = Consumer<ApiException> { onError.accept(it) }
        Amplify.API.query(
            AppSyncGraphQLRequestFactory.buildQuery(clazz, queryPredicate),
            onQuerySuccess,
            onApiFailure
        )
    }

    companion object {
        @JvmStatic
        fun <M : Model> createPreloaded(
            clazz: Class<M>,
            keyMap: Map<String, Any>,
            value: M?

        ): ApiLazyModel<M> {
            return ApiLazyModel(clazz, keyMap, true, value)
        }

        @JvmStatic
        fun <M : Model> createLazy(
            clazz: Class<M>,
            keyMap: Map<String, Any>

        ): ApiLazyModel<M> {
            return ApiLazyModel(clazz, keyMap)
        }
    }
}

internal class LazyListHelper {

    companion object {
        @JvmStatic
        fun <M : Model> createPreloaded(
            value: List<M>

        ): PaginatedResult<M> {
            return PaginatedResult(value, null)
        }

        @JvmStatic
        fun <M : Model> createLazy(
            clazz: Class<M>,
            keyMap: Map<String, Any>

        ): PaginatedResult<M> {
            val request: GraphQLRequest<PaginatedResult<M>> = AppSyncGraphQLRequestFactory.buildQuery(
                clazz,
                AppSyncLazyQueryPredicate<M>().createPredicate(clazz, keyMap)
            )
            return PaginatedResult(emptyList(), request)
        }
    }
}

/*
 Duplicating the query Kotlin Facade method so we aren't pulling in Kotlin Core
 */
@Throws(ApiException::class)
private suspend fun <R> query(request: GraphQLRequest<R>): GraphQLResponse<R> {
    return suspendCoroutine { continuation ->
        Amplify.API.query(
            request,
            { continuation.resume(it) },
            { continuation.resumeWithException(it) }
        )
    }
}