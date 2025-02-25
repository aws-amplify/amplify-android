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
import aws.sdk.kotlin.services.cognitoidentityprovider.confirmForgotPassword
import aws.sdk.kotlin.services.cognitoidentityprovider.model.AnalyticsMetadataType
import aws.sdk.kotlin.services.cognitoidentityprovider.model.CognitoIdentityProviderException
import aws.sdk.kotlin.services.cognitoidentityprovider.model.ConfirmForgotPasswordRequest
import aws.sdk.kotlin.services.cognitoidentityprovider.model.ConfirmForgotPasswordResponse
import com.amplifyframework.auth.cognito.AuthEnvironment
import com.amplifyframework.auth.cognito.AuthStateMachine
import com.amplifyframework.auth.cognito.MockAuthHelperRule
import com.amplifyframework.auth.exceptions.ConfigurationException
import com.amplifyframework.statemachine.codegen.states.AuthenticationState
import io.kotest.assertions.throwables.shouldNotThrowAny
import io.kotest.assertions.throwables.shouldThrowAny
import io.kotest.matchers.shouldBe
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test

class ConfirmResetPasswordUseCaseTest {
    private val client = mockk<CognitoIdentityProviderClient>(relaxed = true)
    private val stateMachine: AuthStateMachine = mockk {
        coEvery { getCurrentState().authNState } returns AuthenticationState.SignedIn(mockk(), mockk())
    }
    private val environment = mockk<AuthEnvironment> {
        coEvery { getDeviceMetadata("user")?.deviceKey } returns "test deviceKey"
        every { getPinpointEndpointId() } returns "pinpointId"
        coEvery { getUserContextData(any()) } returns null
        every { configuration.userPool } returns mockk {
            every { appClient } returns "appClient"
            every { appClientSecret } returns "appClientSecret"
        }
    }

    private val useCase = ConfirmResetPasswordUseCase(
        client = client,
        environment = environment,
        stateMachine = stateMachine
    )

    @get:Rule
    val authHelperMock = MockAuthHelperRule()

    @Test
    fun `confirmResetPassword fails if authentication state is NotConfigured`() = runTest {
        // Given
        coEvery { stateMachine.getCurrentState().authNState } returns AuthenticationState.NotConfigured()

        val expectedError = ConfigurationException(
            "Confirm Reset Password failed.",
            "Cognito User Pool not configured. Please check amplifyconfiguration.json file."
        )

        shouldThrowAny {
            useCase.execute("username", "password", "123456")
        } shouldBe expectedError
    }

    @Test
    fun `confirmResetPassword calls confirmForgotPassword API with given arguments`() = runTest {
        // GIVEN
        coEvery { client.confirmForgotPassword(any()) } returns ConfirmForgotPasswordResponse { }

        val user = "username"
        val pass = "passworD"
        val code = "007"

        val expectedRequestBuilder: ConfirmForgotPasswordRequest.Builder.() -> Unit = {
            username = user
            password = pass
            confirmationCode = code
            clientMetadata = emptyMap()
            clientId = "appClient"
            secretHash = MockAuthHelperRule.DEFAULT_HASH
            userContextData = null
            analyticsMetadata = AnalyticsMetadataType.invoke { analyticsEndpointId = "pinpointId" }
        }

        useCase.execute("username", "passworD", "007")

        coVerify {
            client.confirmForgotPassword(expectedRequestBuilder)
        }
    }

    @Test
    fun `confirmResetPassword call succeeds`() = runTest {
        coEvery { client.confirmForgotPassword(any()) } returns ConfirmForgotPasswordResponse { }

        shouldNotThrowAny {
            useCase.execute("username", "passworD", "007")
        }
    }

    @Test
    fun `Exception is thrown when confirmForgotPassword API call fails`() = runTest {
        val expectedException = CognitoIdentityProviderException("Some SDK Message")
        coEvery { client.confirmForgotPassword(any()) } throws expectedException

        shouldThrowAny {
            useCase.execute("username", "passworD", "007")
        } shouldBe expectedException
    }
}
