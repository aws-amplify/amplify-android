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
import aws.sdk.kotlin.services.cognitoidentityprovider.model.CodeDeliveryDetailsType
import aws.sdk.kotlin.services.cognitoidentityprovider.model.CognitoIdentityProviderException
import aws.sdk.kotlin.services.cognitoidentityprovider.model.DeliveryMediumType
import aws.sdk.kotlin.services.cognitoidentityprovider.model.ForgotPasswordRequest
import aws.sdk.kotlin.services.cognitoidentityprovider.model.ForgotPasswordResponse
import com.amplifyframework.auth.AuthException
import com.amplifyframework.auth.options.AuthResetPasswordOptions
import com.amplifyframework.auth.result.AuthResetPasswordResult
import com.amplifyframework.auth.result.step.AuthNextResetPasswordStep
import com.amplifyframework.auth.result.step.AuthResetPasswordStep
import com.amplifyframework.core.Consumer
import io.mockk.coEvery
import io.mockk.coJustRun
import io.mockk.coVerify
import io.mockk.justRun
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.newSingleThreadContext
import kotlin.test.assertEquals
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Test

@OptIn(DelicateCoroutinesApi::class, ExperimentalCoroutinesApi::class)
class ResetPasswordUseCaseTest {

    private val dummyClientId = "app client id"
    private val dummyUserName = "username"

    private val mockCognitoIPClient: CognitoIdentityProviderClient = mockk()

    private lateinit var resetPasswordUseCase: ResetPasswordUseCase

    // Used to execute a test in situations where the platform Main dispatcher is not available
    // see [https://kotlinlang.org/api/kotlinx.coroutines/kotlinx-coroutines-test/]
    private val mainThreadSurrogate = newSingleThreadContext("Main thread")

    @Before
    fun setUp() {
        Dispatchers.setMain(mainThreadSurrogate)
        resetPasswordUseCase = ResetPasswordUseCase(mockCognitoIPClient, dummyClientId)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `use case calls forgotPassword API with given arguments`() {
        // GIVEN
        val forgotPassWordRequest = ForgotPasswordRequest {
            username = dummyUserName
            clientMetadata = mapOf()
            clientId = dummyClientId
        }
        val forgotPasswordRequestSlot = slot<ForgotPasswordRequest>()
        coJustRun { mockCognitoIPClient.forgotPassword(capture(forgotPasswordRequestSlot)) }
        val stubPasswordRequest = ForgotPasswordRequest {
            username = dummyUserName
            clientMetadata = mapOf()
            clientId = dummyClientId
        }

        // WHEN
        runBlocking {
            resetPasswordUseCase.execute(dummyUserName, AuthResetPasswordOptions.defaults(), {}, {})
        }

        // THEN
        assertEquals(stubPasswordRequest, forgotPassWordRequest)
    }

    @Test
    fun `AuthResetPasswordResult object is returned when reset password succeeds`() {
        // GIVEN
        val onSuccess = mockk<Consumer<AuthResetPasswordResult>>()
        val onError = mockk<Consumer<AuthException>>()

        val dummyCodeDeliveryDetails = CodeDeliveryDetailsType.invoke {
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

        coEvery { mockCognitoIPClient.forgotPassword(captureLambda()) } coAnswers {
            ForgotPasswordResponse.invoke { codeDeliveryDetails = dummyCodeDeliveryDetails }
        }

        val resultCaptor = slot<AuthResetPasswordResult>()
        justRun { onSuccess.accept(capture(resultCaptor)) }

        // WHEN
        runBlocking {
            resetPasswordUseCase.execute(dummyUserName, AuthResetPasswordOptions.defaults(), onSuccess, onError)
        }

        // THEN
        coVerify(exactly = 0) { onError.accept(any()) }
        coVerify(exactly = 1) { onSuccess.accept(resultCaptor.captured) }

        assertEquals(expectedResult, resultCaptor.captured)
    }

    @Test
    fun `AuthException is thrown when forgotPassword API call fails`() {
        // GIVEN
        val onSuccess = mockk<Consumer<AuthResetPasswordResult>>()
        val onError = mockk<Consumer<AuthException>>()
        val expectedException = CognitoIdentityProviderException("Some SDK Message")

        coEvery { mockCognitoIPClient.forgotPassword(captureLambda()) } coAnswers {
            throw expectedException
        }

        val resultCaptor = slot<AuthException>()
        justRun { onError.accept(capture(resultCaptor)) }

        // WHEN
        runBlocking {
            resetPasswordUseCase.execute(dummyUserName, AuthResetPasswordOptions.defaults(), onSuccess, onError)
        }

        // THEN
        coVerify(exactly = 0) { onSuccess.accept(any()) }
        coVerify {
            onError.accept(resultCaptor.captured)
        }

        assertEquals(expectedException, resultCaptor.captured.cause)
    }
}
