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
import com.amplifyframework.auth.cognito.AuthEnvironment
import com.amplifyframework.auth.cognito.AuthStateMachine
import com.amplifyframework.auth.cognito.helpers.AuthHelper
import com.amplifyframework.auth.cognito.options.AWSCognitoAuthConfirmResetPasswordOptions
import com.amplifyframework.auth.exceptions.ConfigurationException
import com.amplifyframework.auth.options.AuthConfirmResetPasswordOptions
import com.amplifyframework.statemachine.codegen.states.AuthenticationState

internal class ConfirmResetPasswordUseCase(
    private val client: CognitoIdentityProviderClient,
    private val environment: AuthEnvironment,
    private val stateMachine: AuthStateMachine
) {

    suspend fun execute(
        username: String,
        newPassword: String,
        code: String,
        options: AuthConfirmResetPasswordOptions = AuthConfirmResetPasswordOptions.defaults()
    ) {
        stateMachine.throwIfNotConfigured()

        val awsOptions = options as? AWSCognitoAuthConfirmResetPasswordOptions

        val encodedContextData = environment.getUserContextData(username)
        val pinpointEndpointId = environment.getPinpointEndpointId()

        client.confirmForgotPassword {
            this.username = username
            confirmationCode = code
            password = newPassword

            secretHash = AuthHelper.getSecretHash(
                username,
                environment.configuration.userPool?.appClient,
                environment.configuration.userPool?.appClientSecret
            )
            clientMetadata = awsOptions?.metadata ?: emptyMap()
            clientId = environment.configuration.userPool?.appClient
            encodedContextData?.let { this.userContextData { encodedData = it } }
            pinpointEndpointId?.let {
                this.analyticsMetadata = AnalyticsMetadataType { analyticsEndpointId = it }
            }
        }
    }

    private suspend fun AuthStateMachine.throwIfNotConfigured() {
        if (getCurrentState().authNState is AuthenticationState.NotConfigured) {
            throw ConfigurationException(
                "Confirm Reset Password failed.",
                "Cognito User Pool not configured. Please check your configuration file."
            )
        }
    }
}
