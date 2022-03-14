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
import com.amplifyframework.auth.cognito.data.AWSCredentials
import com.amplifyframework.auth.cognito.data.AmplifyCredential
import com.amplifyframework.auth.cognito.events.AuthorizationEvent
import com.amplifyframework.auth.cognito.events.FetchAuthSessionEvent
import com.amplifyframework.auth.cognito.events.FetchAwsCredentialsEvent
import com.amplifyframework.statemachine.Action
import com.amplifyframework.statemachine.codegen.actions.FetchAWSCredentialsActions

object FetchAwsCredentialsActions : FetchAWSCredentialsActions {
    override fun initFetchAWSCredentialsAction(amplifyCredential: AmplifyCredential?): Action =
        Action { dispatcher, environment ->
            val env = (environment as AuthEnvironment)
            val getCredentialsForIdentityRequest = GetCredentialsForIdentityRequest.invoke {
                this.identityId = amplifyCredential?.identityId
            }
            try {
                val getCredentialsForIdentityResponse =
                    env.cognitoAuthService.cognitoIdentityClient?.getCredentialsForIdentity(
                        getCredentialsForIdentityRequest
                    )
                val credentials = AWSCredentials(
                    accessKeyId = getCredentialsForIdentityResponse?.credentials?.accessKeyId,
                    secretAccessKey = getCredentialsForIdentityResponse?.credentials?.secretKey,
                    sessionToken = getCredentialsForIdentityResponse?.credentials?.sessionToken,
                    expiration = getCredentialsForIdentityResponse?.credentials?.expiration?.epochSeconds
                )
                val updatedAmplifyCredential = amplifyCredential?.copy(awsCredentials = credentials)

                val event =
                    FetchAwsCredentialsEvent(
                        FetchAwsCredentialsEvent.EventType.Fetched(updatedAmplifyCredential)
                    )
                dispatcher.send(event)
                dispatcher.send(
                    FetchAuthSessionEvent(FetchAuthSessionEvent.EventType.FetchedAuthSession(amplifyCredential))
                )
                dispatcher.send(AuthorizationEvent(AuthorizationEvent.EventType.FetchedAuthSession(amplifyCredential)))
            } catch (e: Exception) {
                val event =
                    FetchAwsCredentialsEvent(
                        FetchAwsCredentialsEvent.EventType.ThrowError(e)
                    )
                dispatcher.send(event)
            }
        }
}