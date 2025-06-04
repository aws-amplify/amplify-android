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
import com.amplifyframework.auth.cognito.exceptions.configuration.InvalidUserPoolConfigurationException
import com.amplifyframework.auth.cognito.testUtil.withSignUpEvent
import com.amplifyframework.auth.result.AuthSignUpResult
import com.amplifyframework.auth.result.step.AuthNextSignUpStep
import com.amplifyframework.auth.result.step.AuthSignUpStep
import com.amplifyframework.statemachine.codegen.events.SignUpEvent
import com.amplifyframework.statemachine.codegen.states.AuthState
import com.amplifyframework.statemachine.codegen.states.AuthenticationState
import com.amplifyframework.statemachine.codegen.states.SignUpState
import io.kotest.assertions.throwables.shouldThrowAny
import io.kotest.matchers.shouldBe
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.justRun
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Test

class ConfirmSignUpUseCaseTest {

    private val stateMachine: AuthStateMachine = mockk {
        justRun { send(any()) }
    }
    private val useCase = ConfirmSignUpUseCase(stateMachine = stateMachine)

    @Test
    fun `fails if not configured`() = runTest {
        val expectedAuthError = InvalidUserPoolConfigurationException()

        coEvery { stateMachine.getCurrentState().authNState } returns AuthenticationState.NotConfigured()

        shouldThrowAny {
            useCase.execute("user", "pass")
        } shouldBe expectedAuthError
    }

    @Test
    fun `fails if error occurs in state machine`() = runTest {
        val exception = AuthException("test", "test")
        coEvery { stateMachine.getCurrentState().authNState } returns AuthenticationState.Configured()
        coEvery { stateMachine.getCurrentState().authSignUpState } returns null

        coEvery { stateMachine.stateTransitions } returns flowOf(
            mockAuthState(SignUpState.ConfirmingSignUp(mockk())),
            mockAuthState(SignUpState.Error(exception))
        )

        shouldThrowAny {
            useCase.execute("user", "pass")
        } shouldBe exception
    }

    @Test
    fun `sends expected event`() = runTest {
        coEvery { stateMachine.getCurrentState().authNState } returns AuthenticationState.Configured()
        coEvery { stateMachine.getCurrentState().authSignUpState } returns null

        coEvery { stateMachine.stateTransitions } returns flowOf(
            mockAuthState(SignUpState.ConfirmingSignUp(mockk())),
            mockAuthState(SignUpState.SignedUp(mockk(), mockk()))
        )

        useCase.execute("user", "pass")

        coVerify {
            stateMachine.send(
                withSignUpEvent<SignUpEvent.EventType.ConfirmSignUp> { event ->
                    event.signUpData.username shouldBe "user"
                    event.confirmationCode shouldBe "pass"
                }
            )
        }
    }

    @Test
    fun `returns auth result`() = runTest {
        val expectedResult = AuthSignUpResult(
            true,
            AuthNextSignUpStep(
                AuthSignUpStep.DONE,
                emptyMap(),
                null
            ),
            null
        )

        coEvery { stateMachine.getCurrentState().authNState } returns AuthenticationState.Configured()
        coEvery { stateMachine.getCurrentState().authSignUpState } returns null

        coEvery { stateMachine.stateTransitions } returns flowOf(
            mockAuthState(SignUpState.ConfirmingSignUp(mockk())),
            mockAuthState(SignUpState.SignedUp(mockk(), expectedResult))
        )

        val result = useCase.execute("user", "pass")

        result shouldBe expectedResult
    }

    private fun mockAuthState(signUpState: SignUpState): AuthState = mockk {
        coEvery { authSignUpState } returns signUpState
    }
}
