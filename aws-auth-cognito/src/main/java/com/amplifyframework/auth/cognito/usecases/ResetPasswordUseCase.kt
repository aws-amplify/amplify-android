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
import com.amplifyframework.AmplifyException
import com.amplifyframework.auth.AuthCodeDeliveryDetails
import com.amplifyframework.auth.AuthException
import com.amplifyframework.auth.cognito.CognitoAuthExceptionConverter
import com.amplifyframework.auth.cognito.helpers.AuthHelper
import com.amplifyframework.auth.cognito.options.AWSCognitoAuthResetPasswordOptions
import com.amplifyframework.auth.options.AuthResetPasswordOptions
import com.amplifyframework.auth.result.AuthResetPasswordResult
import com.amplifyframework.auth.result.step.AuthNextResetPasswordStep
import com.amplifyframework.auth.result.step.AuthResetPasswordStep
import com.amplifyframework.core.Consumer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Business logic to reset password if user forgot the old password.
 */
internal class ResetPasswordUseCase(
    private val cognitoIdentityProviderClient: CognitoIdentityProviderClient,
    private val appClientId: String,
    private val appClientSecret: String?
) {
    suspend fun execute(
        username: String,
        options: AuthResetPasswordOptions,
        encodedContextData: String?,
        pinpointEndpointId: String?,
        onSuccess: Consumer<AuthResetPasswordResult>,
        onError: Consumer<AuthException>
    ) {
        try {
            val response = withContext(Dispatchers.IO) {
                cognitoIdentityProviderClient.forgotPassword {
                    this.username = username
                    this.clientMetadata = (options as? AWSCognitoAuthResetPasswordOptions)?.metadata ?: mapOf()
                    this.clientId = appClientId
                    this.secretHash = AuthHelper.getSecretHash(
                        username,
                        appClientId,
                        appClientSecret
                    )
                    encodedContextData?.let { this.userContextData { encodedData = it } }
                    pinpointEndpointId?.let {
                        this.analyticsMetadata = AnalyticsMetadataType.invoke { analyticsEndpointId = it }
                    }
                }
            }

            withContext(Dispatchers.Main) {
                onSuccess.accept(
                    AuthResetPasswordResult(
                        false,
                        AuthNextResetPasswordStep(
                            AuthResetPasswordStep.CONFIRM_RESET_PASSWORD_WITH_CODE,
                            mapOf(),
                            response.codeDeliveryDetails.toAuthCodeDeliveryDetails()
                        )
                    )
                )
            }
        } catch (ex: Exception) {
            withContext(Dispatchers.Main) {
                onError.accept(
                    CognitoAuthExceptionConverter.lookup(ex, AmplifyException.REPORT_BUG_TO_AWS_SUGGESTION)
                )
            }
        }
    }
}

internal fun CodeDeliveryDetailsType?.toAuthCodeDeliveryDetails(): AuthCodeDeliveryDetails? {
    if (this == null) return this

    requireNotNull(destination)

    return AuthCodeDeliveryDetails(
        destination.toString(),
        AuthCodeDeliveryDetails.DeliveryMedium.fromString(deliveryMedium.toString()),
        attributeName
    )
}
