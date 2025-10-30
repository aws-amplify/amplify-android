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
import com.amplifyframework.auth.cognito.exceptions.service.UserCancelledException
import com.amplifyframework.auth.cognito.options.AWSCognitoAuthSignOutOptions
import com.amplifyframework.auth.cognito.result.AWSCognitoAuthSignOutResult
import com.amplifyframework.auth.cognito.testUtil.authState
import com.amplifyframework.auth.cognito.testUtil.withAuthEvent
import com.amplifyframework.auth.exceptions.InvalidStateException
import com.amplifyframework.auth.plugins.core.AuthHubEventEmitter
import com.amplifyframework.statemachine.codegen.data.HostedUIErrorData
import com.amplifyframework.statemachine.codegen.data.SignedOutData
import com.amplifyframework.statemachine.codegen.events.AuthenticationEvent
import com.amplifyframework.statemachine.codegen.states.AuthState
import com.amplifyframework.statemachine.codegen.states.AuthenticationState
import com.amplifyframework.statemachine.codegen.states.AuthorizationState
import com.amplifyframework.statemachine.codegen.states.SignOutState
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import io.mockk.coEvery
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
class SignOutUseCaseTest {
    private val stateFlow = MutableStateFlow<AuthState>(
        authState(
            authNState = AuthenticationState.SignedIn(mockk(), mockk())
        )
    )

    private val stateMachine: AuthStateMachine = mockk {
        every { state } returns stateFlow
        every { stateTransitions } answers { stateFlow.drop(1) }
        coEvery { getCurrentState() } answers { stateFlow.value }
        justRun { send(any()) }
    }

    private val emitter: AuthHubEventEmitter = mockk(relaxed = true)

    private val useCase = SignOutUseCase(
        stateMachine = stateMachine,
        emitter = emitter
    )

    @Test
    fun `sends sign out event`() = runTest {
        backgroundScope.launch { useCase.execute() }
        runCurrent()

        verify {
            stateMachine.send(
                withAuthEvent<AuthenticationEvent.EventType.SignOutRequested> { event ->
                    event.signOutData.globalSignOut shouldBe false
                    event.signOutData.browserPackage shouldBe null
                }
            )
        }
    }

    @Test
    fun `uses supplied options in sign out event`() = runTest {
        val options = AWSCognitoAuthSignOutOptions.builder()
            .globalSignOut(true)
            .browserPackage("foo")
            .build()

        backgroundScope.launch { useCase.execute(options) }
        runCurrent()

        verify {
            stateMachine.send(
                withAuthEvent<AuthenticationEvent.EventType.SignOutRequested> { event ->
                    event.signOutData.globalSignOut shouldBe true
                    event.signOutData.browserPackage shouldBe "foo"
                }
            )
        }
    }

    @Test
    fun `succeeds if not configured`() = runTest {
        stateFlow.value = authState(authNState = AuthenticationState.NotConfigured())

        val result = useCase.execute()

        result.shouldBeInstanceOf<AWSCognitoAuthSignOutResult.CompleteSignOut>()
    }

    @Test
    fun `fails if sign in is federated`() = runTest {
        stateFlow.value = authState(authNState = AuthenticationState.FederatedToIdentityPool())

        val result = useCase.execute()

        val failed = result.shouldBeInstanceOf<AWSCognitoAuthSignOutResult.FailedSignOut>()
        failed.exception.shouldBeInstanceOf<InvalidStateException>()
    }

    @Test
    fun `fails if in unexpected state`() = runTest {
        stateFlow.value = authState(authNState = AuthenticationState.SigningIn())

        val result = useCase.execute()

        val failed = result.shouldBeInstanceOf<AWSCognitoAuthSignOutResult.FailedSignOut>()
        failed.exception.shouldBeInstanceOf<InvalidStateException>()
    }

    @Test
    fun `fails if user cancels sign out`() = runTest {
        val deferred = backgroundScope.async { useCase.execute() }
        runCurrent()

        val exception = UserCancelledException("failed", "test")
        stateFlow.value = authState(authNState = AuthenticationState.SigningOut(SignOutState.Error(exception)))
        runCurrent()

        stateFlow.value = authState(authNState = AuthenticationState.SignedIn(mockk(), mockk()))

        val result = deferred.await()
        val failed = result.shouldBeInstanceOf<AWSCognitoAuthSignOutResult.FailedSignOut>()
        failed.exception shouldBe exception
    }

    @Test
    fun `fails if reaching error state`() = runTest {
        val deferred = backgroundScope.async { useCase.execute() }
        runCurrent()

        val exception = Exception()
        stateFlow.value = authState(authNState = AuthenticationState.Error(exception = exception))

        val result = deferred.await()
        val failed = result.shouldBeInstanceOf<AWSCognitoAuthSignOutResult.FailedSignOut>()
        failed.exception.cause shouldBe exception
    }

    @Test
    fun `returns complete result`() = runTest {
        val deferred = backgroundScope.async { useCase.execute() }
        runCurrent()

        val signedOutData = SignedOutData()

        stateFlow.value = authState(
            authNState = AuthenticationState.SignedOut(signedOutData),
            authZState = AuthorizationState.Configured()
        )

        val result = deferred.await()
        result shouldBe AWSCognitoAuthSignOutResult.CompleteSignOut
    }

    @Test
    fun `returns partial result`() = runTest {
        val deferred = backgroundScope.async { useCase.execute() }
        runCurrent()

        val exception = Exception()
        val signedOutData = SignedOutData(
            hostedUIErrorData = HostedUIErrorData("url", exception)
        )

        stateFlow.value = authState(
            authNState = AuthenticationState.SignedOut(signedOutData),
            authZState = AuthorizationState.Configured()
        )

        val result = deferred.await()
        val partial = result.shouldBeInstanceOf<AWSCognitoAuthSignOutResult.PartialSignOut>()

        partial.hostedUIError?.url shouldBe "url"
        partial.hostedUIError?.exception shouldBe exception
    }
}
