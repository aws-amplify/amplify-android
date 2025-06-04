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
import com.amplifyframework.statemachine.codegen.data.DeviceMetadata
import com.amplifyframework.statemachine.codegen.data.UserPoolConfiguration
import com.amplifyframework.statemachine.codegen.events.AuthenticationEvent
import com.amplifyframework.statemachine.codegen.events.SRPEvent
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import kotlin.test.assertEquals
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class SRPCognitoActionsTest {
    private val username = "username"
    private val password = "password"
    private val dummyToken = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiIxMjM0NTY3ODkwIiwibmFtZSI6IkpvaG4g" +
        "RG9lIiwiaWF0IjoxNTE2MjM5MDIyfQ.XbPfbIHMI6arZ3Y922BhjWgQzWXcXNrz0ogtVhfEd2o"
    private val dummyIdToken = "id-token"
    private val dummyRefreshToken = "refreshToken"
    private val bearerToken = "Bearer"
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

    private lateinit var authEnvironment: AuthEnvironment

    private val challengeParams = mapOf(
        "SALT" to "salt",
        "SECRET_BLOCK" to "secret-block",
        "SRP_B" to "srp-b",
        "USERNAME" to username,
        "USER_ID_FOR_SRP" to username
    )

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
    fun `Initiate USER_SRP_AUTH with correct password`() = runTest {
        val capturedRequest = slot<InitiateAuthRequest>()
        coEvery {
            cognitoIdentityProviderClientMock.initiateAuth(capture(capturedRequest))
        }.answers {
            InitiateAuthResponse.invoke {
                this.session = dummySession
                this.challengeParameters = challengeParams
                this.challengeName = ChallengeNameType.PasswordVerifier
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

        val expectedEvent = SRPEvent(
            SRPEvent.EventType.RespondPasswordVerifier(
                challengeParams.plus(mapOf("DEVICE_KEY" to dummyDeviceKey)),
                emptyMap(),
                dummySession
            )
        )

        SRPCognitoActions.initiateSRPAuthAction(
            SRPEvent.EventType.InitiateSRP(
                username = username,
                password = password,
                authFlowType = AuthFlowType.USER_SRP_AUTH,
                metadata = mapOf("KEY" to "VALUE")
            )
        ).execute(dispatcher, authEnvironment)

        assertEquals(
            expectedEvent.type,
            capturedEvent.captured.type
        )
        assertEquals(
            dummySession,
            ((capturedEvent.captured as SRPEvent).eventType as SRPEvent.EventType.RespondPasswordVerifier).session
        )
        assertEquals(
            challengeParams.plus(mapOf("DEVICE_KEY" to dummyDeviceKey)),
            ((capturedEvent.captured as SRPEvent).eventType as SRPEvent.EventType.RespondPasswordVerifier)
                .challengeParameters
        )
        assertEquals(
            mapOf("KEY" to "VALUE"),
            ((capturedEvent.captured as SRPEvent).eventType as SRPEvent.EventType.RespondPasswordVerifier)
                .metadata
        )
    }

    @Test
    fun `Initiate USER_SRP_AUTH with incorrect password`() = runTest {
        val capturedRequest = slot<InitiateAuthRequest>()
        coEvery {
            cognitoIdentityProviderClientMock.initiateAuth(capture(capturedRequest))
        }.throws(NotAuthorizedException.invoke { })

        val expectedEvent = AuthenticationEvent(AuthenticationEvent.EventType.CancelSignIn())

        SRPCognitoActions.initiateSRPAuthAction(
            SRPEvent.EventType.InitiateSRP(
                username = username,
                password = password,
                authFlowType = AuthFlowType.USER_SRP_AUTH,
                metadata = mapOf("KEY" to "VALUE")
            )
        ).execute(dispatcher, authEnvironment)

        assertEquals(
            expectedEvent.type,
            capturedEvent.captured.type
        )
    }

    @Test
    fun `RespondToAuth USER_AUTH with correct password`() = runTest {
        val capturedRequest = slot<RespondToAuthChallengeRequest>()
        coEvery {
            cognitoIdentityProviderClientMock.respondToAuthChallenge(capture(capturedRequest))
        }.answers {
            RespondToAuthChallengeResponse.invoke {
                this.session = dummySession
                this.challengeParameters = challengeParams
                this.challengeName = ChallengeNameType.PasswordVerifier
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

        val expectedEvent = SRPEvent(
            SRPEvent.EventType.RespondPasswordVerifier(
                challengeParams.plus(mapOf("DEVICE_KEY" to dummyDeviceKey)),
                emptyMap(),
                dummySession
            )
        )

        SRPCognitoActions.initiateSRPAuthAction(
            SRPEvent.EventType.InitiateSRP(
                username = username,
                password = password,
                authFlowType = AuthFlowType.USER_AUTH,
                metadata = mapOf("KEY" to "VALUE"),
                respondToAuthChallenge = AuthChallenge(
                    challengeName = ChallengeNameType.SelectChallenge.value,
                    session = dummySession,
                    parameters = emptyMap()
                )
            )
        ).execute(dispatcher, authEnvironment)

        assertEquals(
            expectedEvent.type,
            capturedEvent.captured.type
        )
        assertEquals(
            dummySession,
            ((capturedEvent.captured as SRPEvent).eventType as SRPEvent.EventType.RespondPasswordVerifier).session
        )
        assertEquals(
            challengeParams.plus(mapOf("DEVICE_KEY" to dummyDeviceKey)),
            ((capturedEvent.captured as SRPEvent).eventType as SRPEvent.EventType.RespondPasswordVerifier)
                .challengeParameters
        )
        assertEquals(
            mapOf("KEY" to "VALUE"),
            ((capturedEvent.captured as SRPEvent).eventType as SRPEvent.EventType.RespondPasswordVerifier)
                .metadata
        )
    }

    @Test
    fun `RespondToAuth USER_AUTH with incorrect password`() = runTest {
        val capturedRequest = slot<RespondToAuthChallengeRequest>()
        coEvery {
            cognitoIdentityProviderClientMock.respondToAuthChallenge(capture(capturedRequest))
        }.throws(NotAuthorizedException.invoke { })

        val expectedEvent = AuthenticationEvent(AuthenticationEvent.EventType.CancelSignIn())

        SRPCognitoActions.initiateSRPAuthAction(
            SRPEvent.EventType.InitiateSRP(
                username = username,
                password = password,
                authFlowType = AuthFlowType.USER_AUTH,
                metadata = mapOf("KEY" to "VALUE"),
                respondToAuthChallenge = AuthChallenge(
                    challengeName = ChallengeNameType.SelectChallenge.value,
                    session = dummySession,
                    parameters = emptyMap()
                )
            )
        ).execute(dispatcher, authEnvironment)

        assertEquals(
            expectedEvent.type,
            capturedEvent.captured.type
        )
    }

    @Test
    fun `InitAuth response with no challenge params cancels sign in`() = runTest {
        val capturedRequest = slot<InitiateAuthRequest>()
        coEvery {
            cognitoIdentityProviderClientMock.initiateAuth(capture(capturedRequest))
        }.answers {
            InitiateAuthResponse.invoke {
                this.session = dummySession
                this.challengeParameters = null
                this.challengeName = ChallengeNameType.PasswordVerifier
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

        val expectedEvent = AuthenticationEvent(AuthenticationEvent.EventType.CancelSignIn())

        SRPCognitoActions.initiateSRPAuthAction(
            SRPEvent.EventType.InitiateSRP(
                username = username,
                password = password,
                authFlowType = AuthFlowType.USER_SRP_AUTH,
                metadata = mapOf("KEY" to "VALUE"),
                respondToAuthChallenge = AuthChallenge(
                    challengeName = ChallengeNameType.SelectChallenge.value,
                    session = dummySession,
                    parameters = emptyMap()
                )
            )
        ).execute(dispatcher, authEnvironment)

        assertEquals(
            expectedEvent.type,
            capturedEvent.captured.type
        )
    }

    @Test
    fun `RespondToAuth response with no challenge params cancels sign in`() = runTest {
        val capturedRequest = slot<RespondToAuthChallengeRequest>()
        coEvery {
            cognitoIdentityProviderClientMock.respondToAuthChallenge(capture(capturedRequest))
        }.answers {
            RespondToAuthChallengeResponse.invoke {
                this.session = dummySession
                this.challengeParameters = null
                this.challengeName = ChallengeNameType.PasswordVerifier
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

        val expectedEvent = AuthenticationEvent(AuthenticationEvent.EventType.CancelSignIn())

        SRPCognitoActions.initiateSRPAuthAction(
            SRPEvent.EventType.InitiateSRP(
                username = username,
                password = password,
                authFlowType = AuthFlowType.USER_AUTH,
                metadata = mapOf("KEY" to "VALUE"),
                respondToAuthChallenge = AuthChallenge(
                    challengeName = ChallengeNameType.SelectChallenge.value,
                    session = dummySession,
                    parameters = emptyMap()
                )
            )
        ).execute(dispatcher, authEnvironment)

        assertEquals(
            expectedEvent.type,
            capturedEvent.captured.type
        )
    }

    @Test
    fun `InitAuth response with unexpected challenge name cancels sign in`() = runTest {
        val capturedRequest = slot<InitiateAuthRequest>()
        coEvery {
            cognitoIdentityProviderClientMock.initiateAuth(capture(capturedRequest))
        }.answers {
            InitiateAuthResponse.invoke {
                this.session = dummySession
                this.challengeParameters = null
                this.challengeName = ChallengeNameType.SelectChallenge
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

        val expectedEvent = AuthenticationEvent(AuthenticationEvent.EventType.CancelSignIn())

        SRPCognitoActions.initiateSRPAuthAction(
            SRPEvent.EventType.InitiateSRP(
                username = username,
                password = password,
                authFlowType = AuthFlowType.USER_SRP_AUTH,
                metadata = mapOf("KEY" to "VALUE"),
                respondToAuthChallenge = AuthChallenge(
                    challengeName = ChallengeNameType.SelectChallenge.value,
                    session = dummySession,
                    parameters = emptyMap()
                )
            )
        ).execute(dispatcher, authEnvironment)

        assertEquals(
            expectedEvent.type,
            capturedEvent.captured.type
        )
    }

    @Test
    fun `RespondToAuth response with unexpected challenge name cancels sign in`() = runTest {
        val capturedRequest = slot<RespondToAuthChallengeRequest>()
        coEvery {
            cognitoIdentityProviderClientMock.respondToAuthChallenge(capture(capturedRequest))
        }.answers {
            RespondToAuthChallengeResponse.invoke {
                this.session = dummySession
                this.challengeParameters = null
                this.challengeName = ChallengeNameType.SelectChallenge
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

        val expectedEvent = AuthenticationEvent(AuthenticationEvent.EventType.CancelSignIn())

        SRPCognitoActions.initiateSRPAuthAction(
            SRPEvent.EventType.InitiateSRP(
                username = username,
                password = password,
                authFlowType = AuthFlowType.USER_AUTH,
                metadata = mapOf("KEY" to "VALUE"),
                respondToAuthChallenge = AuthChallenge(
                    challengeName = ChallengeNameType.SelectChallenge.value,
                    session = dummySession,
                    parameters = emptyMap()
                )
            )
        ).execute(dispatcher, authEnvironment)

        assertEquals(
            expectedEvent.type,
            capturedEvent.captured.type
        )
    }
}
