/*
 * Copyright 2022 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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

package com.amplifyframework.auth.cognito.helpers

import com.amplifyframework.statemachine.codegen.data.AWSCredentials
import com.amplifyframework.statemachine.codegen.data.CognitoUserPoolTokens
import java.time.Instant
import java.time.temporal.ChronoUnit
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import org.junit.Test

class SessionHelperTests {

    private val dummyToken = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJ1c2VybmFtZSI6IkpvaG4gRG9lIiwiZXhwIjoxNTE2Mj" +
        "M5MDIyfQ.e4RpZTfAb3oXkfq3IwHtR_8Zhn0U1JDV7McZPlBXyhw"

    private val dummyUserPoolTokens = CognitoUserPoolTokens(
        idToken = dummyToken,
        accessToken = dummyToken,
        refreshToken = "",
        expiration = 0
    )

    @Test
    fun testGetExpiration() {
        val expiry = SessionHelper.getExpiration(dummyToken)
        val expected = Instant.parse("2018-01-18T01:30:22Z")
        assertEquals(expected, expiry)
    }

    @Test
    fun testGetUsername() {
        val username = dummyUserPoolTokens.accessToken?.let(SessionHelper::getUsername)
        assertEquals("John Doe", username)
    }

    @Test
    fun testIsInvalid() {
        assertFalse(SessionHelper.isValidTokens(dummyUserPoolTokens))
    }

    @Test
    fun testIsInvalidNullTokens() {
        assertFalse(SessionHelper.isValidTokens(CognitoUserPoolTokens(null, null, null, 0)))
    }

    @Test
    fun testsIsInvalidSession() {
        assertFalse(SessionHelper.isValidSession(AWSCredentials.empty))
    }

    @Test
    fun `Pulling a V1 credential should fail isValidSession check`() {
        // Expiration is encoded in ms to simulate v1 > v2 migration issue
        assertFalse(
            SessionHelper.isValidSession(
                AWSCredentials(
                    accessKeyId = dummyToken,
                    secretAccessKey = dummyToken,
                    sessionToken = dummyToken,
                    expiration = Instant.now().plus(30, ChronoUnit.MINUTES).toEpochMilli()
                )
            )
        )
    }

    @Test
    fun `Session with an expiration in the past should fail isValidSession check`() {
        assertFalse(
            SessionHelper.isValidSession(
                AWSCredentials(
                    accessKeyId = dummyToken,
                    secretAccessKey = dummyToken,
                    sessionToken = dummyToken,
                    expiration = Instant.now().minus(1, ChronoUnit.MINUTES).epochSecond
                )
            )
        )
    }

    @Test
    fun `Session with an expiration in the future should pass isValidSession check`() {
        assertTrue(
            SessionHelper.isValidSession(
                AWSCredentials(
                    accessKeyId = dummyToken,
                    secretAccessKey = dummyToken,
                    sessionToken = dummyToken,
                    expiration = Instant.now().plus(1, ChronoUnit.MINUTES).epochSecond
                )
            )
        )
    }
}
