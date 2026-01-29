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

package com.amplifyframework.api.aws

import androidx.test.core.app.ApplicationProvider
import com.amplifyframework.AmplifyException
import com.amplifyframework.api.ApiException
import com.amplifyframework.api.graphql.model.ModelQuery
import com.amplifyframework.testutils.Await
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import java.io.IOException
import okhttp3.HttpUrl
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.json.JSONException
import org.json.JSONObject
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/**
 * Integration tests for error cause propagation in GraphQL operations.
 * Tests that exception causes are properly preserved when API calls fail.
 */
@RunWith(RobolectricTestRunner::class)
class AppSyncErrorPropagationTest {
    private lateinit var webServer: MockWebServer
    private lateinit var baseUrl: HttpUrl
    private lateinit var plugin: AWSApiPlugin

    /**
     * Sets up the test with a mock web server.
     * @throws IOException On failure to start web server
     * @throws JSONException On failure to arrange configuration JSON
     * @throws AmplifyException On failure to configure plugin
     */
    @Before
    @Throws(AmplifyException::class, IOException::class, JSONException::class)
    fun setup() {
        webServer = MockWebServer()
        webServer.start(8080)
        baseUrl = webServer.url("/")

        val configuration = JSONObject()
            .put(
                "graphQlApi",
                JSONObject()
                    .put("endpointType", "GraphQL")
                    .put("endpoint", baseUrl.toUrl())
                    .put("region", "us-east-1")
                    .put("authorizationType", "API_KEY")
                    .put("apiKey", "FAKE-API-KEY")
            )

        plugin = AWSApiPlugin.builder().build()
        plugin.configure(configuration, ApplicationProvider.getApplicationContext())
    }

    /**
     * Stop the [MockWebServer] that was started in [setup].
     * @throws IOException On failure to shutdown the MockWebServer
     */
    @After
    @Throws(IOException::class)
    fun cleanup() {
        webServer.shutdown()
    }

    /**
     * Tests that query 401 errors preserve the GraphQL error response as the cause.
     * The cause should be a GraphQLResponseException containing the error details.
     */
    @Test
    fun queryUnauthorizedErrorPreservesCause() {
        // Arrange - mock a 401 response with GraphQL error payload
        val errorResponse = """
            {
              "errors": [{
                "errorType": "UnauthorizedException",
                "message": "You are not authorized to make this call."
              }]
            }
        """.trimIndent()

        webServer.enqueue(
            MockResponse()
                .setResponseCode(401)
                .setBody(errorResponse)
        )

        // Act - execute query and capture error
        val error = Await.error { onResult, onError ->
            plugin.query(ModelQuery.list(Todo::class.java), onResult, onError)
        }

        // Assert - verify error has proper cause chain
        error.shouldNotBeNull()
        error.shouldBeInstanceOf<ApiException.NonRetryableException>()

        val cause = error.cause
        cause.shouldNotBeNull()
        cause.shouldBeInstanceOf<GraphQLResponseException>()

        cause.errors.size.shouldBe(1)

        val firstError = cause.errors[0]
        firstError.errorType.shouldBe("UnauthorizedException")
        firstError.message.shouldBe("You are not authorized to make this call.")
    }

    /**
     * Tests that query errors with invalid JSON fall back to IOException with JSONException cause.
     */
    @Test
    fun queryInvalidJsonPreservesJsonException() {
        // Arrange - mock a 400 response with invalid JSON
        val invalidJson = "not valid json at all"

        webServer.enqueue(
            MockResponse()
                .setResponseCode(400)
                .setBody(invalidJson)
        )

        // Act
        val error = Await.error { onResult, onError ->
            plugin.query(ModelQuery.list(Todo::class.java), onResult, onError)
        }

        // Assert
        error.shouldNotBeNull()
        val cause = error.cause
        cause.shouldNotBeNull()
        cause.shouldBeInstanceOf<IOException>()

        // The IOException should have a JSONException as its cause
        val jsonCause = cause.cause
        jsonCause.shouldNotBeNull()
        jsonCause.shouldBeInstanceOf<JSONException>()
    }
}
