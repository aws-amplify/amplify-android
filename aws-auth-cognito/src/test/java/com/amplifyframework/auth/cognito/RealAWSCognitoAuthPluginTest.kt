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

import com.amplifyframework.auth.AuthException
import com.amplifyframework.auth.options.AuthSignUpOptions
import com.amplifyframework.auth.result.AuthSignUpResult
import com.amplifyframework.core.Consumer
import com.amplifyframework.logging.Logger
import com.amplifyframework.statemachine.codegen.data.AuthConfiguration
import com.amplifyframework.statemachine.codegen.states.AuthState
import com.amplifyframework.statemachine.codegen.states.AuthenticationState
import io.mockk.every
import io.mockk.invoke
import io.mockk.mockk
import io.mockk.verify
import org.junit.Before
import org.junit.Test

class RealAWSCognitoAuthPluginTest {

    private var logger = mockk<Logger>(relaxed = true)
    private var authConfiguration = mockk<AuthConfiguration>()
    private var authEnvironment = mockk<AuthEnvironment> {
        every { configuration } returns authConfiguration
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
        verify(exactly = 0) {onSuccess.accept(any()) }
        verify { onError.accept(expectedAuthError) }
    }

}