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
import com.amazonaws.sdk.appsync.core.authorizers.ApiKeyAuthorizer
import com.amazonaws.sdk.appsync.events.data.PublishResult
import com.amazonaws.sdk.appsync.events.utils.getEventsConfig
import com.amplifyframework.auth.cognito.AWSCognitoAuthPlugin
import com.amplifyframework.core.Amplify
import com.amplifyframework.core.configuration.AmplifyOutputs
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.JsonPrimitive
import org.junit.BeforeClass
import org.junit.Test
import java.util.UUID
import com.amazonaws.sdk.appsync.events.test.R

internal class EventsRestClientAmplifyApiKeyTests {
    private val eventsConfig = getEventsConfig(InstrumentationRegistry.getInstrumentation().targetContext)
    private val apiKeyAuthorizer = ApiKeyAuthorizer(eventsConfig.apiKey)
    private val defaultChannel = "default/${UUID.randomUUID()}"
    private val events = Events(eventsConfig.url)

    companion object {
        @BeforeClass
        @JvmStatic
        fun setup() {
            Amplify.addPlugin(AWSCognitoAuthPlugin())
            Amplify.configure(
                AmplifyOutputs.fromResource(R.raw.amplify_outputs),
                ApplicationProvider.getApplicationContext())
        }
    }

    @Test
    fun testPublishWithApiKey(): Unit = runTest {
        // Publish the REST message
        val restClient = events.createRestClient(publishAuthorizer = apiKeyAuthorizer)
        val result = restClient.publish(
            channelName = defaultChannel,
            event = JsonPrimitive(true)
        )

        // Assert expected REST response
        (result is PublishResult.Response) shouldBe true
        (result as PublishResult.Response).apply {
            failedEvents.size shouldBe 0
            successfulEvents.size shouldBe 1
            successfulEvents[0].apply {
                index shouldBe 0
            }
        }
    }
}

