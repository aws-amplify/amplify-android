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

package com.amplifyframework.auth.cognito.actions

import aws.sdk.kotlin.services.cognitoidentity.model.GetCredentialsForIdentityRequest
import com.amplifyframework.auth.cognito.AuthEnvironment
import com.amplifyframework.statemachine.Action
import com.amplifyframework.statemachine.codegen.actions.FetchAWSCredentialsActions
import com.amplifyframework.statemachine.codegen.data.AWSCredentials
import com.amplifyframework.statemachine.codegen.data.AmplifyCredential
import com.amplifyframework.statemachine.codegen.events.FetchAuthSessionEvent
import com.amplifyframework.statemachine.codegen.events.FetchAwsCredentialsEvent

object FetchAwsCredentialsCognitoActions : FetchAWSCredentialsActions {
    override fun initFetchAWSCredentialsAction(amplifyCredential: AmplifyCredential?): Action =
        Action { dispatcher, environment ->
            val env = (environment as AuthEnvironment)
            val idToken = amplifyCredential?.cognitoUserPoolTokens?.idToken
            val loginsMap: Map<String, String>? = env.configuration.userPool?.identityProviderName?.let { provider ->
                idToken?.let { mapOf(provider to idToken) }
            }

            val getCredentialsForIdentityRequest = GetCredentialsForIdentityRequest {
                identityId = amplifyCredential?.identityId
                loginsMap?.let { logins = loginsMap }
            }

            try {
                val getCredentialsForIdentityResponse =
                    env.cognitoAuthService.cognitoIdentityClient?.getCredentialsForIdentity(
                        getCredentialsForIdentityRequest
                    )
                val credentials = getCredentialsForIdentityResponse?.credentials?.let {
                    AWSCredentials(it.accessKeyId, it.secretKey, it.sessionToken, it.expiration?.epochSeconds)
                }
                val updatedAmplifyCredential = amplifyCredential?.copy(awsCredentials = credentials)

                dispatcher.send(FetchAwsCredentialsEvent(FetchAwsCredentialsEvent.EventType.Fetched()))
                dispatcher.send(
                    FetchAuthSessionEvent(FetchAuthSessionEvent.EventType.FetchedAuthSession(updatedAmplifyCredential))
                )
            } catch (e: Exception) {
                dispatcher.send(FetchAwsCredentialsEvent(FetchAwsCredentialsEvent.EventType.ThrowError(e)))
                dispatcher.send(
                    FetchAuthSessionEvent(FetchAuthSessionEvent.EventType.FetchedAuthSession(amplifyCredential))
                )
            }
        }
}
