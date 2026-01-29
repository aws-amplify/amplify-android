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
import com.amplifyframework.auth.cognito.testUtil.authState
import com.amplifyframework.auth.cognito.testUtil.withDeleteEvent
import com.amplifyframework.auth.exceptions.InvalidStateException
import com.amplifyframework.auth.plugins.core.AuthHubEventEmitter
import com.amplifyframework.statemachine.codegen.events.DeleteUserEvent
import com.amplifyframework.statemachine.codegen.states.AuthState
import com.amplifyframework.statemachine.codegen.states.AuthenticationState
import com.amplifyframework.statemachine.codegen.states.AuthorizationState
import com.amplifyframework.statemachine.codegen.states.DeleteUserState
import io.kotest.assertions.throwables.shouldNotThrowAny
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.assertions.throwables.shouldThrowAny
import io.kotest.matchers.shouldBe
import io.mockk.coEvery
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
class DeleteUserUseCaseTest {

    private val stateFlow = MutableStateFlow<AuthState>(
        authState(
            authNState = AuthenticationState.SignedIn(
                signedInData = mockk { every { username } returns "user" },
                deviceMetadata = mockk()
            )
        )
    )

    private val fetchAuthSession: FetchAuthSessionUseCase = mockk {
        coEvery { execute().accessToken } returns "access token"
    }
    private val stateMachine: AuthStateMachine = mockk {
        every { state } returns stateFlow
        coEvery { getCurrentState() } answers { stateFlow.value }
        justRun { send(any()) }
    }
    private val emitter: AuthHubEventEmitter = mockk(relaxed = true)

    private val useCase = DeleteUserUseCase(
        fetchAuthSession = fetchAuthSession,
        stateMachine = stateMachine,
        emitter = emitter
    )

    @Test
    fun `fails if not signed in`() = runTest {
        stateFlow.value = authState(authNState = AuthenticationState.NotConfigured())

        shouldThrow<InvalidStateException> {
            useCase.execute()
        }
    }

    @Test
    fun `fails if no access token`() = runTest {
        coEvery { fetchAuthSession.execute().accessToken } returns null

        shouldThrow<InvalidUserPoolConfigurationException> {
            useCase.execute()
        }
    }

    @Test
    fun `sends the expectedEvent`() = runTest {
        backgroundScope.launch { useCase.execute() }
        runCurrent()

        verify {
            stateMachine.send(
                withDeleteEvent<DeleteUserEvent.EventType.DeleteUser> { event ->
                    event.accessToken shouldBe "access token"
                }
            )
        }
    }

    @Test
    fun `throws if delete user has an error`() = runTest {
        val exception = Exception("something happened")

        backgroundScope.launch {
            val thrown = shouldThrowAny { useCase.execute() }
            thrown.cause shouldBe exception
        }
        runCurrent()

        val authZState = AuthorizationState.DeletingUser(
            deleteUserState = DeleteUserState.Error(exception),
            amplifyCredential = mockk()
        )

        stateFlow.value = authState(authZState = authZState)
        runCurrent()

        stateFlow.value = authState(
            authZState = AuthorizationState.SessionEstablished(mockk())
        )
        runCurrent()
    }

    @Test
    fun `completes successfully`() = runTest {
        val deferred = backgroundScope.async { useCase.execute() }
        runCurrent()

        stateFlow.value = authState(
            authNState = AuthenticationState.SignedOut(mockk()),
            authZState = AuthorizationState.Configured()
        )

        shouldNotThrowAny { deferred.await() }
    }
}
