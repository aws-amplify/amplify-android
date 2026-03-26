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

import android.app.Activity
import com.amplifyframework.auth.AuthException
import com.amplifyframework.auth.AuthProvider
import com.amplifyframework.auth.cognito.AuthConfiguration
import com.amplifyframework.auth.cognito.AuthStateMachine
import com.amplifyframework.auth.cognito.exceptions.configuration.InvalidOauthConfigurationException
import com.amplifyframework.auth.cognito.exceptions.configuration.InvalidUserPoolConfigurationException
import com.amplifyframework.auth.cognito.exceptions.invalidstate.SignedInException
import com.amplifyframework.auth.cognito.testUtil.authState
import com.amplifyframework.auth.exceptions.InvalidStateException
import com.amplifyframework.auth.exceptions.UnknownException
import com.amplifyframework.auth.plugins.core.AuthHubEventEmitter
import com.amplifyframework.auth.result.step.AuthSignInStep
import com.amplifyframework.statemachine.StateMachineEvent
import com.amplifyframework.statemachine.codegen.data.OauthConfiguration
import com.amplifyframework.statemachine.codegen.data.SignInData
import com.amplifyframework.statemachine.codegen.data.SignedOutData
import com.amplifyframework.statemachine.codegen.events.AuthenticationEvent
import com.amplifyframework.statemachine.codegen.states.AuthState
import com.amplifyframework.statemachine.codegen.states.AuthenticationState
import com.amplifyframework.statemachine.codegen.states.AuthorizationState
import com.amplifyframework.statemachine.codegen.states.HostedUISignInState
import com.amplifyframework.statemachine.codegen.states.SignInState
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import io.mockk.coEvery
import io.mockk.every
import io.mockk.justRun
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class WebUiSignInUseCaseTest {

    private val callingActivity: Activity = mockk()

    private val oauthConfig: OauthConfiguration = mockk()

    private val configuration: AuthConfiguration = mockk {
        every { oauth } returns oauthConfig
    }

    private val stateFlow = MutableStateFlow<AuthState>(
        authState(
            authNState = AuthenticationState.SignedOut(SignedOutData()),
            authZState = AuthorizationState.Configured()
        )
    )

    private val stateMachine: AuthStateMachine = mockk {
        every { state } returns stateFlow
        every { stateTransitions } answers { stateFlow.drop(1) }
        coEvery { getCurrentState() } answers { stateFlow.value }
        justRun { send(any()) }
    }

    private val emitter: AuthHubEventEmitter = mockk(relaxed = true)

    private val useCase = WebUiSignInUseCase(
        stateMachine = stateMachine,
        configuration = configuration,
        emitter = emitter
    )

    @Test
    fun `throws InvalidUserPoolConfigurationException when not configured`() = runTest {
        stateFlow.value = authState(
            authNState = AuthenticationState.NotConfigured(),
            authZState = AuthorizationState.Configured()
        )

        shouldThrow<InvalidUserPoolConfigurationException> {
            useCase.execute(callingActivity = callingActivity)
        }
    }

    @Test
    fun `throws SignedInException when already signed in`() = runTest {
        stateFlow.value = authState(
            authNState = AuthenticationState.SignedIn(mockk(), mockk()),
            authZState = AuthorizationState.Configured()
        )

        shouldThrow<SignedInException> {
            useCase.execute(callingActivity = callingActivity)
        }
    }

    @Test
    fun `throws InvalidOauthConfigurationException when OAuth config is null`() = runTest {
        val nullOauthConfig: AuthConfiguration = mockk {
            every { oauth } returns null
        }
        val useCaseNoOauth = WebUiSignInUseCase(
            stateMachine = stateMachine,
            configuration = nullOauthConfig,
            emitter = emitter
        )

        shouldThrow<InvalidOauthConfigurationException> {
            useCaseNoOauth.execute(callingActivity = callingActivity)
        }
    }

    @Test
    fun `sends SignInRequested with HostedUISignInData in SignedOut state`() = runTest {
        backgroundScope.async { useCase.execute(provider = AuthProvider.google(), callingActivity = callingActivity) }
        runCurrent()

        verify {
            stateMachine.send(
                withArg<StateMachineEvent> {
                    val event = it.shouldBeInstanceOf<AuthenticationEvent>()
                    val type = event.eventType
                        .shouldBeInstanceOf<AuthenticationEvent.EventType.SignInRequested>()
                    type.signInData.shouldBeInstanceOf<SignInData.HostedUISignInData>()
                }
            )
        }
    }

    @Test
    fun `cancels existing sign-in and proceeds when SigningIn`() = runTest {
        stateFlow.value = authState(
            authNState = AuthenticationState.SigningIn(SignInState.NotStarted()),
            authZState = AuthorizationState.Configured()
        )

        val deferred = backgroundScope.async {
            useCase.execute(callingActivity = callingActivity)
        }
        runCurrent()

        // Verify CancelSignIn was sent
        verify {
            stateMachine.send(
                withArg<StateMachineEvent> {
                    val event = it.shouldBeInstanceOf<AuthenticationEvent>()
                    event.eventType.shouldBeInstanceOf<AuthenticationEvent.EventType.CancelSignIn>()
                }
            )
        }

        // Transition to SignedOut so the use case can proceed
        stateFlow.value = authState(
            authNState = AuthenticationState.SignedOut(SignedOutData()),
            authZState = AuthorizationState.Configured()
        )
        runCurrent()

        // Now transition to signed in to complete the flow
        stateFlow.value = authState(
            authNState = AuthenticationState.SignedIn(mockk(), mockk()),
            authZState = AuthorizationState.SessionEstablished(mockk())
        )

        val result = deferred.await()
        result.isSignedIn shouldBe true
    }

    @Test
    fun `returns success result with isSignedIn true and DONE step`() = runTest {
        val deferred = backgroundScope.async {
            useCase.execute(callingActivity = callingActivity)
        }
        runCurrent()

        stateFlow.value = authState(
            authNState = AuthenticationState.SignedIn(mockk(), mockk()),
            authZState = AuthorizationState.SessionEstablished(mockk())
        )

        val result = deferred.await()
        result.isSignedIn shouldBe true
        result.nextStep.signInStep shouldBe AuthSignInStep.DONE
    }

    @Test
    fun `publishes SIGNED_IN hub event on success`() = runTest {
        val deferred = backgroundScope.async {
            useCase.execute(callingActivity = callingActivity)
        }
        runCurrent()

        stateFlow.value = authState(
            authNState = AuthenticationState.SignedIn(mockk(), mockk()),
            authZState = AuthorizationState.SessionEstablished(mockk())
        )

        deferred.await()

        verify {
            emitter.sendHubEvent("SIGNED_IN")
        }
    }

    @Test
    fun `throws AuthException on HostedUISignInState Error`() = runTest {
        val authException = AuthException("Hosted UI error", "Try again")

        val supervisor = SupervisorJob(backgroundScope.coroutineContext[Job])
        val deferred = backgroundScope.async(supervisor) {
            useCase.execute(callingActivity = callingActivity)
        }
        runCurrent()

        stateFlow.value = authState(
            authNState = AuthenticationState.SigningIn(
                SignInState.SigningInWithHostedUI(HostedUISignInState.Error(authException))
            ),
            authZState = AuthorizationState.Configured()
        )

        val thrown = shouldThrow<AuthException> {
            deferred.await()
        }
        thrown.message shouldBe "Hosted UI error"
    }

    @Test
    fun `wraps non-AuthException in UnknownException on HostedUISignInState Error`() = runTest {
        val runtimeException = RuntimeException("something broke")

        val supervisor = SupervisorJob(backgroundScope.coroutineContext[Job])
        val deferred = backgroundScope.async(supervisor) {
            useCase.execute(callingActivity = callingActivity)
        }
        runCurrent()

        stateFlow.value = authState(
            authNState = AuthenticationState.SigningIn(
                SignInState.SigningInWithHostedUI(HostedUISignInState.Error(runtimeException))
            ),
            authZState = AuthorizationState.Configured()
        )

        shouldThrow<UnknownException> {
            deferred.await()
        }
    }

    @Test
    fun `sends CancelSignIn on error`() = runTest {
        val authException = AuthException("error", "recovery")

        val supervisor = SupervisorJob(backgroundScope.coroutineContext[Job])
        val deferred = backgroundScope.async(supervisor) {
            useCase.execute(callingActivity = callingActivity)
        }
        runCurrent()

        stateFlow.value = authState(
            authNState = AuthenticationState.SigningIn(
                SignInState.SigningInWithHostedUI(HostedUISignInState.Error(authException))
            ),
            authZState = AuthorizationState.Configured()
        )

        runCatching { deferred.await() }

        verify {
            stateMachine.send(
                withArg<StateMachineEvent> {
                    val event = it.shouldBeInstanceOf<AuthenticationEvent>()
                    event.eventType.shouldBeInstanceOf<AuthenticationEvent.EventType.CancelSignIn>()
                }
            )
        }
    }

    @Test
    fun `throws InvalidStateException for unexpected states`() = runTest {
        stateFlow.value = authState(
            authNState = AuthenticationState.Configured(),
            authZState = AuthorizationState.Configured()
        )

        shouldThrow<InvalidStateException> {
            useCase.execute(callingActivity = callingActivity)
        }
    }
}
