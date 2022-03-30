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

import com.amplifyframework.statemachine.codegen.data.CognitoUserPoolTokens
import java.time.Instant
import kotlin.test.assertEquals
import kotlin.test.assertFalse
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
        val expected = Instant.parse("2018-01-17T17:30:22-08:00")
        assertEquals(expected, expiry)
    }

    @Test
    fun testGetUsername() {
        val username = dummyUserPoolTokens.accessToken?.let(SessionHelper::getUsername)
        assertEquals("John Doe", username)
    }

    @Test
    fun testIsInvalid() {
        assertFalse(SessionHelper.isValid(dummyUserPoolTokens))
    }

    @Test
    fun testIsInvalidNullTokens() {
        assertFalse(SessionHelper.isValid(CognitoUserPoolTokens(null, null, null, 0)))
    }
}
