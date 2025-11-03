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

import androidx.test.core.app.ApplicationProvider
import aws.sdk.kotlin.services.cognitoidentityprovider.CognitoIdentityProviderClient
import aws.sdk.kotlin.services.cognitoidentityprovider.model.ChallengeNameType
import aws.sdk.kotlin.services.cognitoidentityprovider.model.InitiateAuthRequest
import aws.sdk.kotlin.services.cognitoidentityprovider.model.InitiateAuthResponse
import aws.sdk.kotlin.services.cognitoidentityprovider.model.NotAuthorizedException
import aws.sdk.kotlin.services.cognitoidentityprovider.model.RespondToAuthChallengeRequest
import aws.sdk.kotlin.services.cognitoidentityprovider.model.RespondToAuthChallengeResponse
import com.amplifyframework.auth.cognito.AWSCognitoAuthService
import com.amplifyframework.auth.cognito.AuthConfiguration
import com.amplifyframework.auth.cognito.AuthEnvironment
import com.amplifyframework.auth.cognito.StoreClientBehavior
import com.amplifyframework.auth.cognito.options.AuthFlowType
import com.amplifyframework.logging.Logger
import com.amplifyframework.statemachine.EventDispatcher
import com.amplifyframework.statemachine.StateMachineEvent
import com.amplifyframework.statemachine.codegen.data.AmplifyCredential
import com.amplifyframework.statemachine.codegen.data.AuthChallenge
import com.amplifyframework.statemachine.codegen.data.CredentialType
import com.amplifyframework.statemachine.codegen.data.SignInMethod
import com.amplifyframework.statemachine.codegen.data.UserPoolConfiguration
import com.amplifyframework.statemachine.codegen.events.AuthenticationEvent
import com.amplifyframework.statemachine.codegen.events.SignInEvent
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import io.mockk.Runs
import io.mockk.coEvery
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class MigrateAuthCognitoActionsTest {
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
        every { send(capture(capturedEvent)) } just Runs
    }

    private lateinit var authEnvironment: AuthEnvironment

    private val username = "username"
    private val password = "password"
    private val userId = "1234567890"
    private val dummyToken = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiIxMjM0NTY3ODkwIiwibmFtZSI6IkpvaG4g" +
        "RG9lIiwiaWF0IjoxNTE2MjM5MDIyfQ.XbPfbIHMI6arZ3Y922BhjWgQzWXcXNrz0ogtVhfEd2o"
    private val dummyIdToken = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiIxMjM0NTY3ODkwIiwibmFtZSI6IkpvaG4g" +
        "RG9lIiwiaWF0IjoxNTE2MjM5MDIyfQ.XbPfbIHMI6arZ3Y922BhjWgQzWXcXNrz0ogtVhfEd2o"
    private val dummyRefreshToken = "refreshToken"
    private val bearerToken = "Bearer"
    private val dummySession = "session"
    private val dummyDeviceKey = "device-key"
    private val dummyDeviceGroup = "device-group-key"

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
    fun `Initiate USER_AUTH with correct password`() = runTest {
        val capturedRequest = slot<InitiateAuthRequest>()
        coEvery {
            cognitoIdentityProviderClientMock.initiateAuth(capture(capturedRequest))
        }.answers {
            InitiateAuthResponse.invoke {
                this.session = dummySession
                this.challengeParameters = null
                this.challengeName = null
                this.authenticationResult {
                    this.accessToken = dummyToken
                    this.expiresIn = 3600
                    this.idToken = dummyIdToken
                    this.refreshToken = dummyRefreshToken
                    this.tokenType = bearerToken
                    newDeviceMetadata {
                        this.deviceGroupKey = dummyDeviceGroup
                        this.deviceKey = dummyDeviceKey
                    }
                }
            }
        }

        MigrateAuthCognitoActions.initiateMigrateAuthAction(
            SignInEvent.EventType.InitiateMigrateAuth(
                username = username,
                password = password,
                authFlowType = AuthFlowType.USER_AUTH,
                metadata = mapOf("KEY" to "VALUE")
            )
        ).execute(dispatcher, authEnvironment)

        val event = capturedEvent.captured.shouldBeInstanceOf<SignInEvent>()
        val data = event.eventType.shouldBeInstanceOf<SignInEvent.EventType.ConfirmDevice>().signedInData

        data.userId shouldBe userId
        data.username shouldBe username
        data.signInMethod shouldBe SignInMethod.ApiBased(SignInMethod.ApiBased.AuthType.USER_AUTH)
    }

    @Test
    fun `Initiate USER_AUTH with incorrect password`() = runTest {
        val capturedRequest = slot<InitiateAuthRequest>()
        coEvery {
            cognitoIdentityProviderClientMock.initiateAuth(capture(capturedRequest))
        }.throws(NotAuthorizedException.invoke { })

        MigrateAuthCognitoActions.initiateMigrateAuthAction(
            SignInEvent.EventType.InitiateMigrateAuth(
                username = username,
                password = dummySession,
                authFlowType = AuthFlowType.USER_AUTH,
                metadata = mapOf("KEY" to "VALUE")
            )
        ).execute(dispatcher, authEnvironment)

        val event = capturedEvent.captured.shouldBeInstanceOf<AuthenticationEvent>()
        event.eventType.shouldBeInstanceOf<AuthenticationEvent.EventType.CancelSignIn>()
    }

    @Test
    fun `RespondToAuth USER_AUTH with incorrect password`() = runTest {
        val capturedRequest = slot<RespondToAuthChallengeRequest>()
        coEvery {
            cognitoIdentityProviderClientMock.respondToAuthChallenge(capture(capturedRequest))
        }.throws(NotAuthorizedException.invoke { })

        MigrateAuthCognitoActions.initiateMigrateAuthAction(
            SignInEvent.EventType.InitiateMigrateAuth(
                username = username,
                password = password,
                authFlowType = AuthFlowType.USER_AUTH,
                metadata = mapOf("KEY" to "VALUE"),
                respondToAuthChallenge = AuthChallenge(
                    challengeName = ChallengeNameType.SelectChallenge.value,
                    session = dummySession,
                    parameters = null
                )
            )
        ).execute(dispatcher, authEnvironment)

        val event = capturedEvent.captured.shouldBeInstanceOf<AuthenticationEvent>()
        event.eventType.shouldBeInstanceOf<AuthenticationEvent.EventType.CancelSignIn>()
    }

    @Test
    fun `RespondToAuth USER_AUTH with correct password`() = runTest {
        val capturedRequest = slot<RespondToAuthChallengeRequest>()
        coEvery {
            cognitoIdentityProviderClientMock.respondToAuthChallenge(capture(capturedRequest))
        }.answers {
            RespondToAuthChallengeResponse.invoke {
                this.session = dummySession
                this.challengeParameters = null
                this.challengeName = null
                this.authenticationResult {
                    this.accessToken = dummyToken
                    this.expiresIn = 3600
                    this.idToken = dummyIdToken
                    this.refreshToken = dummyRefreshToken
                    this.tokenType = "Bearer"
                    newDeviceMetadata {
                        this.deviceGroupKey = dummyDeviceGroup
                        this.deviceKey = dummyDeviceKey
                    }
                }
            }
        }

        MigrateAuthCognitoActions.initiateMigrateAuthAction(
            SignInEvent.EventType.InitiateMigrateAuth(
                username = username,
                password = password,
                authFlowType = AuthFlowType.USER_AUTH,
                metadata = mapOf("KEY" to "VALUE"),
                respondToAuthChallenge = AuthChallenge(
                    challengeName = ChallengeNameType.SelectChallenge.value,
                    session = dummySession,
                    parameters = null
                )
            )
        ).execute(dispatcher, authEnvironment)

        val event = capturedEvent.captured.shouldBeInstanceOf<SignInEvent>()
        val data = event.eventType.shouldBeInstanceOf<SignInEvent.EventType.ConfirmDevice>().signedInData

        data.userId shouldBe userId
        data.username shouldBe username
        data.signInMethod shouldBe SignInMethod.ApiBased(SignInMethod.ApiBased.AuthType.USER_AUTH)
    }

    @Test
    fun `Initiate USER_PASSWORD_AUTH with correct password`() = runTest {
        val capturedRequest = slot<InitiateAuthRequest>()
        coEvery {
            cognitoIdentityProviderClientMock.initiateAuth(capture(capturedRequest))
        }.answers {
            InitiateAuthResponse.invoke {
                this.session = dummySession
                this.challengeParameters = null
                this.challengeName = null
                this.authenticationResult {
                    this.accessToken = dummyToken
                    this.expiresIn = 3600
                    this.idToken = dummyIdToken
                    this.refreshToken = dummyRefreshToken
                    this.tokenType = bearerToken
                    newDeviceMetadata {
                        this.deviceGroupKey = dummyDeviceGroup
                        this.deviceKey = dummyDeviceKey
                    }
                }
            }
        }

        MigrateAuthCognitoActions.initiateMigrateAuthAction(
            SignInEvent.EventType.InitiateMigrateAuth(
                username = username,
                password = password,
                authFlowType = AuthFlowType.USER_PASSWORD_AUTH,
                metadata = mapOf("KEY" to "VALUE")
            )
        ).execute(dispatcher, authEnvironment)

        val event = capturedEvent.captured.shouldBeInstanceOf<SignInEvent>()
        val data = event.eventType.shouldBeInstanceOf<SignInEvent.EventType.ConfirmDevice>().signedInData

        data.userId shouldBe userId
        data.username shouldBe username
        data.signInMethod shouldBe SignInMethod.ApiBased(SignInMethod.ApiBased.AuthType.USER_SRP_AUTH)
    }

    @Test
    fun `Initiate USER_PASSWORD_AUTH with incorrect password`() = runTest {
        val capturedRequest = slot<InitiateAuthRequest>()
        coEvery {
            cognitoIdentityProviderClientMock.initiateAuth(capture(capturedRequest))
        }.throws(NotAuthorizedException.invoke { })

        val expectedEvent = AuthenticationEvent(AuthenticationEvent.EventType.CancelSignIn())

        MigrateAuthCognitoActions.initiateMigrateAuthAction(
            SignInEvent.EventType.InitiateMigrateAuth(
                username = username,
                password = dummySession,
                authFlowType = AuthFlowType.USER_PASSWORD_AUTH,
                metadata = mapOf("KEY" to "VALUE")
            )
        ).execute(dispatcher, authEnvironment)

        val event = capturedEvent.captured.shouldBeInstanceOf<AuthenticationEvent>()
        event.eventType.shouldBeInstanceOf<AuthenticationEvent.EventType.CancelSignIn>()
    }
}
