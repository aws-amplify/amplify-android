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

package com.amazonaws.appsync

import androidx.test.core.app.ApplicationProvider
import com.amazonaws.appsync.test.R
import com.amplifyframework.annotations.ExperimentalAmplifyApi
import com.amplifyframework.annotations.InternalAmplifyApi
import com.amplifyframework.api.ApiException
import com.amplifyframework.api.aws.GsonVariablesSerializer
import com.amplifyframework.api.graphql.SimpleGraphQLRequest
import com.amplifyframework.core.model.Model
import com.amplifyframework.foundation.result.getOrThrow
import com.amplifyframework.testutils.Assets
import com.amplifyframework.testutils.DeviceFarmTestBase
import com.amplifyframework.testutils.Resources
import java.util.UUID
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Assert.fail
import org.junit.Test

/**
 * Integration test that verifies [AmplifyAppSyncClient] works with raw GraphQL documents
 * (not codegen models). Mirrors the test scenarios from
 * [com.amplifyframework.api.aws.GraphQLInstrumentationTest] in the :aws-api module.
 *
 * Uses the "Event App" AppSync schema which has Event and Comment types.
 */
@OptIn(ExperimentalAmplifyApi::class, InternalAmplifyApi::class)
class GraphQLInstrumentationTest : DeviceFarmTestBase() {

    private val config by lazy {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        Resources.readAsJson(context, R.raw.appsync_client_config)
    }

    // ── Tests ───────────────────────────────────────────────────────────

    /**
     * Test that subscription is authorized properly when using API key as
     * authorization provider.
     */
    @Test
    fun subscriptionReceivesMutationOverApiKey() = runTest {
        val client = AmplifyAppSyncClient(
            AmplifyAppSyncClient.Configuration {
                endpoint = config.getString("endpoint")
                authorization = AppSyncAuthorization.Single(
                    AppSyncClientAuthorizer.ApiKey(config.getString("apiKey"))
                )
            }
        )
        try {
            subscriptionReceivesMutation(client)
        } finally {
            client.close()
        }
    }

    /**
     * Test that subscription fails when using Cognito User Pools as
     * authorization provider, and is not signed in.
     */
    @Test(expected = ApiException::class)
    fun subscriptionWithCognitoUserPoolsFailsAsGuest() = runTest {
        val client = AmplifyAppSyncClient(
            AmplifyAppSyncClient.Configuration {
                endpoint = config.getString("cognitoEndpoint")
                authorization = AppSyncAuthorization.Single(
                    AppSyncClientAuthorizer.UserPools { "" }  // No valid token instead of not signed in user
                )
            }
        )
        try {
            subscriptionReceivesMutation(client)
        } finally {
            client.close()
        }
    }

    // ── Shared test logic ───────────────────────────────────────────────

    /**
     * Validates that we can receive notification of a mutation over a WebSocket
     * subscription. Specifically:
     * 1. Create an event, and validate it
     * 2. Setup a subscription to listen for comments on that event
     * 3. Post a comment about the event, validate that comment
     * 4. Expect the comment to arrive on the subscription
     * 5. Validate that the subscription can be torn down gracefully
     */
    private suspend fun subscriptionReceivesMutation(client: AmplifyAppSyncClient) = coroutineScope {
        // Create an event
        val eventId = createEvent(client)

        // Subscribe to comments on that event
        val subscriptionRequest = SimpleGraphQLRequest<Comment>(
            Assets.readAsString("subscribe-event-comments.graphql"),
            mapOf("eventId" to eventId),
            Comment::class.java,
            GsonVariablesSerializer()
        )

        val connected = CompletableDeferred<Unit>()
        val dataDeferred = async(Dispatchers.Default) {
            withTimeout(30_000) {
                client.subscribe(subscriptionRequest)
                    .onEach { if (it is SubscriptionEvent.Connection.Connected) connected.complete(Unit) }
                    .filterIsInstance<SubscriptionEvent.Data<Comment>>()
                    .first()
                    .response
                    .data
            }
        }

        connected.await()

        // Create a comment on that event
        createComment(client, eventId)

        // Validate that the comment was received over the subscription
        val receivedComment = dataDeferred.await()
        assertEquals("It's going to be fun!", receivedComment.content)
    }

    // ── Helpers ──────────────────────────────────────────────────────────

    /**
     * Create an Event against the GraphQL endpoint using a raw mutation document.
     * @return The unique ID of the newly created event.
     */
    private suspend fun createEvent(client: AmplifyAppSyncClient): String {
        val variables = mapOf(
            "name" to "Pizza Party",
            "when" to "Tomorrow",
            "where" to "Mario's Pizza Emporium",
            "description" to "RSVP for the best possible pizza toppings."
        )

        val createdEvent = client.mutate(
            SimpleGraphQLRequest<Event>(
                Assets.readAsString("create-event.graphql"),
                variables,
                Event::class.java,
                GsonVariablesSerializer()
            )
        ).getOrThrow().data

        assertEquals("Pizza Party", createdEvent.name)
        assertEquals("Tomorrow", createdEvent.`when`)
        assertEquals("Mario's Pizza Emporium", createdEvent.where)
        assertEquals("RSVP for the best possible pizza toppings.", createdEvent.description)

        return createdEvent.id
    }

    /**
     * Creates a comment associated to an event.
     */
    private suspend fun createComment(client: AmplifyAppSyncClient, eventId: String) {
        val variables = mapOf(
            "eventId" to eventId,
            "commentId" to UUID.randomUUID().toString(),
            "content" to "It's going to be fun!",
            "createdAt" to com.amplifyframework.api.aws.Iso8601Timestamp.now()
        )

        val createdComment = client.mutate(
            SimpleGraphQLRequest<Comment>(
                Assets.readAsString("create-comment.graphql"),
                variables,
                Comment::class.java,
                GsonVariablesSerializer()
            )
        ).getOrThrow().data

        assertEquals("It's going to be fun!", createdComment.content)
    }

    // ── Models ──────────────────────────────────────────────────────────

    /** Minimal model of a Comment — just the content field for assertion. */
    data class Comment(val content: String) : Model

    /** Minimal model of an Event. */
    data class Event(
        val id: String,
        val name: String,
        val `when`: String,
        val where: String,
        val description: String
    ) : Model
}
