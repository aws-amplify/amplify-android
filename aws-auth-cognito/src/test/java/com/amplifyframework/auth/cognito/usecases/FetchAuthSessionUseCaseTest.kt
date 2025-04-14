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
import com.amplifyframework.auth.cognito.helpers.SessionHelper
import com.amplifyframework.auth.cognito.mockSignedInData
import com.amplifyframework.auth.cognito.testUtil.authState
import com.amplifyframework.auth.cognito.testUtil.withAuthZEvent
import com.amplifyframework.auth.exceptions.InvalidStateException
import com.amplifyframework.auth.options.AuthFetchSessionOptions
import com.amplifyframework.auth.plugins.core.AuthHubEventEmitter
import com.amplifyframework.auth.result.AuthSessionResult
import com.amplifyframework.statemachine.codegen.data.AWSCredentials
import com.amplifyframework.statemachine.codegen.data.AmplifyCredential
import com.amplifyframework.statemachine.codegen.data.CognitoUserPoolTokens
import com.amplifyframework.statemachine.codegen.data.FederatedToken
import com.amplifyframework.statemachine.codegen.errors.SessionError
import com.amplifyframework.statemachine.codegen.events.AuthorizationEvent
import com.amplifyframework.statemachine.codegen.states.AuthState
import com.amplifyframework.statemachine.codegen.states.AuthorizationState
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.booleans.shouldBeFalse
import io.kotest.matchers.booleans.shouldBeTrue
import io.kotest.matchers.shouldBe
import io.mockk.coEvery
import io.mockk.every
import io.mockk.justRun
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.unmockkObject
import io.mockk.verify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class FetchAuthSessionUseCaseTest {
    private val credential = AmplifyCredential.UserPool(signedInData = mockSignedInData())

    private val stateFlow = MutableStateFlow<AuthState>(
        authState(
            authZState = AuthorizationState.SessionEstablished(amplifyCredential = credential)
        )
    )

    private val stateMachine: AuthStateMachine = mockk {
        every { state } returns stateFlow
        every { stateTransitions } answers { stateFlow.drop(1) }
        coEvery { getCurrentState() } answers { stateFlow.value }
        justRun { send(any()) }
    }

    private val emitter: AuthHubEventEmitter = mockk(relaxed = true)

    private val useCase = FetchAuthSessionUseCase(
        stateMachine = stateMachine,
        emitter = emitter
    )

    @Before
    fun setup() {
        mockkObject(SessionHelper)
        coEvery { SessionHelper.isValidSession(any()) } returns true
        coEvery { SessionHelper.isValidTokens(any()) } returns true
    }

    @After
    fun teardown() {
        unmockkObject(SessionHelper)
    }

    @Test
    fun `fetches unauthed session`() = runTest {
        stateFlow.value = authState(authZState = AuthorizationState.Configured())

        backgroundScope.launch { useCase.execute() }
        runCurrent()

        verify {
            stateMachine.send(
                withArg { it is AuthorizationEvent && it.eventType !is AuthorizationEvent.EventType.FetchUnAuthSession }
            )
        }
    }

    @Test
    fun `returns valid session`() = runTest {
        val result = useCase.execute()
        result.userPoolTokensResult.type shouldBe AuthSessionResult.Type.SUCCESS
    }

    @Test
    fun `refreshes expired session`() = runTest {
        every { SessionHelper.isValidTokens(any()) } returns false

        backgroundScope.launch { useCase.execute() }
        runCurrent()

        verify {
            stateMachine.send(
                withAuthZEvent<AuthorizationEvent.EventType.RefreshSession> { event ->
                    event.amplifyCredential shouldBe credential
                }
            )
        }
    }

    @Test
    fun `sends federation event to refresh federated session`() = runTest {
        val credential = AmplifyCredential.IdentityPoolFederated(
            federatedToken = FederatedToken(token = "token", providerName = "provider"),
            identityId = "federatedIdentity",
            credentials = mockk<AWSCredentials>()
        )

        stateFlow.value = authState(authZState = AuthorizationState.SessionEstablished(credential))
        every { SessionHelper.isValidSession(any()) } returns false

        backgroundScope.launch { useCase.execute() }
        runCurrent()

        verify {
            stateMachine.send(
                withAuthZEvent<AuthorizationEvent.EventType.StartFederationToIdentityPool> { event ->
                    event.token shouldBe credential.federatedToken
                    event.identityId shouldBe credential.identityId
                    event.existingCredential shouldBe credential
                }
            )
        }
    }

    @Test
    fun `refreshes session if forceRefresh is true`() = runTest {
        val options = AuthFetchSessionOptions.builder().forceRefresh(true).build()

        backgroundScope.launch { useCase.execute(options) }
        runCurrent()

        verify {
            stateMachine.send(
                withAuthZEvent<AuthorizationEvent.EventType.RefreshSession> { event ->
                    event.amplifyCredential shouldBe credential
                }
            )
        }
    }

    @Test
    fun `refreshes federated session if forceRefresh is true`() = runTest {
        val credential = AmplifyCredential.IdentityPoolFederated(
            federatedToken = FederatedToken(token = "token", providerName = "provider"),
            identityId = "federatedIdentity",
            credentials = mockk<AWSCredentials>()
        )

        stateFlow.value = authState(authZState = AuthorizationState.SessionEstablished(credential))

        val options = AuthFetchSessionOptions.builder().forceRefresh(true).build()
        backgroundScope.launch { useCase.execute(options) }
        runCurrent()

        verify {
            stateMachine.send(
                withAuthZEvent<AuthorizationEvent.EventType.StartFederationToIdentityPool> { event ->
                    event.token shouldBe credential.federatedToken
                    event.identityId shouldBe credential.identityId
                    event.existingCredential shouldBe credential
                }
            )
        }
    }

    @Test
    fun `refreshes session from a session error state`() = runTest {
        val sessionError = SessionError(Exception("Failure"), credential)
        stateFlow.value = authState(authZState = AuthorizationState.Error(sessionError))

        backgroundScope.launch { useCase.execute() }
        runCurrent()

        verify {
            stateMachine.send(
                withAuthZEvent<AuthorizationEvent.EventType.RefreshSession> { event ->
                    event.amplifyCredential shouldBe credential
                }
            )
        }
    }

    @Test
    fun `fails if authorization is in a non-session error state`() = runTest {
        val error = Exception("Some non-session error")
        stateFlow.value = authState(authZState = AuthorizationState.Error(error))

        shouldThrow<InvalidStateException> {
            useCase.execute()
        }
    }

    @Test
    fun `fails if authorization is in an unexpected state`() = runTest {
        stateFlow.value = authState(authZState = AuthorizationState.NotConfigured())

        shouldThrow<InvalidStateException> {
            useCase.execute()
        }
    }

    @Test
    fun `returns session when session is established`() = runTest {
        val options = AuthFetchSessionOptions.builder().forceRefresh(true).build()
        val result = backgroundScope.async { useCase.execute(options) }
        runCurrent()

        val userPoolTokens = CognitoUserPoolTokens(
            idToken = "idToken",
            accessToken = "access",
            refreshToken = "refresh",
            expiration = 100L
        )
        val newCredential = AmplifyCredential.UserPool(
            signedInData = mockSignedInData(cognitoUserPoolTokens = userPoolTokens)
        )
        stateFlow.value = authState(authZState = AuthorizationState.SessionEstablished(newCredential))

        val session = result.await()
        session.isSignedIn.shouldBeTrue()
        session.userPoolTokensResult.value?.accessToken shouldBe userPoolTokens.accessToken
    }

    @Test
    fun `returns session when error state is reached`() = runTest {
        val options = AuthFetchSessionOptions.builder().forceRefresh(true).build()
        val result = backgroundScope.async { useCase.execute(options) }
        runCurrent()

        val exception = Exception("Something failed")
        stateFlow.value = authState(authZState = AuthorizationState.Error(exception))

        val session = result.await()
        session.isSignedIn.shouldBeFalse()
    }
}
