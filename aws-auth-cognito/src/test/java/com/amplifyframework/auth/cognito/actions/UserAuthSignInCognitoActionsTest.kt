/*
 * Copyright 2024 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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

import android.app.Activity
import androidx.test.core.app.ApplicationProvider
import aws.sdk.kotlin.services.cognitoidentityprovider.CognitoIdentityProviderClient
import aws.sdk.kotlin.services.cognitoidentityprovider.model.ChallengeNameType
import aws.sdk.kotlin.services.cognitoidentityprovider.model.InitiateAuthRequest
import aws.sdk.kotlin.services.cognitoidentityprovider.model.InitiateAuthResponse
import com.amplifyframework.auth.AuthFactorType
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
import com.amplifyframework.statemachine.codegen.data.DeviceMetadata
import com.amplifyframework.statemachine.codegen.data.SignInMethod
import com.amplifyframework.statemachine.codegen.data.UserPoolConfiguration
import com.amplifyframework.statemachine.codegen.data.WebAuthnSignInContext
import com.amplifyframework.statemachine.codegen.events.SignInEvent
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import java.lang.ref.WeakReference
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class UserAuthSignInCognitoActionsTest {
    private val username = "USERNAME"
    private val dummySession = "session"
    private val dummyDeviceKey = "device-key"
    private val dummyDeviceGroup = "device-group-key"

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
        coEvery { loadCredentials(CredentialType.Device(username)) } returns AmplifyCredential.DeviceData(
            DeviceMetadata.Metadata(
                deviceKey = dummyDeviceKey,
                deviceGroupKey = dummyDeviceGroup
            )
        )
    }
    private val logger = mockk<Logger>(relaxed = true)
    private val cognitoIdentityProviderClientMock = mockk<CognitoIdentityProviderClient>()

    private val capturedEvent = slot<StateMachineEvent>()
    private val dispatcher = mockk<EventDispatcher> {
        every { send(capture(capturedEvent)) }.answers { }
    }

    private val callingActivity = mockk<Activity>()

    private lateinit var authEnvironment: AuthEnvironment

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
    fun `Test no preferred preference and receive SELECT_CHALLENGE`() = runTest {
        val capturedRequest = slot<InitiateAuthRequest>()
        coEvery {
            cognitoIdentityProviderClientMock.initiateAuth(capture(capturedRequest))
        }.answers {
            InitiateAuthResponse.invoke {
                this.session = dummySession
                this.challengeParameters = emptyMap()
                this.challengeName = ChallengeNameType.SelectChallenge
                this.availableChallenges = listOf(
                    ChallengeNameType.EmailOtp,
                    ChallengeNameType.WebAuthn,
                    ChallengeNameType.Password
                )
            }
        }

        val availableChallenges = listOf(
            ChallengeNameType.EmailOtp.value,
            ChallengeNameType.WebAuthn.value,
            ChallengeNameType.Password.value
        )

        val expectedEvent = SignInEvent(
            SignInEvent.EventType.ReceivedChallenge(
                AuthChallenge(
                    challengeName = ChallengeNameType.SelectChallenge.value,
                    username = username,
                    session = dummySession,
                    parameters = null,
                    availableChallenges = availableChallenges
                ),
                SignInMethod.ApiBased(SignInMethod.ApiBased.AuthType.USER_SRP_AUTH)
            )
        )

        UserAuthSignInCognitoActions.initiateUserAuthSignIn(
            SignInEvent.EventType.InitiateUserAuth(
                username = username,
                preferredChallenge = null,
                callingActivity = WeakReference(callingActivity),
                metadata = emptyMap()
            )
        ).execute(dispatcher, authEnvironment)

        assertEquals(
            expectedEvent.type,
            capturedEvent.captured.type
        )

        assertEquals(
            dummySession,
            ((capturedEvent.captured as SignInEvent).eventType as SignInEvent.EventType.ReceivedChallenge)
                .challenge.session
        )
        assertEquals(
            ChallengeNameType.SelectChallenge.value,
            ((capturedEvent.captured as SignInEvent).eventType as SignInEvent.EventType.ReceivedChallenge)
                .challenge.challengeName
        )
        assertEquals(
            username,
            ((capturedEvent.captured as SignInEvent).eventType as SignInEvent.EventType.ReceivedChallenge)
                .challenge.username
        )
        assertNull(
            ((capturedEvent.captured as SignInEvent).eventType as SignInEvent.EventType.ReceivedChallenge)
                .challenge.parameters
        )
        assertEquals(
            availableChallenges,
            ((capturedEvent.captured as SignInEvent).eventType as SignInEvent.EventType.ReceivedChallenge)
                .challenge.availableChallenges
        )
    }

    @Test
    fun `Test preferred preference of EMAIL_OTP and receive RECEIVED_CHALLENGE`() = runTest {
        val challengeParams = mapOf(
            "CODE_DELIVERY_DELIVERY_MEDIUM" to "EMAIL",
            "CODE_DELIVERY_DESTINATION" to "a***@a***"
        )
        val capturedRequest = slot<InitiateAuthRequest>()
        coEvery {
            cognitoIdentityProviderClientMock.initiateAuth(capture(capturedRequest))
        }.answers {
            InitiateAuthResponse.invoke {
                this.session = dummySession
                this.challengeParameters = challengeParams
                this.challengeName = ChallengeNameType.EmailOtp
            }
        }

        val expectedEvent = SignInEvent(
            SignInEvent.EventType.ReceivedChallenge(
                AuthChallenge(
                    challengeName = ChallengeNameType.EmailOtp.value,
                    username = username,
                    session = dummySession,
                    availableChallenges = null,
                    parameters = challengeParams
                ),
                SignInMethod.ApiBased(SignInMethod.ApiBased.AuthType.USER_SRP_AUTH)
            )
        )

        UserAuthSignInCognitoActions.initiateUserAuthSignIn(
            SignInEvent.EventType.InitiateUserAuth(
                username = username,
                preferredChallenge = AuthFactorType.EMAIL_OTP,
                callingActivity = WeakReference(callingActivity),
                metadata = emptyMap()
            )
        ).execute(dispatcher, authEnvironment)

        assertEquals(
            expectedEvent.type,
            capturedEvent.captured.type
        )
        assertEquals(
            dummySession,
            ((capturedEvent.captured as SignInEvent).eventType as SignInEvent.EventType.ReceivedChallenge)
                .challenge.session
        )
        assertEquals(
            ChallengeNameType.EmailOtp.value,
            ((capturedEvent.captured as SignInEvent).eventType as SignInEvent.EventType.ReceivedChallenge)
                .challenge.challengeName
        )
        assertEquals(
            username,
            ((capturedEvent.captured as SignInEvent).eventType as SignInEvent.EventType.ReceivedChallenge)
                .challenge.username
        )
        assertEquals(
            challengeParams,
            ((capturedEvent.captured as SignInEvent).eventType as SignInEvent.EventType.ReceivedChallenge)
                .challenge.parameters
        )
        assertNull(
            ((capturedEvent.captured as SignInEvent).eventType as SignInEvent.EventType.ReceivedChallenge)
                .challenge.availableChallenges
        )
    }

    @Test
    fun `Test preferred preference of SMS_OTP and receive RECEIVED_CHALLENGE`() = runTest {
        val challengeParams = mapOf(
            "CODE_DELIVERY_DELIVERY_MEDIUM" to "SMS",
            "CODE_DELIVERY_DESTINATION" to "a***@a***"
        )
        val capturedRequest = slot<InitiateAuthRequest>()
        coEvery {
            cognitoIdentityProviderClientMock.initiateAuth(capture(capturedRequest))
        }.answers {
            InitiateAuthResponse.invoke {
                this.session = dummySession
                this.challengeParameters = challengeParams
                this.challengeName = ChallengeNameType.SmsOtp
            }
        }

        val expectedEvent = SignInEvent(
            SignInEvent.EventType.ReceivedChallenge(
                AuthChallenge(
                    challengeName = ChallengeNameType.SmsOtp.value,
                    username = username,
                    session = dummySession,
                    availableChallenges = null,
                    parameters = challengeParams
                ),
                SignInMethod.ApiBased(SignInMethod.ApiBased.AuthType.USER_SRP_AUTH)
            ),
        )

        UserAuthSignInCognitoActions.initiateUserAuthSignIn(
            SignInEvent.EventType.InitiateUserAuth(
                username = username,
                preferredChallenge = AuthFactorType.SMS_OTP,
                callingActivity = WeakReference(callingActivity),
                metadata = emptyMap()
            )
        ).execute(dispatcher, authEnvironment)

        assertEquals(
            expectedEvent.type,
            capturedEvent.captured.type
        )
        assertEquals(
            dummySession,
            ((capturedEvent.captured as SignInEvent).eventType as SignInEvent.EventType.ReceivedChallenge)
                .challenge.session
        )
        assertEquals(
            ChallengeNameType.SmsOtp.value,
            ((capturedEvent.captured as SignInEvent).eventType as SignInEvent.EventType.ReceivedChallenge)
                .challenge.challengeName
        )
        assertEquals(
            username,
            ((capturedEvent.captured as SignInEvent).eventType as SignInEvent.EventType.ReceivedChallenge)
                .challenge.username
        )
        assertEquals(
            challengeParams,
            ((capturedEvent.captured as SignInEvent).eventType as SignInEvent.EventType.ReceivedChallenge)
                .challenge.parameters
        )
        assertNull(
            ((capturedEvent.captured as SignInEvent).eventType as SignInEvent.EventType.ReceivedChallenge)
                .challenge.availableChallenges
        )
    }

    @Test
    fun `Test preferred preference of WEB_AUTHN and receive RECEIVED_CHALLENGE`() = runTest {
        val capturedRequest = slot<InitiateAuthRequest>()
        coEvery {
            cognitoIdentityProviderClientMock.initiateAuth(capture(capturedRequest))
        }.answers {
            InitiateAuthResponse.invoke {
                this.session = dummySession
                this.challengeParameters = mapOf(
                    "CREDENTIAL_REQUEST_OPTIONS" to "json"
                )
                this.challengeName = ChallengeNameType.WebAuthn
            }
        }

        val callingActivityReference = WeakReference(callingActivity)

        val expectedEvent = SignInEvent(
            SignInEvent.EventType.InitiateWebAuthnSignIn(
                WebAuthnSignInContext(
                    username = username,
                    callingActivity = callingActivityReference,
                    session = dummySession,
                    requestJson = "json"
                )
            )
        )

        UserAuthSignInCognitoActions.initiateUserAuthSignIn(
            SignInEvent.EventType.InitiateUserAuth(
                username = username,
                preferredChallenge = AuthFactorType.WEB_AUTHN,
                callingActivity = callingActivityReference,
                metadata = emptyMap()
            )
        ).execute(dispatcher, authEnvironment)

        assertEquals(
            expectedEvent.type,
            capturedEvent.captured.type
        )
        assertEquals(
            dummySession,
            ((capturedEvent.captured as SignInEvent).eventType as SignInEvent.EventType.InitiateWebAuthnSignIn)
                .signInContext.session
        )
        assertEquals(
            username,
            ((capturedEvent.captured as SignInEvent).eventType as SignInEvent.EventType.InitiateWebAuthnSignIn)
                .signInContext.username
        )
        assertEquals(
            "json",
            ((capturedEvent.captured as SignInEvent).eventType as SignInEvent.EventType.InitiateWebAuthnSignIn)
                .signInContext.requestJson
        )
        assertEquals(
            callingActivityReference,
            ((capturedEvent.captured as SignInEvent).eventType as SignInEvent.EventType.InitiateWebAuthnSignIn)
                .signInContext.callingActivity
        )
    }
}
