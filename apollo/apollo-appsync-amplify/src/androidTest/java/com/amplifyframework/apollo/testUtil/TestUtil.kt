/*
 *  Copyright 2024 Amazon.com, Inc. or its affiliates. All Rights Reserved.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License").
 *  You may not use this file except in compliance with the License.
 *  A copy of the License is located at
 *
 *  http://aws.amazon.com/apache2.0
 *
 *  or in the "license" file accompanying this file. This file is distributed
 *  on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either
 *  express or implied. See the License for the specific language governing
 *  permissions and limitations under the License.
 */

package com.amplifyframework.apollo.testUtil

import com.amplifyframework.apollo.appsync.AppSyncAuthorizer
import com.amplifyframework.apollo.appsync.AppSyncEndpoint
import com.amplifyframework.apollo.appsync.appSync
import com.amplifyframework.apollo.graphql.CreateTodoMutation
import com.amplifyframework.apollo.graphql.DeleteTodoMutation
import com.amplifyframework.apollo.graphql.type.CreateTodoInput
import com.amplifyframework.apollo.graphql.type.DeleteTodoInput
import com.apollographql.apollo.ApolloClient
import com.apollographql.apollo.api.ApolloResponse
import com.apollographql.apollo.api.Optional
import java.util.UUID
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest

suspend fun ApolloClient.createTodoForTest(func: suspend (ApolloResponse<CreateTodoMutation.Data>) -> Unit) {
    val id = UUID.randomUUID().toString()
    val mutation = CreateTodoMutation(CreateTodoInput(id = Optional.present(id)))

    try {
        val result = mutation(mutation).execute()
        func(result)
    } finally {
        // Delete the created data at the end of the test
        mutation(DeleteTodoMutation(DeleteTodoInput(id))).execute()
    }
}

fun runApolloTest(
    endpoint: AppSyncEndpoint,
    authorizer: AppSyncAuthorizer,
    func: suspend TestScope.(ApolloClient) -> Unit
) = runTest {
    val apollo = ApolloClient.Builder()
        .appSync(endpoint, authorizer)
        .build()
    apollo.use { func(it) }
}
