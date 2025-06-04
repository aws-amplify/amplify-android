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
import aws.sdk.kotlin.services.cognitoidentityprovider.changePassword
import com.amplifyframework.auth.cognito.AuthStateMachine
import com.amplifyframework.auth.cognito.requireAccessToken
import com.amplifyframework.auth.cognito.requireSignedInState

internal class UpdatePasswordUseCase(
    private val client: CognitoIdentityProviderClient,
    private val fetchAuthSession: FetchAuthSessionUseCase,
    private val stateMachine: AuthStateMachine
) {
    suspend fun execute(oldPassword: String, newPassword: String) {
        stateMachine.requireSignedInState()
        val token = fetchAuthSession.execute().requireAccessToken()
        client.changePassword {
            previousPassword = oldPassword
            proposedPassword = newPassword
            accessToken = token
        }
    }
}
