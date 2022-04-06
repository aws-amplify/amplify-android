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

import aws.sdk.kotlin.services.cognitoidentityprovider.model.AuthFlowType
import aws.smithy.kotlin.runtime.time.Instant
import com.amplifyframework.auth.cognito.AuthEnvironment
import com.amplifyframework.statemachine.Action
import com.amplifyframework.statemachine.codegen.actions.FetchUserPoolTokensActions
import com.amplifyframework.statemachine.codegen.data.AmplifyCredential
import com.amplifyframework.statemachine.codegen.data.CognitoUserPoolTokens
import com.amplifyframework.statemachine.codegen.events.FetchAuthSessionEvent
import com.amplifyframework.statemachine.codegen.events.FetchUserPoolTokensEvent
import java.lang.Exception
import kotlin.time.Duration.Companion.seconds

object FetchUserPoolTokensCognitoActions : FetchUserPoolTokensActions {
    override fun refreshFetchUserPoolTokensAction(amplifyCredential: AmplifyCredential?): Action =
        Action { dispatcher, environment ->
            val env = (environment as AuthEnvironment)
            try {
                val refreshTokenResponse =
                    env.cognitoAuthService.cognitoIdentityProviderClient?.initiateAuth {
                        authFlow = AuthFlowType.RefreshToken
                        clientId = env.configuration.userPool?.appClient
                        authParameters = amplifyCredential?.cognitoUserPoolTokens?.refreshToken?.let {
                            mapOf("REFRESH_TOKEN" to it)
                        }
                    }
                val expiresIn = refreshTokenResponse?.authenticationResult?.expiresIn?.toLong() ?: 0
                val cognitoUserPoolTokens = CognitoUserPoolTokens(
                    idToken = refreshTokenResponse?.authenticationResult?.idToken,
                    accessToken = refreshTokenResponse?.authenticationResult?.accessToken,
                    refreshToken = refreshTokenResponse?.authenticationResult?.refreshToken,
                    expiration = Instant.now().plus(expiresIn.seconds).epochSeconds
                )

                val updatedCredentials = amplifyCredential?.copy(cognitoUserPoolTokens = cognitoUserPoolTokens)

                dispatcher.send(FetchUserPoolTokensEvent(FetchUserPoolTokensEvent.EventType.Fetched()))
                dispatcher.send(
                    FetchAuthSessionEvent(FetchAuthSessionEvent.EventType.FetchIdentity(updatedCredentials))
                )
            } catch (e: Exception) {
                dispatcher.send(FetchUserPoolTokensEvent(FetchUserPoolTokensEvent.EventType.ThrowError(e)))
                dispatcher.send(
                    FetchAuthSessionEvent(FetchAuthSessionEvent.EventType.FetchIdentity(amplifyCredential))
                )
            }
        }
}
