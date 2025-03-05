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
import aws.sdk.kotlin.services.cognitoidentityprovider.model.AnalyticsMetadataType
import aws.sdk.kotlin.services.cognitoidentityprovider.model.ForgotPasswordResponse
import aws.sdk.kotlin.services.cognitoidentityprovider.model.ResendConfirmationCodeRequest
import aws.sdk.kotlin.services.cognitoidentityprovider.resendConfirmationCode
import com.amplifyframework.auth.cognito.AuthEnvironment
import com.amplifyframework.auth.cognito.AuthStateMachine
import com.amplifyframework.auth.cognito.MockAuthHelperRule
import com.amplifyframework.auth.cognito.exceptions.configuration.InvalidUserPoolConfigurationException
import com.amplifyframework.auth.cognito.mockSignedInData
import com.amplifyframework.statemachine.codegen.states.AuthenticationState
import io.kotest.assertions.throwables.shouldThrow
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test

class ResendSignupCodeUseCaseTest {
    private val client: CognitoIdentityProviderClient = mockk {
        coEvery { forgotPassword(any()) } returns ForgotPasswordResponse {
            codeDeliveryDetails = mockk(relaxed = true)
        }
    }

    private val environment = mockk<AuthEnvironment> {
        coEvery { getDeviceMetadata("user")?.deviceKey } returns "test deviceKey"
        every { getPinpointEndpointId() } returns "pinpointId"
        coEvery { getUserContextData(any()) } returns null
        every { configuration.userPool } returns mockk {
            every { appClient } returns "clientId"
            every { appClientSecret } returns "clientSecret"
        }
    }
    private val stateMachine: AuthStateMachine = mockk {
        coEvery { getCurrentState().authNState } returns AuthenticationState.SignedIn(
            signedInData = mockSignedInData(username = "user"),
            deviceMetadata = mockk()
        )
    }

    private val useCase = ResendSignupCodeUseCase(
        client = client,
        environment = environment,
        stateMachine = stateMachine
    )

    @get:Rule
    val authHelperRule = MockAuthHelperRule()

    @Test
    fun `fails if not configured`() = runTest {
        coEvery { stateMachine.getCurrentState().authNState } returns AuthenticationState.NotConfigured()

        shouldThrow<InvalidUserPoolConfigurationException> {
            useCase.execute("username")
        }
    }

    @Test
    fun `succeeds if signed out`() = runTest {
        coEvery { stateMachine.getCurrentState().authNState } returns AuthenticationState.SignedOut(mockk())
        runSuccessTest()
    }

    @Test
    fun `succeeds if signed in`() = runTest {
        runSuccessTest()
    }

    private suspend fun runSuccessTest() {
        coEvery { client.resendConfirmationCode(any()) } returns mockk(relaxed = true)

        val username = "user"

        val expectedRequest: ResendConfirmationCodeRequest.Builder.() -> Unit = {
            clientId = "clientId"
            this.username = username
            secretHash = MockAuthHelperRule.DEFAULT_HASH
            analyticsMetadata = AnalyticsMetadataType.invoke { analyticsEndpointId = "pinpointId" }
        }

        // WHEN
        useCase.execute(username)

        coVerify {
            client.resendConfirmationCode(expectedRequest)
        }
    }
}
