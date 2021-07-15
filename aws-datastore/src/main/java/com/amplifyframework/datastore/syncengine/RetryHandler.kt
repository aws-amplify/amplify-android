package com.amplifyframework.datastore.syncengine

import com.amplifyframework.api.graphql.GraphQLRequest
import com.amplifyframework.api.graphql.GraphQLResponse
import com.amplifyframework.api.graphql.PaginatedResult
import com.amplifyframework.core.Amplify
import com.amplifyframework.core.Consumer
import com.amplifyframework.core.model.Model
import com.amplifyframework.datastore.AmplifyDisposables
import com.amplifyframework.datastore.DataStoreException
import com.amplifyframework.datastore.ErrorType
import com.amplifyframework.datastore.appsync.AppSync
import com.amplifyframework.datastore.appsync.ModelWithMetadata
import io.reactivex.rxjava3.core.SingleEmitter

class RetryHandler<T: Model>(private val emitter: SingleEmitter<PaginatedResult<ModelWithMetadata<T>>>,
                             private val appSync: AppSync,
                             private val request: GraphQLRequest<PaginatedResult<ModelWithMetadata<T>>>,
                             private val onResponse: Consumer<GraphQLResponse<PaginatedResult<ModelWithMetadata<T>>>>,
                             private val onFailure: Consumer<DataStoreException>): RetryCallbackInterface<T> {

    override fun execute() {
        val cancelable = appSync.sync(request, onResponse, onFailure)
        emitter.setDisposable(AmplifyDisposables.fromCancelable(cancelable))
    }
}

interface RetryCallbackInterface<T: Model>{
    fun execute()
}