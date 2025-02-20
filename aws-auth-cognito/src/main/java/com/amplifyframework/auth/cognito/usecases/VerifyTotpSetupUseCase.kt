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
import aws.sdk.kotlin.services.cognitoidentityprovider.model.VerifySoftwareTokenResponseType
import aws.sdk.kotlin.services.cognitoidentityprovider.verifySoftwareToken
import com.amplifyframework.AmplifyException
import com.amplifyframework.auth.cognito.AuthStateMachine
import com.amplifyframework.auth.cognito.options.AWSCognitoAuthVerifyTOTPSetupOptions
import com.amplifyframework.auth.cognito.requireAccessToken
import com.amplifyframework.auth.cognito.requireSignedInState
import com.amplifyframework.auth.exceptions.ServiceException
import com.amplifyframework.auth.options.AuthVerifyTOTPSetupOptions

internal class VerifyTotpSetupUseCase(
    private val fetchAuthSession: FetchAuthSessionUseCase,
    private val client: CognitoIdentityProviderClient,
    private val stateMachine: AuthStateMachine
) {

    suspend fun execute(code: String, options: AuthVerifyTOTPSetupOptions) {
        val cognitoOptions = options as? AWSCognitoAuthVerifyTOTPSetupOptions
        val deviceName = cognitoOptions?.friendlyDeviceName

        stateMachine.requireSignedInState()

        val token = fetchAuthSession.execute().requireAccessToken()

        val response = client.verifySoftwareToken {
            userCode = code
            friendlyDeviceName = deviceName
            accessToken = token
        }

        if (response.status != VerifySoftwareTokenResponseType.Success) {
            throw ServiceException(
                message = "An unknown service error has occurred",
                recoverySuggestion = AmplifyException.TODO_RECOVERY_SUGGESTION
            )
        }
    }
}
