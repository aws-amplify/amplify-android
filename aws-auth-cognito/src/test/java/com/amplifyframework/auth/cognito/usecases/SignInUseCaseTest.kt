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

package com.amplifyframework.auth.cognito.usecases

import com.amplifyframework.auth.AuthChannelEventName
import com.amplifyframework.auth.AuthException
import com.amplifyframework.auth.AuthFactorType
import com.amplifyframework.auth.cognito.AuthConfiguration
import com.amplifyframework.auth.cognito.AuthStateMachine
import com.amplifyframework.auth.cognito.exceptions.configuration.InvalidUserPoolConfigurationException
import com.amplifyframework.auth.cognito.mockAuthState
import com.amplifyframework.auth.cognito.mockSignedInState
import com.amplifyframework.auth.cognito.mockSignedOutState
import com.amplifyframework.auth.cognito.options.AWSCognitoAuthSignInOptions
import com.amplifyframework.auth.cognito.options.AuthFlowType
import com.amplifyframework.auth.cognito.testUtil.withAuthEvent
import com.amplifyframework.auth.options.AuthSignInOptions
import com.amplifyframework.auth.plugins.core.AuthHubEventEmitter
import com.amplifyframework.auth.result.step.AuthSignInStep
import com.amplifyframework.statemachine.codegen.data.SignInData
import com.amplifyframework.statemachine.codegen.events.AuthenticationEvent
import com.amplifyframework.statemachine.codegen.states.AuthenticationState
import io.kotest.assertions.throwables.shouldThrowAny
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.justRun
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class SignInUseCaseTest {
    private val stateFlow = MutableStateFlow(mockSignedOutState())
    private val stateMachine: AuthStateMachine = mockk {
        justRun { send(any()) }
        every { state } returns stateFlow
        coEvery { getCurrentState() } answers { stateFlow.value }
    }
    private val configuration: AuthConfiguration = mockk {
        every { authFlowType } returns AuthFlowType.USER_SRP_AUTH
    }
    private val emitter: AuthHubEventEmitter = mockk(relaxed = true)

    private val useCase = SignInUseCase(
        stateMachine = stateMachine,
        configuration = configuration,
        hubEmitter = emitter
    )

    @Test
    fun `fails if not configured`() = runTest {
        val expectedAuthError = InvalidUserPoolConfigurationException()
        stateFlow.value = mockAuthState(AuthenticationState.NotConfigured())

        shouldThrowAny {
            useCase.execute("user", "password", AuthSignInOptions.defaults())
        } shouldBe expectedAuthError
    }

    @Test
    fun `fails if authentication error occurs`() = runTest {
        val exception = AuthException("test", "test")
        stateFlow.value = mockAuthState(AuthenticationState.Error(exception))

        shouldThrowAny {
            useCase.execute("user", "password", AuthSignInOptions.defaults())
        } shouldBe exception
    }

    @Test
    fun `cancels existing sign in and proceeds`() = runTest {
        stateFlow.value = mockAuthState(AuthenticationState.SigningIn(mockk()))

        launch {
            useCase.execute("user", "password", AuthSignInOptions.defaults())
        }

        runCurrent()
        coVerify {
            stateMachine.send(match<AuthenticationEvent> { it.eventType is AuthenticationEvent.EventType.CancelSignIn })
        }

        stateFlow.value = mockSignedOutState()
        runCurrent()
        stateFlow.value = mockSignedInState()
    }

    @Test
    fun `sends SRP sign in event for USER_SRP_AUTH flow`() = runTest {
        stateFlow.value = mockAuthState(AuthenticationState.SignedOut(mockk()))

        launch {
            useCase.execute("user", "password", AuthSignInOptions.defaults())
        }

        runCurrent()
        stateFlow.value = mockSignedInState()

        coVerify {
            stateMachine.send(
                withArg<AuthenticationEvent> { event ->
                    val signInData = (event.eventType as AuthenticationEvent.EventType.SignInRequested).signInData
                    signInData.shouldBeInstanceOf<SignInData.SRPSignInData>()
                    signInData.username shouldBe "user"
                    signInData.password shouldBe "password"
                }
            )
        }
    }

    @Test
    fun `sends custom auth event for CUSTOM_AUTH flow`() = runTest {
        val options = AWSCognitoAuthSignInOptions.builder().authFlowType(AuthFlowType.CUSTOM_AUTH).build()
        stateFlow.value = mockAuthState(AuthenticationState.SignedOut(mockk()))

        launch {
            useCase.execute("user", null, options)
        }

        runCurrent()
        stateFlow.value = mockSignedInState()

        coVerify {
            stateMachine.send(
                withArg<AuthenticationEvent> { event ->
                    val signInData = (event.eventType as AuthenticationEvent.EventType.SignInRequested).signInData
                    signInData.shouldBeInstanceOf<SignInData.CustomAuthSignInData>()
                    signInData.username shouldBe "user"
                }
            )
        }
    }

    @Test
    fun `sends user auth event for USER_AUTH flow with preferred factor`() = runTest {
        val options = AWSCognitoAuthSignInOptions.builder()
            .authFlowType(AuthFlowType.USER_AUTH)
            .preferredFirstFactor(AuthFactorType.EMAIL_OTP)
            .build()
        stateFlow.value = mockAuthState(AuthenticationState.SignedOut(mockk()))

        launch {
            useCase.execute("user", null, options)
        }

        runCurrent()
        stateFlow.value = mockSignedInState()

        coVerify {
            stateMachine.send(
                withAuthEvent<AuthenticationEvent.EventType.SignInRequested> {
                    val signInData = it.signInData as SignInData.UserAuthSignInData
                    signInData.username shouldBe "user"
                    signInData.preferredChallenge shouldBe AuthFactorType.EMAIL_OTP
                }
            )
        }
    }

    @Test
    fun `returns successful sign in result`() = runTest {
        val deferred = async {
            useCase.execute("user", "password")
        }

        runCurrent()
        stateFlow.value = mockSignedInState()

        val result = deferred.await()
        result.isSignedIn shouldBe true
        result.nextStep.signInStep shouldBe AuthSignInStep.DONE
    }

    @Test
    fun `emits signed in event to auth hub`() = runTest {
        val deferred = async { useCase.execute("user", "password") }

        runCurrent()
        stateFlow.value = mockSignedInState()
        deferred.await()

        verify {
            emitter.sendHubEvent(AuthChannelEventName.SIGNED_IN.toString())
        }
    }
}
