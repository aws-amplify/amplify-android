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
import aws.sdk.kotlin.services.cognitoidentityprovider.model.CompleteWebAuthnRegistrationResponse
import aws.sdk.kotlin.services.cognitoidentityprovider.model.StartWebAuthnRegistrationResponse
import aws.smithy.kotlin.runtime.content.Document
import com.amplifyframework.auth.cognito.AuthStateMachine
import com.amplifyframework.auth.cognito.helpers.WebAuthnHelper
import com.amplifyframework.auth.exceptions.SignedOutException
import com.amplifyframework.auth.options.AuthAssociateWebAuthnCredentialsOptions
import com.amplifyframework.statemachine.codegen.states.AuthenticationState
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Test

class AssociateWebAuthnCredentialUseCaseTest {
    private val identityProviderClient: CognitoIdentityProviderClient = mockk {
        coEvery { startWebAuthnRegistration(any()) } returns StartWebAuthnRegistrationResponse {
            this.credentialCreationOptions = Document(mapOf("a" to Document("b")))
        }
        coEvery { completeWebAuthnRegistration(any()) } returns CompleteWebAuthnRegistrationResponse {}
    }
    private val fetchAuthSession: FetchAuthSessionUseCase = mockk {
        coEvery { execute().accessToken } returns "access token"
    }
    private val stateMachine: AuthStateMachine = mockk {
        coEvery { getCurrentState().authNState } returns AuthenticationState.SignedIn(mockk(), mockk())
    }
    private val webAuthnHelper: WebAuthnHelper = mockk {
        coEvery { createCredential(any(), any()) } returns """{"created":"credential"}"""
    }

    private val useCase = AssociateWebAuthnCredentialUseCase(
        identityProviderClient,
        fetchAuthSession,
        stateMachine,
        webAuthnHelper
    )

    @Test
    fun `fails if not in SignedIn state`() = runTest {
        coEvery { stateMachine.getCurrentState().authNState } returns AuthenticationState.SignedOut(mockk())

        shouldThrow<SignedOutException> {
            useCase.execute(mockk(), AuthAssociateWebAuthnCredentialsOptions.defaults())
        }
    }

    @Test
    fun `invokes startWebAuthnRegistration`() = runTest {
        useCase.execute(mockk(), AuthAssociateWebAuthnCredentialsOptions.defaults())
        coVerify {
            identityProviderClient.startWebAuthnRegistration(
                withArg { it.accessToken shouldBe "access token" }
            )
        }
    }

    @Test
    fun `invokes webAuthnHelper with expected JSON`() = runTest {
        useCase.execute(mockk(), AuthAssociateWebAuthnCredentialsOptions.defaults())
        coVerify {
            webAuthnHelper.createCredential("{\"a\":\"b\"}", any())
        }
    }

    @Test
    fun `invokes completeWebAuthnRegistration with created credential`() = runTest {
        useCase.execute(mockk(), AuthAssociateWebAuthnCredentialsOptions.defaults())
        coVerify {
            identityProviderClient.completeWebAuthnRegistration(
                withArg {
                    it.credential shouldBe Document(mapOf("created" to Document("credential")))
                    it.accessToken shouldBe "access token"
                }
            )
        }
    }
}
