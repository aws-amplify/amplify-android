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
import com.amplifyframework.connect.ConnectServiceException
import com.amplifyframework.connect.ConnectThrottlingException
import com.amplifyframework.connect.ConnectValidationException
import com.amplifyframework.foundation.credentials.AwsCredentials
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import java.time.ZoneOffset
import java.time.ZonedDateTime
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
class ConnectServiceTest {

    private lateinit var server: MockWebServer
    private lateinit var service: ConnectService
    private val testCredentials = AwsCredentials.Temporary(
        accessKeyId = "AKID",
        secretAccessKey = "secret",
        sessionToken = "session",
        expiration = kotlin.time.Instant.DISTANT_FUTURE
    )

    @Before
    fun setup() {
        server = MockWebServer()
        server.start()
        service = ConnectService(
            endpoint = server.url("").toString().trimEnd('/'),
            region = "us-east-1"
        )
    }

    @After
    fun teardown() {
        server.shutdown()
    }

    @Test
    fun `identifyUser sends POST to identify-user with SigV4`() = runTest {
        server.enqueue(MockResponse().setResponseCode(200).setBody("{}"))

        service.identifyUser(testCredentials, """{"userProfile":{"name":"Alice"}}""")

        val request = server.takeRequest()
        request.method shouldBe "POST"
        request.path shouldBe "/identify-user"
        request.getHeader("Authorization")!! shouldContain "AWS4-HMAC-SHA256"
        request.getHeader("X-Amz-Security-Token") shouldBe "session"
        request.body.readUtf8() shouldBe """{"userProfile":{"name":"Alice"}}"""
    }

    @Test
    fun `registerDevice sends POST to register-device with SigV4`() = runTest {
        server.enqueue(MockResponse().setResponseCode(200).setBody("{}"))

        service.registerDevice(testCredentials, """{"device":{"token":"t","deviceId":"d"}}""")

        val request = server.takeRequest()
        request.method shouldBe "POST"
        request.path shouldBe "/register-device"
        request.getHeader("Authorization")!! shouldContain "AWS4-HMAC-SHA256"
    }

    @Test
    fun `removeDevice sends POST to remove-device with SigV4`() = runTest {
        server.enqueue(MockResponse().setResponseCode(200).setBody("{}"))

        service.removeDevice(testCredentials, """{"deviceId":"d"}""")

        val request = server.takeRequest()
        request.method shouldBe "POST"
        request.path shouldBe "/remove-device"
        request.getHeader("Authorization")!! shouldContain "AWS4-HMAC-SHA256"
    }

    @Test
    fun `requests carry the Amplify connect user agent`() = runTest {
        server.enqueue(MockResponse().setResponseCode(200).setBody("{}"))

        service.identifyUser(testCredentials, "{}")

        val userAgent = server.takeRequest().getHeader("User-Agent")!!
        userAgent shouldContain "md/amplify-connect#"
        userAgent shouldContain "lib/amplify-android#"
    }

    @Test
    fun `buildAuthorizationHeader produces the expected SigV4 signature`() {
        // Known-answer test. The expected Signature was computed independently with a
        // reference SigV4 implementation (Python hmac/sha256) over the same canonical
        // inputs, so it verifies the signing math rather than echoing our own output:
        //   POST /identify-user, host connect.example.com, region us-east-1,
        //   service execute-api, x-amz-date 20230101T000000Z,
        //   body {"userProfile":{"name":"Alice"}}, static creds AKIDEXAMPLE / secret.
        val service = ConnectService(endpoint = "https://connect.example.com", region = "us-east-1")
        val credentials = AwsCredentials.Static(
            accessKeyId = "AKIDEXAMPLE",
            secretAccessKey = "wJalrXUtnFEMI/K7MDENG+bPxRfiCYEXAMPLEKEY"
        )
        val timestamp = ZonedDateTime.of(2023, 1, 1, 0, 0, 0, 0, ZoneOffset.UTC)

        val header = service.buildAuthorizationHeader(
            url = "https://connect.example.com/identify-user",
            credentials = credentials,
            body = """{"userProfile":{"name":"Alice"}}""",
            timestamp = timestamp
        )

        header shouldBe (
            "AWS4-HMAC-SHA256 Credential=AKIDEXAMPLE/20230101/us-east-1/execute-api/aws4_request, " +
                "SignedHeaders=content-type;host;x-amz-date, " +
                "Signature=4993af8526864666b5158fc626c65e5f34b6dfc7fb6f1046b5af042ae5a33b0d"
            )
    }

    @Test
    fun `maps 429 to ThrottlingException`() = runTest {
        server.enqueue(MockResponse().setResponseCode(429).setBody("{}"))

        shouldThrow<ConnectThrottlingException> {
            service.identifyUser(testCredentials, "{}")
        }
    }

    @Test
    fun `maps 401 to AccessDeniedException`() = runTest {
        server.enqueue(MockResponse().setResponseCode(401).setBody("{}"))

        shouldThrow<ConnectAccessDeniedException> {
            service.identifyUser(testCredentials, "{}")
        }
    }

    @Test
    fun `maps 403 to AccessDeniedException`() = runTest {
        server.enqueue(MockResponse().setResponseCode(403).setBody("{}"))

        shouldThrow<ConnectAccessDeniedException> {
            service.identifyUser(testCredentials, "{}")
        }
    }

    @Test
    fun `maps 400 to ValidationException with detail`() = runTest {
        server.enqueue(
            MockResponse().setResponseCode(400).setBody("""{"error":"Bad input"}""")
        )

        val ex = shouldThrow<ConnectValidationException> {
            service.identifyUser(testCredentials, "{}")
        }
        ex.message shouldContain "Bad input"
    }

    @Test
    fun `maps 500 to ServiceException`() = runTest {
        server.enqueue(
            MockResponse().setResponseCode(500).setBody("""{"message":"Internal"}""")
        )

        val ex = shouldThrow<ConnectServiceException> {
            service.identifyUser(testCredentials, "{}")
        }
        ex.message shouldContain "Internal"
    }

    @Test
    fun `network failure throws ConnectNetworkException`() = runTest {
        server.shutdown()
        val brokenService = ConnectService(
            endpoint = "http://localhost:1",
            region = "us-east-1"
        )

        shouldThrow<ConnectNetworkException> {
            brokenService.identifyUser(testCredentials, "{}")
        }
    }
}
