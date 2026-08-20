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
import com.amplifyframework.auth.cognito.mockSignedInData
import com.amplifyframework.auth.exceptions.InvalidStateException
import com.amplifyframework.auth.exceptions.SignedOutException
import com.amplifyframework.statemachine.codegen.states.AuthenticationState
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Test

class GetCurrentUserUseCaseTest {
    private val stateMachine: AuthStateMachine = mockk {
        coEvery { getCurrentState().authNState } returns AuthenticationState.SignedIn(
            signedInData = mockSignedInData(username = "username", userId = "sub"),
            deviceMetadata = mockk()
        )
    }

    private val useCase = GetCurrentUserUseCase(
        stateMachine = stateMachine
    )

    @Test
    fun `gets auth user`() = runTest {
        val user = useCase.execute()

        user.username shouldBe "username"
        user.userId shouldBe "sub"
    }

    @Test
    fun `throws exception if not in signed in state`() = runTest {
        coEvery { stateMachine.getCurrentState().authNState } returns AuthenticationState.NotConfigured()
        shouldThrow<InvalidStateException> {
            useCase.execute()
        }
    }

    @Test
    fun `throws exception if signed out`() = runTest {
        coEvery { stateMachine.getCurrentState().authNState } returns AuthenticationState.SignedOut(mockk())
        shouldThrow<SignedOutException> {
            useCase.execute()
        }
    }
}
