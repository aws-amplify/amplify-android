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

import aws.sdk.kotlin.services.cognitoidentityprovider.CognitoIdentityProviderClient
import aws.sdk.kotlin.services.cognitoidentityprovider.deleteWebAuthnCredential
import com.amplifyframework.auth.cognito.AuthStateMachine
import com.amplifyframework.auth.cognito.requireAuthenticationState
import com.amplifyframework.auth.options.AuthDeleteWebAuthnCredentialOptions
import com.amplifyframework.statemachine.codegen.states.AuthenticationState.SignedIn

internal class DeleteWebAuthnCredentialUseCase(
    private val client: CognitoIdentityProviderClient,
    private val fetchAuthSession: FetchAuthSessionUseCase,
    private val stateMachine: AuthStateMachine
) {
    @Suppress("UNUSED_PARAMETER")
    suspend fun execute(credentialId: String, options: AuthDeleteWebAuthnCredentialOptions) {
        // User must be signed in to call this API
        stateMachine.requireAuthenticationState<SignedIn>()

        val accessToken = fetchAuthSession.execute().accessToken
        client.deleteWebAuthnCredential {
            this.accessToken = accessToken
            this.credentialId = credentialId
        }
    }
}
