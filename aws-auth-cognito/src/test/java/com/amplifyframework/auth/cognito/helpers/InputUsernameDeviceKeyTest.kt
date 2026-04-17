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

package com.amplifyframework.auth.cognito.helpers

import aws.sdk.kotlin.services.cognitoidentityprovider.model.AuthenticationResultType
import aws.sdk.kotlin.services.cognitoidentityprovider.model.ChallengeNameType
import com.amplifyframework.statemachine.codegen.data.AuthChallenge
import com.amplifyframework.statemachine.codegen.data.CognitoUserPoolTokens
import com.amplifyframework.statemachine.codegen.data.SignInMethod
import com.amplifyframework.statemachine.codegen.data.SignedInData
import com.amplifyframework.statemachine.codegen.events.AuthenticationEvent
import com.amplifyframework.statemachine.codegen.events.SRPEvent
import com.amplifyframework.statemachine.codegen.events.SignInEvent
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import java.util.Date
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.junit.Test

class InputUsernameDeviceKeyTest {

    private val json = Json { ignoreUnknownKeys = true }

    // --- SignedInData serialization tests ---

    @Test
    fun `SignedInData deserialization without inputUsername field yields null`() {
        // Simulates decoding persisted data from before this change
        val jsonString = """
            {
                "userId": "user-123",
                "username": "cognito-sub-uuid",
                "signedInDate": 1700000000000,
                "signInMethod": {
                    "type": "SignInMethod.ApiBased",
                    "authType": "USER_SRP_AUTH"
                },
                "cognitoUserPoolTokens": {
                    "idToken": null,
                    "accessToken": null,
                    "refreshToken": null,
                    "expiration": 1700003600
                }
            }
        """.trimIndent()

        val data = json.decodeFromString<SignedInData>(jsonString)
        data.inputUsername.shouldBeNull()
        data.userId shouldBe "user-123"
        data.username shouldBe "cognito-sub-uuid"
    }

    @Test
    fun `SignedInData round-trip serialization preserves inputUsername`() {
        val original = SignedInData(
            userId = "user-123",
            username = "cognito-sub-uuid",
            signedInDate = Date(1700000000000),
            signInMethod = SignInMethod.ApiBased(SignInMethod.ApiBased.AuthType.USER_SRP_AUTH),
            cognitoUserPoolTokens = CognitoUserPoolTokens(null, null, null, 1700003600L),
            inputUsername = "user@example.com"
        )

        val serialized = json.encodeToString(original)
        val deserialized = json.decodeFromString<SignedInData>(serialized)

        deserialized.inputUsername shouldBe "user@example.com"
        deserialized.userId shouldBe original.userId
        deserialized.username shouldBe original.username
    }

    @Test
    fun `SignedInData round-trip with null inputUsername`() {
        val original = SignedInData(
            userId = "user-123",
            username = "cognito-sub-uuid",
            signedInDate = Date(1700000000000),
            signInMethod = SignInMethod.ApiBased(SignInMethod.ApiBased.AuthType.USER_SRP_AUTH),
            cognitoUserPoolTokens = CognitoUserPoolTokens(null, null, null, 1700003600L)
        )

        val serialized = json.encodeToString(original)
        val deserialized = json.decodeFromString<SignedInData>(serialized)

        deserialized.inputUsername.shouldBeNull()
    }

    // --- AuthChallenge serialization tests ---

    @Test
    fun `AuthChallenge deserialization without inputUsername field yields null`() {
        val jsonString = """
            {
                "challengeName": "SMS_MFA",
                "username": "cognito-sub-uuid",
                "session": "session-abc",
                "parameters": null
            }
        """.trimIndent()

        val challenge = json.decodeFromString<AuthChallenge>(jsonString)
        challenge.inputUsername.shouldBeNull()
        challenge.challengeName shouldBe "SMS_MFA"
    }

    @Test
    fun `AuthChallenge round-trip serialization preserves inputUsername`() {
        val original = AuthChallenge(
            challengeName = "SMS_MFA",
            username = "cognito-sub-uuid",
            session = "session-abc",
            parameters = null,
            inputUsername = "user@example.com"
        )

        val serialized = json.encodeToString(original)
        val deserialized = json.decodeFromString<AuthChallenge>(serialized)

        deserialized.inputUsername shouldBe "user@example.com"
    }

    // --- SRPEvent inputUsername threading tests ---

    @Test
    fun `RespondPasswordVerifier carries inputUsername`() {
        val event = SRPEvent.EventType.RespondPasswordVerifier(
            challengeParameters = mapOf("USERNAME" to "cognito-sub"),
            metadata = emptyMap(),
            session = "session-1",
            inputUsername = "user@example.com"
        )

        event.inputUsername shouldBe "user@example.com"
    }

    @Test
    fun `RespondPasswordVerifier defaults inputUsername to null`() {
        val event = SRPEvent.EventType.RespondPasswordVerifier(
            challengeParameters = mapOf("USERNAME" to "cognito-sub"),
            metadata = emptyMap(),
            session = "session-1"
        )

        event.inputUsername.shouldBeNull()
    }

    @Test
    fun `RetryRespondPasswordVerifier carries inputUsername`() {
        val event = SRPEvent.EventType.RetryRespondPasswordVerifier(
            challengeParameters = mapOf("USERNAME" to "cognito-sub"),
            metadata = emptyMap(),
            session = "session-1",
            signInMethod = SignInMethod.ApiBased(SignInMethod.ApiBased.AuthType.USER_SRP_AUTH),
            inputUsername = "user@example.com"
        )

        event.inputUsername shouldBe "user@example.com"
    }

    // --- evaluateNextStep tests ---

    @Test
    fun `evaluateNextStep sets inputUsername on SignedInData for successful auth`() {
        val dummyToken = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiIxMjM0NTY3ODkwIiwibmFtZSI6IkpvaG4g" +
            "RG9lIiwiaWF0IjoxNTE2MjM5MDIyfQ.XbPfbIHMI6arZ3Y922BhjWgQzWXcXNrz0ogtVhfEd2o"

        val result = SignInChallengeHelper.evaluateNextStep(
            username = "cognito-sub-uuid",
            challengeNameType = null,
            session = null,
            authenticationResult = AuthenticationResultType.invoke {
                accessToken = dummyToken
                idToken = dummyToken
                refreshToken = "refresh"
                expiresIn = 3600
            },
            signInMethod = SignInMethod.ApiBased(SignInMethod.ApiBased.AuthType.USER_SRP_AUTH),
            inputUsername = "user@example.com"
        )

        // Without newDeviceMetadata, this produces a SignInCompleted event
        result.shouldBeInstanceOf<AuthenticationEvent>()
        val eventType = (result as AuthenticationEvent).eventType
        eventType.shouldBeInstanceOf<AuthenticationEvent.EventType.SignInCompleted>()
        val signedInData = (eventType as AuthenticationEvent.EventType.SignInCompleted).signedInData
        signedInData.inputUsername shouldBe "user@example.com"
        signedInData.username shouldBe "cognito-sub-uuid"
    }

    @Test
    fun `evaluateNextStep sets inputUsername on SignedInData for ConfirmDevice event`() {
        val dummyToken = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiIxMjM0NTY3ODkwIiwibmFtZSI6IkpvaG4g" +
            "RG9lIiwiaWF0IjoxNTE2MjM5MDIyfQ.XbPfbIHMI6arZ3Y922BhjWgQzWXcXNrz0ogtVhfEd2o"

        val result = SignInChallengeHelper.evaluateNextStep(
            username = "cognito-sub-uuid",
            challengeNameType = null,
            session = null,
            authenticationResult = AuthenticationResultType.invoke {
                accessToken = dummyToken
                idToken = dummyToken
                refreshToken = "refresh"
                expiresIn = 3600
                newDeviceMetadata {
                    deviceKey = "device-key-1"
                    deviceGroupKey = "device-group-1"
                }
            },
            signInMethod = SignInMethod.ApiBased(SignInMethod.ApiBased.AuthType.USER_SRP_AUTH),
            inputUsername = "user@example.com"
        )

        result.shouldBeInstanceOf<SignInEvent>()
        val eventType = (result as SignInEvent).eventType
        eventType.shouldBeInstanceOf<SignInEvent.EventType.ConfirmDevice>()
        val signedInData = (eventType as SignInEvent.EventType.ConfirmDevice).signedInData
        signedInData.inputUsername shouldBe "user@example.com"
        signedInData.username shouldBe "cognito-sub-uuid"
    }

    @Test
    fun `evaluateNextStep sets inputUsername on AuthChallenge for challenge events`() {
        val result = SignInChallengeHelper.evaluateNextStep(
            username = "cognito-sub-uuid",
            challengeNameType = ChallengeNameType.SmsMfa,
            session = "session-1",
            challengeParameters = mapOf(
                "CODE_DELIVERY_DELIVERY_MEDIUM" to "sms",
                "CODE_DELIVERY_DESTINATION" to "+15555555555"
            ),
            authenticationResult = null,
            signInMethod = SignInMethod.ApiBased(SignInMethod.ApiBased.AuthType.USER_SRP_AUTH),
            inputUsername = "user@example.com"
        )

        result.shouldBeInstanceOf<SignInEvent>()
        val eventType = (result as SignInEvent).eventType
        eventType.shouldBeInstanceOf<SignInEvent.EventType.ReceivedChallenge>()
        val challenge = (eventType as SignInEvent.EventType.ReceivedChallenge).challenge
        challenge.inputUsername shouldBe "user@example.com"
        challenge.username shouldBe "cognito-sub-uuid"
    }

    @Test
    fun `evaluateNextStep sets inputUsername on AuthChallenge for SelectChallenge`() {
        val result = SignInChallengeHelper.evaluateNextStep(
            username = "cognito-sub-uuid",
            challengeNameType = ChallengeNameType.SelectChallenge,
            session = "session-1",
            availableChallenges = listOf("EMAIL_OTP", "SMS_OTP"),
            authenticationResult = null,
            signInMethod = SignInMethod.ApiBased(SignInMethod.ApiBased.AuthType.USER_AUTH),
            inputUsername = "user@example.com"
        )

        result.shouldBeInstanceOf<SignInEvent>()
        val eventType = (result as SignInEvent).eventType
        eventType.shouldBeInstanceOf<SignInEvent.EventType.ReceivedChallenge>()
        val challenge = (eventType as SignInEvent.EventType.ReceivedChallenge).challenge
        challenge.inputUsername shouldBe "user@example.com"
    }

    @Test
    fun `evaluateNextStep sets inputUsername on AuthChallenge for MfaSetup`() {
        val result = SignInChallengeHelper.evaluateNextStep(
            username = "cognito-sub-uuid",
            challengeNameType = ChallengeNameType.MfaSetup,
            session = "session-1",
            challengeParameters = mapOf("MFAS_CAN_SETUP" to "\"EMAIL_OTP\""),
            authenticationResult = null,
            signInMethod = SignInMethod.ApiBased(SignInMethod.ApiBased.AuthType.USER_SRP_AUTH),
            inputUsername = "user@example.com"
        )

        result.shouldBeInstanceOf<SignInEvent>()
        val eventType = (result as SignInEvent).eventType
        eventType.shouldBeInstanceOf<SignInEvent.EventType.ReceivedChallenge>()
        val challenge = (eventType as SignInEvent.EventType.ReceivedChallenge).challenge
        challenge.inputUsername shouldBe "user@example.com"
    }

    @Test
    fun `evaluateNextStep falls back to username when inputUsername is null`() {
        val dummyToken = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiIxMjM0NTY3ODkwIiwibmFtZSI6IkpvaG4g" +
            "RG9lIiwiaWF0IjoxNTE2MjM5MDIyfQ.XbPfbIHMI6arZ3Y922BhjWgQzWXcXNrz0ogtVhfEd2o"

        val result = SignInChallengeHelper.evaluateNextStep(
            username = "cognito-sub-uuid",
            challengeNameType = null,
            session = null,
            authenticationResult = AuthenticationResultType.invoke {
                accessToken = dummyToken
                idToken = dummyToken
                refreshToken = "refresh"
                expiresIn = 3600
            },
            signInMethod = SignInMethod.ApiBased(SignInMethod.ApiBased.AuthType.USER_SRP_AUTH),
            inputUsername = null
        )

        result.shouldBeInstanceOf<AuthenticationEvent>()
        val eventType = (result as AuthenticationEvent).eventType
        eventType.shouldBeInstanceOf<AuthenticationEvent.EventType.SignInCompleted>()
        val signedInData = (eventType as AuthenticationEvent.EventType.SignInCompleted).signedInData
        // When inputUsername is null, falls back to username
        signedInData.inputUsername shouldBe "cognito-sub-uuid"
    }

    @Test
    fun `evaluateNextStep challenge events have null inputUsername when not provided`() {
        val result = SignInChallengeHelper.evaluateNextStep(
            username = "cognito-sub-uuid",
            challengeNameType = ChallengeNameType.SmsMfa,
            session = "session-1",
            challengeParameters = mapOf(
                "CODE_DELIVERY_DELIVERY_MEDIUM" to "sms",
                "CODE_DELIVERY_DESTINATION" to "+15555555555"
            ),
            authenticationResult = null,
            signInMethod = SignInMethod.ApiBased(SignInMethod.ApiBased.AuthType.USER_SRP_AUTH)
        )

        result.shouldBeInstanceOf<SignInEvent>()
        val eventType = (result as SignInEvent).eventType
        eventType.shouldBeInstanceOf<SignInEvent.EventType.ReceivedChallenge>()
        val challenge = (eventType as SignInEvent.EventType.ReceivedChallenge).challenge
        challenge.inputUsername.shouldBeNull()
    }

    // --- SignedInData equality tests ---

    @Test
    fun `SignedInData equality includes inputUsername`() {
        val tokens = CognitoUserPoolTokens(null, null, null, 1700003600L)
        val data1 = SignedInData(
            userId = "user-123",
            username = "cognito-sub",
            signedInDate = Date(1700000000000),
            signInMethod = SignInMethod.ApiBased(SignInMethod.ApiBased.AuthType.USER_SRP_AUTH),
            cognitoUserPoolTokens = tokens,
            inputUsername = "user@example.com"
        )
        val data2 = SignedInData(
            userId = "user-123",
            username = "cognito-sub",
            signedInDate = Date(1700000000000),
            signInMethod = SignInMethod.ApiBased(SignInMethod.ApiBased.AuthType.USER_SRP_AUTH),
            cognitoUserPoolTokens = tokens,
            inputUsername = "different@example.com"
        )
        val data3 = SignedInData(
            userId = "user-123",
            username = "cognito-sub",
            signedInDate = Date(1700000000000),
            signInMethod = SignInMethod.ApiBased(SignInMethod.ApiBased.AuthType.USER_SRP_AUTH),
            cognitoUserPoolTokens = tokens,
            inputUsername = "user@example.com"
        )

        (data1 == data2) shouldBe false
        (data1 == data3) shouldBe true
    }
}
