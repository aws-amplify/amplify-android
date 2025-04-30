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
import aws.sdk.kotlin.services.cognitoidentityprovider.model.CodeMismatchException
import aws.sdk.kotlin.services.cognitoidentityprovider.model.VerifySoftwareTokenRequest
import aws.sdk.kotlin.services.cognitoidentityprovider.model.VerifySoftwareTokenResponse
import aws.sdk.kotlin.services.cognitoidentityprovider.model.VerifySoftwareTokenResponseType
import com.amplifyframework.auth.cognito.AuthStateMachine
import com.amplifyframework.auth.cognito.mockSignedInData
import com.amplifyframework.auth.cognito.options.AWSCognitoAuthVerifyTOTPSetupOptions
import com.amplifyframework.statemachine.codegen.states.AuthenticationState
import io.kotest.assertions.throwables.shouldNotThrow
import io.kotest.assertions.throwables.shouldThrowWithMessage
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Test

class VerifyTotpSetupUseCaseTest {
    private val client: CognitoIdentityProviderClient = mockk()
    private val fetchAuthSession: FetchAuthSessionUseCase = mockk {
        coEvery { execute().accessToken } returns "access token"
    }
    private val stateMachine: AuthStateMachine = mockk {
        coEvery { getCurrentState().authNState } returns AuthenticationState.SignedIn(
            signedInData = mockSignedInData(),
            deviceMetadata = mockk()
        )
    }

    private val useCase = VerifyTotpSetupUseCase(
        fetchAuthSession = fetchAuthSession,
        client = client,
        stateMachine = stateMachine
    )

    @Test
    fun `verifyTOTP on success`() = runTest {
        val code = "123456"
        val deviceName = "DEVICE_NAME"
        val options = AWSCognitoAuthVerifyTOTPSetupOptions.builder().friendlyDeviceName(deviceName).build()

        coEvery {
            client.verifySoftwareToken(
                VerifySoftwareTokenRequest {
                    userCode = code
                    friendlyDeviceName = deviceName
                    accessToken = "access token"
                }
            )
        } returns VerifySoftwareTokenResponse { status = VerifySoftwareTokenResponseType.Success }

        shouldNotThrow<Exception> {
            useCase.execute(code, options)
        }
    }

    @Test
    fun `verifyTOTP on error`() = runTest {
        val code = "123456"
        val errorMessage = "Invalid code"
        val options = AWSCognitoAuthVerifyTOTPSetupOptions.builder().build()

        coEvery {
            client.verifySoftwareToken(
                VerifySoftwareTokenRequest {
                    userCode = code
                    accessToken = "access token"
                }
            )
        } throws CodeMismatchException { message = errorMessage }

        shouldThrowWithMessage<CodeMismatchException>(errorMessage) {
            useCase.execute(code, options)
        }
    }
}
