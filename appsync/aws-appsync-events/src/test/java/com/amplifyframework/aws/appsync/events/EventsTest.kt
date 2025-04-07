/*
 * Copyright 2025 Amazon.com, Inc. or its affiliates. All Rights Reserved.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License").
 *  You may not use this file except in compliance with the License.
 *  A copy of the License is located at
 *
 *   http://aws.amazon.com/apache2.0
 *
 *  or in the "license" file accompanying this file. This file is distributed
 *  on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either
 *  express or implied. See the License for the specific language governing
 *  permissions and limitations under the License.
 */
package com.amplifyframework.aws.appsync.events

import com.amplifyframework.aws.appsync.core.AppSyncAuthorizer
import com.amplifyframework.aws.appsync.core.AppSyncRequest
import com.amplifyframework.aws.appsync.core.authorizers.ApiKeyAuthorizer
import com.amplifyframework.aws.appsync.events.data.ChannelAuthorizers
import com.amplifyframework.aws.appsync.events.data.PublishResult
import com.amplifyframework.aws.appsync.events.utils.HeaderKeys
import com.amplifyframework.aws.appsync.events.utils.HeaderValues
import io.kotest.matchers.maps.shouldContainAll
import io.kotest.matchers.shouldBe
import java.io.IOException
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.JsonPrimitive
import mockwebserver3.MockResponse
import mockwebserver3.MockWebServer
import okhttp3.HttpUrl
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okio.Buffer
import org.junit.After
import org.junit.Before
import org.junit.Test

class EventsTest {
    private val mockWebServer = MockWebServer()
    private val expectedEndpoint = "https://abc.appsync-api.us-east-1.amazonaws.com/event"
    private val expectedHost = "abc.appsync-api.us-east-1.amazonaws.com"
    private val expectedStandardHeaders = mapOf(
        HeaderKeys.HOST to expectedHost,
        HeaderKeys.CONTENT_TYPE to HeaderValues.CONTENT_TYPE_APPLICATION_JSON,
        HeaderKeys.ACCEPT to HeaderValues.ACCEPT_APPLICATION_JSON,
    )
    private val interceptor = ConvertToMockRequestInterceptor(mockWebServer.url("/event"))
    private val events = Events(
        endpoint = expectedEndpoint,
        connectAuthorizer = ApiKeyAuthorizer("abc"),
        defaultChannelAuthorizers = ChannelAuthorizers(
            subscribeAuthorizer = ApiKeyAuthorizer("123"),
            publishAuthorizer = TestAuthorizer()
        ),
        okHttpClient = OkHttpClient.Builder()
            .addInterceptor(interceptor)
            .build()
    )

    @Before
    fun setUp() {
        mockWebServer.start()
    }

    @After
    fun tearDown() {
        mockWebServer.shutdown()
    }

    @Test
    fun `single publish with default authorizer`() = runTest {
        // GIVEN
        val expectedChannel = "default/testChannel"
        val expectedRequestBody = "{\"channel\":\"default/testChannel\",\"events\":[\"1\"]}"
        val responseBody = javaClass.classLoader!!
            .getResourceAsStream("publish_single_success.json")
            .bufferedReader()
            .readText()
        mockWebServer.enqueue(
            MockResponse()
                .setResponseCode(200)
                .setBody(responseBody)
        )

        // WHEN
        val response = events.publish(expectedChannel, JsonPrimitive(1))

        // THEN
        interceptor.originalRequests.first().let {
            it.method shouldBe "POST"
            it.headers.toMap().apply {
                this shouldContainAll TestAuthorizer().expectedHeaders
                this shouldContainAll expectedStandardHeaders
            }
            val actualRequestBody = Buffer().let { buffer ->
                it.body!!.writeTo(buffer)
                buffer.readUtf8()
            }
            actualRequestBody shouldBe expectedRequestBody
        }
        response.apply {
            status shouldBe PublishResult.Status.Successful
            failedEvents.size shouldBe 0
            successfulEvents.size shouldBe 1
        }
    }

    @Test
    fun `single publish with override authorizer`() = runTest {
        // GIVEN
        val overrideAuthorizer = TestAuthorizer("override")
        val expectedChannel = "default/testChannel"
        val expectedRequestBody = "{\"channel\":\"default/testChannel\",\"events\":[\"1\"]}"
        val responseBody = javaClass.classLoader!!
            .getResourceAsStream("publish_single_success.json")
            .bufferedReader()
            .readText()
        mockWebServer.enqueue(
            MockResponse()
                .setResponseCode(200)
                .setBody(responseBody)
        )

        // WHEN
        val response = events.publish(expectedChannel, JsonPrimitive(1), overrideAuthorizer)

        // THEN
        interceptor.originalRequests.first().let {
            it.method shouldBe "POST"
            it.headers.toMap().apply {
                this shouldContainAll overrideAuthorizer.expectedHeaders
                this shouldContainAll expectedStandardHeaders
            }
            val actualRequestBody = Buffer().let { buffer ->
                it.body!!.writeTo(buffer)
                buffer.readUtf8()
            }
            actualRequestBody shouldBe expectedRequestBody
        }
        response.apply {
            status shouldBe PublishResult.Status.Successful
            failedEvents.size shouldBe 0
            successfulEvents.size shouldBe 1
        }
    }

    @Test
    fun `multi publish with default authorizer`() = runTest {
        // GIVEN
        val expectedChannel = "default/testChannel"
        val expectedRequestBody = "{\"channel\":\"default/testChannel\",\"events\":[\"1\",\"2\"]}"
        val responseBody = javaClass.classLoader!!
            .getResourceAsStream("publish_single_success.json")
            .bufferedReader()
            .readText()
        mockWebServer.enqueue(
            MockResponse()
                .setResponseCode(200)
                .setBody(responseBody)
        )

        // WHEN
        val response = events.publish(expectedChannel, listOf(JsonPrimitive(1), JsonPrimitive(2)))

        // THEN
        interceptor.originalRequests.first().let {
            it.method shouldBe "POST"
            it.headers.toMap().apply {
                this shouldContainAll TestAuthorizer().expectedHeaders
                this shouldContainAll expectedStandardHeaders
            }
            val actualRequestBody = Buffer().let { buffer ->
                it.body!!.writeTo(buffer)
                buffer.readUtf8()
            }
            actualRequestBody shouldBe expectedRequestBody
        }
        response.apply {
            status shouldBe PublishResult.Status.Successful
            failedEvents.size shouldBe 0
            successfulEvents.size shouldBe 1
        }
    }

    @Test
    fun `multi publish with partial success`() = runTest {
        // GIVEN
        val expectedChannel = "default/testChannel"
        val expectedRequestBody = "{\"channel\":\"default/testChannel\",\"events\":[\"1\",\"2\"]}"
        val responseBody = javaClass.classLoader!!
            .getResourceAsStream("publish_multi_partial_success.json")
            .bufferedReader()
            .readText()
        mockWebServer.enqueue(
            MockResponse()
                .setResponseCode(200)
                .setBody(responseBody)
        )

        // WHEN
        val response = events.publish(expectedChannel, listOf(JsonPrimitive(1), JsonPrimitive(2)))

        // THEN
        interceptor.originalRequests.first().let {
            it.method shouldBe "POST"
            it.headers.toMap().apply {
                this shouldContainAll TestAuthorizer().expectedHeaders
                this shouldContainAll expectedStandardHeaders
            }
            val actualRequestBody = Buffer().let { buffer ->
                it.body!!.writeTo(buffer)
                buffer.readUtf8()
            }
            actualRequestBody shouldBe expectedRequestBody
        }
        response.apply {
            status shouldBe PublishResult.Status.PartialSuccess
            failedEvents.size shouldBe 1
            successfulEvents.size shouldBe 1
        }
    }

    @Test
    fun `multi publish with complete failure`() = runTest {
        // GIVEN
        val expectedChannel = "default/testChannel"
        val expectedRequestBody = "{\"channel\":\"default/testChannel\",\"events\":[\"1\",\"2\"]}"
        val responseBody = javaClass.classLoader!!
            .getResourceAsStream("publish_multi_failure.json")
            .bufferedReader()
            .readText()
        mockWebServer.enqueue(
            MockResponse()
                .setResponseCode(200)
                .setBody(responseBody)
        )

        // WHEN
        val response = events.publish(expectedChannel, listOf(JsonPrimitive(1), JsonPrimitive(2)))

        // THEN
        interceptor.originalRequests.first().let {
            it.method shouldBe "POST"
            it.headers.toMap().apply {
                this shouldContainAll TestAuthorizer().expectedHeaders
                this shouldContainAll expectedStandardHeaders
            }
            val actualRequestBody = Buffer().let { buffer ->
                it.body!!.writeTo(buffer)
                buffer.readUtf8()
            }
            actualRequestBody shouldBe expectedRequestBody
        }
        response.apply {
            status shouldBe PublishResult.Status.Failed
            failedEvents.size shouldBe 2
            failedEvents[0].errorMessage shouldBe "error1"
            failedEvents[1].errorMessage shouldBe "error2"
        }
    }

    @Test
    fun `multi publish with override authorizer`() = runTest {
        // GIVEN
        val overrideAuthorizer = TestAuthorizer("override")
        val expectedChannel = "default/testChannel"
        val expectedRequestBody = "{\"channel\":\"default/testChannel\",\"events\":[\"1\",\"2\"]}"
        val responseBody = javaClass.classLoader!!
            .getResourceAsStream("publish_single_success.json")
            .bufferedReader()
            .readText()
        mockWebServer.enqueue(
            MockResponse()
                .setResponseCode(200)
                .setBody(responseBody)
        )

        // WHEN
        val response = events.publish(
            expectedChannel,
            listOf(JsonPrimitive(1), JsonPrimitive(2)),
            overrideAuthorizer
        )

        // THEN
        interceptor.originalRequests.first().let {
            it.method shouldBe "POST"
            it.headers.toMap().apply {
                this shouldContainAll overrideAuthorizer.expectedHeaders
                this shouldContainAll expectedStandardHeaders
            }
            val actualRequestBody = Buffer().let { buffer ->
                it.body!!.writeTo(buffer)
                buffer.readUtf8()
            }
            actualRequestBody shouldBe expectedRequestBody
        }
        response.apply {
            status shouldBe PublishResult.Status.Successful
            failedEvents.size shouldBe 0
            successfulEvents.size shouldBe 1
        }
    }
}

private class ConvertToMockRequestInterceptor(private val mockUrl: HttpUrl) : Interceptor {
    val originalRequests = mutableListOf<Request>()

    @Throws(IOException::class)
    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        originalRequests.add(request)
        return chain.proceed(request.newBuilder().url(mockUrl).build())
    }
}

private class TestAuthorizer(testKeyValue: String = "default") : AppSyncAuthorizer {
    val expectedHeaders = mapOf("testKey" to testKeyValue)
    override suspend fun getAuthorizationHeaders(request: AppSyncRequest) = expectedHeaders
}
