/*
 * Copyright 2022 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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

package com.amplifyframework.auth.cognito

import aws.sdk.kotlin.services.cognitoidentityprovider.model.ChangePasswordRequest
import aws.sdk.kotlin.services.cognitoidentityprovider.model.ChangePasswordResponse
import com.amplifyframework.auth.AuthException
import com.amplifyframework.auth.options.AuthSignUpOptions
import com.amplifyframework.auth.result.AuthSignUpResult
import com.amplifyframework.core.Action
import com.amplifyframework.core.Consumer
import com.amplifyframework.logging.Logger
import com.amplifyframework.statemachine.codegen.data.AmplifyCredential
import com.amplifyframework.statemachine.codegen.data.AuthConfiguration
import com.amplifyframework.statemachine.codegen.data.CognitoUserPoolTokens
import com.amplifyframework.statemachine.codegen.states.AuthState
import com.amplifyframework.statemachine.codegen.states.AuthenticationState
import com.amplifyframework.statemachine.codegen.states.CredentialStoreState
import io.mockk.CapturingSlot
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.invoke
import io.mockk.mockk
import io.mockk.verify
import java.util.UUID
import kotlin.test.assertTrue
import org.junit.Before
import org.junit.Test

class RealAWSCognitoAuthPluginTest {

    private var logger = mockk<Logger>(relaxed = true)
    private var authConfiguration = mockk<AuthConfiguration>()
    private var cognitoAuthService = mockk<AWSCognitoAuthServiceBehavior>()
    private var authEnvironment = mockk<AuthEnvironment> {
        every { configuration } returns authConfiguration
        every { cognitoAuthService } returns this@RealAWSCognitoAuthPluginTest.cognitoAuthService
        every { logger } returns this@RealAWSCognitoAuthPluginTest.logger
    }
    private var authStateMachine = mockk<AuthStateMachine>(relaxed = true)
    private var credentialStoreStateMachine = mockk<CredentialStoreStateMachine>(relaxed = true)

    private lateinit var plugin: RealAWSCognitoAuthPlugin

    @Before
    fun setup() {
        plugin = RealAWSCognitoAuthPlugin(
            authConfiguration,
            authEnvironment,
            authStateMachine,
            credentialStoreStateMachine,
            logger
        )
    }

    @Test
    fun signUpFailsIfNotConfigured() {
        // GIVEN
        val onSuccess = mockk<Consumer<AuthSignUpResult>>()
        val onError = mockk<Consumer<AuthException>>(relaxed = true)
        val expectedAuthError = AuthException(
            "Sign up failed.",
            "Cognito User Pool not configured. Please check amplifyconfiguration.json file."
        )
        val currentAuthState = mockk<AuthState> {
            every { authNState } returns AuthenticationState.NotConfigured()
        }
        every { authStateMachine.getCurrentState(captureLambda()) } answers {
            lambda<(AuthState) -> Unit>().invoke(currentAuthState)
        }

        // WHEN
        plugin.signUp("user", "pass", AuthSignUpOptions.builder().build(), onSuccess, onError)

        // THEN
        verify(exactly = 0) { onSuccess.accept(any()) }
        verify { onError.accept(expectedAuthError) }
    }

    @Test
    fun `update password with success`() {
        // GIVEN
        val onSuccess = mockk<Action>(relaxed = true)
        val onError = mockk<Consumer<AuthException>>(relaxed = true)
        val credential = AmplifyCredential(
            CognitoUserPoolTokens("idToken", "accessToken", "refreshToken", 120L),
            null,
            null
        )

        val eventSlot = CapturingSlot<(CredentialStoreState) -> Unit>()
        every { credentialStoreStateMachine.listen(capture(eventSlot), any()) } answers {
            eventSlot.captured.invoke(CredentialStoreState.Success(credential))
            UUID.randomUUID()
        }
        coEvery {
            cognitoAuthService.cognitoIdentityProviderClient?.changePassword(any<ChangePasswordRequest>())
        } returns ChangePasswordResponse.invoke { }

        // WHEN
        plugin.updatePassword("old", "new", onSuccess, onError)
        Thread.sleep(1_000)
        assertTrue { eventSlot.isCaptured }

        verify { onSuccess.call() }
        coVerify(exactly = 0) { onError.accept(any()) }
    }

    @Test
    fun `update password fails when cognitoIdentityProviderClient not set`() {
        val onSuccess = mockk<Action>(relaxed = true)
        val onError = mockk<Consumer<AuthException>>(relaxed = true)
        val credential = AmplifyCredential(
            CognitoUserPoolTokens("idToken", "accessToken", "refreshToken", 120L),
            null,
            null
        )
        val slot = CapturingSlot<(CredentialStoreState) -> Unit>()
        every { credentialStoreStateMachine.listen(capture(slot), any()) } answers
            {
                slot.captured.invoke(CredentialStoreState.Success(credential))
                UUID.randomUUID()
            }
        plugin.updatePassword("old", "new", onSuccess, onError)
        Thread.sleep(1_000)
        assertTrue { slot.isCaptured }
        verify(exactly = 0) { onSuccess.call() }
        coVerify { onError.accept(any()) }
    }
}
