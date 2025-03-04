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
import aws.sdk.kotlin.services.cognitoidentityprovider.model.DeviceRememberedStatusType
import aws.sdk.kotlin.services.cognitoidentityprovider.model.UpdateDeviceStatusResponse
import com.amplifyframework.auth.cognito.AuthEnvironment
import com.amplifyframework.auth.cognito.AuthStateMachine
import com.amplifyframework.auth.exceptions.InvalidStateException
import com.amplifyframework.auth.exceptions.SignedOutException
import com.amplifyframework.statemachine.codegen.states.AuthenticationState
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.assertions.throwables.shouldThrowWithMessage
import io.kotest.matchers.shouldBe
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Test

class RememberDeviceUseCaseTest {
    private val client: CognitoIdentityProviderClient = mockk()
    private val fetchAuthSession: FetchAuthSessionUseCase = mockk {
        coEvery { execute().accessToken } returns "access token"
    }
    private val stateMachine: AuthStateMachine = mockk {
        coEvery { getCurrentState().authNState } returns AuthenticationState.SignedIn(
            signedInData = mockk { every { username } returns "user" },
            deviceMetadata = mockk()
        )
    }
    private val authEnvironment = mockk<AuthEnvironment> {
        coEvery { getDeviceMetadata("user")?.deviceKey } returns "test deviceKey"
    }

    private val useCase = RememberDeviceUseCase(
        client = client,
        fetchAuthSession = fetchAuthSession,
        stateMachine = stateMachine,
        environment = authEnvironment
    )

    @Test
    fun `remember device invokes API`() = runTest {
        coEvery { client.updateDeviceStatus(any()) } returns UpdateDeviceStatusResponse { }

        useCase.execute()
        coVerify {
            client.updateDeviceStatus(
                withArg { request ->
                    request.accessToken shouldBe "access token"
                    request.deviceKey shouldBe "test deviceKey"
                    request.deviceRememberedStatus shouldBe DeviceRememberedStatusType.Remembered
                }
            )
        }
    }

    @Test
    fun `remember device returns error if updateDeviceStatus fails`() = runTest {
        coEvery { client.updateDeviceStatus(any()) } throws Exception("bad")

        shouldThrowWithMessage<Exception>("bad") {
            useCase.execute()
        }
    }

    @Test
    fun `remember device returns error if signed out`() = runTest {
        coEvery { stateMachine.getCurrentState().authNState } returns AuthenticationState.SignedOut(mockk())

        shouldThrow<SignedOutException> {
            useCase.execute()
        }
    }

    @Test
    fun `remember device returns error if not signed in`() = runTest {
        coEvery { stateMachine.getCurrentState().authNState } returns AuthenticationState.NotConfigured()

        shouldThrow<InvalidStateException> {
            useCase.execute()
        }
    }
}
