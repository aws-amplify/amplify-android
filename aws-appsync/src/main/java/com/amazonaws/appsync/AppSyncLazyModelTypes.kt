/*
 * Copyright 2026 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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
package com.amazonaws.appsync

import com.amplifyframework.AmplifyException
import com.amplifyframework.api.aws.AppSyncGraphQLRequestFactory
import com.amplifyframework.api.graphql.GraphQLRequest
import com.amplifyframework.core.Consumer
import com.amplifyframework.core.NullableConsumer
import com.amplifyframework.core.model.LazyModelList
import com.amplifyframework.core.model.LazyModelReference
import com.amplifyframework.core.model.Model
import com.amplifyframework.core.model.ModelPage
import com.amplifyframework.core.model.PaginationToken
import com.amplifyframework.core.model.query.predicate.QueryField
import com.amplifyframework.core.model.query.predicate.QueryPredicate
import com.amplifyframework.core.model.query.predicate.QueryPredicates
import java.util.concurrent.atomic.AtomicReference
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * A related model that was not included in the response, fetched on first access and then cached.
 *
 * The related model is identified by [keyMap], the primary key values that the parent's response
 * carried in place of the model itself. An empty [keyMap] means the parent had no related model at
 * all: such a reference resolves to null without ever issuing a request.
 *
 * Both `fetchModel` overloads report failure as [AmplifyException] whose `cause` is always an
 * [AppSyncException], so a caller that wants the typed error can match on it:
 *
 * ```kotlin
 * val blog = try {
 *     post.blog.fetchModel()
 * } catch (e: AmplifyException) {
 *     when (e.cause) {
 *         is AppSyncNetworkException -> retryLater()
 *         else -> report(e)
 *     }
 * }
 * ```
 */
internal class AppSyncLazyModelReference<M : Model>(
    private val modelClass: Class<M>,
    private val keyMap: Map<String, Any>,
    private val loader: AppSyncModelLoader,
    private val callbackScope: CoroutineScope = CoroutineScope(Dispatchers.IO)
) : LazyModelReference<M> {

    // Wrapped rather than held bare, because a loaded value may itself be null: a bare null could not
    // be told apart from "nothing has been loaded yet", and would be re-fetched on every access.
    private val cached = AtomicReference<Cached<M>?>(null)
    private val mutex = Mutex()

    init {
        if (keyMap.isEmpty()) {
            cached.set(Cached(null))
        }
    }

    override fun getIdentifier(): Map<String, Any> = keyMap

    override suspend fun fetchModel(): M? {
        val hit = cached.get()
        if (hit != null) return hit.value
        return load()
    }

    override fun fetchModel(onSuccess: NullableConsumer<M?>, onError: Consumer<AmplifyException>) {
        val hit = cached.get()
        if (hit != null) {
            // Returning here is what keeps onSuccess to a single call: falling through would deliver
            // the cached value and then deliver it again from the coroutine below.
            onSuccess.accept(hit.value)
            return
        }
        callbackScope.launch {
            try {
                onSuccess.accept(fetchModel())
            } catch (error: AmplifyException) {
                onError.accept(error)
            }
        }
    }

    /**
     * Fetches under a lock, so concurrent callers issue one request between them rather than one each.
     * The cache is re-read inside the lock: by the time a queued caller acquires it the value is
     * usually already there.
     */
    private suspend fun load(): M? = mutex.withLock {
        cached.get()?.let { return it.value }

        val value = try {
            val request: GraphQLRequest<M> = AppSyncGraphQLRequestFactory.buildQueryFromKeyMap(modelClass, keyMap)
            loader.load(request)
        } catch (cancellation: CancellationException) {
            throw cancellation
        } catch (error: Exception) {
            throw lazyLoadFailure("Could not load the related ${modelClass.simpleName}.", error)
        }

        cached.set(Cached(value))
        value
    }

    /** Distinguishes a loaded null from an absent value. */
    private class Cached<M : Model>(val value: M?)
}

/**
 * A related list that was not included in the response, fetched a page at a time.
 *
 * Unlike a single reference, pages are not cached: each `fetchPage` issues a request, because the
 * caller controls which page it asks for.
 *
 * Every `fetchPage` overload reports failure as [AmplifyException] whose `cause` is always an
 * [AppSyncException], so a caller that wants the typed error can match on it.
 */
// TODO: nothing constructs this yet. A response leaves a lazy list field null, so filling it in
//  needs a post-deserialization pass that derives the foreign-key map from the child schema's
//  associations. Until that exists this type is reachable only from tests.
internal class AppSyncLazyModelList<out M : Model>(
    private val modelClass: Class<M>,
    keyMap: Map<String, Any>,
    private val loader: AppSyncModelLoader,
    private val callbackScope: CoroutineScope = CoroutineScope(Dispatchers.IO)
) : LazyModelList<M> {

    // The parent's key values are the filter for the child query: they name the foreign key fields on
    // the child that point back at the parent.
    private val predicate: QueryPredicate = keyMap.entries.fold(QueryPredicates.all()) { predicate, (field, value) ->
        predicate.and(QueryField.field(modelClass.simpleName, field).eq(value))
    }

    override suspend fun fetchPage(paginationToken: PaginationToken?): ModelPage<M> {
        val page = try {
            val request: GraphQLRequest<ModelPage<M>> = AppSyncGraphQLRequestFactory.buildModelPageQuery(
                modelClass,
                predicate,
                AppSyncModelPage::class.java,
                (paginationToken as? AppSyncPaginationToken)?.nextToken
            )
            loader.load(request)
        } catch (cancellation: CancellationException) {
            throw cancellation
        } catch (error: Exception) {
            throw lazyLoadFailure("Could not load a page of related ${modelClass.simpleName}.", error)
        }

        return page ?: throw lazyLoadFailure(
            "Could not load a page of related ${modelClass.simpleName}.",
            AppSyncUnknownException(
                message = "The response carried no page of results.",
                recoverySuggestion = "Verify the API returns a list for this relationship."
            )
        )
    }

    override fun fetchPage(onSuccess: Consumer<ModelPage<@UnsafeVariance M>>, onError: Consumer<AmplifyException>) =
        fetchPage(null, onSuccess, onError)

    override fun fetchPage(
        paginationToken: PaginationToken?,
        onSuccess: Consumer<ModelPage<@UnsafeVariance M>>,
        onError: Consumer<AmplifyException>
    ) {
        callbackScope.launch {
            try {
                onSuccess.accept(fetchPage(paginationToken))
            } catch (error: AmplifyException) {
                onError.accept(error)
            }
        }
    }
}

/**
 * Wraps a failure so it can be thrown from a lazy load.
 *
 * The two hierarchies involved are unrelated classes that happen to share a name, and the lazy
 * interfaces declare the one in `com.amplifyframework`. Throwing the client's own exception directly
 * would be an undeclared checked exception that a Java caller's `catch (AmplifyException)` could not
 * catch, so it travels as the cause instead, with its recovery suggestion carried across.
 */
private fun lazyLoadFailure(message: String, error: Throwable): AmplifyException {
    val cause = AppSyncException.from(error)
    return AmplifyException(message, cause, cause.recoverySuggestion)
}
