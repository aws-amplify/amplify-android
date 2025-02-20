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
import aws.sdk.kotlin.services.cognitoidentityprovider.associateSoftwareToken
import com.amplifyframework.AmplifyException
import com.amplifyframework.auth.AuthException
import com.amplifyframework.auth.TOTPSetupDetails
import com.amplifyframework.auth.cognito.AuthStateMachine
import com.amplifyframework.auth.cognito.requireAccessToken
import com.amplifyframework.auth.cognito.requireSignedInState

internal class SetupTotpUseCase(
    private val fetchAuthSession: FetchAuthSessionUseCase,
    private val client: CognitoIdentityProviderClient,
    private val stateMachine: AuthStateMachine
) {

    suspend fun execute(): TOTPSetupDetails {
        val state = stateMachine.requireSignedInState()

        val token = fetchAuthSession.execute().requireAccessToken()

        val response = client.associateSoftwareToken {
            accessToken = token
        }

        val sharedSecret = response.secretCode ?: throw AuthException(
            "Shared secret missing from response",
            AmplifyException.REPORT_BUG_TO_AWS_SUGGESTION
        )

        return TOTPSetupDetails(
            sharedSecret = sharedSecret,
            username = state.signedInData.username
        )
    }
}
