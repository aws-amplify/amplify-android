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

import com.amplifyframework.auth.cognito.AuthStateMachine
import com.amplifyframework.auth.cognito.exceptions.configuration.InvalidUserPoolConfigurationException
import com.amplifyframework.auth.exceptions.InvalidStateException
import com.amplifyframework.auth.plugins.core.AuthHubEventEmitter
import com.amplifyframework.auth.result.step.AuthSignInStep
import com.amplifyframework.statemachine.codegen.data.SignUpData
import com.amplifyframework.statemachine.codegen.events.AuthenticationEvent
import com.amplifyframework.statemachine.codegen.states.AuthState
import com.amplifyframework.statemachine.codegen.states.AuthenticationState
import com.amplifyframework.statemachine.codegen.states.AuthorizationState
import com.amplifyframework.statemachine.codegen.states.SignUpState
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import io.mockk.every
import io.mockk.justRun
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class AutoSignInUseCaseTest {
    private val stateFlow = MutableStateFlow<AuthState>(AuthState.NotConfigured())
    private val stateMachine: AuthStateMachine = mockk {
        justRun { send(any()) }
        every { state } returns stateFlow
        every { stateTransitions } returns stateFlow.drop(1)
    }
    private val hubEmitter: AuthHubEventEmitter = mockk(relaxed = true)

    private val useCase = AutoSignInUseCase(
        stateMachine = stateMachine,
        hubEmitter = hubEmitter
    )

    private val signUpData = SignUpData(
        username = "username",
        userId = "userId",
        session = "session"
    )

    private fun authState(
        authNState: AuthenticationState? = null,
        authZState: AuthorizationState? = null,
        authSignUpState: SignUpState? = null
    ) = AuthState.Configured(authNState, authZState, authSignUpState)

    @Test
    fun `throws if not configured`() = runTest {
        stateFlow.value = authState(AuthenticationState.NotConfigured())

        shouldThrow<InvalidUserPoolConfigurationException> {
            useCase.execute()
        }
    }

    @Test
    fun `throws if state machine is SignedIn`() = runTest {
        stateFlow.value = authState(AuthenticationState.SignedIn(mockk(), mockk()))

        shouldThrow<InvalidStateException> {
            useCase.execute()
        }
    }

    @Test
    fun `cancels sign in in progress`() = runTest {
        stateFlow.value = authState(AuthenticationState.SigningIn())

        backgroundScope.launch { useCase.execute() }
        runCurrent()

        verify {
            stateMachine.send(
                withArg {
                    it.shouldBeInstanceOf<AuthenticationEvent>()
                    it.eventType.shouldBeInstanceOf<AuthenticationEvent.EventType.CancelSignIn>()
                }
            )
        }
    }

    @Test
    fun `sends sign in event`() = runTest {
        stateFlow.value = authState(
            AuthenticationState.SignedOut(mockk()),
            authSignUpState = SignUpState.SignedUp(signUpData, mockk())
        )

        backgroundScope.launch { useCase.execute() }
        runCurrent()

        verify {
            stateMachine.send(
                withArg {
                    it.shouldBeInstanceOf<AuthenticationEvent>()
                    it.eventType.shouldBeInstanceOf<AuthenticationEvent.EventType.SignInRequested>()
                }
            )
        }
    }

    @Test
    fun `returns expected auth data`() = runTest {
        stateFlow.value = authState(
            AuthenticationState.SignedOut(mockk()),
            authSignUpState = SignUpState.SignedUp(signUpData, mockk())
        )

        val deferred = backgroundScope.async { useCase.execute() }
        runCurrent()

        stateFlow.value = authState(AuthenticationState.SigningIn())
        runCurrent()

        stateFlow.value = authState(
            AuthenticationState.SignedIn(mockk(), mockk()),
            AuthorizationState.SessionEstablished(mockk())
        )

        val result = deferred.await()

        result.isSignedIn shouldBe true
        result.nextStep.signInStep shouldBe AuthSignInStep.DONE
    }
}
