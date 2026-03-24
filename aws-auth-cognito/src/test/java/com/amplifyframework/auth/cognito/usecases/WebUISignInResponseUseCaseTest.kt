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

import android.content.Intent
import android.net.Uri
import com.amplifyframework.auth.cognito.AuthEnvironment
import com.amplifyframework.auth.cognito.AuthStateMachine
import com.amplifyframework.auth.cognito.exceptions.service.UserCancelledException
import com.amplifyframework.auth.cognito.testUtil.authState
import com.amplifyframework.statemachine.StateMachineEvent
import com.amplifyframework.statemachine.codegen.data.SignInMethod
import com.amplifyframework.statemachine.codegen.data.SignedInData
import com.amplifyframework.statemachine.codegen.events.HostedUIEvent
import com.amplifyframework.statemachine.codegen.events.SignOutEvent
import com.amplifyframework.statemachine.codegen.states.AuthState
import com.amplifyframework.statemachine.codegen.states.AuthenticationState
import com.amplifyframework.statemachine.codegen.states.SignOutState
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.test.runTest
import org.junit.Test

class WebUISignInResponseUseCaseTest {

    private val stateMachine: AuthStateMachine = mockk(relaxed = true)
    private val authEnvironment: AuthEnvironment = mockk {
        every { hostedUIClient?.createSignOutUri() } returns mockk()
    }

    private val useCase = WebUiSignInResponseUseCase(
        stateMachine = stateMachine,
        authEnvironment = authEnvironment
    )

    private fun mockCurrentState(state: AuthState) {
        coEvery { stateMachine.getCurrentState() } returns state
    }

    private fun signedInData(signInMethod: SignInMethod = SignInMethod.HostedUI()): SignedInData = SignedInData(
        userId = "userId",
        username = "username",
        signedInDate = java.util.Date(),
        signInMethod = signInMethod,
        cognitoUserPoolTokens = mockk()
    )

    private fun intentWithUri(uri: Uri): Intent = mockk {
        every { data } returns uri
    }

    private fun nullDataIntent(): Intent = mockk {
        every { data } returns null
    }

    @Test
    fun `sends FetchToken for non-null URI in SigningIn state`() = runTest {
        val uri: Uri = mockk()
        val intent = intentWithUri(uri)
        mockCurrentState(authState(authNState = AuthenticationState.SigningIn(mockk())))

        useCase.execute(intent)

        verify {
            stateMachine.send(
                withArg { event ->
                    val hostedUIEvent = event.shouldBeInstanceOf<HostedUIEvent>()
                    val eventType = hostedUIEvent.eventType.shouldBeInstanceOf<HostedUIEvent.EventType.FetchToken>()
                    eventType.uri shouldBe uri
                }
            )
        }
    }

    @Test
    fun `sends ThrowError with UserCancelledException for null URI in SigningIn state`() = runTest {
        mockCurrentState(authState(authNState = AuthenticationState.SigningIn(mockk())))

        useCase.execute(null)

        verify {
            stateMachine.send(
                withArg { event ->
                    val hostedUIEvent = event.shouldBeInstanceOf<HostedUIEvent>()
                    val eventType = hostedUIEvent.eventType.shouldBeInstanceOf<HostedUIEvent.EventType.ThrowError>()
                    eventType.exception.shouldBeInstanceOf<UserCancelledException>()
                }
            )
        }
    }

    @Test
    fun `sends UserCancelled for null URI in SigningOutHostedUI with non-UNKNOWN method and bypassCancel false`() =
        runTest {
            val data = signedInData(signInMethod = SignInMethod.HostedUI())
            val signOutState = SignOutState.SigningOutHostedUI(
                signedInData = data,
                globalSignOut = false,
                bypassCancel = false
            )
            mockCurrentState(
                authState(authNState = AuthenticationState.SigningOut(signOutState))
            )

            useCase.execute(null)

            verify {
                stateMachine.send(
                    withArg { event ->
                        val signOutEvent = event.shouldBeInstanceOf<SignOutEvent>()
                        val eventType = signOutEvent.eventType
                            .shouldBeInstanceOf<SignOutEvent.EventType.UserCancelled>()
                        eventType.signedInData shouldBe data
                    }
                )
            }
        }

    @Test
    fun `sends SignOutGlobally when globalSignOut is true`() = runTest {
        val data = signedInData(
            signInMethod = SignInMethod.ApiBased(SignInMethod.ApiBased.AuthType.UNKNOWN)
        )
        val signOutState = SignOutState.SigningOutHostedUI(
            signedInData = data,
            globalSignOut = true,
            bypassCancel = false
        )
        mockCurrentState(
            authState(authNState = AuthenticationState.SigningOut(signOutState))
        )

        useCase.execute(null)

        verify {
            stateMachine.send(
                withArg { event ->
                    val signOutEvent = event.shouldBeInstanceOf<SignOutEvent>()
                    val eventType = signOutEvent.eventType.shouldBeInstanceOf<SignOutEvent.EventType.SignOutGlobally>()
                    eventType.signedInData shouldBe data
                }
            )
        }
    }

    @Test
    fun `sends RevokeToken when globalSignOut is false`() = runTest {
        val data = signedInData(
            signInMethod = SignInMethod.ApiBased(SignInMethod.ApiBased.AuthType.UNKNOWN)
        )
        val signOutState = SignOutState.SigningOutHostedUI(
            signedInData = data,
            globalSignOut = false,
            bypassCancel = false
        )
        mockCurrentState(
            authState(authNState = AuthenticationState.SigningOut(signOutState))
        )

        useCase.execute(null)

        verify {
            stateMachine.send(
                withArg { event ->
                    val signOutEvent = event.shouldBeInstanceOf<SignOutEvent>()
                    val eventType = signOutEvent.eventType.shouldBeInstanceOf<SignOutEvent.EventType.RevokeToken>()
                    eventType.signedInData shouldBe data
                }
            )
        }
    }

    @Test
    fun `ends no events for unrelated states`() = runTest {
        mockCurrentState(
            authState(authNState = AuthenticationState.SignedIn(mockk(), mockk()))
        )

        useCase.execute(null)

        verify(exactly = 0) { stateMachine.send(any<StateMachineEvent>()) }
    }
}
