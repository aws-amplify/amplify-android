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
import aws.sdk.kotlin.services.cognitoidentityprovider.model.InitiateAuthResponse
import com.amplifyframework.auth.cognito.AWSCognitoAuthService
import com.amplifyframework.auth.cognito.AuthConfiguration
import com.amplifyframework.auth.cognito.AuthEnvironment
import com.amplifyframework.auth.cognito.StoreClientBehavior
import com.amplifyframework.logging.Logger
import com.amplifyframework.statemachine.EventDispatcher
import com.amplifyframework.statemachine.StateMachineEvent
import com.amplifyframework.statemachine.codegen.data.AmplifyCredential
import com.amplifyframework.statemachine.codegen.data.CognitoUserPoolTokens
import com.amplifyframework.statemachine.codegen.data.CredentialType
import com.amplifyframework.statemachine.codegen.data.SignInData
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
class AutoSignInCognitoActionsTest {

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
    fun `auto sign in succeeds with signed in state`() = runTest {
        val username = "USERNAME"
        val userSub = "userId"
        val session = "SESSION"
        val accessToken = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiJ1c2VySWQiLCJ1c2VybmFtZSI6InVzZXJuYW1lIiw" +
            "iZXhwIjoxNTE2MjM5MDIyLCJvcmlnaW5fanRpIjoib3JpZ2luX2p0aSJ9.Xqa-vjJe5wwwsqeRAdHf8kTBn_rYSkDn2lB7xj9Z1xU"
        val idToken = "ID_TOKEN"
        val refreshToken = "REFRESH_TOKEN"
        coEvery { cognitoIdentityProviderClientMock.initiateAuth(any()) } returns InitiateAuthResponse {
            this.session = session
            this.authenticationResult = AuthenticationResultType.invoke {
                this.accessToken = accessToken
                this.idToken = idToken
                this.refreshToken = refreshToken
                this.expiresIn = 100
            }
        }

        val signInData = SignInData.AutoSignInData(username, session, mapOf(), userSub)
        val initiateAutoSignInEvent = SignInEvent.EventType.InitiateAutoSignIn(signInData)
        SignInCognitoActions.autoSignInAction(initiateAutoSignInEvent).execute(dispatcher, authEnvironment)

        val cognitoUserPoolTokens = CognitoUserPoolTokens(idToken, accessToken, refreshToken, 100)
        val signInMethod = SignInMethod.ApiBased(SignInMethod.ApiBased.AuthType.USER_AUTH)

        val event = capturedEvent.captured.shouldBeInstanceOf<AuthenticationEvent>()
        val data = event.eventType.shouldBeInstanceOf<AuthenticationEvent.EventType.SignInCompleted>().signedInData

        data.username shouldBe username
        data.userId shouldBe userSub
        data.signInMethod shouldBe signInMethod
        data.cognitoUserPoolTokens shouldBe cognitoUserPoolTokens
    }

    @Test
    fun `auto sign in fails with error when user pool tokens are invalid`() = runTest {
        val username = "USERNAME"
        val userSub = "123"
        val session = "SESSION"
        val accessToken = "INVALID_JSON"
        coEvery { cognitoIdentityProviderClientMock.initiateAuth(any()) } returns InitiateAuthResponse {
            this.session = session
            this.authenticationResult = AuthenticationResultType.invoke {
                this.accessToken = accessToken
                this.idToken = null
                this.refreshToken = null
                this.expiresIn = 100
            }
        }

        val signInData = SignInData.AutoSignInData(username, session, mapOf(), userSub)
        val initiateAutoSignInEvent = SignInEvent.EventType.InitiateAutoSignIn(signInData)
        SignInCognitoActions.autoSignInAction(initiateAutoSignInEvent).execute(dispatcher, authEnvironment)

        val expectedEvent = AuthenticationEvent(AuthenticationEvent.EventType.CancelSignIn())

        capturedEvent.captured.type shouldBe expectedEvent.type
    }
}
