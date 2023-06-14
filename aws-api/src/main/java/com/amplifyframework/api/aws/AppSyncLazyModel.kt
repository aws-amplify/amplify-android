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
import com.amplifyframework.core.model.LazyModel
import com.amplifyframework.core.model.Model
import com.amplifyframework.kotlin.core.Amplify
import com.amplifyframework.core.Amplify as coreAmplify

class AppSyncLazyModel<M : Model>(
    private val clazz: Class<M>,
    private val keyMap: Map<String, Any>,
    private val predicate: AppSyncLazyQueryPredicate<M>
) : LazyModel<M>() {

    private var value: M? = null

    override fun getValue(): M? {
        return value
    }

    override fun getIdentifier(): Map<String, Any>? {
        return keyMap
    }

    override suspend fun get(): M? {
        value?.let { return value }
        val queryPredicate = predicate.createPredicate(clazz, keyMap)
        try {
            val resultIterator = Amplify.API.query(
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
        } catch (error: ApiException) {
            Log.e("MyAmplifyApp", "Query failure", error)
        }
        return value
    }

    override fun get(onSuccess: Consumer<M>, onFailure: Consumer<AmplifyException>) {
        value?.let { modelValue ->
            onSuccess.accept(modelValue)
            return
        }
        val onQuerySuccess = Consumer<GraphQLResponse<PaginatedResult<M>>> {
            val resultIterator = it.data.items.iterator()
            value = if (resultIterator.hasNext()) {
                resultIterator.next()
            } else {
                null
            }
            // TODO : remove !! and allow null value to be passed in
            onSuccess.accept(value!!)
        }
        val onApiFailure = Consumer<ApiException> { onFailure.accept(it) }
        coreAmplify.API.query(
            AppSyncGraphQLRequestFactory.buildQuery(clazz, predicate.createPredicate(clazz, keyMap)),
            onQuerySuccess,
            onApiFailure
        )
    }
}
