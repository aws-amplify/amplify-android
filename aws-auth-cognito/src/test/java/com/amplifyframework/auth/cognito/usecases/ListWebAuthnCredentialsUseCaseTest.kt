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
import aws.sdk.kotlin.services.cognitoidentityprovider.model.ListWebAuthnCredentialsResponse
import com.amplifyframework.auth.cognito.AuthStateMachine
import com.amplifyframework.auth.cognito.mockWebAuthnCredentialDescription
import com.amplifyframework.auth.cognito.options.AWSCognitoAuthListWebAuthnCredentialsOptions
import com.amplifyframework.auth.exceptions.SignedOutException
import com.amplifyframework.auth.options.AuthListWebAuthnCredentialsOptions
import com.amplifyframework.statemachine.codegen.states.AuthenticationState
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.collections.shouldMatchEach
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Test

class ListWebAuthnCredentialsUseCaseTest {

    private val identityProviderClient: CognitoIdentityProviderClient = mockk {
        coEvery { listWebAuthnCredentials(any()) } returns ListWebAuthnCredentialsResponse { credentials = emptyList() }
    }
    private val fetchAuthSession: FetchAuthSessionUseCase = mockk {
        coEvery { execute().accessToken } returns "access token"
    }
    private val stateMachine: AuthStateMachine = mockk {
        coEvery { getCurrentState().authNState } returns AuthenticationState.SignedIn(mockk(), mockk())
    }
    private val useCase = ListWebAuthnCredentialsUseCase(identityProviderClient, fetchAuthSession, stateMachine)

    @Test
    fun `invokes identity provider service and maps result`() = runTest {
        val credentials = listOf(
            mockWebAuthnCredentialDescription(friendlyName = "a"),
            mockWebAuthnCredentialDescription(friendlyName = "b")
        )

        coEvery { identityProviderClient.listWebAuthnCredentials(any()) } returns ListWebAuthnCredentialsResponse {
            this.credentials = credentials
            nextToken = "next"
        }

        val result = useCase.execute(AuthListWebAuthnCredentialsOptions.defaults())

        result.nextToken shouldBe "next"
        result.credentials shouldMatchEach listOf(
            { it.friendlyName shouldBe "a" },
            { it.friendlyName shouldBe "b" }
        )
    }

    @Test
    fun `sets options in request`() = runTest {
        val options = AWSCognitoAuthListWebAuthnCredentialsOptions {
            nextToken = "testNext"
            maxResults = 34
        }
        useCase.execute(options)

        coVerify {
            identityProviderClient.listWebAuthnCredentials(
                withArg {
                    it.nextToken shouldBe "testNext"
                    it.maxResults shouldBe 34
                }
            )
        }
    }

    @Test
    fun `passes null for missing options`() = runTest {
        useCase.execute(AuthListWebAuthnCredentialsOptions.defaults())

        coVerify {
            identityProviderClient.listWebAuthnCredentials(
                withArg {
                    it.nextToken.shouldBeNull()
                    it.maxResults.shouldBeNull()
                }
            )
        }
    }

    @Test
    fun `fails if not in SignedIn state`() = runTest {
        coEvery { stateMachine.getCurrentState().authNState } returns AuthenticationState.SignedOut(mockk())

        shouldThrow<SignedOutException> {
            useCase.execute(AuthListWebAuthnCredentialsOptions.defaults())
        }
    }
}
