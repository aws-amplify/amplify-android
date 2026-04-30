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
import com.amplifyframework.api.aws.GsonVariablesSerializer
import com.amplifyframework.api.graphql.SimpleGraphQLRequest
import com.amplifyframework.testutils.Assets
import com.amplifyframework.testutils.DeviceFarmTestBase
import com.amplifyframework.testutils.Resources
import java.util.UUID
import java.util.concurrent.atomic.AtomicInteger
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Integration test that verifies [AmplifyAppSyncClient] subscription behavior.
 * Mirrors the test scenarios from
 * [com.amplifyframework.api.aws.SubscriptionEndpointTest] in the :aws-api module.
 *
 * The original test exercises the internal [SubscriptionEndpoint] directly. This test
 * verifies the same behavior (multiple subscriptions, OkHttp configurator) through the
 * client's public API, which wraps [SubscriptionEndpoint] internally.
 */
@OptIn(ExperimentalAmplifyApi::class, InternalAmplifyApi::class)
class SubscriptionInstrumentationTest : DeviceFarmTestBase() {

    private val config by lazy {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        Resources.readAsJson(context, R.raw.appsync_client_config)
    }

    private fun subscribeToEventComments(eventId: String) = SimpleGraphQLRequest<String>(
        Assets.readAsString("subscribe-event-comments.graphql"),
        mapOf("eventId" to eventId),
        String::class.java,
        GsonVariablesSerializer()
    )

    /**
     * It should be possible to create two subscriptions to the same type of model.
     * Both should connect successfully and independently.
     */
    @Test
    fun twoSubscriptionsToTheSameThing() = runTest {
        val client = AmplifyAppSyncClient(
            AmplifyAppSyncClient.Configuration {
                endpoint = config.getString("endpoint")
                authorization = AppSyncAuthorization.Single(
                    AppSyncClientAuthorizer.ApiKey(config.getString("apiKey"))
                )
            }
        )

        try {
            val eventId = UUID.randomUUID().toString()

            coroutineScope {
                // Start two subscriptions to the same thing concurrently
                val first = async(Dispatchers.Default) {
                    withTimeout(30_000) {
                        client.subscribe(subscribeToEventComments(eventId))
                            .first { it is SubscriptionEvent.Connection.Connected }
                    }
                }

                val second = async(Dispatchers.Default) {
                    withTimeout(30_000) {
                        client.subscribe(subscribeToEventComments(eventId))
                            .first { it is SubscriptionEvent.Connection.Connected }
                    }
                }

                // Both should connect successfully
                assertTrue(first.await() is SubscriptionEvent.Connection.Connected)
                assertTrue(second.await() is SubscriptionEvent.Connection.Connected)
            }
        } finally {
            client.close()
        }
    }

    /**
     * It uses a configurator if present.
     *
     * This test verifies that a webSocketClientConfigurator can add an interceptor
     * and that it will be run when establishing subscriptions.
     */
    @Test
    fun usesConfiguratorIfPresent() = runTest {
        val counter = AtomicInteger()

        val client = AmplifyAppSyncClient(
            AmplifyAppSyncClient.Configuration {
                endpoint = config.getString("endpoint")
                authorization = AppSyncAuthorization.Single(
                    AppSyncClientAuthorizer.ApiKey(config.getString("apiKey"))
                )
                webSocketClientConfigurator = { builder ->
                    counter.incrementAndGet()
                    builder.addInterceptor { chain ->
                        counter.incrementAndGet()
                        chain.proceed(chain.request())
                    }
                }
            }
        )

        try {
            val eventId = UUID.randomUUID().toString()

            withTimeout(30_000) {
                client.subscribe(subscribeToEventComments(eventId))
                    .first { it is SubscriptionEvent.Connection.Connected }
            }

            // The configurator should have been called (1) and the interceptor should have run (1+)
            assertTrue("Expected configurator and interceptor to run, but counter was ${counter.get()}", counter.get() >= 2)
        } finally {
            client.close()
        }
    }
}
