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
package com.amplifyframework.connect.internal

import com.amplifyframework.connect.ConnectAccessDeniedException
import com.amplifyframework.connect.ConnectNetworkException
import com.amplifyframework.connect.ConnectNotSignedInException
import com.amplifyframework.connect.ConnectServiceException
import com.amplifyframework.connect.ConnectSession
import com.amplifyframework.connect.ConnectThrottlingException
import com.amplifyframework.connect.ConnectValidationException
import com.amplifyframework.foundation.credentials.AwsCredentials
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import kotlinx.coroutines.test.runTest
import mockwebserver3.MockResponse
import mockwebserver3.MockWebServer
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@OptIn(kotlin.time.ExperimentalTime::class)
@RunWith(RobolectricTestRunner::class)
class IdentifyUserServiceTest {

    private lateinit var server: MockWebServer
    private lateinit var service: IdentifyUserService

    @Before
    fun setup() {
        server = MockWebServer()
        server.start()
        service = IdentifyUserService(
            endpoint = server.url("").toString().trimEnd('/'),
            region = "us-east-1"
        )
    }

    @After
    fun teardown() {
        server.shutdown()
    }

    @Test
    fun `authenticated path sends POST to identify-user with bearer token`() = runTest {
        server.enqueue(MockResponse().setResponseCode(200).setBody("{}"))

        val session = ConnectSession(accessToken = "my-token")
        service.identify(session, """{"userId":"u1"}""")

        val request = server.takeRequest()
        request.method shouldBe "POST"
        request.path shouldBe "/identify-user"
        request.getHeader("Authorization") shouldBe "Bearer my-token"
        request.getHeader("Content-Type") shouldContain "application/json"
        request.body.readUtf8() shouldBe """{"userId":"u1"}"""
    }

    @Test
    fun `guest path sends POST to identify-user-guest with SigV4 headers`() = runTest {
        server.enqueue(MockResponse().setResponseCode(200).setBody("{}"))

        val session = ConnectSession(
            credentials = AwsCredentials.Temporary(
                accessKeyId = "AKID",
                secretAccessKey = "secret",
                sessionToken = "session",
                expiration = kotlin.time.Instant.DISTANT_FUTURE
            )
        )
        service.identify(session, """{"userId":"g1"}""")

        val request = server.takeRequest()
        request.method shouldBe "POST"
        request.path shouldBe "/identify-user-guest"
        // SigV4 adds Authorization header with AWS4-HMAC-SHA256
        request.getHeader("Authorization")!! shouldContain "AWS4-HMAC-SHA256"
        request.getHeader("X-Amz-Security-Token") shouldBe "session"
    }

    @Test
    fun `throws NotSignedInException when no token and no credentials`() = runTest {
        val session = ConnectSession()
        shouldThrow<ConnectNotSignedInException> {
            service.identify(session, "{}")
        }
    }

    @Test
    fun `maps 429 to ThrottlingException`() = runTest {
        server.enqueue(MockResponse().setResponseCode(429).setBody("{}"))

        shouldThrow<ConnectThrottlingException> {
            service.identify(ConnectSession(accessToken = "t"), "{}")
        }
    }

    @Test
    fun `maps 401 to AccessDeniedException`() = runTest {
        server.enqueue(MockResponse().setResponseCode(401).setBody("{}"))

        shouldThrow<ConnectAccessDeniedException> {
            service.identify(ConnectSession(accessToken = "t"), "{}")
        }
    }

    @Test
    fun `maps 403 to AccessDeniedException`() = runTest {
        server.enqueue(MockResponse().setResponseCode(403).setBody("{}"))

        shouldThrow<ConnectAccessDeniedException> {
            service.identify(ConnectSession(accessToken = "t"), "{}")
        }
    }

    @Test
    fun `maps 400 to ValidationException with error detail`() = runTest {
        server.enqueue(
            MockResponse().setResponseCode(400)
                .setBody("""{"error":"Invalid userId"}""")
        )

        val ex = shouldThrow<ConnectValidationException> {
            service.identify(ConnectSession(accessToken = "t"), "{}")
        }
        ex.message shouldContain "Invalid userId"
    }

    @Test
    fun `maps 500 to ServiceException`() = runTest {
        server.enqueue(
            MockResponse().setResponseCode(500)
                .setBody("""{"message":"Internal error"}""")
        )

        val ex = shouldThrow<ConnectServiceException> {
            service.identify(ConnectSession(accessToken = "t"), "{}")
        }
        ex.message shouldContain "Internal error"
    }

    @Test
    fun `network failure throws ConnectNetworkException`() = runTest {
        server.shutdown() // force connection refused
        val brokenService = IdentifyUserService(
            endpoint = "http://localhost:1",
            region = "us-east-1"
        )

        shouldThrow<ConnectNetworkException> {
            brokenService.identify(ConnectSession(accessToken = "t"), "{}")
        }
    }
}
