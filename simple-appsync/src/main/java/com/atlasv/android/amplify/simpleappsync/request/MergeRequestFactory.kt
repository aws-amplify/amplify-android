package com.atlasv.android.amplify.simpleappsync

import android.content.Context
import com.amplifyframework.api.aws.AppSyncGraphQLRequest
import com.amplifyframework.api.aws.AppSyncGraphQLRequestFactory
import com.amplifyframework.api.aws.GsonVariablesSerializer
import com.amplifyframework.api.graphql.GraphQLRequest
import com.amplifyframework.api.graphql.GraphQLResponse
import com.amplifyframework.api.graphql.PaginatedResult
import com.amplifyframework.api.graphql.model.ModelPagination
import com.amplifyframework.core.model.Model
import com.amplifyframework.core.model.ModelProvider
import com.amplifyframework.core.model.SchemaRegistry
import com.amplifyframework.core.model.query.predicate.QueryPredicate
import com.amplifyframework.core.model.query.predicate.QueryPredicates
import com.amplifyframework.core.model.temporal.Temporal
import com.amplifyframework.datastore.DataStoreConfiguration
import com.amplifyframework.datastore.appsync.DataStoreGraphQLRequestOptions
import com.amplifyframework.datastore.appsync.ModelWithMetadata
import com.amplifyframework.util.TypeMaker
import com.amplifyframework.util.Wrap
import com.atlasv.android.amplify.simpleappsync.ext.andIfNotNull
import com.atlasv.android.amplify.simpleappsync.ext.queryField
import com.atlasv.android.amplify.simpleappsync.request.MergeListRequest
import java.util.*

/**
 * [灰度发布说明](https://cwtus1pn64.feishu.cn/docs/doccnDayF0ZErCLnMyySPGJchOg#9m94LF)
 * weiping@atlasv.com
 * 2022/12/7
 */
interface MergeRequestFactory {
    fun create(
        appContext: Context,
        dataStoreConfiguration: DataStoreConfiguration,
        modelProvider: ModelProvider,
        schemaRegistry: SchemaRegistry,
        lastSync: Long,
        grayRelease: Int,
        locale: String,
        rebuildLocale: Boolean
    ): GraphQLRequest<List<GraphQLResponse<PaginatedResult<ModelWithMetadata<Model>>>>>
}

object DefaultMergeRequestFactory : MergeRequestFactory {
    private const val VAR_LIMIT = "limit"
    private const val VAR_FILTER = "filter"
    private const val VAR_LAST_SYNC = "lastSync"

    override fun create(
        appContext: Context,
        dataStoreConfiguration: DataStoreConfiguration,
        modelProvider: ModelProvider,
        schemaRegistry: SchemaRegistry,
        lastSync: Long,
        grayRelease: Int,
        locale: String,
        rebuildLocale: Boolean
    ): GraphQLRequest<List<GraphQLResponse<PaginatedResult<ModelWithMetadata<Model>>>>> {
        val requests = modelProvider.models().map {
            val predicate =
                dataStoreConfiguration.syncExpressions[it.simpleName]?.resolvePredicate() ?: QueryPredicates.all()
            getModelRequest(
                it,
                predicate,
                if (rebuildLocale && it.simpleName.endsWith("Locale")) 0 else lastSync,
                grayRelease,
                locale
            ) as AppSyncGraphQLRequest<Any>
        }

        val inputTypeString = createInputTypeString(requests)

        val operationString = requests.joinToString(separator = "\n") {
            it.operationString
                .replace("\$$VAR_FILTER", "\$$VAR_FILTER${it.modelSchema.name}")
        }

        val query = "query ListAllModel${inputTypeString}${
            Wrap.inPrettyBraces(
                operationString,
                "",
                "  "
            )
        }\n"

        val variables = hashMapOf<String, Any>()
        requests.forEach { req ->
            variables.putAll(
                req.variables.map {
                    if (isSharedKey(it.key)) {
                        it.key to it.value
                    } else {
                        "${it.key}${req.modelSchema.name}" to it.value
                    }
                }
            )
        }
        return MergeListRequest(
            requests,
            query,
            variables,
            TypeMaker.getParameterizedType(
                String::class.java
            ), GsonVariablesSerializer()
        )
    }

    private fun isSharedKey(key: String): Boolean {
        return key.replace("\$", "").let {
            it == VAR_LIMIT || it == VAR_LAST_SYNC
        }
    }

    private fun <T : Model> getModelRequest(
        modelClass: Class<T>, predicate: QueryPredicate, lastSync: Long, grayRelease: Int, locale: String
    ): GraphQLRequest<PaginatedResult<ModelWithMetadata<T>>> {
        val pageLimit = ModelPagination.limit(Int.MAX_VALUE)
        var targetPredicate = predicate
        if (lastSync > 0) {
            targetPredicate = predicate.andIfNotNull(
                modelClass.queryField("updatedAt")?.gt(Temporal.DateTime(Date(lastSync), 0))
            )
        }
        targetPredicate = targetPredicate.andIfNotNull(
            modelClass.queryField("grayRelease")?.gt(grayRelease)
        )
        targetPredicate = targetPredicate.andIfNotNull(
            modelClass.queryField("locale")?.eq(locale)
        )
        return AppSyncGraphQLRequestFactory.buildQuery(
            modelClass, targetPredicate, pageLimit.limit, TypeMaker.getParameterizedType(
                PaginatedResult::class.java, ModelWithMetadata::class.java, modelClass
            ), DataStoreGraphQLRequestOptions()
        )
    }

    private fun createInputTypeString(requests: List<AppSyncGraphQLRequest<Any>>): String? {
        val kvSet = mutableSetOf<Pair<String, String>>()
        requests.forEach { req ->
            req.inputTypeString
                .removePrefix("(")
                .removeSuffix(")")
                .replace(" ", "").split(",").forEach {
                    val kv = it.split(":")
                    if (kv.size == 2) {
                        val newKey = if (isSharedKey(kv[0])) kv[0] else "${kv[0]}${req.modelSchema.name}"
                        kvSet.add(newKey to kv[1])
                    }
                }
        }
        return Wrap.inParentheses(kvSet.joinToString { (k, v) ->
            "$k: $v"
        })

    }

}