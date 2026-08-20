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

import aws.sdk.kotlin.services.cognitoidentityprovider.model.ChallengeNameType
import com.amplifyframework.auth.AuthChannelEventName
import com.amplifyframework.auth.AuthFactorType
import com.amplifyframework.auth.cognito.AuthStateMachine
import com.amplifyframework.auth.cognito.exceptions.service.InvalidParameterException
import com.amplifyframework.auth.cognito.mockAuthState
import com.amplifyframework.auth.cognito.mockSignedInState
import com.amplifyframework.auth.cognito.options.AWSCognitoAuthConfirmSignInOptions
import com.amplifyframework.auth.cognito.testUtil.withChallengeEvent
import com.amplifyframework.auth.cognito.testUtil.withSetupTotpEvent
import com.amplifyframework.auth.cognito.testUtil.withSignInEvent
import com.amplifyframework.auth.exceptions.InvalidStateException
import com.amplifyframework.auth.options.AuthConfirmSignInOptions
import com.amplifyframework.auth.plugins.core.AuthHubEventEmitter
import com.amplifyframework.auth.result.step.AuthSignInStep
import com.amplifyframework.statemachine.codegen.data.AuthChallenge
import com.amplifyframework.statemachine.codegen.data.SignInTOTPSetupData
import com.amplifyframework.statemachine.codegen.data.WebAuthnSignInContext
import com.amplifyframework.statemachine.codegen.events.SetupTOTPEvent
import com.amplifyframework.statemachine.codegen.events.SignInChallengeEvent
import com.amplifyframework.statemachine.codegen.events.SignInEvent
import com.amplifyframework.statemachine.codegen.states.AuthenticationState
import com.amplifyframework.statemachine.codegen.states.SetupTOTPState
import com.amplifyframework.statemachine.codegen.states.SignInChallengeState
import com.amplifyframework.statemachine.codegen.states.SignInState
import com.amplifyframework.statemachine.codegen.states.WebAuthnSignInState
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.justRun
import io.mockk.mockk
import io.mockk.verify
import java.lang.ref.WeakReference
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ConfirmSignInUseCaseTest {
    private val stateFlow = MutableStateFlow(mockAuthState())
    private val stateMachine: AuthStateMachine = mockk {
        justRun { send(any()) }
        every { state } returns stateFlow
        coEvery { getCurrentState() } answers { stateFlow.value }
    }
    private val emitter: AuthHubEventEmitter = mockk(relaxed = true)

    private val useCase = ConfirmSignInUseCase(stateMachine = stateMachine, hubEmitter = emitter)

    @Test
    fun `fails with invalid state when not signing in`() = runTest {
        stateFlow.value = mockAuthState(mockk<AuthenticationState.SignedOut>())

        shouldThrow<InvalidStateException> {
            useCase.execute("123456", AuthConfirmSignInOptions.defaults())
        }
    }

    @Test
    fun `fails with invalid state when challenge state is not waiting`() = runTest {
        val challengeState = mockk<SignInChallengeState.NotStarted>()
        val signInState = SignInState.ResolvingChallenge(challengeState)
        stateFlow.value = mockAuthState(AuthenticationState.SigningIn(signInState))

        shouldThrow<InvalidStateException> {
            useCase.execute("123456", AuthConfirmSignInOptions.defaults())
        }
    }

    @Test
    fun `fails with invalid parameter for invalid MFA type selection`() = runTest {
        val challenge = AuthChallenge(
            challengeName = ChallengeNameType.SelectMfaType.value,
            username = "user",
            session = "session",
            parameters = emptyMap()
        )
        val challengeState = SignInChallengeState.WaitingForAnswer(challenge, mockk())
        val signInState = SignInState.ResolvingChallenge(challengeState)
        stateFlow.value = mockAuthState(AuthenticationState.SigningIn(signInState))

        shouldThrow<InvalidParameterException> {
            useCase.execute("INVALID_MFA", AuthConfirmSignInOptions.defaults())
        }
    }

    @Test
    fun `sends challenge verification event for SMS MFA`() = runTest {
        val challenge = AuthChallenge(
            challengeName = ChallengeNameType.SmsMfa.value,
            username = "user",
            session = "session",
            parameters = emptyMap()
        )
        val challengeState = SignInChallengeState.WaitingForAnswer(challenge, mockk())
        val signInState = SignInState.ResolvingChallenge(challengeState)
        stateFlow.value = mockAuthState(AuthenticationState.SigningIn(signInState))

        launch {
            useCase.execute("123456", AuthConfirmSignInOptions.defaults())
        }

        runCurrent()
        stateFlow.value = mockSignedInState()

        coVerify {
            stateMachine.send(
                withChallengeEvent<SignInChallengeEvent.EventType.VerifyChallengeAnswer> {
                    it.answer shouldBe "123456"
                }
            )
        }
    }

    @Test
    fun `sends WebAuthn sign in event for WebAuthn challenge selection`() = runTest {
        val challenge = AuthChallenge(
            challengeName = ChallengeNameType.SelectChallenge.value,
            username = "user",
            session = "session",
            parameters = emptyMap()
        )
        val challengeState = SignInChallengeState.WaitingForAnswer(challenge, mockk())
        val signInState = SignInState.ResolvingChallenge(challengeState)
        val authState = AuthenticationState.SigningIn(signInState)
        stateFlow.value = mockAuthState(authState)

        launch {
            useCase.execute(AuthFactorType.WEB_AUTHN.challengeResponse, AuthConfirmSignInOptions.defaults())
        }

        runCurrent()
        stateFlow.value = mockSignedInState()

        coVerify {
            stateMachine.send(
                withSignInEvent<SignInEvent.EventType.InitiateWebAuthnSignIn> {
                    it.signInContext.username shouldBe "user"
                }
            )
        }
    }

    @Test
    fun `sends password challenge event for password selection`() = runTest {
        val challenge = AuthChallenge(
            challengeName = ChallengeNameType.SelectChallenge.value,
            username = "user",
            session = "session",
            parameters = emptyMap()
        )
        val challengeState = SignInChallengeState.WaitingForAnswer(challenge, mockk())
        val signInState = SignInState.ResolvingChallenge(challengeState)
        val authState = AuthenticationState.SigningIn(signInState)
        stateFlow.value = mockAuthState(authState)

        launch {
            useCase.execute(ChallengeNameType.Password.value, AuthConfirmSignInOptions.defaults())
        }

        runCurrent()
        stateFlow.value = mockSignedInState()

        coVerify {
            stateMachine.send(
                withSignInEvent<SignInEvent.EventType.ReceivedChallenge> {
                    it.challenge.challengeName shouldBe ChallengeNameType.Password.value
                }
            )
        }
    }

    @Test
    fun `sends migrate auth event for password challenge`() = runTest {
        val challenge = AuthChallenge(
            challengeName = ChallengeNameType.Password.value,
            username = "user",
            session = "session",
            parameters = emptyMap()
        )
        val challengeState = SignInChallengeState.WaitingForAnswer(challenge, mockk())
        val signInState = SignInState.ResolvingChallenge(challengeState)
        val authState = AuthenticationState.SigningIn(signInState)
        stateFlow.value = mockAuthState(authState)

        launch {
            useCase.execute("password123", AuthConfirmSignInOptions.defaults())
        }

        runCurrent()
        stateFlow.value = mockSignedInState()

        coVerify {
            stateMachine.send(
                withSignInEvent<SignInEvent.EventType.InitiateMigrateAuth> {
                    it.username shouldBe "user"
                    it.password shouldBe "password123"
                }
            )
        }
    }

    @Test
    fun `sends TOTP verification event for TOTP setup`() = runTest {
        val totpData = SignInTOTPSetupData(secretCode = "secret", username = "user", session = "session")
        val totpState = SetupTOTPState.WaitingForAnswer(totpData, emptyMap(), mockk())
        val signInState = SignInState.ResolvingTOTPSetup(totpState)
        val authState = AuthenticationState.SigningIn(signInState)
        stateFlow.value = mockAuthState(authState)

        val options = AWSCognitoAuthConfirmSignInOptions.builder()
            .friendlyDeviceName("MyDevice")
            .build()

        launch {
            useCase.execute("123456", options)
        }

        runCurrent()
        stateFlow.value = mockSignedInState()

        coVerify {
            stateMachine.send(
                withSetupTotpEvent<SetupTOTPEvent.EventType.VerifyChallengeAnswer> {
                    it.answer shouldBe "123456"
                    it.friendlyDeviceName shouldBe "MyDevice"
                    it.username shouldBe "user"
                }
            )
        }
    }

    @Test
    fun `sends WebAuthn retry event for WebAuthn error state`() = runTest {
        val context = WebAuthnSignInContext("user", WeakReference(null), "session")
        val webAuthnState = WebAuthnSignInState.Error(mockk(), context)
        val signInState = SignInState.SigningInWithWebAuthn(webAuthnState)
        val authState = AuthenticationState.SigningIn(signInState)
        stateFlow.value = mockAuthState(authState)

        launch {
            useCase.execute(AuthFactorType.WEB_AUTHN.challengeResponse, AuthConfirmSignInOptions.defaults())
        }

        runCurrent()
        stateFlow.value = mockSignedInState()

        coVerify {
            stateMachine.send(
                withSignInEvent<SignInEvent.EventType.InitiateWebAuthnSignIn> {
                    it.signInContext.username shouldBe "user"
                }
            )
        }
    }

    @Test
    fun `returns successful sign in result`() = runTest {
        val challenge = AuthChallenge(
            challengeName = ChallengeNameType.SmsMfa.value,
            username = "user",
            session = "session",
            parameters = emptyMap()
        )
        val challengeState = SignInChallengeState.WaitingForAnswer(challenge, mockk())
        val signInState = SignInState.ResolvingChallenge(challengeState)
        stateFlow.value = mockAuthState(AuthenticationState.SigningIn(signInState))

        val deferred = async {
            useCase.execute("123456", AuthConfirmSignInOptions.defaults())
        }

        runCurrent()
        stateFlow.value = mockSignedInState()

        val result = deferred.await()
        result.isSignedIn shouldBe true
        result.nextStep.signInStep shouldBe AuthSignInStep.DONE
    }

    @Test
    fun `emits signed in hub event`() = runTest {
        val challenge = AuthChallenge(
            challengeName = ChallengeNameType.SmsMfa.value,
            username = "user",
            session = "session",
            parameters = emptyMap()
        )
        val challengeState = SignInChallengeState.WaitingForAnswer(challenge, mockk())
        val signInState = SignInState.ResolvingChallenge(challengeState)
        stateFlow.value = mockAuthState(AuthenticationState.SigningIn(signInState))

        val deferred = async {
            useCase.execute("123456")
        }

        runCurrent()
        stateFlow.value = mockSignedInState()
        deferred.await()

        verify {
            emitter.sendHubEvent(AuthChannelEventName.SIGNED_IN.toString())
        }
    }
}
