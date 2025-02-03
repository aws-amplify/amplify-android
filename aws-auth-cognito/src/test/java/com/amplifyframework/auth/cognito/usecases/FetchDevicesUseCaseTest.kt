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

import aws.sdk.kotlin.services.cognitoidentityprovider.CognitoIdentityProviderClient
import aws.sdk.kotlin.services.cognitoidentityprovider.model.AttributeType
import aws.sdk.kotlin.services.cognitoidentityprovider.model.DeviceType
import aws.sdk.kotlin.services.cognitoidentityprovider.model.ListDevicesResponse
import com.amplifyframework.auth.cognito.AuthStateMachine
import com.amplifyframework.auth.exceptions.InvalidStateException
import com.amplifyframework.auth.exceptions.SignedOutException
import com.amplifyframework.statemachine.codegen.states.AuthenticationState
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.assertions.throwables.shouldThrowWithMessage
import io.kotest.matchers.shouldBe
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Test

class FetchDevicesUseCaseTest {
    private val client = mockk<CognitoIdentityProviderClient>()
    private val fetchAuthSession: FetchAuthSessionUseCase = mockk {
        coEvery { execute().accessToken } returns "access token"
    }
    private val stateMachine: AuthStateMachine = mockk {
        coEvery { getCurrentState().authNState } returns AuthenticationState.SignedIn(mockk(), mockk())
    }

    private val useCase = FetchDevicesUseCase(
        client = client,
        fetchAuthSession = fetchAuthSession,
        stateMachine = stateMachine
    )

    @Test
    fun `fetch devices returns device id and name`() = runTest {
        coEvery { client.listDevices(any()) } returns ListDevicesResponse {
            devices = listOf(
                DeviceType {
                    deviceKey = "id1"
                    deviceAttributes = listOf(
                        AttributeType {
                            name = "device_name"
                            value = "name1"
                        }
                    )
                }
            )
        }

        val result = useCase.execute()
        result.first().id shouldBe "id1"
        result.first().name shouldBe "name1"
    }

    @Test
    fun `fetch devices returns error if listDevices fails`() = runTest {
        coEvery { client.listDevices(any()) } throws Exception("bad")

        shouldThrowWithMessage<Exception>("bad") {
            useCase.execute()
        }
    }

    @Test
    fun `fetch devices returns error if signed out`() = runTest {
        coEvery { stateMachine.getCurrentState().authNState } returns AuthenticationState.SignedOut(mockk())

        shouldThrow<SignedOutException> {
            useCase.execute()
        }
    }

    @Test
    fun `fetch devices returns error if not signed in`() = runTest {
        coEvery { stateMachine.getCurrentState().authNState } returns AuthenticationState.NotConfigured()

        shouldThrow<InvalidStateException> {
            useCase.execute()
        }
    }
}
