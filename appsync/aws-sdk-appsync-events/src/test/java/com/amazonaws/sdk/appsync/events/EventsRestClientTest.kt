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

import com.amazonaws.sdk.appsync.core.AppSyncAuthorizer
import com.amazonaws.sdk.appsync.core.AppSyncRequest
import com.amazonaws.sdk.appsync.events.data.BadRequestException
import com.amazonaws.sdk.appsync.events.data.EventsException
import com.amazonaws.sdk.appsync.events.data.PublishResult
import com.amazonaws.sdk.appsync.events.utils.HeaderKeys
import com.amazonaws.sdk.appsync.events.utils.HeaderValues
import io.kotest.assertions.fail
import io.kotest.matchers.maps.shouldContainAll
import io.kotest.matchers.shouldBe
import java.io.IOException
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.JsonPrimitive
import mockwebserver3.MockResponse
import mockwebserver3.MockWebServer
import mockwebserver3.SocketPolicy
import okhttp3.HttpUrl
import okhttp3.Interceptor
import okhttp3.Request
import okhttp3.Response
import okio.Buffer
import org.junit.After
import org.junit.Before
import org.junit.Test

class EventsRestClientTest {
    private val mockWebServer = MockWebServer()
    private val expectedEndpoint = "https://abc.appsync-api.us-east-1.amazonaws.com/event"
    private val expectedHost = "abc.appsync-api.us-east-1.amazonaws.com"
    private val expectedStandardHeaders = mapOf(
        HeaderKeys.HOST to expectedHost,
        HeaderKeys.CONTENT_TYPE to HeaderValues.CONTENT_TYPE_APPLICATION_JSON,
        HeaderKeys.ACCEPT to HeaderValues.ACCEPT_APPLICATION_JSON,
        HeaderKeys.X_AMZ_USER_AGENT to HeaderValues.USER_AGENT
    )
    private val interceptor = ConvertToMockRequestInterceptor(mockWebServer.url("/event"))
    private val events = Events(endpoint = expectedEndpoint)
    private val client = events.createRestClient(
        TestAuthorizer(),
        Events.Options.Rest(
            okHttpConfigurationProvider = {
                it.addInterceptor(interceptor)
                it.writeTimeout(100, TimeUnit.MILLISECONDS)
            },
            loggerProvider = null
        )
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
        val response = client.publish(expectedChannel, JsonPrimitive(1))

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

        if (response !is PublishResult.Response) {
            fail("Unexpected PublishResult type")
        }

        response.apply {
            status shouldBe PublishResult.Response.Status.Successful
            failedEvents.size shouldBe 0
            successfulEvents.size shouldBe 1
        }
    }

    @Test
    fun `single publish with network exception`() = runTest {
        // GIVEN
        val expectedChannel = "default/testChannel"
        val expectedRequestBody = "{\"channel\":\"default/testChannel\",\"events\":[\"1\"]}"
        val responseBody = javaClass.classLoader!!
            .getResourceAsStream("publish_single_success.json")
            .bufferedReader()
            .readText()

        mockWebServer.enqueue(
            MockResponse()
                .setSocketPolicy(SocketPolicy.DISCONNECT_AT_START)
                .setResponseCode(200)
                .setBody(responseBody)
        )

        val response = client.publish(expectedChannel, JsonPrimitive(1))

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

        if (response !is PublishResult.Failure) {
            fail("Unexpected PublishResult type")
        }
    }

    @Test
    fun `single publish with parseable failure`() = runTest {
        // GIVEN
        val expectedChannel = "default/testChannel"
        val expectedRequestBody = "{\"channel\":\"default/testChannel\",\"events\":[\"1\"]}"
        val responseBody = javaClass.classLoader!!
            .getResourceAsStream("publish_errors.json")
            .bufferedReader()
            .readText()

        mockWebServer.enqueue(
            MockResponse()
                .setResponseCode(400)
                .setBody(responseBody)
        )

        val response = client.publish(expectedChannel, JsonPrimitive(1))

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

        if (response !is PublishResult.Failure) {
            fail("Unexpected PublishResult type")
        }

        response.apply {
            error::class shouldBe BadRequestException::class
            error.message shouldBe "Input exceeded 5 event limit"
        }
    }

    @Test
    fun `single publish with unknown body failure`() = runTest {
        // GIVEN
        val expectedChannel = "default/testChannel"
        val expectedRequestBody = "{\"channel\":\"default/testChannel\",\"events\":[\"1\"]}"
        val responseBody = "uh-oh"

        mockWebServer.enqueue(
            MockResponse()
                .setResponseCode(400)
                .setBody(responseBody)
        )

        val response = client.publish(expectedChannel, JsonPrimitive(1))

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

        if (response !is PublishResult.Failure) {
            fail("Unexpected PublishResult type")
        }

        response.apply {
            error::class shouldBe EventsException::class
            error.message shouldBe "Failed to post event(s)"
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
        val response = client.publish(expectedChannel, JsonPrimitive(1), overrideAuthorizer)

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

        if (response !is PublishResult.Response) {
            fail("Unexpected PublishResult type")
        }

        response.apply {
            status shouldBe PublishResult.Response.Status.Successful
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
        val response = client.publish(expectedChannel, listOf(JsonPrimitive(1), JsonPrimitive(2)))

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

        if (response !is PublishResult.Response) {
            fail("Unexpected PublishResult type")
        }

        response.apply {
            status shouldBe PublishResult.Response.Status.Successful
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
        val response = client.publish(expectedChannel, listOf(JsonPrimitive(1), JsonPrimitive(2)))

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
        if (response !is PublishResult.Response) {
            fail("Unexpected PublishResult type")
        }
        response.apply {
            status shouldBe PublishResult.Response.Status.PartialSuccess
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
        val response = client.publish(expectedChannel, listOf(JsonPrimitive(1), JsonPrimitive(2)))

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
        if (response !is PublishResult.Response) {
            fail("Unexpected PublishResult type")
        }
        response.apply {
            status shouldBe PublishResult.Response.Status.Failed
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
        val response = client.publish(
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
        if (response !is PublishResult.Response) {
            fail("Unexpected PublishResult type")
        }
        response.apply {
            status shouldBe PublishResult.Response.Status.Successful
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
