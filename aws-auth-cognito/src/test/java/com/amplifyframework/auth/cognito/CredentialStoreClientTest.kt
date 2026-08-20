/*
 * Copyright 2023 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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

package com.amplifyframework.auth.cognito

import com.amplifyframework.auth.exceptions.InvalidStateException
import com.amplifyframework.logging.Logger
import com.amplifyframework.statemachine.codegen.data.AmplifyCredential
import com.amplifyframework.statemachine.codegen.data.CredentialType
import com.amplifyframework.statemachine.codegen.errors.CredentialStoreError
import com.amplifyframework.statemachine.codegen.events.CredentialStoreEvent
import com.amplifyframework.statemachine.codegen.states.CredentialStoreState
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import io.mockk.every
import io.mockk.justRun
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test

internal class CredentialStoreClientTest {

    private val stateFlow = MutableSharedFlow<CredentialStoreState>(replay = 1)
    private val stateMachine: CredentialStoreStateMachine = mockk {
        every { state } returns stateFlow
        justRun { send(any()) }
    }
    private val logger: Logger = mockk(relaxed = true)
    private lateinit var client: CredentialStoreClient

    @Before
    fun setup() {
        client = CredentialStoreClient(stateMachine, logger)
    }

    private suspend fun emitStateSequence(vararg states: CredentialStoreState) {
        for (state in states) {
            stateFlow.emit(state)
        }
    }

    @Test
    fun `loadCredentials returns credentials on success`() = runTest {
        val expectedCredential = AmplifyCredential.Empty
        val credentialType = CredentialType.Amplify

        // Emit initial state so the flow has a replay value
        stateFlow.emit(CredentialStoreState.NotConfigured())

        launch {
            emitStateSequence(
                CredentialStoreState.LoadingStoredCredentials(),
                CredentialStoreState.Success(expectedCredential),
                CredentialStoreState.Idle()
            )
        }

        val result = client.loadCredentials(credentialType)

        result shouldBe expectedCredential
        val eventSlot = slot<CredentialStoreEvent>()
        verify { stateMachine.send(capture(eventSlot)) }
        eventSlot.captured.eventType.shouldBeInstanceOf<CredentialStoreEvent.EventType.LoadCredentialStore>()
    }

    @Test
    fun `storeCredentials sends store event and completes on success`() = runTest {
        val credential = AmplifyCredential.Empty
        val credentialType = CredentialType.Amplify

        stateFlow.emit(CredentialStoreState.Idle())

        launch {
            emitStateSequence(
                CredentialStoreState.StoringCredentials(),
                CredentialStoreState.Success(credential),
                CredentialStoreState.Idle()
            )
        }

        client.storeCredentials(credentialType, credential)

        val eventSlot = slot<CredentialStoreEvent>()
        verify { stateMachine.send(capture(eventSlot)) }
        eventSlot.captured.eventType.shouldBeInstanceOf<CredentialStoreEvent.EventType.StoreCredentials>()
    }

    @Test
    fun `clearCredentials sends clear event and completes on success`() = runTest {
        val credentialType = CredentialType.Amplify

        stateFlow.emit(CredentialStoreState.Idle())

        launch {
            emitStateSequence(
                CredentialStoreState.ClearingCredentials(),
                CredentialStoreState.Success(AmplifyCredential.Empty),
                CredentialStoreState.Idle()
            )
        }

        client.clearCredentials(credentialType)

        val eventSlot = slot<CredentialStoreEvent>()
        verify { stateMachine.send(capture(eventSlot)) }
        eventSlot.captured.eventType.shouldBeInstanceOf<CredentialStoreEvent.EventType.ClearCredentialStore>()
    }

    @Test
    fun `loadCredentials throws on state machine error`() = runTest {
        val error = CredentialStoreError("test error")
        val credentialType = CredentialType.Amplify

        stateFlow.emit(CredentialStoreState.NotConfigured())

        launch {
            emitStateSequence(
                CredentialStoreState.LoadingStoredCredentials(),
                CredentialStoreState.Error(error),
                CredentialStoreState.Idle()
            )
        }

        val thrown = shouldThrow<CredentialStoreError> {
            client.loadCredentials(credentialType)
        }
        thrown.message shouldBe "test error"
    }

    @Test
    fun `loadCredentials throws InvalidStateException when no result before idle`() = runTest {
        val credentialType = CredentialType.Amplify

        stateFlow.emit(CredentialStoreState.NotConfigured())

        launch {
            // Go straight to Idle without Success or Error
            stateFlow.emit(CredentialStoreState.Idle())
        }

        shouldThrow<InvalidStateException> {
            client.loadCredentials(credentialType)
        }
    }

    @Test
    fun `loadCredentials with device credential type sends correct event`() = runTest {
        val expectedCredential = AmplifyCredential.Empty
        val credentialType = CredentialType.Device("testUser")

        stateFlow.emit(CredentialStoreState.Idle())

        launch {
            emitStateSequence(
                CredentialStoreState.LoadingStoredCredentials(),
                CredentialStoreState.Success(expectedCredential),
                CredentialStoreState.Idle()
            )
        }

        val result = client.loadCredentials(credentialType)

        result shouldBe expectedCredential
        val eventSlot = slot<CredentialStoreEvent>()
        verify { stateMachine.send(capture(eventSlot)) }
        val eventType = eventSlot.captured.eventType
        eventType.shouldBeInstanceOf<CredentialStoreEvent.EventType.LoadCredentialStore>()
        eventType.credentialType shouldBe CredentialType.Device("testUser")
    }

    @Test
    fun `error result is captured only once when multiple errors emitted`() = runTest {
        val firstError = CredentialStoreError("first error")
        val secondError = CredentialStoreError("second error")
        val credentialType = CredentialType.Amplify

        stateFlow.emit(CredentialStoreState.NotConfigured())

        launch {
            emitStateSequence(
                CredentialStoreState.Error(firstError),
                CredentialStoreState.Error(secondError),
                CredentialStoreState.Idle()
            )
        }

        val thrown = shouldThrow<CredentialStoreError> {
            client.loadCredentials(credentialType)
        }
        // The first error should be captured (result ?: prevents overwrite)
        thrown.message shouldBe "first error"
    }
}
