/*
 * Copyright 2025 Amazon.com, Inc. or its affiliates. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License").
 * You may not use this file except in compliance with the License.
 * A copy of the License is located at
 *
 *  http://aws.amazon.com/apache2.0
 *
 * or in the "license" file accompanying this file. This file is distributed
 * on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either
 * express or implied. See the License for the specific language governing
 * permissions and limitations under the License.
 */
package com.amplifyframework.api.aws

import com.amplifyframework.api.graphql.GraphQLRequest
import com.amplifyframework.api.graphql.GraphQLResponse
import com.amplifyframework.core.Amplify

/**
 * Default [LazyQueryExecutor] that delegates to [Amplify.API].
 * Used by the existing AWSApiPlugin path to maintain backward compatibility.
 */
internal class AmplifyApiQueryExecutor(
    private val apiName: String? = null
) : LazyQueryExecutor {
    override suspend fun <R> execute(request: GraphQLRequest<R>): GraphQLResponse<R> =
        query(Amplify.API, request, apiName)
}
