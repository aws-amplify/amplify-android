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

import com.amplifyframework.statemachine.codegen.data.CognitoUserPoolTokens
import com.amplifyframework.statemachine.codegen.data.asAccessToken
import com.amplifyframework.statemachine.codegen.data.asIdToken
import com.amplifyframework.statemachine.codegen.data.asRefreshToken
import io.kotest.matchers.shouldBe
import java.time.Instant
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.junit.Test

class TokensTest {
    private val tokenString =
        "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiIxMjM0NTY3ODkwIiwibmFtZSI6IkpvaG4gRG9lIiwidXNlcm5hbWUiOiJqZG" +
                "9lIiwiaWF0IjoxNzU2OTk4Mjc4LCJleHAiOjE3NTY5OTg1NzgsIm9yaWdpbl9qdGkiOiJhYWFhYWFhYS1iYmJiLWNjY2MtZGRkZC1lZ" +
                "WVlZWVlZWVlZWUifQ.3Mvd5WVi1z1GpQ37hEoev6DzYNv9lWNL-fGfQTxUYx4"

    @Test
    fun `identity token returns expiry`() {
        val token = tokenString.asIdToken()
        token?.expiration shouldBe Instant.ofEpochSecond(1756998578)
    }

    @Test
    fun `access token returns expiry`() {
        val token = tokenString.asAccessToken()
        token?.expiration shouldBe Instant.ofEpochSecond(1756998578)
    }

    @Test
    fun `access token returns username`() {
        val token = tokenString.asAccessToken()
        token?.username shouldBe "jdoe"
    }

    @Test
    fun `access token returns userId`() {
        val token = tokenString.asAccessToken()
        token?.userSub shouldBe "1234567890"
    }

    @Test
    fun `access token returns revocationId`() {
        val token = tokenString.asAccessToken()
        token?.tokenRevocationId shouldBe "aaaaaaaa-bbbb-cccc-dddd-eeeeeeeeeeee"
    }

    @Test
    fun `id token string value is masked`() {
        val idToken = tokenString.asIdToken()
        idToken.toString() shouldBe "eyJh***"
    }

    @Test
    fun `access token string value is masked`() {
        val accessToken = tokenString.asAccessToken()
        accessToken.toString() shouldBe "eyJh***"
    }

    @Test
    fun `refresh token string value is masked`() {
        val refreshToken = tokenString.asRefreshToken()
        refreshToken.toString() shouldBe "eyJh***"
    }

    @Test
    fun `cognito tokens string value is masked`() {
        val cognitoTokens = CognitoUserPoolTokens(
            idToken = tokenString,
            accessToken = tokenString,
            refreshToken = tokenString,
            expiration = null
        )
        cognitoTokens.toString() shouldBe
                "CognitoUserPoolTokens(idToken=eyJh***, accessToken=eyJh***, " +
                "refreshToken=eyJh***, expiration=null)"
    }

    @Test
    fun `non nested tokens are parsed correctly`() {
        val flatFormatJson = """
        {
            "idToken": "$tokenString",
            "accessToken": "$tokenString", 
            "refreshToken": "refresh_token_value",
            "expiration": 1756998578
        }
        """.trimIndent()

        val tokens = Json.decodeFromString<CognitoUserPoolTokens>(flatFormatJson)

        tokens.idToken?.tokenValue shouldBe tokenString
        tokens.accessToken?.tokenValue shouldBe tokenString
        tokens.refreshToken?.tokenValue shouldBe "refresh_token_value"
        tokens.expiration shouldBe 1756998578L

        // Verify JWT parsing still works
        tokens.accessToken?.userSub shouldBe "1234567890"
        tokens.accessToken?.username shouldBe "jdoe"
    }

    @Test
    fun `nested tokens are read correctly`() {
        val nestedFormatJson = """
        {
            "idToken": {"tokenValue": "$tokenString"},
            "accessToken": {"tokenValue": "$tokenString"},
            "refreshToken": {"tokenValue": "refresh_token_value"},
            "expiration": 1756998578
        }
        """.trimIndent()

        val tokens = Json.decodeFromString<CognitoUserPoolTokens>(nestedFormatJson)

        tokens.idToken?.tokenValue shouldBe tokenString
        tokens.accessToken?.tokenValue shouldBe tokenString
        tokens.refreshToken?.tokenValue shouldBe "refresh_token_value"
        tokens.expiration shouldBe 1756998578L

        // Verify JWT parsing still works after extracting from nested format
        tokens.accessToken?.userSub shouldBe "1234567890"
        tokens.accessToken?.username shouldBe "jdoe"
    }

    @Test
    fun `nested tokens are saved as non nested`() {
        // Start with nested format
        val nestedFormatJson = """
        {
            "idToken": {"tokenValue": "$tokenString"},
            "accessToken": {"tokenValue": "$tokenString"},
            "refreshToken": {"tokenValue": "refresh_token_value"},
            "expiration": 1756998578
        }
        """.trimIndent()

        // Deserialize nested format
        val tokens = Json.decodeFromString<CognitoUserPoolTokens>(nestedFormatJson)

        // Serialize back to JSON
        val serializedJson = Json.encodeToString(tokens)

        // Should now be in flat format
        val expectedFlatJson =
            """{"idToken":"$tokenString","accessToken":"$tokenString","refreshToken":"refresh_token_value","expiration":1756998578}"""
        serializedJson shouldBe expectedFlatJson

        // Verify we can deserialize the flat format again
        val tokensFromFlat = Json.decodeFromString<CognitoUserPoolTokens>(serializedJson)
        tokensFromFlat.idToken?.tokenValue shouldBe tokenString
        tokensFromFlat.accessToken?.tokenValue shouldBe tokenString
        tokensFromFlat.refreshToken?.tokenValue shouldBe "refresh_token_value"
    }

    @Test
    fun `flat and nested formats produce identical results`() {
        val flatFormatJson = """
        {
            "idToken": "$tokenString",
            "accessToken": "$tokenString",
            "refreshToken": "refresh_token_value",
            "expiration": 1756998578
        }
        """.trimIndent()

        val nestedFormatJson = """
        {
            "idToken": {"tokenValue": "$tokenString"},
            "accessToken": {"tokenValue": "$tokenString"},
            "refreshToken": {"tokenValue": "refresh_token_value"},
            "expiration": 1756998578
        }
        """.trimIndent()

        val tokensFromFlat = Json.decodeFromString<CognitoUserPoolTokens>(flatFormatJson)
        val tokensFromNested = Json.decodeFromString<CognitoUserPoolTokens>(nestedFormatJson)

        // Both should have identical token values
        tokensFromFlat.idToken?.tokenValue shouldBe tokensFromNested.idToken?.tokenValue
        tokensFromFlat.accessToken?.tokenValue shouldBe tokensFromNested.accessToken?.tokenValue
        tokensFromFlat.refreshToken?.tokenValue shouldBe tokensFromNested.refreshToken?.tokenValue
        tokensFromFlat.expiration shouldBe tokensFromNested.expiration

        // Both should serialize to the same flat format
        val serializedFlat = Json.encodeToString(tokensFromFlat)
        val serializedNested = Json.encodeToString(tokensFromNested)
        serializedFlat shouldBe serializedNested
    }
}