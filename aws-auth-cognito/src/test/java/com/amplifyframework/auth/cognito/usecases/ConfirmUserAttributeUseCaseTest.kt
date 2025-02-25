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
import aws.sdk.kotlin.services.cognitoidentityprovider.model.CognitoIdentityProviderException
import aws.sdk.kotlin.services.cognitoidentityprovider.model.VerifyUserAttributeResponse
import com.amplifyframework.auth.AuthUserAttributeKey
import com.amplifyframework.auth.cognito.AuthStateMachine
import com.amplifyframework.auth.cognito.exceptions.configuration.InvalidUserPoolConfigurationException
import com.amplifyframework.auth.exceptions.InvalidStateException
import com.amplifyframework.statemachine.codegen.states.AuthenticationState
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Test

class ConfirmUserAttributeUseCaseTest {

    private val client = mockk<CognitoIdentityProviderClient>(relaxed = true)
    private val fetchAuthSession: FetchAuthSessionUseCase = mockk {
        coEvery { execute().accessToken } returns "access token"
    }
    private val stateMachine: AuthStateMachine = mockk {
        coEvery { getCurrentState().authNState } returns AuthenticationState.SignedIn(
            signedInData = mockk { every { username } returns "user" },
            deviceMetadata = mockk()
        )
    }

    private val useCase = ConfirmUserAttributeUseCase(
        client = client,
        fetchAuthSession = fetchAuthSession,
        stateMachine = stateMachine
    )

    @Test
    fun `confirm user attribute fails when not in SignedIn state`() = runTest {
        coEvery { stateMachine.getCurrentState().authNState } returns AuthenticationState.NotConfigured()

        shouldThrow<InvalidStateException> {
            useCase.execute(AuthUserAttributeKey.email(), "000000")
        }
    }

    @Test
    fun `confirm user attribute fails when access token is invalid`() = runTest {
        coEvery { fetchAuthSession.execute().accessToken } returns null

        shouldThrow<InvalidUserPoolConfigurationException> {
            useCase.execute(AuthUserAttributeKey.email(), "000000")
        }
    }

    @Test
    fun `confirm user attributes with cognito api call error`() = runTest {
        val expectedException = CognitoIdentityProviderException("Some Cognito Message")
        coEvery { client.verifyUserAttribute(any()) } throws expectedException

        val exception = shouldThrow<CognitoIdentityProviderException> {
            useCase.execute(AuthUserAttributeKey.email(), "000000")
        }

        exception shouldBe expectedException
    }

    @Test
    fun `confirm user attributes with success`() = runTest {
        coEvery { client.verifyUserAttribute(any()) } returns VerifyUserAttributeResponse {}

        useCase.execute(AuthUserAttributeKey.email(), "000000")

        coVerify {
            client.verifyUserAttribute(
                withArg {
                    it.attributeName shouldBe "email"
                    it.code shouldBe "000000"
                }
            )
        }
    }
}
