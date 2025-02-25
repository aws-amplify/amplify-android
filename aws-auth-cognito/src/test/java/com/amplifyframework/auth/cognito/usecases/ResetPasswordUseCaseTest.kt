/*
 * Copyright 2022 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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
import aws.sdk.kotlin.services.cognitoidentityprovider.forgotPassword
import aws.sdk.kotlin.services.cognitoidentityprovider.model.AnalyticsMetadataType
import aws.sdk.kotlin.services.cognitoidentityprovider.model.CodeDeliveryDetailsType
import aws.sdk.kotlin.services.cognitoidentityprovider.model.CognitoIdentityProviderException
import aws.sdk.kotlin.services.cognitoidentityprovider.model.DeliveryMediumType
import aws.sdk.kotlin.services.cognitoidentityprovider.model.ForgotPasswordRequest
import aws.sdk.kotlin.services.cognitoidentityprovider.model.ForgotPasswordResponse
import com.amplifyframework.auth.cognito.AuthEnvironment
import com.amplifyframework.auth.cognito.MockAuthHelperRule
import com.amplifyframework.auth.cognito.exceptions.configuration.InvalidUserPoolConfigurationException
import com.amplifyframework.auth.cognito.util.toAuthCodeDeliveryDetails
import com.amplifyframework.auth.options.AuthResetPasswordOptions
import com.amplifyframework.auth.result.AuthResetPasswordResult
import com.amplifyframework.auth.result.step.AuthNextResetPasswordStep
import com.amplifyframework.auth.result.step.AuthResetPasswordStep
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.assertions.throwables.shouldThrowAny
import io.kotest.matchers.shouldBe
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test

class ResetPasswordUseCaseTest {

    private val dummyClientId = "app client id"
    private val dummyAppClientSecret = "app client secret"
    private val dummyUserName = "username"
    private val expectedPinpointEndpointId = "abc123"

    private val client: CognitoIdentityProviderClient = mockk {
        coEvery { forgotPassword(any()) } returns ForgotPasswordResponse {
            codeDeliveryDetails = mockk(relaxed = true)
        }
    }

    private val environment = mockk<AuthEnvironment> {
        coEvery { getDeviceMetadata("user")?.deviceKey } returns "test deviceKey"
        every { getPinpointEndpointId() } returns expectedPinpointEndpointId
        coEvery { getUserContextData(any()) } returns null
        every { configuration.userPool } returns mockk {
            every { appClient } returns dummyClientId
            every { appClientSecret } returns dummyAppClientSecret
        }
    }

    private val useCase = ResetPasswordUseCase(
        client = client,
        environment = environment
    )

    @get:Rule
    val authHelperRule = MockAuthHelperRule()

    @Test
    fun `use case calls forgotPassword API with given arguments`() = runTest {
        val expectedRequestBuilder: ForgotPasswordRequest.Builder.() -> Unit = {
            username = dummyUserName
            clientMetadata = mapOf()
            clientId = dummyClientId
            secretHash = MockAuthHelperRule.DEFAULT_HASH
            analyticsMetadata = AnalyticsMetadataType.invoke { analyticsEndpointId = expectedPinpointEndpointId }
        }

        useCase.execute(dummyUserName, AuthResetPasswordOptions.defaults())

        coVerify {
            client.forgotPassword(expectedRequestBuilder)
        }
    }

    @Test
    fun `AuthResetPasswordResult object is returned when reset password succeeds`() = runTest {
        val dummyCodeDeliveryDetails = CodeDeliveryDetailsType {
            destination = "dummy destination"
            deliveryMedium = DeliveryMediumType.Email
            attributeName = "dummy attribute"
        }

        val expectedResult = AuthResetPasswordResult(
            false,
            AuthNextResetPasswordStep(
                AuthResetPasswordStep.CONFIRM_RESET_PASSWORD_WITH_CODE,
                mapOf(),
                dummyCodeDeliveryDetails.toAuthCodeDeliveryDetails()
            )
        )

        coEvery { client.forgotPassword(any()) } returns
            ForgotPasswordResponse { codeDeliveryDetails = dummyCodeDeliveryDetails }

        val result = useCase.execute(dummyUserName)

        result shouldBe expectedResult
    }

    @Test
    fun `AuthException is thrown when forgotPassword API call fails`() = runTest {
        val expectedException = CognitoIdentityProviderException("Some SDK Message")

        coEvery { client.forgotPassword(any()) } throws expectedException

        shouldThrowAny {
            useCase.execute(dummyUserName)
        } shouldBe expectedException
    }

    @Test
    fun `reset password fails if appClientId is not set`() = runTest {
        every { environment.configuration.userPool?.appClient } returns null

        shouldThrow<InvalidUserPoolConfigurationException> {
            useCase.execute(dummyUserName)
        }
    }
}
