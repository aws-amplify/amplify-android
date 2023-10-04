/*
 * Copyright 2023 Amazon.com, Inc. or its affiliates. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License").
 * You may not use this file except in compliance with the License.
 * A copy of the License is located at
 *
 *   http://aws.amazon.com/apache2.0
 *
 * or in the "license" file accompanying this file. This file is distributed
 * on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either
 * express or implied. See the License for the specific language governing
 * permissions and limitations under the License.
 */

package com.amplifyframework.api.aws

import com.amplifyframework.api.ApiCategory
import com.amplifyframework.api.ApiException
import com.amplifyframework.api.graphql.GraphQLRequest
import com.amplifyframework.api.graphql.GraphQLResponse
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine

/*
 Duplicating the query Kotlin Facade method so we aren't pulling in Kotlin Core
 */
@Throws(ApiException::class)
internal suspend fun <R> query(apiCategory: ApiCategory, request: GraphQLRequest<R>, apiName: String?):
    GraphQLResponse<R> {
    return suspendCoroutine { continuation ->
        if (apiName != null) {
            apiCategory.query(
                apiName,
                request,
                { continuation.resume(it) },
                { continuation.resumeWithException(it) }
            )
        } else {
            apiCategory.query(
                request,
                { continuation.resume(it) },
                { continuation.resumeWithException(it) }
            )
        }
    }
}
