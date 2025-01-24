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
import aws.sdk.kotlin.services.cognitoidentityprovider.model.CodeDeliveryDetailsType
import aws.sdk.kotlin.services.cognitoidentityprovider.model.CognitoIdentityProviderException
import aws.sdk.kotlin.services.cognitoidentityprovider.model.DeliveryMediumType
import aws.sdk.kotlin.services.cognitoidentityprovider.model.GetUserAttributeVerificationCodeResponse
import com.amplifyframework.auth.AuthCodeDeliveryDetails
import com.amplifyframework.auth.AuthUserAttributeKey
import com.amplifyframework.auth.cognito.AuthStateMachine
import com.amplifyframework.auth.cognito.exceptions.configuration.InvalidUserPoolConfigurationException
import com.amplifyframework.auth.cognito.options.AWSCognitoAuthResendUserAttributeConfirmationCodeOptions
import com.amplifyframework.auth.cognito.shouldHaveAttributeName
import com.amplifyframework.auth.cognito.shouldHaveDeliveryMedium
import com.amplifyframework.auth.cognito.shouldHaveDestination
import com.amplifyframework.auth.exceptions.InvalidStateException
import com.amplifyframework.statemachine.codegen.states.AuthenticationState
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Test

class ResendUserAttributeConfirmationUseCaseTest {

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

    private val useCase = ResendUserAttributeConfirmationUseCase(
        client = client,
        fetchAuthSession = fetchAuthSession,
        stateMachine = stateMachine
    )

    @Test
    fun `resend user attribute confirmation code fails when not in SignedIn state`() = runTest {
        coEvery { stateMachine.getCurrentState().authNState } returns AuthenticationState.NotConfigured()

        shouldThrow<InvalidStateException> {
            useCase.execute(AuthUserAttributeKey.email())
        }
    }

    @Test
    fun `resend user attribute confirmation code fails when access token is invalid`() = runTest {
        coEvery { fetchAuthSession.execute().accessToken } returns null

        shouldThrow<InvalidUserPoolConfigurationException> {
            useCase.execute(AuthUserAttributeKey.email())
        }
    }

    @Test
    fun `resend user attribute confirmation code with cognito api call error`() = runTest {
        val expectedException = CognitoIdentityProviderException("Some Cognito Message")
        coEvery { client.getUserAttributeVerificationCode(any()) } throws expectedException

        val exception = shouldThrow<CognitoIdentityProviderException> {
            useCase.execute(AuthUserAttributeKey.email())
        }

        exception shouldBe expectedException
    }

    @Test
    fun `resend user attribute confirmation code with delivery code success`() = runTest {
        coEvery { client.getUserAttributeVerificationCode(any()) } returns GetUserAttributeVerificationCodeResponse {
            codeDeliveryDetails = CodeDeliveryDetailsType {
                attributeName = "email"
                deliveryMedium = DeliveryMediumType.Email
                destination = "test"
            }
        }

        val options = AWSCognitoAuthResendUserAttributeConfirmationCodeOptions.builder().metadata(
            mapOf("x" to "x", "y" to "y", "z" to "z")
        ).build()

        val result = useCase.execute(AuthUserAttributeKey.email(), options)

        result.shouldHaveAttributeName("email")
            .shouldHaveDestination("test")
            .shouldHaveDeliveryMedium(AuthCodeDeliveryDetails.DeliveryMedium.EMAIL)
    }
}
