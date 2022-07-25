/*
 * Copyright (c) 2022-2022 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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
import com.amplifyframework.auth.AuthException
import com.amplifyframework.auth.AuthException.REPORT_BUG_TO_AWS_SUGGESTION
import com.amplifyframework.auth.cognito.CognitoAuthExceptionConverter
import com.amplifyframework.auth.options.AuthConfirmResetPasswordOptions
import com.amplifyframework.core.Action
import com.amplifyframework.core.Consumer
import java.lang.Exception
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * UseCase to run after [ResetPasswordUseCase] to respond to any challenges that service may request.
 * [Request API](https://docs.aws.amazon.com/cognito-user-identity-pools/latest/APIReference/API_ConfirmForgotPassword.html)
 */
internal class ConfirmResetPasswordUseCase(
    private val cognitoIdentityProviderClient: CognitoIdentityProviderClient,
    private val appClientId: String
) {
    suspend fun execute(
        username: String,
        newPassword: String,
        confirmationCode: String,
        options: AuthConfirmResetPasswordOptions,
        onSuccess: Action,
        onException: Consumer<AuthException>
    ) {
        try {
            withContext(Dispatchers.IO) {
                cognitoIdentityProviderClient.confirmForgotPassword {
                    this.username = username
                    this.password = newPassword
                    this.confirmationCode = confirmationCode
                    this.clientMetadata = (options as? AWSCognitoAuthConfirmResetPasswordOptions)?.metadata ?: mapOf()
                    this.clientId = appClientId
                }
            }

            withContext(Dispatchers.Main) { onSuccess.call() }
        } catch (ex: Exception) {
            withContext(Dispatchers.Main) {
                onException.accept(CognitoAuthExceptionConverter.lookup(ex, REPORT_BUG_TO_AWS_SUGGESTION))
            }
        }
    }
}

/**
 * Cognito extension of confirm reset password options to add the platform specific fields.
 */
data class AWSCognitoAuthConfirmResetPasswordOptions(
    val metadata: Map<String, String>
) : AuthConfirmResetPasswordOptions()
