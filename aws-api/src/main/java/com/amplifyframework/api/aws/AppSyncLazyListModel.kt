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
import com.amplifyframework.core.Amplify as coreAmplify
import com.amplifyframework.core.Consumer
import com.amplifyframework.core.model.LazyList
import com.amplifyframework.core.model.Model
import com.amplifyframework.kotlin.core.Amplify

@InternalAmplifyApi
class AppSyncLazyListModel<M : Model>(
    private val clazz: Class<M>,
    keyMap: Map<String, Any>,
) : LazyList<M> {

    private var value: MutableList<M> = mutableListOf()
    private var paginatedResult: PaginatedResult<M>? = null
    private val queryPredicate = AppSyncLazyQueryPredicate<M>().createPredicate(clazz, keyMap)

    override fun getItems(): List<M> {
        return value
    }

    override suspend fun getNextPage(): List<M> {
        if (!hasNextPage()) {
            return emptyList()
        }
        val request = createGraphQLRequest()
        paginatedResult = Amplify.API.query(request).data
        val nextPageOfItems = paginatedResult!!.items.toList()
        value.addAll(nextPageOfItems)
        return nextPageOfItems
    }

    override fun getNextPage(onSuccess: Consumer<List<M>>, onError: Consumer<AmplifyException>) {
        if (!hasNextPage()) {
            onSuccess.accept(emptyList())
            return
        }
        val onQuerySuccess = Consumer<GraphQLResponse<PaginatedResult<M>>> {
            paginatedResult = it.data
            val nextPageOfItems = paginatedResult!!.items.toList()
            value.addAll(nextPageOfItems)
            onSuccess.accept(nextPageOfItems)
        }
        val onApiFailure = Consumer<ApiException> { onError.accept(it) }
        val request = createGraphQLRequest()
        coreAmplify.API.query(request, onQuerySuccess, onApiFailure)
    }

    override fun hasNextPage(): Boolean {
        return paginatedResult?.hasNextResult() ?: true
    }

    private fun createGraphQLRequest(): GraphQLRequest<PaginatedResult<M>> {
        return if (paginatedResult != null) {
            paginatedResult!!.requestForNextResult
        } else {
            AppSyncGraphQLRequestFactory.buildQuery(
                clazz,
                queryPredicate
            )
        }
    }
}
