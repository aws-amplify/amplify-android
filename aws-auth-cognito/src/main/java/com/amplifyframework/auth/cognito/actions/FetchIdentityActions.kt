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

import aws.sdk.kotlin.services.cognitoidentity.model.GetIdRequest
import com.amplifyframework.auth.cognito.AuthEnvironment
import com.amplifyframework.auth.cognito.data.AmplifyCredential
import com.amplifyframework.auth.cognito.events.FetchAuthSessionEvent
import com.amplifyframework.auth.cognito.events.FetchAwsCredentialsEvent
import com.amplifyframework.auth.cognito.events.FetchIdentityEvent
import com.amplifyframework.statemachine.Action
import com.amplifyframework.statemachine.codegen.actions.FetchIdentityActions

object FetchIdentityActions : FetchIdentityActions {
    override fun initFetchIdentityAction(amplifyCredential: AmplifyCredential?): Action =
        Action { dispatcher, environment ->
            val env = (environment as AuthEnvironment)
            val idToken = amplifyCredential?.cognitoUserPoolTokens?.idToken ?: ""
            val loginsMap: Map<String, String>? =
                env.configuration.userPool?.identityProviderName?.let { mapOf(it to idToken) }

            val getIdRequest = GetIdRequest.invoke {
                identityPoolId = env.configuration.identityPool?.poolId
                logins = loginsMap
            }
            try {
                val getIDResponse =
                    env.cognitoAuthService.cognitoIdentityClient?.getId(getIdRequest)

                val updatedAmplifyCredential =
                    amplifyCredential?.copy(identityId = getIDResponse?.identityId)
                val event =
                    FetchIdentityEvent(
                        FetchIdentityEvent.EventType.Fetched(updatedAmplifyCredential)
                    )
                dispatcher.send(event)
                dispatcher.send(
                    FetchAuthSessionEvent(
                        FetchAuthSessionEvent.EventType.FetchAwsCredentials(updatedAmplifyCredential)
                    )
                )
            } catch (e: Exception) {
                val event =
                    FetchIdentityEvent(
                        FetchIdentityEvent.EventType.ThrowError(e)
                    )
                dispatcher.send(event)
            }
        }
}