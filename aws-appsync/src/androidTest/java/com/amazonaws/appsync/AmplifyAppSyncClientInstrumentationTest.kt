/*
 * Copyright 2026 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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
package com.amazonaws.appsync

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.amazonaws.appsync.testutils.AppSyncTestConfig
import com.amazonaws.appsync.testutils.AppSyncTestCredentials
import com.amazonaws.appsync.testutils.readAppSyncConfig
import com.amazonaws.appsync.testutils.readAppSyncCredentials
import com.amplifyframework.api.aws.GsonVariablesSerializer
import com.amplifyframework.api.graphql.GraphQLRequest
import com.amplifyframework.api.graphql.SimpleGraphQLRequest
import com.amplifyframework.auth.cognito.AWSCognitoAuthPlugin
import com.amplifyframework.auth.cognito.AWSCognitoAuthSession
import com.amplifyframework.core.configuration.AmplifyOutputs
import com.amplifyframework.kotlin.core.Amplify
import com.amplifyframework.testutils.DeviceFarmTestBase
import com.amplifyframework.testutils.assertions.shouldBeSuccess
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import java.util.UUID
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.After
import org.junit.Assume.assumeNotNull
import org.junit.Assume.assumeTrue
import org.junit.BeforeClass
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Integration tests for [AmplifyAppSyncClient] against a real, deployed AppSync GraphQL API.
 *
 * Nothing here is mocked, deliberately. What these cover is what only a live endpoint can settle: the
 * WebSocket upgrade AppSync actually accepts, the connection and per-subscription authorization
 * objects it actually validates, and a genuine server-originated data frame. A mocked transport can
 * confirm the client's own bookkeeping but not that the service agrees with it — a connection
 * authorization object missing its `host` entry, for instance, satisfies every unit test and is
 * rejected by every non-IAM auth mode on the real service.
 *
 * ## Setup
 *
 * Place the following in `src/androidTest/res/raw/` (both gitignored, so they are provisioned per
 * developer and per CI lane):
 *
 * **amplify_outputs.json** — the deployed API's config, as `npx ampx sandbox` writes it:
 * ```json
 * {
 *   "data": {
 *     "url": "https://<api-id>.appsync-api.<region>.amazonaws.com/graphql",
 *     "aws_region": "us-east-1",
 *     "api_key": "da2-..."
 *   },
 *   "auth": { ... }
 * }
 * ```
 *
 * **credentials.json** — a Cognito user, read only by the User Pools test:
 * ```json
 * { "credentials": [ { "username": "integ-test-user", "password": "..." } ] }
 * ```
 *
 * The backend's schema must define a `Todo` model with a `content` field, which is what
 * `a.model({ content: a.string() })` produces:
 * ```ts
 * const schema = a.schema({
 *   Todo: a.model({ content: a.string() })
 *     .authorization(allow => [allow.publicApiKey(), allow.authenticated()])
 * })
 * ```
 *
 * A test whose resources are absent skips rather than fails, so a lane with no provisioned backend
 * stays green.
 *
 * ## CI gating
 *
 * Not part of the unit-test or apiCheck gates. Runs only in an instrumentation lane, with a device or
 * emulator and a configured backend.
 */
@RunWith(AndroidJUnit4::class)
class AmplifyAppSyncClientInstrumentationTest : DeviceFarmTestBase() {

    private var client: AmplifyAppSyncClient? = null

    @After
    fun tearDown() {
        client?.close()
    }

    // ── Queries and mutations ───────────────────────────────────────────

    @Test
    fun queryAgainstRealApiReturnsData() = runSuspending {
        val response = apiKeyClient().query(listTodos()).shouldBeSuccess().data

        // An empty list is a valid answer. What matters is that the service accepted the document and
        // answered with a data payload rather than errors.
        response.hasErrors() shouldBe false
        response.data.shouldNotBeNull()
    }

    @Test
    fun mutationAgainstRealApiSucceeds() = runSuspending {
        val content = uniqueContent()

        val response = apiKeyClient().mutate(createTodo(content)).shouldBeSuccess().data

        response.hasErrors() shouldBe false
        response.data.shouldNotBeNull() shouldContain content
    }

    // ── Subscriptions ───────────────────────────────────────────────────

    @Test
    fun subscriptionReceivesRealServerPushWithApiKey() = runSuspending {
        subscriptionObservesMutation(apiKeyClient())
    }

    @Test
    fun subscriptionReceivesRealServerPushWithUserPools() = runSuspending {
        // The regression that motivated this suite was auth-mode specific, so one mode passing says
        // less about the others than it appears to.
        val user = credentials
        assumeNotNull(user)
        assumeTrue("Amplify Auth is not configured; check amplify_outputs.json", authConfigured)
        signIn(user!!)

        subscriptionObservesMutation(
            newClient(AppSyncAuthorization.Single(AppSyncClientAuthorizer.UserPools { accessToken() }))
        )
    }

    /**
     * Subscribes, then mutates, and asserts the subscriber observes the mutation.
     *
     * The broadest thing in the suite: reaching the final assertion requires the WebSocket upgrade to
     * be accepted, the connection authorization object to be validated, the `start`/`start_ack` round
     * trip to complete, and the service to originate a frame of its own accord.
     */
    private suspend fun subscriptionObservesMutation(client: AmplifyAppSyncClient) = coroutineScope {
        val content = uniqueContent()
        val connected = CompletableDeferred<Unit>()

        val observed = async {
            withTimeout(SUBSCRIPTION_TIMEOUT) {
                client.subscribe(onCreateTodo()).first { event ->
                    when (event) {
                        is SubscriptionEvent.Connected -> {
                            connected.complete(Unit)
                            false
                        }
                        is SubscriptionEvent.Data -> event.response.data?.contains(content) == true
                        else -> false
                    }
                }
            }
        }

        // The mutation must not be issued until the subscription is registered: the service does not
        // replay events from before that point, so an early mutation is one the subscriber can never
        // observe and the test would fail on a timeout for the wrong reason.
        withTimeout(CONNECT_TIMEOUT) { connected.await() }

        client.mutate(createTodo(content)).shouldBeSuccess()

        observed.await()
    }

    // ── Client construction ─────────────────────────────────────────────

    /** Skips the test unless the raw resources supply an API key, then builds an API-key client. */
    private fun apiKeyClient(): AmplifyAppSyncClient {
        val apiKey = config?.apiKey
        assumeNotNull(apiKey)
        return newClient(AppSyncAuthorization.Single(AppSyncClientAuthorizer.ApiKey(apiKey!!)))
    }

    private fun newClient(authorization: AppSyncAuthorization): AmplifyAppSyncClient {
        val resolved = config.shouldNotBeNull()
        return AmplifyAppSyncClient(
            AmplifyAppSyncClient.Configuration {
                endpoint = resolved.endpoint
                region = resolved.region
                this.authorization = authorization
            }
        ).also { client = it }
    }

    // ── Auth ────────────────────────────────────────────────────────────

    private suspend fun signIn(user: AppSyncTestCredentials) {
        // Signed out first so the sign-in does not depend on what a previous test left behind. Both
        // calls tolerate failure: an already-signed-out session and an already-signed-in user are both
        // acceptable starting points, and accessToken() is what actually requires a session.
        runCatching { Amplify.Auth.signOut() }
        runCatching { Amplify.Auth.signIn(user.username, user.password) }
    }

    private suspend fun accessToken(): String {
        val session = Amplify.Auth.fetchAuthSession() as AWSCognitoAuthSession
        return session.userPoolTokensResult.value?.accessToken
            ?: error("Signed in, but no User Pools access token is available.")
    }

    // ── Requests ────────────────────────────────────────────────────────

    private fun uniqueContent() = "appsync-integ-${UUID.randomUUID()}"

    private fun listTodos() = request("query { listTodos { items { id content } } }")

    private fun createTodo(content: String) = request(
        """mutation { createTodo(input: {content: "$content"}) { id content } }"""
    )

    private fun onCreateTodo() = request("subscription { onCreateTodo { id content } }")

    /**
     * Raw documents deserialized to String, so the suite does not need code-generated model classes
     * for a schema it does not own.
     */
    private fun request(document: String): GraphQLRequest<String> = SimpleGraphQLRequest(
        document,
        emptyMap(),
        String::class.java,
        GsonVariablesSerializer()
    )

    /**
     * Connected tests drive real network I/O on real threads, so a virtual-time test dispatcher is not
     * usable. Typed to Unit so a test body cannot return a value, which JUnit rejects.
     */
    private fun runSuspending(body: suspend () -> Unit) = runBlocking { body() }

    private companion object {
        val SUBSCRIPTION_TIMEOUT = 60.seconds
        val CONNECT_TIMEOUT = 30.seconds

        var config: AppSyncTestConfig? = null
        var credentials: AppSyncTestCredentials? = null
        var authConfigured = false

        @BeforeClass
        @JvmStatic
        fun setUpClass() {
            val context = ApplicationProvider.getApplicationContext<Context>()
            config = readAppSyncConfig(context)
            credentials = readAppSyncCredentials(context)

            val outputsId = context.resources.getIdentifier("amplify_outputs", "raw", context.packageName)
            if (outputsId == 0) return

            // Needed only by the User Pools test. A failure here is not fatal: the API-key tests never
            // touch Amplify, and the test that does checks this flag and skips.
            authConfigured = runCatching {
                Amplify.addPlugin(AWSCognitoAuthPlugin())
                Amplify.configure(AmplifyOutputs(outputsId), context)
            }.isSuccess
        }
    }
}
