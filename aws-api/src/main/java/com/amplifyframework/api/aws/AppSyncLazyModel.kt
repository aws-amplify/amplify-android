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

class AppSyncLazyModel<M : Model>(private val clazz: Class<M>,
                                  private val keyMap: Map<String, Any>,
                                  private val predicate: AppSyncLazyQueryPredicate<M>) : LazyModel<M>() {

    private var value: M? = null

    override fun getValue(): M? {
        return value;
    }

    override suspend fun get(): M? {
        value?.let { return value }
        val queryPredicate = predicate.createPredicate(clazz, keyMap)
        try {
            return Amplify.API
                .query(AppSyncGraphQLRequestFactory.buildQuery<PaginatedResult<M>,
                            M>(clazz, queryPredicate)).data.items.iterator().next()
        } catch (error: ApiException) {
            Log.e("MyAmplifyApp", "Query failure", error)
        }
        return null
    }

    override fun get(onSuccess: Consumer<M>, onFailure: Consumer<AmplifyException>) {
        val onQuerySuccess = Consumer<GraphQLResponse<M>> {
            onSuccess.accept(it.data)}
        val onApiFailure = Consumer<ApiException> {
            onFailure.accept(it)}
        coreAmplify.API.query(AppSyncGraphQLRequestFactory.buildQuery(clazz, predicate.createPredicate(clazz, keyMap)),
            onQuerySuccess, onApiFailure)
    }
}