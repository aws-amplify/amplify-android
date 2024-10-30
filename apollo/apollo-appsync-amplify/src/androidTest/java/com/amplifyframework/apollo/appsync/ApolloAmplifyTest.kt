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

package com.amplifyframework.apollo.appsync

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import app.cash.turbine.test
import com.amplifyframework.apollo.graphql.GetTodoQuery
import com.amplifyframework.apollo.graphql.OnCreateSubscription
import com.amplifyframework.apollo.testUtil.ApiKeyProvider
import com.amplifyframework.apollo.testUtil.AuthTokenProvider
import com.amplifyframework.apollo.testUtil.AuthorizerProvider
import com.amplifyframework.apollo.testUtil.IamProvider
import com.amplifyframework.apollo.testUtil.createTodoForTest
import com.amplifyframework.apollo.testUtil.runApolloTest
import com.amplifyframework.auth.cognito.AWSCognitoAuthPlugin
import com.amplifyframework.core.Amplify
import com.amplifyframework.core.Resources
import com.amplifyframework.core.configuration.AmplifyOutputs
import com.apollographql.apollo.ApolloClient
import io.kotest.assertions.fail
import io.kotest.assertions.throwables.shouldThrowWithMessage
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.withContext
import org.junit.BeforeClass
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import org.junit.runners.Parameterized.Parameters

/**
 * Contains E2E tests for Apollo connecting to AppSync. Runs each test for each different authorizer type.
 */
@RunWith(Parameterized::class)
class ApolloAmplifyTest(private val provider: AuthorizerProvider) {

    companion object {
        private val outputs = AmplifyOutputs(
            Resources.getRawResourceId(ApplicationProvider.getApplicationContext(), "amplify_outputs")
        )

        private val connector = ApolloAmplifyConnector(
            context = ApplicationProvider.getApplicationContext(),
            outputs = outputs
        )

        @BeforeClass
        @JvmStatic
        fun setupClass() {
            val context = ApplicationProvider.getApplicationContext<Context>()
            Amplify.addPlugin(AWSCognitoAuthPlugin())
            Amplify.configure(outputs, context)
        }

        @JvmStatic
        @Parameters(name = "{0}")
        fun authorizerProviders(): Collection<AuthorizerProvider> = listOf(
            ApiKeyProvider(connector),
            AuthTokenProvider(connector),
            IamProvider(connector)
        )
    }

    @Test
    fun creates_todo() = runApolloTest(provider.valid) { apollo ->
        apollo.createTodoForTest { result ->
            result.data?.createTodo.shouldNotBeNull()
            result.errors?.shouldBeNull()
        }
    }

    @Test
    fun can_query_created_todo() = runApolloTest(provider.valid) { apollo ->
        apollo.createTodoForTest { result ->
            val id = result.data?.createTodo?.todoFragment?.id ?: fail("Todo was not created")
            val queryResult = apollo.query(GetTodoQuery(id)).execute()
            queryResult.data?.getTodo?.todoFragment?.id shouldBe id
        }
    }

    @Test
    fun create_fails_with_invalid_authorizer() = runApolloTest(provider.invalid) { apollo ->
        apollo.createTodoForTest { result ->
            result.data?.createTodo.shouldBeNull()
            result.errors?.shouldNotBeNull()
        }
    }

    @Test
    fun create_throws_if_authorizer_throws() = runApolloTest(provider.throwing) { apollo ->
        shouldThrowWithMessage<IllegalStateException>(provider.errorMessage) {
            apollo.createTodoForTest { }
        }
    }

    @Test
    fun subscription_receives_mutation() = runApolloTest(provider.valid) { apollo ->
        apollo.subscription(OnCreateSubscription()).toFlow().test {
            // Allow the subscription to be established before sending the mutation
            withContext(Dispatchers.Default) { delay(3.seconds) }

            apollo.createTodoForTest { result ->
                val created = result.data?.createTodo?.todoFragment
                val eventTodo = awaitItem().data?.onCreateTodo?.todoFragment
                eventTodo.shouldNotBeNull()
                eventTodo shouldBe created
            }
        }
    }

    private fun runApolloTest(authorizer: AppSyncAuthorizer, func: suspend TestScope.(ApolloClient) -> Unit) =
        runApolloTest(connector.endpoint, authorizer) { client ->
            try {
                provider.setupPrerequisites()
                func(client)
            } finally {
                provider.teardownPrerequisites()
            }
        }
}
