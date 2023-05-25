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

import android.util.Log
import com.amplifyframework.AmplifyException
import com.amplifyframework.api.ApiException
import com.amplifyframework.api.graphql.GraphQLResponse
import com.amplifyframework.api.graphql.PaginatedResult
import com.amplifyframework.core.Consumer
import com.amplifyframework.core.model.LazyList
import com.amplifyframework.core.model.Model
import com.amplifyframework.kotlin.core.Amplify
import com.amplifyframework.core.Amplify as coreAmplify

class AppSyncLazyListModel<M : Model>(
    private val clazz: Class<M>,
    private val keyMap: List<Map<String, Any>>,
    private val predicate: AppSyncLazyQueryPredicate<M>
) : LazyList<M>() {

    private var value: List<M>? = null

    override fun getValue(): List<M>? {
        return value
    }

    override suspend fun get(): List<M>? {
        value?.let { return value }
        try {
            value = Amplify.API.query(
                AppSyncGraphQLRequestFactory.buildQuery<PaginatedResult<M>, M>(
                    clazz,
                    predicate.createListPredicate(clazz, keyMap)
                )
            ).data.items.toList() // TODO : retrieve all pages of items?
        } catch (error: ApiException) {
            Log.e("MyAmplifyApp", "Query failure", error)
        }
        return value
    }

    override fun get(onSuccess: Consumer<List<M>>, onFailure: Consumer<AmplifyException>) {
        value?.let { modelListValue ->
            onSuccess.accept(modelListValue)
            return
        }
        val onQuerySuccess = Consumer<GraphQLResponse<PaginatedResult<M>>> {
            val result = it.data.items.toList() // TODO : retrieve all pages of items?
            value = result
            onSuccess.accept(result)
        }
        val onApiFailure = Consumer<ApiException> { onFailure.accept(it) }
        coreAmplify.API.query(
            AppSyncGraphQLRequestFactory.buildQuery(clazz, predicate.createListPredicate(clazz, keyMap)),
            onQuerySuccess,
            onApiFailure
        )
    }
}
