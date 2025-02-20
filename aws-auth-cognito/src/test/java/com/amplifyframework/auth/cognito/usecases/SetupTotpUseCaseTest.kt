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
import aws.sdk.kotlin.services.cognitoidentityprovider.model.AssociateSoftwareTokenResponse
import aws.sdk.kotlin.services.cognitoidentityprovider.model.DeleteWebAuthnCredentialResponse
import aws.sdk.kotlin.services.cognitoidentityprovider.model.SoftwareTokenMfaNotFoundException
import com.amplifyframework.auth.cognito.AuthStateMachine
import com.amplifyframework.auth.cognito.mockSignedInData
import com.amplifyframework.statemachine.codegen.states.AuthenticationState
import io.kotest.assertions.throwables.shouldThrowWithMessage
import io.kotest.matchers.shouldBe
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Test

class SetupTotpUseCaseTest {
    private val client: CognitoIdentityProviderClient = mockk {
        coEvery { deleteWebAuthnCredential(any()) } returns DeleteWebAuthnCredentialResponse { }
    }
    private val fetchAuthSession: FetchAuthSessionUseCase = mockk {
        coEvery { execute().accessToken } returns "access token"
    }
    private val stateMachine: AuthStateMachine = mockk {
        coEvery { getCurrentState().authNState } returns AuthenticationState.SignedIn(
            signedInData = mockSignedInData(username = "user"),
            deviceMetadata = mockk()
        )
    }

    private val useCase = SetupTotpUseCase(
        fetchAuthSession = fetchAuthSession,
        client = client,
        stateMachine = stateMachine
    )

    @Test
    fun `setupTOTP on success`() = runTest {
        val session = "SESSION"
        val secretCode = "SECRET_CODE"

        coEvery { client.associateSoftwareToken(any()) } returns AssociateSoftwareTokenResponse {
            this.session = session
            this.secretCode = secretCode
        }

        val result = useCase.execute()

        result.username shouldBe "user"
        result.sharedSecret shouldBe secretCode
    }

    @Test
    fun `setupTOTP on error`() = runTest {
        val expectedErrorMessage = "Software token MFA not enabled"
        coEvery { client.associateSoftwareToken(any()) } throws SoftwareTokenMfaNotFoundException {
            message = expectedErrorMessage
        }

        shouldThrowWithMessage<SoftwareTokenMfaNotFoundException>(expectedErrorMessage) {
            useCase.execute()
        }
    }
}
