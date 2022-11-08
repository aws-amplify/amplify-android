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

import com.amplifyframework.auth.AuthException
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import org.junit.Test

class JWTParserTests {

    private val dummyHeader = "{\"typ\":\"JWT\",\"alg\":\"HS256\"}"
    private val dummyPayload = "{\"sub\":\"1234567890\",\"name\":\"John Doe\",\"iat\":1516239022}"
    private val dummyToken = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiIxMjM0NTY3ODkwIiwibmFtZSI6IkpvaG4g" +
        "RG9lIiwiaWF0IjoxNTE2MjM5MDIyfQ.XbPfbIHMI6arZ3Y922BhjWgQzWXcXNrz0ogtVhfEd2o"

    @Test
    fun testGetClaim() {
        val sub = JWTParser.getClaim(dummyToken, "sub")
        assertEquals("1234567890", sub)
    }

    @Test
    fun testGetHeader() {
        val header = JWTParser.getHeader(dummyToken).toString()
        assertEquals(dummyHeader, header)
    }

    @Test
    fun testGetPayload() {
        val payload = JWTParser.getPayload(dummyToken).toString()
        assertEquals(dummyPayload, payload)
    }

    @Test
    fun testHasClaim() {
        val hasName = JWTParser.hasClaim(dummyToken, "name")
        assertTrue(hasName)
    }

    @Test
    fun testHasClaimFail() {
        val hasExpiry = JWTParser.hasClaim(dummyToken, "expiry")
        assertFalse(hasExpiry)
    }

    @Test(expected = AuthException::class)
    fun testInvalidJWT() {
        val invalidToken = "xxxxxx.yyyyyy"
        JWTParser.validateJWT(invalidToken)
    }
}
