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
import aws.sdk.kotlin.services.cognitoidentity.model.GetIdRequest
import com.amplifyframework.auth.cognito.AuthEnvironment
import com.amplifyframework.statemachine.Action
import com.amplifyframework.statemachine.codegen.actions.FetchAuthSessionActions
import com.amplifyframework.statemachine.codegen.data.AWSCredentials
import com.amplifyframework.statemachine.codegen.data.AmplifyCredential
import com.amplifyframework.statemachine.codegen.data.LoginsMapProvider
import com.amplifyframework.statemachine.codegen.events.AuthorizationEvent
import com.amplifyframework.statemachine.codegen.events.FetchAuthSessionEvent

object FetchAuthSessionCognitoActions : FetchAuthSessionActions {
    override fun fetchIdentityAction(loginsMap: LoginsMapProvider): Action =
        Action<AuthEnvironment>("FetchIdentity") { id, dispatcher ->
            logger?.verbose("$id Starting execution")
            val evt = try {
                val request = GetIdRequest {
                    identityPoolId = configuration.identityPool?.poolId
                    this.logins = loginsMap.logins
                }

                val response = cognitoAuthService.cognitoIdentityClient?.getId(request)

                response?.identityId?.let {
                    FetchAuthSessionEvent(FetchAuthSessionEvent.EventType.FetchAwsCredentials(it, loginsMap))
                } ?: throw Exception("Fetching identity id failed.")
            } catch (e: Exception) {
                AuthorizationEvent(AuthorizationEvent.EventType.ThrowError(e))
            }
            logger?.verbose("$id Sending event ${evt.type}")
            dispatcher.send(evt)
        }

    override fun fetchAWSCredentialsAction(identityId: String, loginsMap: LoginsMapProvider): Action =
        Action<AuthEnvironment>("FetchAWSCredentials") { id, dispatcher ->
            logger?.verbose("$id Starting execution")
            val evt = try {
                val request = GetCredentialsForIdentityRequest {
                    this.identityId = identityId
                    this.logins = loginsMap.logins
                }

                val response = cognitoAuthService.cognitoIdentityClient?.getCredentialsForIdentity(request)

                response?.credentials?.let {
                    val credentials = AWSCredentials(
                        it.accessKeyId,
                        it.secretKey,
                        it.sessionToken,
                        it.expiration?.epochSeconds
                    )
                    FetchAuthSessionEvent(FetchAuthSessionEvent.EventType.Fetched(identityId, credentials))
                } ?: throw Exception("Fetching AWS credentials failed.")
            } catch (e: Exception) {
                AuthorizationEvent(AuthorizationEvent.EventType.ThrowError(e))
            }
            logger?.verbose("$id Sending event ${evt.type}")
            dispatcher.send(evt)
        }

    override fun notifySessionEstablishedAction(identityId: String, awsCredentials: AWSCredentials): Action =
        Action<AuthEnvironment>("NotifySessionEstablished") { id, dispatcher ->
            logger?.verbose("$id Starting execution")
            val evt = AuthorizationEvent(AuthorizationEvent.EventType.Fetched(identityId, awsCredentials))
            logger?.verbose("$id Sending event ${evt.type}")
            dispatcher.send(evt)
        }
}
