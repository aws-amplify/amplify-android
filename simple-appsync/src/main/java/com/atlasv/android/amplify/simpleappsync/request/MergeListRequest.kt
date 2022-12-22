package com.atlasv.android.amplify.simpleappsync.request

import com.amplifyframework.api.aws.AppSyncGraphQLRequest
import com.amplifyframework.api.graphql.SimpleGraphQLRequest
import java.lang.reflect.Type

/**
 * weiping@atlasv.com
 * 2022/12/5
 */
class MergeListRequest<R>(
    val children: List<AppSyncGraphQLRequest<Any>>,
    document: String?,
    variables: MutableMap<String, Any>?,
    responseType: Type?,
    variablesSerializer: VariablesSerializer?
) : SimpleGraphQLRequest<R>(document, variables, responseType, variablesSerializer)