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

import com.amplifyframework.auth.AuthCodeDeliveryDetails
import com.amplifyframework.auth.AuthException
import com.amplifyframework.auth.AuthUser
import com.amplifyframework.auth.cognito.AuthEnvironment
import com.amplifyframework.auth.cognito.CognitoAuthExceptionConverter
import com.amplifyframework.auth.cognito.helpers.SRPHelper
import com.amplifyframework.auth.cognito.options.AWSCognitoAuthResendSignUpCodeOptions
import com.amplifyframework.auth.options.AuthResendSignUpCodeOptions
import com.amplifyframework.auth.result.AuthSignUpResult
import com.amplifyframework.auth.result.step.AuthNextSignUpStep
import com.amplifyframework.auth.result.step.AuthSignUpStep
import com.amplifyframework.core.Consumer

internal class ResendSignUpCodeUseCase(environment: AuthEnvironment) {
    private val logger = environment.logger
    private val configuration = environment.configuration
    private val client = environment.cognitoAuthService.cognitoIdentityProviderClient

    suspend fun execute(
        username: String,
        options: AuthResendSignUpCodeOptions,
        onSuccess: Consumer<AuthSignUpResult>,
        onError: Consumer<AuthException>
    ) {
        logger?.verbose("ResendSignUpCode Starting execution")
        try {
            val metadata = (options as? AWSCognitoAuthResendSignUpCodeOptions)?.metadata

            val response = client?.resendConfirmationCode {
                clientId = configuration.userPool?.appClient
                this.username = username
                secretHash = SRPHelper.getSecretHash(
                    username,
                    configuration.userPool?.appClient,
                    configuration.userPool?.appClientSecret
                )
                clientMetadata = metadata
            }

            val deliveryDetails = response?.codeDeliveryDetails?.let { details ->
                mapOf(
                    "DESTINATION" to details.destination,
                    "MEDIUM" to details.deliveryMedium?.value,
                    "ATTRIBUTE" to details.attributeName
                )
            }

            val authSignUpResult = AuthSignUpResult(
                false,
                AuthNextSignUpStep(
                    AuthSignUpStep.CONFIRM_SIGN_UP_STEP,
                    mapOf(),
                    AuthCodeDeliveryDetails(
                        deliveryDetails?.getValue("DESTINATION") ?: "",
                        AuthCodeDeliveryDetails.DeliveryMedium.fromString(
                            deliveryDetails?.getValue("MEDIUM")
                        ),
                        deliveryDetails?.getValue("ATTRIBUTE")
                    )
                ),
                AuthUser("", username)
            )
            onSuccess.accept(authSignUpResult)
            logger?.verbose("ResendSignUpCode Execution complete")
        } catch (exception: Exception) {
            onError.accept(CognitoAuthExceptionConverter.lookup(exception, "Resend sign up code failed."))
        }
    }
}
