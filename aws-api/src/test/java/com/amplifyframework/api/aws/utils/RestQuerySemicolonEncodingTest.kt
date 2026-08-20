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

package com.amplifyframework.api.aws.utils

import com.amplifyframework.api.ApiException
import com.amplifyframework.api.aws.operation.AWSRestOperation
import com.amplifyframework.api.rest.HttpMethod
import com.amplifyframework.api.rest.RestOperationRequest
import com.amplifyframework.api.rest.RestResponse
import com.amplifyframework.testutils.Await
import io.kotest.matchers.shouldBe
import java.io.IOException
import okhttp3.HttpUrl
import okhttp3.OkHttpClient
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/**
 * Tests that a `;` in a REST query parameter value is percent-encoded. API Gateway
 * treats a raw `;` as a parameter delimiter and drops everything after it.
 */
@RunWith(RobolectricTestRunner::class)
class RestQuerySemicolonEncodingTest {
    private lateinit var server: MockWebServer
    private lateinit var baseUrl: HttpUrl
    private lateinit var client: OkHttpClient

    /**
     * Starts a mock web server to capture the outgoing request.
     * @throws IOException On failure to start the server
     */
    @Before
    @Throws(IOException::class)
    fun setup() {
        server = MockWebServer()
        server.start()
        baseUrl = server.url("/")
        server.enqueue(MockResponse().setResponseCode(200).setBody("{}"))
        client = OkHttpClient()
    }

    /**
     * Shuts down the mock web server.
     * @throws IOException On failure to shut down the server
     */
    @After
    @Throws(IOException::class)
    fun cleanup() {
        server.shutdown()
    }

    @Test
    fun `createURL percent-encodes a semicolon in a query value`() {
        val url = RestRequestFactory.createURL(
            "http://amplify-android.com",
            "/path",
            mapOf("filter" to "X;Y")
        )
        url.toString() shouldBe "http://amplify-android.com/path?filter=X%3BY"
    }

    @Test
    fun `createURL preserves a pre-encoded semicolon in a query value`() {
        val url = RestRequestFactory.createURL(
            "http://amplify-android.com",
            "/path",
            mapOf("filter" to "X%3BY")
        )
        url.toString() shouldBe "http://amplify-android.com/path?filter=X%3BY"
    }

    @Test
    fun `semicolon in a query value is percent-encoded on the wire`() {
        val request = RestOperationRequest(HttpMethod.GET, "path", emptyMap(), mapOf("filter" to "X;Y"))
        Await.result<RestResponse, ApiException> { onResult, onError ->
            AWSRestOperation(request, baseUrl.toUrl().toString(), client, onResult, onError).start()
        }
        server.takeRequest().path shouldBe "/path?filter=X%3BY"
    }
}
