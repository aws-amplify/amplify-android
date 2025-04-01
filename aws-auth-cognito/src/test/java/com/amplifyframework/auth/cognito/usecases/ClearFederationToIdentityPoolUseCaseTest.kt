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

import com.amplifyframework.auth.AuthException
import com.amplifyframework.auth.cognito.AuthStateMachine
import com.amplifyframework.auth.cognito.result.AWSCognitoAuthSignOutResult
import com.amplifyframework.auth.cognito.testUtil.authState
import com.amplifyframework.auth.cognito.testUtil.withAuthEvent
import com.amplifyframework.auth.exceptions.InvalidStateException
import com.amplifyframework.auth.plugins.core.AuthHubEventEmitter
import com.amplifyframework.statemachine.codegen.data.AmplifyCredential
import com.amplifyframework.statemachine.codegen.errors.SessionError
import com.amplifyframework.statemachine.codegen.events.AuthenticationEvent
import com.amplifyframework.statemachine.codegen.states.AuthState
import com.amplifyframework.statemachine.codegen.states.AuthenticationState
import com.amplifyframework.statemachine.codegen.states.AuthorizationState
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.assertions.throwables.shouldThrowAny
import io.kotest.matchers.shouldBe
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.justRun
import io.mockk.mockk
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.test.runTest
import org.junit.Test

class ClearFederationToIdentityPoolUseCaseTest {
    private val credential = AmplifyCredential.IdentityPoolFederated(mockk(), "id", mockk())
    private val stateFlow = MutableStateFlow<AuthState>(
        authState(
            authNState = AuthenticationState.FederatedToIdentityPool(),
            authZState = AuthorizationState.SessionEstablished(credential)
        )
    )

    private val stateMachine: AuthStateMachine = mockk {
        every { state } returns stateFlow
        every { stateTransitions } answers { stateFlow.drop(1) }
        coEvery { getCurrentState() } answers { stateFlow.value }
        justRun { send(any()) }
    }

    private val signOut: SignOutUseCase = mockk {
        coEvery { completeSignOut(any()) } returns AWSCognitoAuthSignOutResult.CompleteSignOut
    }
    private val emitter: AuthHubEventEmitter = mockk(relaxed = true)

    private val useCase = ClearFederationToIdentityPoolUseCase(
        stateMachine = stateMachine,
        signOut = signOut,
        emitter = emitter
    )

    @Test
    fun `throws InvalidStateException if not federated sign in`() = runTest {
        stateFlow.value = authState(authNState = AuthenticationState.SignedIn(mockk(), mockk()))

        shouldThrow<InvalidStateException> {
            useCase.execute()
        }
    }

    @Test
    fun `sends event if federated sign in`() = runTest {
        useCase.execute()

        coVerify {
            stateMachine.send(withAuthEvent<AuthenticationEvent.EventType.ClearFederationToIdentityPool>())
        }
    }

    @Test
    fun `sends event if error state for federated sign in`() = runTest {
        val exception = Exception()
        stateFlow.value = authState(
            authZState = AuthorizationState.Error(exception = SessionError(exception, credential))
        )

        useCase.execute()

        coVerify {
            stateMachine.send(withAuthEvent<AuthenticationEvent.EventType.ClearFederationToIdentityPool>())
        }
    }

    @Test
    fun `throws exception from failed sign out`() = runTest {
        val exception = AuthException("failed", "test")
        coEvery { signOut.completeSignOut(any()) } returns AWSCognitoAuthSignOutResult.FailedSignOut(exception)

        shouldThrowAny { useCase.execute() } shouldBe exception
    }
}
