package com.amplifyframework.datastore.syncengine

import com.amplifyframework.api.graphql.GraphQLRequest
import com.amplifyframework.api.graphql.GraphQLResponse
import com.amplifyframework.api.graphql.PaginatedResult
import com.amplifyframework.core.Amplify
import com.amplifyframework.core.Consumer
import com.amplifyframework.core.model.Model
import com.amplifyframework.datastore.AmplifyDisposables
import com.amplifyframework.datastore.DataStoreException
import com.amplifyframework.datastore.appsync.AppSync
import com.amplifyframework.datastore.appsync.ModelWithMetadata
import io.reactivex.rxjava3.core.SingleEmitter
import java.util.*
import java.util.concurrent.TimeUnit

class RetryHandler() {

    private var numberOfAttempts = 0
    private val maxExponent = 8
    private val jitterFactor = 100
    private val LOG = Amplify.Logging.forNamespace("amplify:aws-datastore")

    fun <T : Model> retry(
        emitter: SingleEmitter<PaginatedResult<ModelWithMetadata<T>>>,
        appSync: AppSync,
        request: GraphQLRequest<PaginatedResult<ModelWithMetadata<T>>>,
        onResponse: Consumer<GraphQLResponse<PaginatedResult<ModelWithMetadata<T>>>>,
        onFailure: Consumer<DataStoreException>
    ) {
        numberOfAttempts++
        val jitteredDelay = jitteredDelay()
        Timer().schedule(object : TimerTask() {
            override fun run() {
                LOG.verbose("Retrying attempt number:$numberOfAttempts jittered delay:$jitteredDelay")
                val cancelable = appSync.sync(request, onResponse, onFailure)
                emitter.setDisposable(AmplifyDisposables.fromCancelable(cancelable))
                cancel()
            }
        }, jitteredDelay)
    }


    private fun jitteredDelay(): Long {
        val waitTimeSeconds: Long = java.lang.Double.valueOf(
            Math.pow(2.0, (numberOfAttempts % maxExponent).toDouble())
                    + jitterFactor * Math.random()
        ).toLong()
        return TimeUnit.MILLISECONDS.toMillis(waitTimeSeconds)
    }

}