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
    private var value: M? = null,
    private val apiName: String? = null
) : LazyModel<M> {

    private val queryPredicate = AppSyncLazyQueryPredicate<M>().createPredicate(clazz, keyMap)

    override fun getValue(): M? {
        return value
    }

    override fun getIdentifier(): Map<String, Any> {
        return keyMap
    }

    override suspend fun fetchModel(): M? {
        if (loadedValue) {
            return value
        }

        try {
            val resultIterator = query(
                AppSyncGraphQLRequestFactory.buildQuery<PaginatedResult<M>, M>(
                    clazz,
                    queryPredicate
                ),
                apiName
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

    override fun fetchModel(onSuccess: NullableConsumer<M?>, onError: Consumer<AmplifyException>) {
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
        if (apiName != null) {
            Amplify.API.query(
                apiName,
                AppSyncGraphQLRequestFactory.buildQuery(clazz, queryPredicate),
                onQuerySuccess,
                onApiFailure
            )
        } else {
            Amplify.API.query(
                AppSyncGraphQLRequestFactory.buildQuery(clazz, queryPredicate),
                onQuerySuccess,
                onApiFailure
            )
        }
    }

    override fun isLoaded() = loadedValue

    internal companion object {
        fun <M : Model> createPreloaded(
            clazz: Class<M>,
            keyMap: Map<String, Any>,
            value: M?
        ): ApiLazyModel<M> {
            return ApiLazyModel(clazz, keyMap, true, value)
        }

        fun <M : Model> createLazy(
            clazz: Class<M>,
            keyMap: Map<String, Any>,
            apiName: String?
        ): ApiLazyModel<M> {
            return ApiLazyModel(clazz, keyMap, apiName = apiName)
        }
    }
}

internal class LazyListHelper {

    companion object {
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
private suspend fun <R> query(request: GraphQLRequest<R>, apiName: String?):
    GraphQLResponse<R> {
    return suspendCoroutine { continuation ->
        if (apiName != null) {
            Amplify.API.query(
                apiName,
                request,
                { continuation.resume(it) },
                { continuation.resumeWithException(it) }
            )
        } else {
            Amplify.API.query(
                request,
                { continuation.resume(it) },
                { continuation.resumeWithException(it) }
            )
        }
    }
}
