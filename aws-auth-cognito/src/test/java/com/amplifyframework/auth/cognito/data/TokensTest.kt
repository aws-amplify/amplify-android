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

package com.amplifyframework.auth.cognito.data

import com.amplifyframework.statemachine.codegen.data.asAccessToken
import com.amplifyframework.statemachine.codegen.data.asIdToken
import io.kotest.matchers.shouldBe
import java.time.Instant
import org.junit.Test

class TokensTest {
    private val accessTokenPayload =
        "{\"sub\":\"1234567890\",\"name\":\"John Doe\",\"username\":\"jdoe\",\"iat\":1756998278,\"exp\":1756998578," +
            "\"origin_jti\": \"aaaaaaaa-bbbb-cccc-dddd-eeeeeeeeeeee\"}"
    private val accessToken =
        "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiIxMjM0NTY3ODkwIiwibmFtZSI6IkpvaG4gRG9lIiwidXNlcm5hbWUiOiJqZG" +
            "9lIiwiaWF0IjoxNzU2OTk4Mjc4LCJleHAiOjE3NTY5OTg1NzgsIm9yaWdpbl9qdGkiOiJhYWFhYWFhYS1iYmJiLWNjY2MtZGRkZC1lZ" +
            "WVlZWVlZWVlZWUifQ.3Mvd5WVi1z1GpQ37hEoev6DzYNv9lWNL-fGfQTxUYx4"

    @Test
    fun `identity token returns expiry`() {
        val token = accessToken.asIdToken()
        token?.expiration shouldBe Instant.ofEpochSecond(1756998578)
    }

    @Test
    fun `access token returns expiry`() {
        val token = accessToken.asAccessToken()
        token?.expiration shouldBe Instant.ofEpochSecond(1756998578)
    }

    @Test
    fun `access token returns username`() {
        val token = accessToken.asAccessToken()
        token?.username shouldBe "jdoe"
    }

    @Test
    fun `access token returns userId`() {
        val token = accessToken.asAccessToken()
        token?.userSub shouldBe "1234567890"
    }

    @Test
    fun `access token returns revocationId`() {
        val token = accessToken.asAccessToken()
        token?.tokenRevocationId shouldBe "aaaaaaaa-bbbb-cccc-dddd-eeeeeeeeeeee"
    }
}
