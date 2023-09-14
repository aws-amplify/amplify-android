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
import com.amplifyframework.api.ApiException
import com.amplifyframework.api.graphql.GraphQLRequest
import com.amplifyframework.api.graphql.GraphQLResponse
import com.amplifyframework.api.graphql.PaginatedResult
import com.amplifyframework.core.Amplify
import com.amplifyframework.core.Consumer
import com.amplifyframework.core.NullableConsumer
import com.amplifyframework.core.model.LazyModelReference
import com.amplifyframework.core.model.Model
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import java.util.concurrent.atomic.AtomicReference
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine

internal class ApiLazyModelReference<M : Model> internal constructor(
    private val clazz: Class<M>,
    private val keyMap: Map<String, Any>,
    private val apiName: String? = null
) : LazyModelReference<M> {
    private val cachedValue = AtomicReference<LoadedValue<M>?>(null)
    private val semaphore = Semaphore(1) // prevents multiple fetches
    private val callbackScope = CoroutineScope(Dispatchers.IO)

    override fun getIdentifier(): Map<String, Any> {
        return keyMap
    }

    override suspend fun fetchModel(): M? {
        val cached = cachedValue.get()
        if (cached != null) {
            // Quick return if value is already present
            return cached.value
        }

        return fetchInternal()
    }

    override fun fetchModel(onSuccess: NullableConsumer<M?>, onError: Consumer<AmplifyException>) {
        val cached = cachedValue.get()
        if (cached != null) {
            // Quick return if value is already present
            onSuccess.accept(cached.value)
        }

        // To ensure we only have 1 live request at a time, we jump to our own single threaded scope
        callbackScope.launch {
            try {
                val model = fetchInternal()
                onSuccess.accept(model)
            } catch (e: AmplifyException) {
                onError.accept(e)
            }
        }
    }

    private suspend fun fetchInternal(): M? {
        // Use Semaphore with 1 permit to only allow 1 execution at a time
        semaphore.withPermit {

            // Quick return if value is already present
            val cached = cachedValue.get()
            if (cached != null) {
                return cached.value
            }

            return try {
                val resultIterator = query(
                    AppSyncGraphQLRequestFactory.buildQuery<PaginatedResult<M>, M>(
                        clazz,
                        AppSyncLazyQueryPredicate<M>().createPredicate(clazz, keyMap)
                    ),
                    apiName
                ).data.items.iterator()
                val value = if (resultIterator.hasNext()) {
                    resultIterator.next()
                } else {
                    null
                }
                cachedValue.set(LoadedValue(value))
                value
            } catch (error: ApiException) {
                throw AmplifyException("Error lazy loading the model.", error, error.message ?: "")
            }
        }
    }

    private companion object {
        // Wraps the value to determine difference between null/unloaded and null/loaded
        private class LoadedValue<M: Model>(val value: M?)
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
