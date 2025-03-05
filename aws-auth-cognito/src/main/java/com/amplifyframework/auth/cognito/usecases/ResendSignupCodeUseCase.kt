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
import aws.sdk.kotlin.services.cognitoidentityprovider.model.AnalyticsMetadataType
import aws.sdk.kotlin.services.cognitoidentityprovider.resendConfirmationCode
import com.amplifyframework.auth.AuthCodeDeliveryDetails
import com.amplifyframework.auth.cognito.AuthEnvironment
import com.amplifyframework.auth.cognito.AuthStateMachine
import com.amplifyframework.auth.cognito.exceptions.configuration.InvalidUserPoolConfigurationException
import com.amplifyframework.auth.cognito.helpers.AuthHelper
import com.amplifyframework.auth.cognito.options.AWSCognitoAuthResendSignUpCodeOptions
import com.amplifyframework.auth.cognito.util.toAuthCodeDeliveryDetails
import com.amplifyframework.auth.exceptions.InvalidStateException
import com.amplifyframework.auth.options.AuthResendSignUpCodeOptions
import com.amplifyframework.statemachine.codegen.states.AuthenticationState

internal class ResendSignupCodeUseCase(
    val client: CognitoIdentityProviderClient,
    val environment: AuthEnvironment,
    val stateMachine: AuthStateMachine
) {
    suspend fun execute(
        username: String,
        options: AuthResendSignUpCodeOptions = AuthResendSignUpCodeOptions.defaults()
    ): AuthCodeDeliveryDetails {
        stateMachine.requireSignedInOrSignedOutState()

        val metadata = (options as? AWSCognitoAuthResendSignUpCodeOptions)?.metadata

        val encodedContextData = environment.getUserContextData(username)
        val pinpointEndpointId = environment.getPinpointEndpointId()

        val configuration = environment.configuration

        val response = client.resendConfirmationCode {
            clientId = configuration.userPool?.appClient
            this.username = username
            secretHash = AuthHelper.getSecretHash(
                username,
                configuration.userPool?.appClient,
                configuration.userPool?.appClientSecret
            )
            clientMetadata = metadata
            pinpointEndpointId?.let {
                this.analyticsMetadata = AnalyticsMetadataType.invoke { analyticsEndpointId = it }
            }
            encodedContextData?.let { this.userContextData { encodedData = it } }
        }

        return response.codeDeliveryDetails.toAuthCodeDeliveryDetails()
    }
}

private suspend fun AuthStateMachine.requireSignedInOrSignedOutState(): AuthenticationState {
    when (val state = getCurrentState().authNState) {
        is AuthenticationState.SignedIn, is AuthenticationState.SignedOut -> return state
        is AuthenticationState.NotConfigured -> throw InvalidUserPoolConfigurationException()
        else -> throw InvalidStateException()
    }
}
