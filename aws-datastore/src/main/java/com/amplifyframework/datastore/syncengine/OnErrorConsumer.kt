package com.amplifyframework.datastore.syncengine

import com.amplifyframework.api.graphql.GraphQLRequest
import com.amplifyframework.api.graphql.GraphQLResponse
import com.amplifyframework.api.graphql.PaginatedResult
import com.amplifyframework.core.Consumer
import com.amplifyframework.core.model.Model
import com.amplifyframework.datastore.DataStoreException
import com.amplifyframework.datastore.ErrorType
import com.amplifyframework.datastore.appsync.AppSync
import com.amplifyframework.datastore.appsync.ModelWithMetadata
import io.reactivex.rxjava3.core.SingleEmitter

class OnErrorConsumer<T: Model>( val emitter: SingleEmitter<PaginatedResult<ModelWithMetadata<T>>>,
                                 val appSync: AppSync,
                                 val request: GraphQLRequest<PaginatedResult<ModelWithMetadata<T>>>,
        private val onResponse: Consumer<GraphQLResponse<PaginatedResult<ModelWithMetadata<T>>>>,
        private val retryHandler: RequestRetry): Consumer<DataStoreException> {


    override fun accept(result: DataStoreException) {
        if (result.type.equals( ErrorType.IRRECOVERABLE_ERROR)) {
            emitter.onError(
               result
            )
        } else {
            retryHandler.retry(RetryHandler(emitter, appSync, request, onResponse, this))
        }
    }
}