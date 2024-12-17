/*
 * Copyright 2024 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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
import aws.sdk.kotlin.services.cognitoidentityprovider.model.DeleteWebAuthnCredentialResponse
import com.amplifyframework.auth.cognito.AuthStateMachine
import com.amplifyframework.auth.exceptions.InvalidStateException
import com.amplifyframework.auth.options.AuthDeleteWebAuthnCredentialOptions
import com.amplifyframework.statemachine.codegen.states.AuthenticationState
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Test

class DeleteWebAuthnCredentialsUseCaseTest {
    private val identityProviderClient: CognitoIdentityProviderClient = mockk {
        coEvery { deleteWebAuthnCredential(any()) } returns DeleteWebAuthnCredentialResponse { }
    }
    private val fetchAuthSession: FetchAuthSessionUseCase = mockk {
        coEvery { execute().accessToken } returns "access token"
    }
    private val stateMachine: AuthStateMachine = mockk {
        coEvery { getCurrentState().authNState } returns AuthenticationState.SignedIn(mockk(), mockk())
    }
    private val useCase = DeleteWebAuthnCredentialUseCase(
        identityProviderClient,
        fetchAuthSession,
        stateMachine
    )

    @Test
    fun `fails if not in SignedIn state`() = runTest {
        coEvery { stateMachine.getCurrentState().authNState } returns AuthenticationState.SignedOut(mockk())

        shouldThrow<InvalidStateException> {
            useCase.execute("credentialId", AuthDeleteWebAuthnCredentialOptions.defaults())
        }
    }

    @Test
    fun `invokes Kotlin SDK`() = runTest {
        useCase.execute("credentialId", AuthDeleteWebAuthnCredentialOptions.defaults())

        coVerify {
            identityProviderClient.deleteWebAuthnCredential(
                withArg {
                    it.credentialId shouldBe "credentialId"
                    it.accessToken shouldBe "access token"
                }
            )
        }
    }
}
