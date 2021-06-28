package com.amplifyframework.datastore.syncengine

import com.amplifyframework.api.graphql.GraphQLResponse
import com.amplifyframework.api.graphql.PaginatedResult
import com.amplifyframework.core.Consumer
import com.amplifyframework.core.model.Model
import com.amplifyframework.datastore.DataStoreException
import com.amplifyframework.datastore.ErrorType
import com.amplifyframework.datastore.appsync.ModelWithMetadata
import io.reactivex.rxjava3.core.SingleEmitter


class OnSuccessConsumer<T : Model>(private val emitter: SingleEmitter<PaginatedResult<ModelWithMetadata<T>>>) :
    Consumer<GraphQLResponse<PaginatedResult<ModelWithMetadata<T>>>> {

    override fun accept(result: GraphQLResponse<PaginatedResult<ModelWithMetadata<T>>>) {
        if (result.hasErrors()) {
            emitter.onError(DataStoreException(
                    "A model sync failed: $result.errors",
                    "Check your schema."))
        } else if (!result.hasData()) {
            emitter.onError(DataStoreException(
                    "Empty response from AppSync.", "Report to AWS team.", ErrorType.IRRECOVERABLE_ERROR))
        } else {
            emitter.onSuccess(result.data)
        }
    }
}