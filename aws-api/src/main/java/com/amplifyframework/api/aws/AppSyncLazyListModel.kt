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
    private val keyMap: Map<String, Any>,
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
                    predicate.createPredicate(clazz, keyMap)
                )
            ).data.items.toList()
        } catch (error: ApiException) {
            Log.e("MyAmplifyApp", "Query failure", error)
        }
        return value
    }

    override fun get(onSuccess: Consumer<List<M>>, onFailure: Consumer<AmplifyException>) {
        val onQuerySuccess = Consumer<GraphQLResponse<List<M>>> {
            value = it.data
            onSuccess.accept(it.data)
        }
        val onApiFailure = Consumer<ApiException> { onFailure.accept(it) }
        coreAmplify.API.query(
            AppSyncGraphQLRequestFactory.buildQuery(clazz, predicate.createPredicate(clazz, keyMap)),
            onQuerySuccess,
            onApiFailure
        )
    }
}
