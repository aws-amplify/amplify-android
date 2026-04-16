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

import com.amplifyframework.auth.AuthProvider
import com.amplifyframework.auth.cognito.AWSCognitoAuthChannelEventName
import com.amplifyframework.auth.cognito.AuthStateMachine
import com.amplifyframework.auth.cognito.testUtil.authState
import com.amplifyframework.auth.exceptions.InvalidStateException
import com.amplifyframework.auth.exceptions.UnknownException
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
import io.mockk.verify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class FederateToIdentityPoolUseCaseTest {

    private val credentials = AWSCredentials(
        accessKeyId = "accessKeyId",
        secretAccessKey = "secretAccessKey",
        sessionToken = "sessionToken",
        expiration = 9999999999L
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

    private val useCase = FederateToIdentityPoolUseCase(
        stateMachine = stateMachine,
        emitter = emitter
    )

    @Test
    fun `sends StartFederationToIdentityPool in valid states`() = runTest {
        backgroundScope.async { useCase.execute("token", AuthProvider.facebook()) }
        runCurrent()

        verify {
            stateMachine.send(
                withArg<StateMachineEvent> {
                    val event = it.shouldBeInstanceOf<AuthorizationEvent>()
                    val type = event.eventType
                        .shouldBeInstanceOf<AuthorizationEvent.EventType.StartFederationToIdentityPool>()
                    type.token.token shouldBe "token"
                    type.token.providerName shouldBe "graph.facebook.com"
                    type.identityId shouldBe null
                    type.existingCredential shouldBe null
                }
            )
        }
    }

    @Test
    fun `returns FederateToIdentityPoolResult with credentials and identity ID on success`() = runTest {
        val deferred = backgroundScope.async { useCase.execute("token", AuthProvider.facebook()) }
        runCurrent()

        stateFlow.value = authState(
            authNState = AuthenticationState.FederatedToIdentityPool(),
            authZState = AuthorizationState.SessionEstablished(
                AmplifyCredential.IdentityPoolFederated(
                    federatedToken = FederatedToken("token", "graph.facebook.com"),
                    identityId = "identity-id-123",
                    credentials = credentials
                )
            )
        )

        val result = deferred.await()
        result.identityId shouldBe "identity-id-123"
        result.credentials.accessKeyId shouldBe "accessKeyId"
        result.credentials.secretAccessKey shouldBe "secretAccessKey"
        result.credentials.sessionToken shouldBe "sessionToken"
    }

    @Test
    fun `publishes FEDERATED_TO_IDENTITY_POOL hub event on success`() = runTest {
        val deferred = backgroundScope.async { useCase.execute("token", AuthProvider.facebook()) }
        runCurrent()

        stateFlow.value = authState(
            authNState = AuthenticationState.FederatedToIdentityPool(),
            authZState = AuthorizationState.SessionEstablished(
                AmplifyCredential.IdentityPoolFederated(
                    federatedToken = FederatedToken("token", "graph.facebook.com"),
                    identityId = "identity-id-123",
                    credentials = credentials
                )
            )
        )

        deferred.await()

        verify {
            emitter.sendHubEvent(AWSCognitoAuthChannelEventName.FEDERATED_TO_IDENTITY_POOL.toString())
        }
    }

    @Test
    fun `throws UnknownException when credentials cannot be parsed`() = runTest {
        val supervisor = SupervisorJob(backgroundScope.coroutineContext[Job])
        val deferred = backgroundScope.async(supervisor) {
            useCase.execute("token", AuthProvider.facebook())
        }
        runCurrent()

        // Use a non-IdentityPoolFederated credential so identityId/temporaryAwsCredentials are null
        stateFlow.value = authState(
            authNState = AuthenticationState.FederatedToIdentityPool(),
            authZState = AuthorizationState.SessionEstablished(AmplifyCredential.Empty)
        )

        shouldThrow<UnknownException> {
            deferred.await()
        }
    }

    @Test
    fun `throws InvalidStateException for invalid states`() = runTest {
        stateFlow.value = authState(
            authNState = AuthenticationState.SignedIn(mockk(), mockk()),
            authZState = AuthorizationState.Configured()
        )

        shouldThrow<InvalidStateException> {
            useCase.execute("token", AuthProvider.facebook())
        }
    }

    @Test
    fun `throws converted exception on AuthN and AuthZ error`() = runTest {
        val supervisor = SupervisorJob(backgroundScope.coroutineContext[Job])
        val deferred = backgroundScope.async(supervisor) { useCase.execute("token", AuthProvider.facebook()) }
        runCurrent()

        val exception = Exception("federation failed")
        stateFlow.value = authState(
            authNState = AuthenticationState.Error(exception),
            authZState = AuthorizationState.Error(
                exception = SessionError(exception, AmplifyCredential.Empty)
            )
        )

        shouldThrow<com.amplifyframework.auth.AuthException> {
            deferred.await()
        }
    }
}
