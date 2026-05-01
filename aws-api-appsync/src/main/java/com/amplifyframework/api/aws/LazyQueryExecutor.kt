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

import com.amplifyframework.annotations.InternalAmplifyApi
import com.amplifyframework.api.graphql.GraphQLRequest
import com.amplifyframework.api.graphql.GraphQLResponse

/**
 * Abstraction for executing GraphQL queries used by lazy model types.
 *
 * Lazy model references and lists need to make follow-up queries to resolve
 * their data. This interface decouples them from a specific query mechanism,
 * allowing both the Amplify plugin (`Amplify.API`) and the standalone
 * `AmplifyAppSyncClient` to provide their own implementations.
 */
@InternalAmplifyApi
interface LazyQueryExecutor {
    /**
     * Execute a GraphQL query and return the typed response.
     */
    suspend fun <R> execute(request: GraphQLRequest<R>): GraphQLResponse<R>
}
