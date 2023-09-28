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
import com.amplifyframework.api.ApiCategory
import com.amplifyframework.api.ApiException
import com.amplifyframework.api.graphql.GraphQLRequest
import com.amplifyframework.api.graphql.GraphQLResponse
import com.amplifyframework.core.Amplify
import com.amplifyframework.core.Consumer
import com.amplifyframework.core.model.LazyModelList
import com.amplifyframework.core.model.LoadedModelList
import com.amplifyframework.core.model.Model
import com.amplifyframework.core.model.ModelPage
import com.amplifyframework.core.model.PaginationToken
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

internal class ApiLoadedModelList<out M : Model>(
    override val items: List<M>
) : LoadedModelList<M>

internal class ApiModelPage<out M : Model>(
    override val items: List<M>,
    override val nextToken: ApiPaginationToken?
) : ModelPage<M>

internal class ApiPaginationToken(val nextToken: String) : PaginationToken

internal class ApiLazyModelList<out M : Model> constructor(
    private val clazz: Class<M>,
    keyMap: Map<String, Any>,
    private val apiName: String?,
    private val apiCategory: ApiCategory = Amplify.API
) : LazyModelList<M> {

    private val callbackScope = CoroutineScope(Dispatchers.IO)
    private val queryPredicate = AppSyncLazyQueryPredicate<M>().createPredicate(clazz, keyMap)

    override suspend fun fetchPage(paginationToken: PaginationToken?): ModelPage<M> {
        try {
            val response = query(apiCategory, apiName, createRequest(paginationToken))
            return response.data
        } catch (error: AmplifyException) {
            throw createLazyException(error)
        }
    }

    override fun fetchPage(onSuccess: Consumer<ModelPage<@UnsafeVariance M>>, onError: Consumer<AmplifyException>) {
        callbackScope.launch {
            try {
                val page = fetchPage()
                onSuccess.accept(page)
            } catch (e: AmplifyException) {
                onError.accept(e)
            }
        }
    }

    override fun fetchPage(
        paginationToken: PaginationToken?,
        onSuccess: Consumer<ModelPage<@UnsafeVariance M>>,
        onError: Consumer<AmplifyException>
    ) {
        callbackScope.launch {
            try {
                val page = fetchPage(paginationToken)
                onSuccess.accept(page)
            } catch (e: AmplifyException) {
                onError.accept(e)
            }
        }
    }

    private fun createRequest(paginationToken: PaginationToken? = null): GraphQLRequest<ModelPage<M>> {
        return AppSyncGraphQLRequestFactory.buildModelPageQuery(
            clazz,
            queryPredicate,
            (paginationToken as? ApiPaginationToken)?.nextToken
        )
    }

    @Throws(ApiException::class)
    private suspend fun <R> query(apiCategory: ApiCategory, apiName: String?, request: GraphQLRequest<R>):
        GraphQLResponse<R> {
        return suspendCoroutine { continuation ->
            if (apiName != null) {
                apiCategory.query(
                    apiName,
                    request,
                    { continuation.resume(it) },
                    { continuation.resumeWithException(it) }
                )
            } else {
                apiCategory.query(
                    request,
                    { continuation.resume(it) },
                    { continuation.resumeWithException(it) }
                )
            }
        }
    }

    private fun createLazyException(exception: AmplifyException) =
        AmplifyException("Error lazy loading the model list.", exception, exception.message ?: "")
}
