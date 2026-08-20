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
import com.amplifyframework.auth.cognito.AWSCognitoAuthSession
import com.amplifyframework.auth.cognito.AuthStateMachine
import com.amplifyframework.auth.cognito.isValid
import com.amplifyframework.auth.cognito.testUtil.authState
import com.amplifyframework.auth.exceptions.ConfigurationException
import com.amplifyframework.auth.exceptions.InvalidStateException
import com.amplifyframework.auth.exceptions.NotAuthorizedException
import com.amplifyframework.auth.exceptions.ServiceException
import com.amplifyframework.auth.exceptions.SessionExpiredException
import com.amplifyframework.auth.exceptions.SignedOutException
import com.amplifyframework.auth.exceptions.UnknownException
import com.amplifyframework.auth.options.AuthFetchSessionOptions
import com.amplifyframework.auth.plugins.core.AuthHubEventEmitter
import com.amplifyframework.statemachine.StateMachineEvent
import com.amplifyframework.statemachine.codegen.data.AWSCredentials
import com.amplifyframework.statemachine.codegen.data.AmplifyCredential
import com.amplifyframework.statemachine.codegen.data.FederatedToken
import com.amplifyframework.statemachine.codegen.data.SignedOutData
import com.amplifyframework.statemachine.codegen.errors.SessionError
import com.amplifyframework.statemachine.codegen.events.AuthorizationEvent
import com.amplifyframework.statemachine.codegen.states.AuthState
import com.amplifyframework.statemachine.codegen.states.AuthenticationState
import com.amplifyframework.statemachine.codegen.states.AuthorizationState
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import io.mockk.coEvery
import io.mockk.every
import io.mockk.justRun
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkStatic
import io.mockk.verify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class FetchAuthSessionUseCaseTest {

    private val awsCredentials = AWSCredentials(
        accessKeyId = "accessKeyId",
        secretAccessKey = "secretAccessKey",
        sessionToken = "sessionToken",
        expiration = 9999999999L
    )

    private val userPoolCredential: AmplifyCredential.UserAndIdentityPool = mockk(relaxed = true) {
        every { identityId } returns "identity-id"
        every { credentials } returns awsCredentials
    }

    private val federatedCredential = AmplifyCredential.IdentityPoolFederated(
        federatedToken = FederatedToken("token", "graph.facebook.com"),
        identityId = "identity-id",
        credentials = awsCredentials
    )

    private val stateFlow = MutableStateFlow<AuthState>(
        authState(
            authNState = AuthenticationState.SignedOut(SignedOutData()),
            authZState = AuthorizationState.Configured()
        )
    )

    private val stateMachine: AuthStateMachine = mockk {
        every { state } returns stateFlow
        coEvery { getCurrentState() } answers { stateFlow.value }
        justRun { send(any()) }
    }

    private val emitter: AuthHubEventEmitter = mockk(relaxed = true)

    private val useCase = FetchAuthSessionUseCase(
        stateMachine = stateMachine,
        emitter = emitter
    )

    @Before
    fun setUp() {
        mockkStatic("com.amplifyframework.auth.cognito.AWSCognitoAuthSessionKt")
        every { any<AmplifyCredential>().isValid() } returns true
    }

    @After
    fun tearDown() {
        unmockkStatic("com.amplifyframework.auth.cognito.AWSCognitoAuthSessionKt")
    }

    @Test
    fun `sends FetchUnAuthSession when authorization state is Configured`() = runTest {
        backgroundScope.async { useCase.execute() }
        runCurrent()

        verify {
            stateMachine.send(
                withArg<StateMachineEvent> {
                    val event = it.shouldBeInstanceOf<AuthorizationEvent>()
                    event.eventType.shouldBeInstanceOf<AuthorizationEvent.EventType.FetchUnAuthSession>()
                }
            )
        }
    }

    @Test
    fun `returns cached session for valid credentials with forceRefresh false`() = runTest {
        stateFlow.value = authState(
            authNState = AuthenticationState.SignedIn(mockk(), mockk()),
            authZState = AuthorizationState.SessionEstablished(userPoolCredential)
        )

        val result = useCase.execute()
        result.shouldBeInstanceOf<AWSCognitoAuthSession>()
    }

    @Test
    fun `sends RefreshSession for invalid non-federated credentials`() = runTest {
        every { any<AmplifyCredential>().isValid() } returns false

        stateFlow.value = authState(
            authNState = AuthenticationState.SignedIn(mockk(), mockk()),
            authZState = AuthorizationState.SessionEstablished(userPoolCredential)
        )

        backgroundScope.async { useCase.execute() }
        runCurrent()

        verify {
            stateMachine.send(
                withArg<StateMachineEvent> {
                    val event = it.shouldBeInstanceOf<AuthorizationEvent>()
                    val type = event.eventType
                        .shouldBeInstanceOf<AuthorizationEvent.EventType.RefreshSession>()
                    type.amplifyCredential shouldBe userPoolCredential
                }
            )
        }
    }

    @Test
    fun `sends StartFederationToIdentityPool for invalid federated credentials`() = runTest {
        every { any<AmplifyCredential>().isValid() } returns false

        stateFlow.value = authState(
            authNState = AuthenticationState.SignedIn(mockk(), mockk()),
            authZState = AuthorizationState.SessionEstablished(federatedCredential)
        )

        backgroundScope.async { useCase.execute() }
        runCurrent()

        verify {
            stateMachine.send(
                withArg<StateMachineEvent> {
                    val event = it.shouldBeInstanceOf<AuthorizationEvent>()
                    val type = event.eventType
                        .shouldBeInstanceOf<AuthorizationEvent.EventType.StartFederationToIdentityPool>()
                    type.token shouldBe federatedCredential.federatedToken
                    type.identityId shouldBe federatedCredential.identityId
                }
            )
        }
    }

    @Test
    fun `sends RefreshSession for forceRefresh with valid non-federated credentials`() = runTest {
        stateFlow.value = authState(
            authNState = AuthenticationState.SignedIn(mockk(), mockk()),
            authZState = AuthorizationState.SessionEstablished(userPoolCredential)
        )

        val options = AuthFetchSessionOptions.builder().forceRefresh(true).build()
        backgroundScope.async { useCase.execute(options) }
        runCurrent()

        verify {
            stateMachine.send(
                withArg<StateMachineEvent> {
                    val event = it.shouldBeInstanceOf<AuthorizationEvent>()
                    event.eventType.shouldBeInstanceOf<AuthorizationEvent.EventType.RefreshSession>()
                }
            )
        }
    }

    @Test
    fun `sends RefreshSession for SessionError with non-federated credentials`() = runTest {
        stateFlow.value = authState(
            authNState = AuthenticationState.SignedIn(mockk(), mockk()),
            authZState = AuthorizationState.Error(
                SessionError(Exception("session error"), userPoolCredential)
            )
        )

        backgroundScope.async { useCase.execute() }
        runCurrent()

        verify {
            stateMachine.send(
                withArg<StateMachineEvent> {
                    val event = it.shouldBeInstanceOf<AuthorizationEvent>()
                    event.eventType.shouldBeInstanceOf<AuthorizationEvent.EventType.RefreshSession>()
                }
            )
        }
    }

    @Test
    fun `sends StartFederationToIdentityPool for SessionError with federated credentials`() = runTest {
        stateFlow.value = authState(
            authNState = AuthenticationState.SignedIn(mockk(), mockk()),
            authZState = AuthorizationState.Error(
                SessionError(Exception("session error"), federatedCredential)
            )
        )

        backgroundScope.async { useCase.execute() }
        runCurrent()

        verify {
            stateMachine.send(
                withArg<StateMachineEvent> {
                    val event = it.shouldBeInstanceOf<AuthorizationEvent>()
                    event.eventType
                        .shouldBeInstanceOf<AuthorizationEvent.EventType.StartFederationToIdentityPool>()
                }
            )
        }
    }

    @Test
    fun `throws InvalidStateException for non-SessionError in error state`() = runTest {
        stateFlow.value = authState(
            authNState = AuthenticationState.SignedIn(mockk(), mockk()),
            authZState = AuthorizationState.Error(RuntimeException("not a session error"))
        )

        shouldThrow<InvalidStateException> {
            useCase.execute()
        }
    }

    @Test
    fun `embeds SignedOutException in session result`() = runTest {
        stateFlow.value = authState(
            authNState = AuthenticationState.SignedOut(SignedOutData()),
            authZState = AuthorizationState.Configured()
        )

        val deferred = backgroundScope.async { useCase.execute() }
        runCurrent()

        stateFlow.value = authState(
            authNState = AuthenticationState.SignedOut(SignedOutData()),
            authZState = AuthorizationState.Error(
                SessionError(SignedOutException(), AmplifyCredential.Empty)
            )
        )

        val result = deferred.await()
        result.shouldBeInstanceOf<AWSCognitoAuthSession>()
        result.identityIdResult.error.shouldBeInstanceOf<SignedOutException>()
    }

    @Test
    fun `embeds SessionExpiredException in session result and publishes hub event`() = runTest {
        stateFlow.value = authState(
            authNState = AuthenticationState.SignedOut(SignedOutData()),
            authZState = AuthorizationState.Configured()
        )

        val deferred = backgroundScope.async { useCase.execute() }
        runCurrent()

        stateFlow.value = authState(
            authNState = AuthenticationState.SignedIn(mockk(), mockk()),
            authZState = AuthorizationState.Error(
                SessionError(SessionExpiredException(), AmplifyCredential.Empty)
            )
        )

        val result = deferred.await()
        result.shouldBeInstanceOf<AWSCognitoAuthSession>()
        result.identityIdResult.error.shouldBeInstanceOf<SessionExpiredException>()

        verify {
            emitter.sendHubEvent(AuthChannelEventName.SESSION_EXPIRED.toString())
        }
    }

    @Test
    fun `embeds ServiceException in session result`() = runTest {
        stateFlow.value = authState(
            authNState = AuthenticationState.SignedOut(SignedOutData()),
            authZState = AuthorizationState.Configured()
        )

        val deferred = backgroundScope.async { useCase.execute() }
        runCurrent()

        stateFlow.value = authState(
            authNState = AuthenticationState.SignedIn(mockk(), mockk()),
            authZState = AuthorizationState.Error(
                SessionError(ServiceException("service error", "retry"), AmplifyCredential.Empty)
            )
        )

        val result = deferred.await()
        result.shouldBeInstanceOf<AWSCognitoAuthSession>()
        result.identityIdResult.error.shouldBeInstanceOf<ServiceException>()
    }

    @Test
    fun `embeds NotAuthorizedException in session result`() = runTest {
        stateFlow.value = authState(
            authNState = AuthenticationState.SignedOut(SignedOutData()),
            authZState = AuthorizationState.Configured()
        )

        val deferred = backgroundScope.async { useCase.execute() }
        runCurrent()

        stateFlow.value = authState(
            authNState = AuthenticationState.SignedIn(mockk(), mockk()),
            authZState = AuthorizationState.Error(
                SessionError(NotAuthorizedException(), AmplifyCredential.Empty)
            )
        )

        val result = deferred.await()
        result.shouldBeInstanceOf<AWSCognitoAuthSession>()
        result.identityIdResult.error.shouldBeInstanceOf<NotAuthorizedException>()
    }

    @Test
    fun `wraps unknown exceptions in UnknownException`() = runTest {
        stateFlow.value = authState(
            authNState = AuthenticationState.SignedOut(SignedOutData()),
            authZState = AuthorizationState.Configured()
        )

        val deferred = backgroundScope.async { useCase.execute() }
        runCurrent()

        stateFlow.value = authState(
            authNState = AuthenticationState.SignedIn(mockk(), mockk()),
            authZState = AuthorizationState.Error(
                SessionError(RuntimeException("something broke"), AmplifyCredential.Empty)
            )
        )

        val result = deferred.await()
        result.shouldBeInstanceOf<AWSCognitoAuthSession>()
        result.identityIdResult.error.shouldBeInstanceOf<UnknownException>()
    }

    @Test
    fun `returns empty session with InvalidAccountTypeException for ConfigurationException`() = runTest {
        stateFlow.value = authState(
            authNState = AuthenticationState.SignedOut(SignedOutData()),
            authZState = AuthorizationState.Configured()
        )

        val deferred = backgroundScope.async { useCase.execute() }
        runCurrent()

        stateFlow.value = authState(
            authNState = AuthenticationState.SignedIn(mockk(), mockk()),
            authZState = AuthorizationState.Error(
                ConfigurationException("config error", "fix config")
            )
        )

        val result = deferred.await()
        result.shouldBeInstanceOf<AWSCognitoAuthSession>()
        result.isSignedIn shouldBe false
    }

    @Test
    fun `throws InvalidStateException for unexpected authorization states`() = runTest {
        stateFlow.value = authState(
            authNState = AuthenticationState.SignedIn(mockk(), mockk()),
            authZState = AuthorizationState.FederatingToIdentityPool(mockk(), mockk(), mockk())
        )

        shouldThrow<InvalidStateException> {
            useCase.execute()
        }
    }
}
