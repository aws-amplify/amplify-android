/*
 * Copyright 2024 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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

import android.app.Activity
import aws.sdk.kotlin.services.cognitoidentityprovider.CognitoIdentityProviderClient
import aws.sdk.kotlin.services.cognitoidentityprovider.completeWebAuthnRegistration
import aws.sdk.kotlin.services.cognitoidentityprovider.startWebAuthnRegistration
import com.amplifyframework.auth.cognito.AuthStateMachine
import com.amplifyframework.auth.cognito.helpers.WebAuthnHelper
import com.amplifyframework.auth.cognito.helpers.authLogger
import com.amplifyframework.auth.cognito.requireAuthenticationState
import com.amplifyframework.auth.options.AuthAssociateWebAuthnCredentialsOptions
import com.amplifyframework.statemachine.codegen.states.AuthenticationState.SignedIn
import com.amplifyframework.statemachine.util.mask
import com.amplifyframework.util.JsonDocument
import com.amplifyframework.util.toJsonString

internal class AssociateWebAuthnCredentialUseCase(
    private val client: CognitoIdentityProviderClient,
    private val fetchAuthSession: FetchAuthSessionUseCase,
    private val stateMachine: AuthStateMachine,
    private val webAuthnHelper: WebAuthnHelper
) {
    private val logger = authLogger()

    @Suppress("UNUSED_PARAMETER")
    suspend fun execute(callingActivity: Activity, options: AuthAssociateWebAuthnCredentialsOptions) {
        // User must be signed in to call this API
        stateMachine.requireAuthenticationState<SignedIn>()

        val accessToken = fetchAuthSession.execute().accessToken

        // Step 1: Get the credential request JSON from Cognito
        val requestJson = getCredentialRequestJson(accessToken)
        logger.debug("Received credential request: ${requestJson.mask()}")

        // Step 2: Create the credential with Android and get the response JSON
        val responseJson = webAuthnHelper.createCredential(requestJson, callingActivity)
        logger.debug("Sending credential response: ${responseJson.mask()}")

        // Step 3: Send the response JSON back to Cognito to complete the registration
        associateCredential(responseJson, accessToken)
    }

    private suspend fun getCredentialRequestJson(accessToken: String?): String {
        val response = client.startWebAuthnRegistration {
            this.accessToken = accessToken
        }
        return response.credentialCreationOptions!!.toJsonString()
    }

    private suspend fun associateCredential(credentialResponseJson: String, accessToken: String?) {
        client.completeWebAuthnRegistration {
            this.credential = JsonDocument(credentialResponseJson)
            this.accessToken = accessToken
        }
    }
}
