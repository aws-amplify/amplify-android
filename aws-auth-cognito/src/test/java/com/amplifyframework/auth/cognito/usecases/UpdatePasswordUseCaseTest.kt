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
import aws.sdk.kotlin.services.cognitoidentityprovider.model.ChangePasswordResponse
import com.amplifyframework.auth.cognito.AuthStateMachine
import com.amplifyframework.auth.exceptions.InvalidStateException
import com.amplifyframework.statemachine.codegen.states.AuthenticationState
import io.kotest.assertions.throwables.shouldNotThrowAny
import io.kotest.assertions.throwables.shouldThrow
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Test

class UpdatePasswordUseCaseTest {
    private val client = mockk<CognitoIdentityProviderClient>(relaxed = true)
    private val fetchAuthSession: FetchAuthSessionUseCase = mockk {
        coEvery { execute().accessToken } returns "access token"
    }
    private val stateMachine: AuthStateMachine = mockk {
        coEvery { getCurrentState().authNState } returns AuthenticationState.SignedIn(mockk(), mockk())
    }

    private val useCase = UpdatePasswordUseCase(
        client = client,
        fetchAuthSession = fetchAuthSession,
        stateMachine = stateMachine
    )

    @Test
    fun `update password with success`() = runTest {
        coEvery { client.changePassword(any()) } returns ChangePasswordResponse { }

        // WHEN
        shouldNotThrowAny {
            useCase.execute("old", "new")
        }
    }

    @Test
    fun `update password fails when not in SignedIn state`() = runTest {
        coEvery { stateMachine.getCurrentState().authNState } returns AuthenticationState.NotConfigured()

        shouldThrow<InvalidStateException> {
            useCase.execute("old", "new")
        }
    }
}
