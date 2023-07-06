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
import com.amplifyframework.api.graphql.GraphQLResponse
import com.amplifyframework.api.graphql.PaginatedResult
import com.amplifyframework.core.Consumer
import com.amplifyframework.core.model.LazyModel
import com.amplifyframework.core.model.Model
import com.amplifyframework.kotlin.core.Amplify
import com.amplifyframework.core.Amplify as coreAmplify

@InternalAmplifyApi
class AppSyncLazyModel<M : Model>(
    private val clazz: Class<M>,
    private val keyMap: Map<String, Any>
) : LazyModel<M> {

    private var value: M? = null
    private var loadedValue = false
    private val queryPredicate = AppSyncLazyQueryPredicate<M>().createPredicate(clazz, keyMap)

    override fun getValue(): M? {
        return value
    }

    override fun getIdentifier(): Map<String, Any> {
        return keyMap
    }

    override suspend fun getModel(): M? {
        if (loadedValue) {
            return  value
        }
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
            loadedValue = true
        } catch (error: ApiException) {
            throw AmplifyException("Error lazy loading the model.", error, error.message ?: "")
        }
        return value
    }

    override fun getModel(onSuccess: (M?) -> Unit, onError: Consumer<AmplifyException>) {
        if (loadedValue) {
            onSuccess(value)
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
            onSuccess(value)
        }
        val onApiFailure = Consumer<ApiException> { onError.accept(it) }
        coreAmplify.API.query(
            AppSyncGraphQLRequestFactory.buildQuery(clazz, queryPredicate),
            onQuerySuccess,
            onApiFailure
        )
    }
}
