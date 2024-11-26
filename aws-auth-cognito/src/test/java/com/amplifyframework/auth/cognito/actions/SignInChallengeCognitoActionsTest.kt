/*
 * Copyright 2023 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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
package com.amplifyframework.auth.cognito.actions

import androidx.test.core.app.ApplicationProvider
import aws.sdk.kotlin.services.cognitoidentityprovider.CognitoIdentityProviderClient
import aws.sdk.kotlin.services.cognitoidentityprovider.model.RespondToAuthChallengeRequest
import com.amplifyframework.auth.AuthUserAttribute
import com.amplifyframework.auth.AuthUserAttributeKey
import com.amplifyframework.auth.cognito.AWSCognitoAuthService
import com.amplifyframework.auth.cognito.AuthConfiguration
import com.amplifyframework.auth.cognito.AuthEnvironment
import com.amplifyframework.auth.cognito.StoreClientBehavior
import com.amplifyframework.logging.Logger
import com.amplifyframework.statemachine.EventDispatcher
import com.amplifyframework.statemachine.StateMachineEvent
import com.amplifyframework.statemachine.codegen.data.AmplifyCredential
import com.amplifyframework.statemachine.codegen.data.AuthChallenge
import com.amplifyframework.statemachine.codegen.data.CredentialType
import com.amplifyframework.statemachine.codegen.data.UserPoolConfiguration
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import junit.framework.TestCase.assertTrue
import kotlin.test.assertEquals
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class SignInChallengeCognitoActionsTest {

    private val pool = mockk<UserPoolConfiguration> {
        every { appClient } returns "client"
        every { appClientSecret } returns null
        every { pinpointAppId } returns null
    }
    private val configuration = mockk<AuthConfiguration> {
        every { userPool } returns pool
    }
    private val cognitoAuthService = mockk<AWSCognitoAuthService>()
    private val credentialStoreClient = mockk<StoreClientBehavior> {
        coEvery { loadCredentials(CredentialType.ASF) } returns AmplifyCredential.ASFDevice("asf_id")
    }
    private val logger = mockk<Logger>(relaxed = true)
    private val cognitoIdentityProviderClientMock = mockk<CognitoIdentityProviderClient>()

    private val capturedEvent = slot<StateMachineEvent>()
    private val dispatcher = mockk<EventDispatcher> {
        every { send(capture(capturedEvent)) }.answers { }
    }

    private lateinit var authEnvironment: AuthEnvironment

    private val answer = "myAnswer"
    private val username = "fakeUserName"

    @Before
    fun setup() {
        every { cognitoAuthService.cognitoIdentityProviderClient }.answers { cognitoIdentityProviderClientMock }
        authEnvironment = AuthEnvironment(
            ApplicationProvider.getApplicationContext(),
            configuration,
            cognitoAuthService,
            credentialStoreClient,
            null,
            null,
            logger
        )
    }

    @Test
    fun `very auth challenge without user attributes`() = runTest {
        val expectedChallengeResponses = mapOf(
            "USERNAME" to username
        )
        val capturedRequest = slot<RespondToAuthChallengeRequest>()
        coEvery {
            cognitoIdentityProviderClientMock.respondToAuthChallenge(capture(capturedRequest))
        }.answers {
            mockk()
        }

        SignInChallengeCognitoActions.verifyChallengeAuthAction(
            answer,
            emptyMap(),
            emptyList(),
            AuthChallenge(
                "CONFIRM_SIGN_IN_WITH_NEW_PASSWORD",
                username = username,
                session = null,
                parameters = null
            ),
            configuration.authFlowType
        ).execute(dispatcher, authEnvironment)

        assertTrue(capturedRequest.isCaptured)
        assertEquals(expectedChallengeResponses, capturedRequest.captured.challengeResponses)
    }

    @Test
    fun `user attributes are added to auth challenge`() = runTest {
        val providedUserAttributes = listOf(AuthUserAttribute(AuthUserAttributeKey.phoneNumber(), "+15555555555"))
        val expectedChallengeResponses = mapOf(
            "USERNAME" to username,
            "userAttributes.phone_number" to "+15555555555"
        )
        val capturedRequest = slot<RespondToAuthChallengeRequest>()
        coEvery {
            cognitoIdentityProviderClientMock.respondToAuthChallenge(capture(capturedRequest))
        }.answers {
            mockk()
        }

        SignInChallengeCognitoActions.verifyChallengeAuthAction(
            answer,
            emptyMap(),
            providedUserAttributes,
            AuthChallenge(
                "CONFIRM_SIGN_IN_WITH_NEW_PASSWORD",
                username = username,
                session = null,
                parameters = null
            ),
            configuration.authFlowType
        ).execute(dispatcher, authEnvironment)

        assertTrue(capturedRequest.isCaptured)
        assertEquals(expectedChallengeResponses, capturedRequest.captured.challengeResponses)
    }

    @Test
    fun `verify email MFA setup selection challenge is handled`() = runTest {
        val expectedChallengeResponses = mapOf(
            "USERNAME" to username,
        )

        val capturedRequest = slot<RespondToAuthChallengeRequest>()
        coEvery {
            cognitoIdentityProviderClientMock.respondToAuthChallenge(capture(capturedRequest))
        }.answers {
            mockk()
        }

        SignInChallengeCognitoActions.verifyChallengeAuthAction(
            "EMAIL_OTP",
            emptyMap(),
            emptyList(),
            AuthChallenge(
                "MFA_SETUP",
                username = username,
                session = null,
                parameters = null
            ),
            configuration.authFlowType
        ).execute(dispatcher, authEnvironment)

        assertTrue(capturedRequest.isCaptured)
        assertEquals(expectedChallengeResponses, capturedRequest.captured.challengeResponses)
    }

    @Test
    fun `verify challenge response key for Email Otp`() = runTest {
        val expectedChallengeResponses = mapOf(
            "USERNAME" to username,
            "EMAIL_OTP_CODE" to answer
        )

        val capturedRequest = slot<RespondToAuthChallengeRequest>()
        coEvery {
            cognitoIdentityProviderClientMock.respondToAuthChallenge(capture(capturedRequest))
        }.answers {
            mockk()
        }

        SignInChallengeCognitoActions.verifyChallengeAuthAction(
            answer,
            emptyMap(),
            emptyList(),
            AuthChallenge(
                "EMAIL_OTP",
                username = username,
                session = null,
                parameters = null
            ),
            configuration.authFlowType
        ).execute(dispatcher, authEnvironment)

        assertTrue(capturedRequest.isCaptured)
        assertEquals(expectedChallengeResponses, capturedRequest.captured.challengeResponses)
    }

    @Test
    fun `verify challenge response key for SMS Otp`() = runTest {
        val expectedChallengeResponses = mapOf(
            "USERNAME" to username,
            "SMS_OTP_CODE" to answer
        )

        val capturedRequest = slot<RespondToAuthChallengeRequest>()
        coEvery {
            cognitoIdentityProviderClientMock.respondToAuthChallenge(capture(capturedRequest))
        }.answers {
            mockk()
        }

        SignInChallengeCognitoActions.verifyChallengeAuthAction(
            answer,
            emptyMap(),
            emptyList(),
            AuthChallenge(
                "SMS_OTP",
                username = username,
                session = null,
                parameters = null
            ),
            configuration.authFlowType
        ).execute(dispatcher, authEnvironment)

        assertTrue(capturedRequest.isCaptured)
        assertEquals(expectedChallengeResponses, capturedRequest.captured.challengeResponses)
    }

    @Test
    fun `verify challenge response key for Select Challenge`() = runTest {
        val selectChallengeChallengeResponseKey = "ANSWER"
        val expectedChallengeResponses = mapOf(
            "USERNAME" to username,
            selectChallengeChallengeResponseKey to answer
        )

        val capturedRequest = slot<RespondToAuthChallengeRequest>()
        coEvery {
            cognitoIdentityProviderClientMock.respondToAuthChallenge(capture(capturedRequest))
        }.answers {
            mockk()
        }

        SignInChallengeCognitoActions.verifyChallengeAuthAction(
            answer,
            emptyMap(),
            emptyList(),
            AuthChallenge(
                "SELECT_CHALLENGE",
                username = username,
                session = null,
                parameters = null
            ),
            configuration.authFlowType
        ).execute(dispatcher, authEnvironment)

        assertTrue(capturedRequest.isCaptured)
        assertEquals(expectedChallengeResponses, capturedRequest.captured.challengeResponses)
    }
}
