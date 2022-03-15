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
import com.amplifyframework.auth.cognito.data.AmplifyCredential
import com.amplifyframework.auth.cognito.data.CognitoUserPoolTokens
import com.amplifyframework.auth.cognito.events.FetchAuthSessionEvent
import com.amplifyframework.auth.cognito.events.FetchUserPoolTokensEvent
import com.amplifyframework.statemachine.Action
import com.amplifyframework.statemachine.codegen.actions.FetchUserPoolTokensActions
import java.lang.Exception
import kotlin.time.Duration.Companion.seconds

object FetchUserPoolTokensActions : FetchUserPoolTokensActions {
    override fun refreshFetchUserPoolTokensAction(amplifyCredential: AmplifyCredential?): Action =
        Action { dispatcher, environment ->
            val env = (environment as AuthEnvironment)
            try {
                val refreshTokenResponse =
                    env.cognitoAuthService.cognitoIdentityProviderClient?.initiateAuth {
                        authFlow = AuthFlowType.RefreshToken
                        clientId = env.configuration.userPool?.appClient
                        authParameters = mapOf(
                            "REFRESH_TOKEN" to amplifyCredential?.cognitoUserPoolTokens?.refreshToken as String
                        )
                    }
                val expiresIn = refreshTokenResponse?.authenticationResult?.expiresIn?.toLong() ?: 0
                val cognitoUserPoolTokens = CognitoUserPoolTokens(
                    idToken = refreshTokenResponse?.authenticationResult?.idToken,
                    accessToken = refreshTokenResponse?.authenticationResult?.accessToken,
                    refreshToken = refreshTokenResponse?.authenticationResult?.refreshToken,
                    tokenExpiration = Instant.now().plus(expiresIn.seconds).epochSeconds
                )

                val updatedCredentials = amplifyCredential?.copy(cognitoUserPoolTokens = cognitoUserPoolTokens)

                val event =
                    FetchUserPoolTokensEvent(
                        FetchUserPoolTokensEvent.EventType.Fetched(updatedCredentials)
                    )
                dispatcher.send(event)
                dispatcher.send(FetchAuthSessionEvent(FetchAuthSessionEvent.EventType.FetchIdentity(amplifyCredential)))
            } catch (e: Exception) {
                val event =
                    FetchUserPoolTokensEvent(
                        FetchUserPoolTokensEvent.EventType.ThrowError(e.localizedMessage)
                    )
                dispatcher.send(event)
            }

        }
}