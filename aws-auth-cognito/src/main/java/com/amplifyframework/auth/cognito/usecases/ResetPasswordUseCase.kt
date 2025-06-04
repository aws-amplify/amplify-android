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
import com.amplifyframework.auth.cognito.AuthEnvironment
import com.amplifyframework.auth.cognito.helpers.AuthHelper
import com.amplifyframework.auth.cognito.options.AWSCognitoAuthResetPasswordOptions
import com.amplifyframework.auth.cognito.util.toAuthCodeDeliveryDetails
import com.amplifyframework.auth.options.AuthResetPasswordOptions
import com.amplifyframework.auth.result.AuthResetPasswordResult
import com.amplifyframework.auth.result.step.AuthNextResetPasswordStep
import com.amplifyframework.auth.result.step.AuthResetPasswordStep
import com.amplifyframework.statemachine.codegen.data.requireAppClientId

/**
 * Business logic to reset password if user forgot the old password.
 */
internal class ResetPasswordUseCase(
    private val client: CognitoIdentityProviderClient,
    private val environment: AuthEnvironment
) {
    suspend fun execute(
        username: String,
        options: AuthResetPasswordOptions = AuthResetPasswordOptions.defaults()
    ): AuthResetPasswordResult {
        val awsOptions = options as? AWSCognitoAuthResetPasswordOptions

        val appClientId = environment.configuration.userPool.requireAppClientId()
        val appClientSecret = environment.configuration.userPool?.appClientSecret
        val encodedContextData = environment.getUserContextData(username)
        val pinpointEndpointId = environment.getPinpointEndpointId()

        val response = client.forgotPassword {
            this.username = username
            clientMetadata = awsOptions?.metadata ?: emptyMap()
            clientId = appClientId
            secretHash = AuthHelper.getSecretHash(
                username,
                appClientId,
                appClientSecret
            )
            encodedContextData?.let { this.userContextData { encodedData = it } }
            pinpointEndpointId?.let {
                this.analyticsMetadata = AnalyticsMetadataType { analyticsEndpointId = it }
            }
        }

        return AuthResetPasswordResult(
            false,
            AuthNextResetPasswordStep(
                AuthResetPasswordStep.CONFIRM_RESET_PASSWORD_WITH_CODE,
                emptyMap(),
                response.codeDeliveryDetails.toAuthCodeDeliveryDetails()
            )
        )
    }
}
