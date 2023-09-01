package com.amplifyframework.api.aws

import com.amplifyframework.AmplifyException
import com.amplifyframework.api.ApiException
import com.amplifyframework.api.graphql.GraphQLRequest
import com.amplifyframework.api.graphql.GraphQLResponse
import com.amplifyframework.core.Amplify
import com.amplifyframework.core.Consumer
import com.amplifyframework.core.model.LazyModelList
import com.amplifyframework.core.model.LoadedModelList
import com.amplifyframework.core.model.Model
import com.amplifyframework.core.model.ModelPage
import com.amplifyframework.core.model.PaginationToken
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine

internal class ApiLoadedModelList<out M : Model>(
    override val items: List<M>
) : LoadedModelList<M>

internal class ApiModelPage<out M : Model>(
    override val items: List<M>,
    override val nextToken: ApiPaginationToken?
): ModelPage<M>

internal class ApiPaginationToken(val nextToken: String) : PaginationToken

internal class ApiLazyModelList<out M : Model>  constructor(
    private val clazz: Class<M>,
    keyMap: Map<String, Any>,
    private val apiName: String?
) : LazyModelList<M> {

    private val queryPredicate = AppSyncLazyQueryPredicate<M>().createPredicate(clazz, keyMap)

    override suspend fun loadPage(paginationToken: PaginationToken?): ModelPage<M> {
        val response = query(apiName, createRequest(paginationToken))
        return response.data
    }

    override fun loadPage(onSuccess: Consumer<ModelPage<@UnsafeVariance M>>, onError: Consumer<AmplifyException>) {
        query(apiName, createRequest(), onSuccess, onError)
    }

    override fun loadPage(
        paginationToken: PaginationToken,
        onSuccess: Consumer<ModelPage<@UnsafeVariance M>>,
        onError: Consumer<AmplifyException>
    ) {
        query(apiName, createRequest(paginationToken), onSuccess, onError)
    }

    private fun createRequest(paginationToken: PaginationToken? = null): GraphQLRequest<ModelPage<M>> {
        return AppSyncGraphQLRequestFactory.buildModelPageQuery(
            clazz,
            queryPredicate,
            (paginationToken as? ApiPaginationToken)?.nextToken
        )
    }

    private fun query(
        apiName: String?,
        request: GraphQLRequest<ModelPage<M>>,
        onSuccess: Consumer<ModelPage<@UnsafeVariance M>>,
        onError: Consumer<AmplifyException>) {

        if (apiName != null) {
            Amplify.API.query(
                apiName,
                request,
                { onSuccess.accept(it.data) },
                { onError.accept(it) }
            )
        } else {
            Amplify.API.query(
                request,
                { onSuccess.accept(it.data) },
                { onError.accept(it) }
            )
        }

    }

    @Throws(ApiException::class)
    private suspend fun <R> query(apiName: String?, request: GraphQLRequest<R>):
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
}

