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
import com.amplifyframework.statemachine.codegen.data.SignUpData
import com.amplifyframework.statemachine.codegen.events.SignUpEvent
import com.amplifyframework.statemachine.codegen.states.AuthState
import com.amplifyframework.statemachine.codegen.states.AuthenticationState
import com.amplifyframework.statemachine.codegen.states.SignUpState
import io.kotest.assertions.throwables.shouldThrowAny
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.justRun
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ConfirmSignUpUseCaseTest {

    private val stateFlow = MutableStateFlow(mockAuthState(mockk()))
    private val stateMachine: AuthStateMachine = mockk {
        justRun { send(any()) }
        every { state } returns stateFlow
    }
    private val useCase = ConfirmSignUpUseCase(stateMachine = stateMachine)

    private val signUpData = SignUpData(
        username = "username",
        validationData = mapOf("key" to "value"),
        clientMetadata = mapOf("meta" to "data"),
        session = "session",
        userId = "userId"
    )

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

        launch {
            shouldThrowAny {
                useCase.execute("user", "pass")
            } shouldBe exception
        }

        runCurrent()
        stateFlow.emit(mockAuthState(SignUpState.ConfirmingSignUp(mockk())))
        stateFlow.emit(mockAuthState(SignUpState.Error(mockk(), exception)))
    }

    @Test
    fun `sends expected event`() = runTest {
        coEvery { stateMachine.getCurrentState().authNState } returns AuthenticationState.Configured()
        coEvery { stateMachine.getCurrentState().authSignUpState } returns null

        executeUseCaseToCompletion()

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
    fun `uses signUpData values from existing error state`() = runTest {
        coEvery { stateMachine.getCurrentState().authNState } returns AuthenticationState.Configured()
        coEvery { stateMachine.getCurrentState().authSignUpState } returns
            SignUpState.Error(signUpData = signUpData, exception = mockk())

        executeUseCaseToCompletion(username = signUpData.username)

        coVerify {
            stateMachine.send(
                withSignUpEvent<SignUpEvent.EventType.ConfirmSignUp> { event ->
                    event.signUpData.run {
                        username shouldBe signUpData.username
                        validationData shouldBe signUpData.validationData
                        clientMetadata.shouldBeNull() // was not passed in options
                        session shouldBe signUpData.session
                        userId shouldBe signUpData.userId
                    }
                    event.confirmationCode shouldBe "pass"
                }
            )
        }
    }

    @Test
    fun `uses session value from existing AwaitingConfirmation state`() = runTest {
        coEvery { stateMachine.getCurrentState().authNState } returns AuthenticationState.Configured()
        coEvery { stateMachine.getCurrentState().authSignUpState } returns
            SignUpState.AwaitingUserConfirmation(signUpData = signUpData, signUpResult = mockk())

        executeUseCaseToCompletion(username = signUpData.username)

        coVerify {
            stateMachine.send(
                withSignUpEvent<SignUpEvent.EventType.ConfirmSignUp> { event ->
                    event.signUpData.run {
                        username shouldBe signUpData.username
                        validationData shouldBe signUpData.validationData
                        clientMetadata.shouldBeNull() // was not passed in options
                        session shouldBe signUpData.session
                        userId shouldBe signUpData.userId
                    }
                    event.confirmationCode shouldBe "pass"
                }
            )
        }
    }

    @Test
    fun `does not use values from existing state if username does not match`() = runTest {
        coEvery { stateMachine.getCurrentState().authNState } returns AuthenticationState.Configured()
        coEvery { stateMachine.getCurrentState().authSignUpState } returns
            SignUpState.AwaitingUserConfirmation(signUpData = signUpData, signUpResult = mockk())

        executeUseCaseToCompletion(username = "bob")

        coVerify {
            stateMachine.send(
                withSignUpEvent<SignUpEvent.EventType.ConfirmSignUp> { event ->
                    event.signUpData.run {
                        username shouldBe "bob"
                        validationData.shouldBeNull()
                        clientMetadata.shouldBeNull() // was not passed in options
                        session.shouldBeNull()
                        userId.shouldBeNull()
                    }
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

        val result = executeUseCaseToCompletion(signUpResult = expectedResult)
        result shouldBe expectedResult
    }

    private fun mockAuthState(signUpState: SignUpState): AuthState = mockk {
        coEvery { authSignUpState } returns signUpState
    }

    @Suppress("SuspendFunctionOnCoroutineScope")
    private suspend fun TestScope.executeUseCaseToCompletion(
        username: String = "user",
        confirmationCode: String = "pass",
        signUpResult: AuthSignUpResult = mockk()
    ): AuthSignUpResult {
        val result = async { useCase.execute(username, confirmationCode) }
        runCurrent()
        stateFlow.emit(mockAuthState(SignUpState.ConfirmingSignUp(mockk())))
        stateFlow.emit(mockAuthState(SignUpState.SignedUp(mockk(), signUpResult)))
        return result.await()
    }
}
