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
import aws.sdk.kotlin.services.cognitoidentityprovider.model.CognitoIdentityProviderException
import aws.sdk.kotlin.services.cognitoidentityprovider.model.ConfirmForgotPasswordRequest
import aws.sdk.kotlin.services.cognitoidentityprovider.model.ConfirmForgotPasswordResponse
import com.amplifyframework.auth.AuthException
import com.amplifyframework.auth.options.AuthConfirmResetPasswordOptions
import com.amplifyframework.core.Action
import com.amplifyframework.core.Consumer
import io.mockk.coEvery
import io.mockk.coJustRun
import io.mockk.coVerify
import io.mockk.justRun
import io.mockk.mockk
import io.mockk.slot
import kotlin.test.assertEquals
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.newSingleThreadContext
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Test

@OptIn(DelicateCoroutinesApi::class, ExperimentalCoroutinesApi::class)
class ConfirmResetPasswordUseCaseTest {
    private val dummyClientId = "app client id"
    private val dummyUserName = "username"
    private val dummyPassword = "new password"
    private val dummyCode = "new code"

    private val mockCognitoIPClient: CognitoIdentityProviderClient = mockk()

    private lateinit var confirmResetPasswordUseCase: ConfirmResetPasswordUseCase

    // Used to execute a test in situations where the platform Main dispatcher is not available
    // see [https://kotlinlang.org/api/kotlinx.coroutines/kotlinx-coroutines-test/]
    private val mainThreadSurrogate = newSingleThreadContext("Main thread")

    @Before
    fun setUp() {
        Dispatchers.setMain(mainThreadSurrogate)
        confirmResetPasswordUseCase = ConfirmResetPasswordUseCase(mockCognitoIPClient, dummyClientId)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `use case calls confirmForgotPassword API with given arguments`() {
        // GIVEN
        val requestBuilderCaptor = slot<ConfirmForgotPasswordRequest.Builder.() -> Unit>()
        coJustRun { mockCognitoIPClient.confirmForgotPassword(capture(requestBuilderCaptor)) }

        val expectedRequestBuilder: ConfirmForgotPasswordRequest.Builder.() -> Unit = {
            username = dummyUserName
            password = dummyPassword
            this.confirmationCode = dummyCode
            clientMetadata = mapOf()
            clientId = dummyClientId
        }

        // WHEN
        runBlocking {
            confirmResetPasswordUseCase.execute(
                dummyUserName,
                dummyPassword,
                dummyCode,
                AuthConfirmResetPasswordOptions.defaults(),
                {},
                {}
            )
        }

        // THEN
        assertEquals(
            ConfirmForgotPasswordRequest.invoke(expectedRequestBuilder),
            ConfirmForgotPasswordRequest.invoke(requestBuilderCaptor.captured)
        )
    }

    @Test
    fun `onSuccess is called when confirm reset password call succeeds`() {
        // GIVEN
        val onSuccess = mockk<Action>()
        val onError = mockk<Consumer<AuthException>>()

        coEvery { mockCognitoIPClient.confirmForgotPassword(captureLambda()) } coAnswers {
            ConfirmForgotPasswordResponse.invoke { }
        }

        justRun { onSuccess.call() }

        // WHEN
        runBlocking {
            confirmResetPasswordUseCase.execute(
                dummyUserName,
                dummyPassword,
                dummyCode,
                AuthConfirmResetPasswordOptions.defaults(),
                onSuccess,
                onError
            )
        }

        // THEN
        coVerify(exactly = 0) { onError.accept(any()) }
        coVerify(exactly = 1) { onSuccess.call() }
    }

    @Test
    fun `AuthException is thrown when confirmForgotPassword API call fails`() {
        // GIVEN
        val onSuccess = mockk<Action>()
        val onError = mockk<Consumer<AuthException>>()
        val expectedException = CognitoIdentityProviderException("Some SDK Message")

        coEvery { mockCognitoIPClient.confirmForgotPassword(captureLambda()) } coAnswers {
            throw expectedException
        }

        val resultCaptor = slot<AuthException>()
        justRun { onError.accept(capture(resultCaptor)) }

        // WHEN
        runBlocking {
            confirmResetPasswordUseCase.execute(
                dummyUserName,
                dummyPassword,
                dummyCode,
                AuthConfirmResetPasswordOptions.defaults(),
                onSuccess,
                onError
            )
        }

        // THEN
        coVerify(exactly = 0) { onSuccess.call() }
        coVerify {
            onError.accept(resultCaptor.captured)
        }

        assertEquals(expectedException, resultCaptor.captured.cause)
    }
}
