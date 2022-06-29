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
    override fun initFetchAWSCredentialsAction(amplifyCredential: AmplifyCredential): Action =
        Action<AuthEnvironment>("InitFetchAWSCredentials") { id, dispatcher ->
            logger?.verbose("$id Starting execution")
            val evt = try {
                val idToken = when (amplifyCredential) {
                    is AmplifyCredential.UserPool -> amplifyCredential.tokens.idToken
                    is AmplifyCredential.UserAndIdentityPool -> amplifyCredential.tokens.idToken
                    else -> null
                }
                val identityId = when (amplifyCredential) {
                    is AmplifyCredential.IdentityPool -> amplifyCredential.identityId
                    is AmplifyCredential.UserAndIdentityPool -> amplifyCredential.identityId
                    else -> null
                }

                val loginsMap: Map<String, String>? = configuration.userPool?.identityProviderName?.let { provider ->
                    idToken?.let { mapOf(provider to idToken) }
                }

                val getCredentialsForIdentityRequest = GetCredentialsForIdentityRequest {
                    this.identityId = identityId
                    loginsMap?.let { logins = loginsMap }
                }

                val getCredentialsForIdentityResponse =
                    cognitoAuthService.cognitoIdentityClient?.getCredentialsForIdentity(
                        getCredentialsForIdentityRequest
                    )

                val credentials = getCredentialsForIdentityResponse?.credentials?.let {
                    AWSCredentials(it.accessKeyId, it.secretKey, it.sessionToken, it.expiration?.epochSeconds)
                }

                val updatedAmplifyCredential = amplifyCredential.update(awsCredentials = credentials)

                val fetchedEvent = FetchAwsCredentialsEvent(FetchAwsCredentialsEvent.EventType.Fetched())
                logger?.verbose("$id Sending event ${fetchedEvent.type}")
                dispatcher.send(fetchedEvent)

                FetchAuthSessionEvent(FetchAuthSessionEvent.EventType.FetchedAuthSession(updatedAmplifyCredential))
            } catch (e: Exception) {
                val errorEvent = FetchAwsCredentialsEvent(FetchAwsCredentialsEvent.EventType.ThrowError(e))
                logger?.verbose("$id Sending event ${errorEvent.type}")
                dispatcher.send(errorEvent)

                FetchAuthSessionEvent(FetchAuthSessionEvent.EventType.FetchedAuthSession(amplifyCredential))
            }
            logger?.verbose("$id Sending event ${evt.type}")
            dispatcher.send(evt)
        }
}
