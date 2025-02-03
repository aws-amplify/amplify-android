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
import aws.sdk.kotlin.services.cognitoidentityprovider.listWebAuthnCredentials
import aws.smithy.kotlin.runtime.time.toJvmInstant
import com.amplifyframework.auth.cognito.AuthStateMachine
import com.amplifyframework.auth.cognito.options.AWSCognitoAuthListWebAuthnCredentialsOptions.Companion.maxResults
import com.amplifyframework.auth.cognito.options.AWSCognitoAuthListWebAuthnCredentialsOptions.Companion.nextToken
import com.amplifyframework.auth.cognito.requireSignedInState
import com.amplifyframework.auth.cognito.result.AWSCognitoAuthListWebAuthnCredentialsResult
import com.amplifyframework.auth.cognito.result.CognitoWebAuthnCredential
import com.amplifyframework.auth.options.AuthListWebAuthnCredentialsOptions

internal class ListWebAuthnCredentialsUseCase(
    private val client: CognitoIdentityProviderClient,
    private val fetchAuthSession: FetchAuthSessionUseCase,
    private val stateMachine: AuthStateMachine
) {
    suspend fun execute(options: AuthListWebAuthnCredentialsOptions): AWSCognitoAuthListWebAuthnCredentialsResult {
        // User must be SignedIn to call this API
        stateMachine.requireSignedInState()

        val token = fetchAuthSession.execute().accessToken

        val response = client.listWebAuthnCredentials {
            accessToken = token
            nextToken = options.nextToken
            maxResults = options.maxResults
        }

        val credentials = response.credentials.map { credential ->
            CognitoWebAuthnCredential(
                credentialId = credential.credentialId,
                friendlyName = credential.friendlyCredentialName,
                relyingPartyId = credential.relyingPartyId,
                createdAt = credential.createdAt.toJvmInstant()
            )
        }

        return AWSCognitoAuthListWebAuthnCredentialsResult(
            credentials = credentials,
            nextToken = response.nextToken
        )
    }
}
