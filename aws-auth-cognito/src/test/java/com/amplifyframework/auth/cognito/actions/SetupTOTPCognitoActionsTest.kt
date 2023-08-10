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
import aws.sdk.kotlin.services.cognitoidentityprovider.model.AssociateSoftwareTokenRequest
import aws.sdk.kotlin.services.cognitoidentityprovider.model.AssociateSoftwareTokenResponse
import aws.sdk.kotlin.services.cognitoidentityprovider.model.CodeMismatchException
import aws.sdk.kotlin.services.cognitoidentityprovider.model.SoftwareTokenMfaNotFoundException
import aws.sdk.kotlin.services.cognitoidentityprovider.model.VerifySoftwareTokenResponse
import aws.sdk.kotlin.services.cognitoidentityprovider.model.VerifySoftwareTokenResponseType
import aws.sdk.kotlin.services.cognitoidentityprovider.verifySoftwareToken
import com.amplifyframework.auth.cognito.AWSCognitoAuthService
import com.amplifyframework.auth.cognito.AuthEnvironment
import com.amplifyframework.auth.cognito.StoreClientBehavior
import com.amplifyframework.logging.Logger
import com.amplifyframework.statemachine.EventDispatcher
import com.amplifyframework.statemachine.StateMachineEvent
import com.amplifyframework.statemachine.codegen.data.AuthConfiguration
import com.amplifyframework.statemachine.codegen.data.SignInTOTPSetupData
import com.amplifyframework.statemachine.codegen.events.SetupTOTPEvent
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import kotlin.test.assertEquals
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
@OptIn(ExperimentalCoroutinesApi::class)
class SetupTOTPCognitoActionsTest {

    private val configuration = mockk<AuthConfiguration>()
    private val cognitoAuthService = mockk<AWSCognitoAuthService>()
    private val credentialStoreClient = mockk<StoreClientBehavior>()
    private val logger = mockk<Logger>()
    private val cognitoIdentityProviderClientMock = mockk<CognitoIdentityProviderClient>()
    private val dispatcher = mockk<EventDispatcher>()

    private val capturedEvent = slot<StateMachineEvent>()

    private lateinit var authEnvironment: AuthEnvironment

    @Before
    fun setup() {
        every { logger.verbose(any()) }.answers {}
        every { dispatcher.send(capture(capturedEvent)) }.answers { }
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
    fun `initiateTOTPSetup send waitForAnswer on success`() = runTest {
        val secretCode = "SECRET_CODE"
        val session = "SESSION"
        val username = "USERNAME"
        coEvery {
            cognitoIdentityProviderClientMock.associateSoftwareToken(any())
        }.answers {
            AssociateSoftwareTokenResponse.invoke {
                this.secretCode = secretCode
                this.session = session
            }
        }
        val initiateAction = SetupTOTPCognitoActions.initiateTOTPSetup(
            SetupTOTPEvent.EventType.SetupTOTP(
                SignInTOTPSetupData("", "SESSION", "USERNAME")
            )
        )
        initiateAction.execute(dispatcher, authEnvironment)

        val expectedEvent = SetupTOTPEvent(
            SetupTOTPEvent.EventType.WaitForAnswer(SignInTOTPSetupData(secretCode, session, username))
        )
        assertEquals(
            expectedEvent.type,
            capturedEvent.captured.type
        )
        assertEquals(
            secretCode,
            (
                (capturedEvent.captured as SetupTOTPEvent).eventType as SetupTOTPEvent.EventType.WaitForAnswer
                ).totpSetupDetails.secretCode
        )
    }

    @Test
    fun `initiateTOTPSetup send waitForAnswer on failure`() = runTest {
        val session = "SESSION"
        val serviceException = SoftwareTokenMfaNotFoundException {
            message = "TOTP is not enabled"
        }
        coEvery {
            cognitoIdentityProviderClientMock.associateSoftwareToken(
                AssociateSoftwareTokenRequest.invoke {
                    this.session = session
                }
            )
        }.answers {
            throw serviceException
        }
        val initiateAction = SetupTOTPCognitoActions.initiateTOTPSetup(
            SetupTOTPEvent.EventType.SetupTOTP(
                SignInTOTPSetupData("", "SESSION", "USERNAME")
            )
        )
        initiateAction.execute(dispatcher, authEnvironment)

        val expectedEvent = SetupTOTPEvent(
            SetupTOTPEvent.EventType.ThrowAuthError(serviceException, "USERNAME", "SESSION")
        )
        assertEquals(
            expectedEvent.type,
            capturedEvent.captured.type
        )
        assertEquals(
            serviceException,
            ((capturedEvent.captured as SetupTOTPEvent).eventType as SetupTOTPEvent.EventType.ThrowAuthError).exception
        )
    }

    @Test
    fun `verifyChallengeAnswer send RespondToAuthChallenge on success`() = runTest {
        val answer = "123456"
        val session = "SESSION"
        val username = "USERNAME"
        val friendlyDeviceName = "TEST_DEVICE"
        coEvery {
            cognitoIdentityProviderClientMock.verifySoftwareToken {
                this.userCode = answer
                this.session = session
                this.friendlyDeviceName = friendlyDeviceName
            }
        }.answers {
            VerifySoftwareTokenResponse.invoke {
                this.session = session
                this.status = VerifySoftwareTokenResponseType.Success
            }
        }
        val expectedEvent = SetupTOTPEvent(
            SetupTOTPEvent.EventType.RespondToAuthChallenge(username, session)
        )

        val verifyChallengeAnswerAction = SetupTOTPCognitoActions.verifyChallengeAnswer(
            SetupTOTPEvent.EventType.VerifyChallengeAnswer(
                answer,
                username,
                session,
                friendlyDeviceName
            )
        )
        verifyChallengeAnswerAction.execute(dispatcher, authEnvironment)

        assertEquals(
            expectedEvent.type,
            capturedEvent.captured.type
        )

        assertEquals(
            session,
            (
                (capturedEvent.captured as SetupTOTPEvent).eventType as SetupTOTPEvent.EventType.RespondToAuthChallenge
                ).session
        )
    }

    @Test
    fun `verifyChallengeAnswer send RespondToAuthChallenge on Error`() = runTest {
        val answer = "123456"
        val session = "SESSION"
        val username = "USERNAME"
        val friendlyDeviceName = "TEST_DEVICE"
        coEvery {
            cognitoIdentityProviderClientMock.verifySoftwareToken {
                this.userCode = answer
                this.session = session
                this.friendlyDeviceName = friendlyDeviceName
            }
        }.answers {
            VerifySoftwareTokenResponse.invoke {
                this.session = session
                this.status = VerifySoftwareTokenResponseType.Error
            }
        }
        val expectedEvent = SetupTOTPEvent(
            SetupTOTPEvent.EventType.ThrowAuthError(
                Exception("An unknown service error has occurred"),
                "USERNAME",
                "SESSION"
            )
        )

        val verifyChallengeAnswerAction = SetupTOTPCognitoActions.verifyChallengeAnswer(
            SetupTOTPEvent.EventType.VerifyChallengeAnswer(
                answer,
                username,
                session,
                friendlyDeviceName
            )
        )
        verifyChallengeAnswerAction.execute(dispatcher, authEnvironment)

        assertEquals(
            expectedEvent.type,
            capturedEvent.captured.type
        )

        assertEquals(
            (expectedEvent.eventType as SetupTOTPEvent.EventType.ThrowAuthError).exception.message,
            (
                (capturedEvent.captured as SetupTOTPEvent).eventType as SetupTOTPEvent.EventType.ThrowAuthError
                ).exception.message
        )
    }

    @Test
    fun `verifyChallengeAnswer send RespondToAuthChallenge on exception`() = runTest {
        val answer = "123456"
        val session = "SESSION"
        val username = "USERNAME"
        val friendlyDeviceName = "TEST_DEVICE"
        val serviceException = CodeMismatchException {
            message = "Invalid Code"
        }
        coEvery {
            cognitoIdentityProviderClientMock.verifySoftwareToken {
                this.userCode = answer
                this.session = session
                this.friendlyDeviceName = friendlyDeviceName
            }
        }.answers {
            throw serviceException
        }
        val expectedEvent = SetupTOTPEvent(
            SetupTOTPEvent.EventType.ThrowAuthError(serviceException, "USERNAME", "SESSION")
        )

        val verifyChallengeAnswerAction = SetupTOTPCognitoActions.verifyChallengeAnswer(
            SetupTOTPEvent.EventType.VerifyChallengeAnswer(
                answer,
                username,
                session,
                friendlyDeviceName
            )
        )
        verifyChallengeAnswerAction.execute(dispatcher, authEnvironment)

        assertEquals(
            expectedEvent.type,
            capturedEvent.captured.type
        )

        assertEquals(
            (expectedEvent.eventType as SetupTOTPEvent.EventType.ThrowAuthError).exception.message,
            (
                (capturedEvent.captured as SetupTOTPEvent).eventType as SetupTOTPEvent.EventType.ThrowAuthError
                ).exception.message
        )
    }
}
