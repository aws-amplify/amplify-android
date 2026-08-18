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
import aws.sdk.kotlin.services.cognitoidentityprovider.model.AuthenticationResultType
import aws.sdk.kotlin.services.cognitoidentityprovider.model.GetTokensFromRefreshTokenResponse
import aws.sdk.kotlin.services.cognitoidentityprovider.model.NotAuthorizedException
import com.amplifyframework.auth.cognito.AWSCognitoAuthService
import com.amplifyframework.auth.cognito.AuthConfiguration
import com.amplifyframework.auth.cognito.AuthEnvironment
import com.amplifyframework.auth.cognito.StoreClientBehavior
import com.amplifyframework.auth.cognito.mockSignedInData
import com.amplifyframework.auth.exceptions.SessionExpiredException
import com.amplifyframework.logging.Logger
import com.amplifyframework.statemachine.EventDispatcher
import com.amplifyframework.statemachine.StateMachineEvent
import com.amplifyframework.statemachine.codegen.data.AmplifyCredential
import com.amplifyframework.statemachine.codegen.data.CognitoUserPoolTokens
import com.amplifyframework.statemachine.codegen.data.CredentialType
import com.amplifyframework.statemachine.codegen.data.DeviceMetadata
import com.amplifyframework.statemachine.codegen.data.UserPoolConfiguration
import com.amplifyframework.statemachine.codegen.events.AuthorizationEvent
import com.amplifyframework.statemachine.codegen.events.RefreshSessionEvent
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
class FetchAuthSessionCognitoActionsTest {

    private val pool = mockk<UserPoolConfiguration> {
        every { appClient } returns "client"
        every { appClientSecret } returns "secret"
        every { region } returns "us-east-1"
        every { poolId } returns "pool_id"
    }
    private val configuration = mockk<AuthConfiguration> {
        every { userPool } returns pool
        every { identityPool } returns null
    }
    private val cognitoAuthService = mockk<AWSCognitoAuthService>()
    private val credentialStoreClient = mockk<StoreClientBehavior> {
        coEvery { loadCredentials(any<CredentialType.Device>()) } returns AmplifyCredential.DeviceData(
            DeviceMetadata.Metadata(deviceKey = "device_key", deviceGroupKey = "device_group")
        )
    }
    private val logger = mockk<Logger>(relaxed = true)
    private val cognitoIdentityProviderClientMock = mockk<CognitoIdentityProviderClient>()

    private val capturedEvent = slot<StateMachineEvent>()
    private val dispatcher = mockk<EventDispatcher> {
        every { send(capture(capturedEvent)) } just Runs
    }

    private lateinit var authEnvironment: AuthEnvironment

    private val notAuthorizedSignedInData = mockSignedInData(
        username = "username",
        cognitoUserPoolTokens = CognitoUserPoolTokens(
            accessToken = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9." +
                "eyJzdWIiOiJ1c2VySWQiLCJ1c2VybmFtZSI6InVzZXJuYW1lIiwiZXhwIjoxNTE2MjM5MDIyfQ." +
                "XbPfbIHMI6arZ3Y922BhjWgQzWXcXNrz0ogtVhfEd2o",
            idToken = "old_id",
            refreshToken = "refresh_token",
            expiration = 0
        )
    )

    private fun capturedThrownError(): Exception {
        val event = capturedEvent.captured
        event.shouldBeInstanceOf<AuthorizationEvent>()
        val eventType = event.eventType
        eventType.shouldBeInstanceOf<AuthorizationEvent.EventType.ThrowError>()
        return eventType.exception
    }

    /**
     * The service message travels in [ServiceErrorMetadata], which the wire deserializer populates and the
     * generated builder does not, so stub it rather than constructing the exception directly. Note that
     * NotAuthorizedException.message is synthesized from error metadata and is not the service message.
     */
    private fun notAuthorizedWithServiceMessage(serviceMessage: String?) = mockk<NotAuthorizedException> {
        every { sdkErrorMetadata } returns mockk {
            every { errorMessage } returns serviceMessage
        }
    }

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
    fun `refreshUserPoolTokensAction calls getTokensFromRefreshToken and handles token rotation`() = runTest {
        val originalRefreshToken = "original_refresh_token"
        val newRefreshToken = "new_refresh_token"

        coEvery {
            cognitoIdentityProviderClientMock.getTokensFromRefreshToken(any())
        } returns GetTokensFromRefreshTokenResponse {
            authenticationResult = AuthenticationResultType {
                this.accessToken = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9." +
                    "eyJzdWIiOiJ1c2VySWQiLCJ1c2VybmFtZSI6InVzZXJuYW1lIiwiZXhwIjoxNTE2MjM5MDIyfQ." +
                    "XbPfbIHMI6arZ3Y922BhjWgQzWXcXNrz0ogtVhfEd2o"
                this.idToken = "id_token"
                this.refreshToken = newRefreshToken
                this.expiresIn = 3600
            }
        }

        val signedInData = mockSignedInData(
            username = "username",
            cognitoUserPoolTokens = CognitoUserPoolTokens(
                accessToken = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9." +
                    "eyJzdWIiOiJ1c2VySWQiLCJ1c2VybmFtZSI6InVzZXJuYW1lIiwiZXhwIjoxNTE2MjM5MDIyfQ." +
                    "XbPfbIHMI6arZ3Y922BhjWgQzWXcXNrz0ogtVhfEd2o",
                idToken = "old_id",
                refreshToken = originalRefreshToken,
                expiration = 0
            )
        )

        FetchAuthSessionCognitoActions.refreshUserPoolTokensAction(signedInData)
            .execute(dispatcher, authEnvironment)

        val event = capturedEvent.captured.shouldBeInstanceOf<RefreshSessionEvent>()
        val refreshedData = event.eventType.shouldBeInstanceOf<RefreshSessionEvent.EventType.Refreshed>().signedInData
        refreshedData.cognitoUserPoolTokens.refreshToken?.tokenValue shouldBe newRefreshToken
    }

    @Test
    fun `refreshUserPoolTokensAction falls back to original refresh token when rotation is not enabled`() = runTest {
        val originalRefreshToken = "original_refresh_token"

        coEvery {
            cognitoIdentityProviderClientMock.getTokensFromRefreshToken(any())
        } returns GetTokensFromRefreshTokenResponse {
            authenticationResult = AuthenticationResultType {
                this.accessToken = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9." +
                    "eyJzdWIiOiJ1c2VySWQiLCJ1c2VybmFtZSI6InVzZXJuYW1lIiwiZXhwIjoxNTE2MjM5MDIyfQ." +
                    "XbPfbIHMI6arZ3Y922BhjWgQzWXcXNrz0ogtVhfEd2o"
                this.idToken = "id_token"
                this.refreshToken = null
                this.expiresIn = 3600
            }
        }

        val signedInData = mockSignedInData(
            username = "username",
            cognitoUserPoolTokens = CognitoUserPoolTokens(
                accessToken = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9." +
                    "eyJzdWIiOiJ1c2VySWQiLCJ1c2VybmFtZSI6InVzZXJuYW1lIiwiZXhwIjoxNTE2MjM5MDIyfQ." +
                    "XbPfbIHMI6arZ3Y922BhjWgQzWXcXNrz0ogtVhfEd2o",
                idToken = "old_id",
                refreshToken = originalRefreshToken,
                expiration = 0
            )
        )

        FetchAuthSessionCognitoActions.refreshUserPoolTokensAction(signedInData)
            .execute(dispatcher, authEnvironment)

        val event = capturedEvent.captured.shouldBeInstanceOf<RefreshSessionEvent>()
        val refreshedData = event.eventType.shouldBeInstanceOf<RefreshSessionEvent.EventType.Refreshed>().signedInData
        refreshedData.cognitoUserPoolTokens.refreshToken?.tokenValue shouldBe originalRefreshToken
    }

    @Test
    fun `refreshUserPoolTokensAction handles NotAuthorizedException`() = runTest {
        coEvery {
            cognitoIdentityProviderClientMock.getTokensFromRefreshToken(any())
        } throws NotAuthorizedException {
            message = "Token expired"
        }

        val signedInData = mockSignedInData(
            username = "username",
            cognitoUserPoolTokens = CognitoUserPoolTokens(
                accessToken = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9." +
                    "eyJzdWIiOiJ1c2VySWQiLCJ1c2VybmFtZSI6InVzZXJuYW1lIiwiZXhwIjoxNTE2MjM5MDIyfQ." +
                    "XbPfbIHMI6arZ3Y922BhjWgQzWXcXNrz0ogtVhfEd2o",
                idToken = "old_id",
                refreshToken = "refresh_token",
                expiration = 0
            )
        )

        FetchAuthSessionCognitoActions.refreshUserPoolTokensAction(signedInData)
            .execute(dispatcher, authEnvironment)

        capturedEvent.captured.shouldBeInstanceOf<AuthorizationEvent>()
    }

    @Test
    fun `refreshUserPoolTokensAction reports the reason Cognito rejected the refresh token`() = runTest {
        // Cognito uses NotAuthorizedException for several unrelated failures and only distinguishes them in
        // the service message, so the message must reach the caller for the failures to be told apart.
        val reasons = listOf(
            "Refresh Token has expired",
            "Refresh Token has been revoked",
            "Invalid Refresh Token."
        )

        reasons.forEach { reason ->
            val serviceException = notAuthorizedWithServiceMessage(reason)
            coEvery {
                cognitoIdentityProviderClientMock.getTokensFromRefreshToken(any())
            } throws serviceException

            FetchAuthSessionCognitoActions.refreshUserPoolTokensAction(notAuthorizedSignedInData)
                .execute(dispatcher, authEnvironment)

            val error = capturedThrownError()
            error.shouldBeInstanceOf<SessionExpiredException>()
            error.message shouldBe "Your session has expired. ($reason)"
            // The originating exception stays reachable for callers that need the raw service error.
            error.cause shouldBe serviceException
        }
    }

    @Test
    fun `refreshUserPoolTokensAction falls back to the default message when Cognito sends no reason`() = runTest {
        coEvery {
            cognitoIdentityProviderClientMock.getTokensFromRefreshToken(any())
        } throws notAuthorizedWithServiceMessage(null)

        FetchAuthSessionCognitoActions.refreshUserPoolTokensAction(notAuthorizedSignedInData)
            .execute(dispatcher, authEnvironment)

        val error = capturedThrownError()
        error.shouldBeInstanceOf<SessionExpiredException>()
        error.message shouldBe "Your session has expired."
    }

    @Test
    fun `refreshUserPoolTokensAction ignores a blank reason from Cognito`() = runTest {
        coEvery {
            cognitoIdentityProviderClientMock.getTokensFromRefreshToken(any())
        } throws notAuthorizedWithServiceMessage("   ")

        FetchAuthSessionCognitoActions.refreshUserPoolTokensAction(notAuthorizedSignedInData)
            .execute(dispatcher, authEnvironment)

        val error = capturedThrownError()
        error.shouldBeInstanceOf<SessionExpiredException>()
        error.message shouldBe "Your session has expired."
    }

    @Test
    fun `refreshUserPoolTokensAction handles generic exception`() = runTest {
        coEvery {
            cognitoIdentityProviderClientMock.getTokensFromRefreshToken(any())
        } throws RuntimeException("Network error")

        val signedInData = mockSignedInData(
            username = "username",
            cognitoUserPoolTokens = CognitoUserPoolTokens(
                accessToken = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9." +
                    "eyJzdWIiOiJ1c2VySWQiLCJ1c2VybmFtZSI6InVzZXJuYW1lIiwiZXhwIjoxNTE2MjM5MDIyfQ." +
                    "XbPfbIHMI6arZ3Y922BhjWgQzWXcXNrz0ogtVhfEd2o",
                idToken = "old_id",
                refreshToken = "refresh_token",
                expiration = 0
            )
        )

        FetchAuthSessionCognitoActions.refreshUserPoolTokensAction(signedInData)
            .execute(dispatcher, authEnvironment)

        capturedEvent.captured.shouldBeInstanceOf<AuthorizationEvent>()
    }

    @Test
    fun `refreshUserPoolTokensAction handles null authentication result with identity pool`() = runTest {
        every { configuration.identityPool } returns mockk {
            every { poolId } returns "identity_pool_id"
        }

        coEvery {
            cognitoIdentityProviderClientMock.getTokensFromRefreshToken(any())
        } returns GetTokensFromRefreshTokenResponse {
            authenticationResult = null
        }

        val signedInData = mockSignedInData(
            username = "username",
            cognitoUserPoolTokens = CognitoUserPoolTokens(
                accessToken = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9." +
                    "eyJzdWIiOiJ1c2VySWQiLCJ1c2VybmFtZSI6InVzZXJuYW1lIiwiZXhwIjoxNTE2MjM5MDIyfQ." +
                    "XbPfbIHMI6arZ3Y922BhjWgQzWXcXNrz0ogtVhfEd2o",
                idToken = "old_id",
                refreshToken = "refresh_token",
                expiration = 0
            )
        )

        FetchAuthSessionCognitoActions.refreshUserPoolTokensAction(signedInData)
            .execute(dispatcher, authEnvironment)

        capturedEvent.captured.shouldBeInstanceOf<AuthorizationEvent>()
    }

    @Test
    fun `refreshUserPoolTokensAction includes device key when available`() = runTest {
        val requestSlot =
            slot<aws.sdk.kotlin.services.cognitoidentityprovider.model.GetTokensFromRefreshTokenRequest>()
        coEvery {
            cognitoIdentityProviderClientMock.getTokensFromRefreshToken(capture(requestSlot))
        } returns GetTokensFromRefreshTokenResponse {
            authenticationResult = AuthenticationResultType {
                this.accessToken = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9." +
                    "eyJzdWIiOiJ1c2VySWQiLCJ1c2VybmFtZSI6InVzZXJuYW1lIiwiZXhwIjoxNTE2MjM5MDIyfQ." +
                    "XbPfbIHMI6arZ3Y922BhjWgQzWXcXNrz0ogtVhfEd2o"
                this.idToken = "id_token"
                this.refreshToken = "new_refresh_token"
                this.expiresIn = 3600
            }
        }

        val signedInData = mockSignedInData(
            username = "username",
            cognitoUserPoolTokens = CognitoUserPoolTokens(
                accessToken = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9." +
                    "eyJzdWIiOiJ1c2VySWQiLCJ1c2VybmFtZSI6InVzZXJuYW1lIiwiZXhwIjoxNTE2MjM5MDIyfQ." +
                    "XbPfbIHMI6arZ3Y922BhjWgQzWXcXNrz0ogtVhfEd2o",
                idToken = "old_id",
                refreshToken = "refresh_token",
                expiration = 0
            )
        )

        FetchAuthSessionCognitoActions.refreshUserPoolTokensAction(signedInData)
            .execute(dispatcher, authEnvironment)

        requestSlot.captured.deviceKey shouldBe "device_key"
    }

    @Test
    fun `refreshUserPoolTokensAction works without device metadata`() = runTest {
        coEvery {
            credentialStoreClient.loadCredentials(any<CredentialType.Device>())
        } returns AmplifyCredential.DeviceData(DeviceMetadata.Empty)

        val requestSlot =
            slot<aws.sdk.kotlin.services.cognitoidentityprovider.model.GetTokensFromRefreshTokenRequest>()
        coEvery {
            cognitoIdentityProviderClientMock.getTokensFromRefreshToken(capture(requestSlot))
        } returns GetTokensFromRefreshTokenResponse {
            authenticationResult = AuthenticationResultType {
                this.accessToken = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9." +
                    "eyJzdWIiOiJ1c2VySWQiLCJ1c2VybmFtZSI6InVzZXJuYW1lIiwiZXhwIjoxNTE2MjM5MDIyfQ." +
                    "XbPfbIHMI6arZ3Y922BhjWgQzWXcXNrz0ogtVhfEd2o"
                this.idToken = "id_token"
                this.refreshToken = "new_refresh_token"
                this.expiresIn = 3600
            }
        }

        val signedInData = mockSignedInData(
            username = "username",
            cognitoUserPoolTokens = CognitoUserPoolTokens(
                accessToken = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9." +
                    "eyJzdWIiOiJ1c2VySWQiLCJ1c2VybmFtZSI6InVzZXJuYW1lIiwiZXhwIjoxNTE2MjM5MDIyfQ." +
                    "XbPfbIHMI6arZ3Y922BhjWgQzWXcXNrz0ogtVhfEd2o",
                idToken = "old_id",
                refreshToken = "refresh_token",
                expiration = 0
            )
        )

        FetchAuthSessionCognitoActions.refreshUserPoolTokensAction(signedInData)
            .execute(dispatcher, authEnvironment)

        requestSlot.captured.deviceKey shouldBe null
        capturedEvent.captured.shouldBeInstanceOf<RefreshSessionEvent>()
    }

    @Test
    fun `refreshUserPoolTokensAction works without client secret`() = runTest {
        val poolWithoutSecret = mockk<UserPoolConfiguration> {
            every { appClient } returns "client"
            every { appClientSecret } returns null
            every { region } returns "us-east-1"
            every { poolId } returns "pool_id"
        }
        val configWithoutSecret = mockk<AuthConfiguration> {
            every { userPool } returns poolWithoutSecret
            every { identityPool } returns null
        }
        val envWithoutSecret = AuthEnvironment(
            ApplicationProvider.getApplicationContext(),
            configWithoutSecret,
            cognitoAuthService,
            credentialStoreClient,
            null,
            null,
            logger
        )

        val requestSlot =
            slot<aws.sdk.kotlin.services.cognitoidentityprovider.model.GetTokensFromRefreshTokenRequest>()
        coEvery {
            cognitoIdentityProviderClientMock.getTokensFromRefreshToken(capture(requestSlot))
        } returns GetTokensFromRefreshTokenResponse {
            authenticationResult = AuthenticationResultType {
                this.accessToken = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9." +
                    "eyJzdWIiOiJ1c2VySWQiLCJ1c2VybmFtZSI6InVzZXJuYW1lIiwiZXhwIjoxNTE2MjM5MDIyfQ." +
                    "XbPfbIHMI6arZ3Y922BhjWgQzWXcXNrz0ogtVhfEd2o"
                this.idToken = "id_token"
                this.refreshToken = "new_refresh_token"
                this.expiresIn = 3600
            }
        }

        val signedInData = mockSignedInData(
            username = "username",
            cognitoUserPoolTokens = CognitoUserPoolTokens(
                accessToken = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9." +
                    "eyJzdWIiOiJ1c2VySWQiLCJ1c2VybmFtZSI6InVzZXJuYW1lIiwiZXhwIjoxNTE2MjM5MDIyfQ." +
                    "XbPfbIHMI6arZ3Y922BhjWgQzWXcXNrz0ogtVhfEd2o",
                idToken = "old_id",
                refreshToken = "refresh_token",
                expiration = 0
            )
        )

        FetchAuthSessionCognitoActions.refreshUserPoolTokensAction(signedInData)
            .execute(dispatcher, envWithoutSecret)

        requestSlot.captured.clientSecret shouldBe null
        capturedEvent.captured.shouldBeInstanceOf<RefreshSessionEvent>()
    }

    @Test
    fun `refreshUserPoolTokensAction includes client secret when available`() = runTest {
        val requestSlot =
            slot<aws.sdk.kotlin.services.cognitoidentityprovider.model.GetTokensFromRefreshTokenRequest>()
        coEvery {
            cognitoIdentityProviderClientMock.getTokensFromRefreshToken(capture(requestSlot))
        } returns GetTokensFromRefreshTokenResponse {
            authenticationResult = AuthenticationResultType {
                this.accessToken = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9." +
                    "eyJzdWIiOiJ1c2VySWQiLCJ1c2VybmFtZSI6InVzZXJuYW1lIiwiZXhwIjoxNTE2MjM5MDIyfQ." +
                    "XbPfbIHMI6arZ3Y922BhjWgQzWXcXNrz0ogtVhfEd2o"
                this.idToken = "id_token"
                this.refreshToken = "new_refresh_token"
                this.expiresIn = 3600
            }
        }

        val signedInData = mockSignedInData(
            username = "username",
            cognitoUserPoolTokens = CognitoUserPoolTokens(
                accessToken = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9." +
                    "eyJzdWIiOiJ1c2VySWQiLCJ1c2VybmFtZSI6InVzZXJuYW1lIiwiZXhwIjoxNTE2MjM5MDIyfQ." +
                    "XbPfbIHMI6arZ3Y922BhjWgQzWXcXNrz0ogtVhfEd2o",
                idToken = "old_id",
                refreshToken = "refresh_token",
                expiration = 0
            )
        )

        FetchAuthSessionCognitoActions.refreshUserPoolTokensAction(signedInData)
            .execute(dispatcher, authEnvironment)

        requestSlot.captured.clientSecret shouldBe "secret"
        capturedEvent.captured.shouldBeInstanceOf<RefreshSessionEvent>()
    }
}
