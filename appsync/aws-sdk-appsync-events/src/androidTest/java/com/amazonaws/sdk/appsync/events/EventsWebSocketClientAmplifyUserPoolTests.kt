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
package com.amazonaws.sdk.appsync.events

import androidx.test.core.app.ApplicationProvider
import androidx.test.platform.app.InstrumentationRegistry
import com.amazonaws.sdk.appsync.amplify.authorizers.AmplifyUserPoolAuthorizer
import com.amazonaws.sdk.appsync.events.data.EventsException
import com.amazonaws.sdk.appsync.events.data.PublishResult
import com.amazonaws.sdk.appsync.events.test.R
import com.amazonaws.sdk.appsync.events.utils.Credentials
import com.amazonaws.sdk.appsync.events.utils.getEventsConfig
import com.amplifyframework.auth.AuthException
import com.amplifyframework.auth.cognito.AWSCognitoAuthPlugin
import com.amplifyframework.core.configuration.AmplifyOutputs
import com.amplifyframework.kotlin.core.Amplify
import com.amplifyframework.testutils.coroutines.runBlockingWithTimeout
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import java.util.UUID
import kotlinx.serialization.json.JsonPrimitive
import org.junit.BeforeClass
import org.junit.Test

internal class EventsWebSocketClientAmplifyUserPoolTests {
    private val eventsConfig = getEventsConfig(InstrumentationRegistry.getInstrumentation().targetContext)
    private val userPoolAuthorizer = AmplifyUserPoolAuthorizer()
    private val defaultChannel = "default/${UUID.randomUUID()}"
    private val events = Events(eventsConfig.url)

    companion object {
        @BeforeClass
        @JvmStatic
        fun setup() {
            Amplify.addPlugin(AWSCognitoAuthPlugin())
            Amplify.configure(
                AmplifyOutputs.fromResource(R.raw.amplify_outputs),
                ApplicationProvider.getApplicationContext()
            )
        }
    }

    @Test
    fun testFailedPublishWithUnauthenticatedUserPool(): Unit = runBlockingWithTimeout {
        // Publish the message
        val webSocketClient = events.createWebSocketClient(userPoolAuthorizer, userPoolAuthorizer, userPoolAuthorizer)
        val result = webSocketClient.publish(
            channelName = defaultChannel,
            event = JsonPrimitive(true)
        )

        // Assert expected response
        (result is PublishResult.Failure) shouldBe true
        (result as PublishResult.Failure).apply {
            error shouldBe EventsException(
                "An unknown error occurred",
                cause = AuthException("Token is null", "Token received but is null. Check if you are signed in")
            )
        }
    }

    @Test
    fun testPublishWithAuthenticatedUserPool(): Unit = runBlockingWithTimeout {
        val credentials = Credentials.load(InstrumentationRegistry.getInstrumentation().targetContext)
        Amplify.Auth.signIn(credentials.first, credentials.second)

        // Publish the message
        val webSocketClient = events.createWebSocketClient(userPoolAuthorizer, userPoolAuthorizer, userPoolAuthorizer)
        val result = webSocketClient.publish(
            channelName = defaultChannel,
            event = JsonPrimitive(true)
        )

        // Assert expected response
        val response = result.shouldBeInstanceOf<PublishResult.Response>()
        response.failedEvents.shouldBeEmpty()
        response.successfulEvents.shouldHaveSize(1)
        response.successfulEvents.first().index shouldBe 0
    }
}
