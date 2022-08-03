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
import com.amplifyframework.auth.cognito.AWSCognitoAuthServiceBehavior
import com.amplifyframework.auth.cognito.AuthEnvironment
import com.amplifyframework.statemachine.Action
import com.amplifyframework.statemachine.codegen.actions.AuthorizationActions
import com.amplifyframework.statemachine.codegen.data.AmplifyCredential
import com.amplifyframework.statemachine.codegen.data.AuthConfiguration
import com.amplifyframework.statemachine.codegen.data.CognitoUserPoolTokens
import com.amplifyframework.statemachine.codegen.events.AuthEvent
import com.amplifyframework.statemachine.codegen.events.AuthorizationEvent
import com.amplifyframework.statemachine.codegen.events.FetchAuthSessionEvent
import kotlin.time.Duration.Companion.seconds

object AuthorizationCognitoActions : AuthorizationActions {
    override fun resetAuthorizationAction() = Action<AuthEnvironment>("resetAuthZ") { id, dispatcher ->
        logger?.verbose("$id Starting execution")
        // TODO: recover from error
//        val evt = AuthorizationEvent(AuthorizationEvent.EventType.Configure(configuration))
//        logger?.verbose("$id Sending event ${evt.type}")
//        dispatcher.send(evt)
    }

    override fun configureAuthorizationAction() = Action<AuthEnvironment>("ConfigureAuthZ") { id, dispatcher ->
        logger?.verbose("$id Starting execution")
        val evt = AuthEvent(AuthEvent.EventType.ConfiguredAuthorization)
        logger?.verbose("$id Sending event ${evt.type}")
        dispatcher.send(evt)
    }

    override fun initializeFetchAuthSession(amplifyCredential: AmplifyCredential) =
        Action<AuthEnvironment>("InitFetchAuthSession") { id, dispatcher ->
            logger?.verbose("$id Starting execution")
            val evt = FetchAuthSessionEvent(FetchAuthSessionEvent.EventType.FetchIdentity(amplifyCredential))
            logger?.verbose("$id Sending event ${evt.type}")
            dispatcher.send(evt)
        }

    override fun refreshAuthSessionAction(amplifyCredential: AmplifyCredential) =
        Action<AuthEnvironment>("RefreshUserPoolTokens") { id, dispatcher ->
            logger?.verbose("$id Starting execution")
            val evt = try {
                when (amplifyCredential) {
                    is AmplifyCredential.UserPool -> {
                        val updatedCredential = refreshUserPoolTokens(
                            configuration,
                            amplifyCredential,
                            cognitoAuthService
                        )
                        if (configuration.identityPool != null) {
                            FetchAuthSessionEvent(FetchAuthSessionEvent.EventType.FetchIdentity(amplifyCredential))
                        } else {
                            AuthorizationEvent(AuthorizationEvent.EventType.Fetched(updatedCredential))
                        }
                    }
                    is AmplifyCredential.UserAndIdentityPool -> {
                        val updatedCredential = refreshUserPoolTokens(
                            configuration,
                            amplifyCredential,
                            cognitoAuthService
                        )
                        FetchAuthSessionEvent(FetchAuthSessionEvent.EventType.FetchIdentity(updatedCredential))
                    }
                    is AmplifyCredential.IdentityPool -> {
                        FetchAuthSessionEvent(FetchAuthSessionEvent.EventType.FetchAwsCredentials(amplifyCredential))
                    }
                    else -> throw Exception("Credentials empty, cannot refresh.")
                }
            } catch (e: Exception) {
                AuthorizationEvent(AuthorizationEvent.EventType.ThrowError(e))
            }
            logger?.verbose("$id Sending event ${evt.type}")
            dispatcher.send(evt)
        }

    private suspend fun refreshUserPoolTokens(
        configuration: AuthConfiguration,
        amplifyCredential: AmplifyCredential,
        cognitoAuthService: AWSCognitoAuthServiceBehavior
    ): AmplifyCredential {
        val authParameters = when (amplifyCredential) {
            is AmplifyCredential.UserPool -> amplifyCredential.tokens.refreshToken?.let {
                mapOf("REFRESH_TOKEN" to it)
            }
            is AmplifyCredential.UserAndIdentityPool -> amplifyCredential.tokens.refreshToken?.let {
                mapOf("REFRESH_TOKEN" to it)
            }
            else -> null
        }
        val refreshTokenResponse = cognitoAuthService.cognitoIdentityProviderClient?.initiateAuth {
            authFlow = AuthFlowType.RefreshToken
            clientId = configuration.userPool?.appClient
            this.authParameters = authParameters
        }

        val expiresIn = refreshTokenResponse?.authenticationResult?.expiresIn?.toLong() ?: 0
        val cognitoUserPoolTokens = CognitoUserPoolTokens(
            idToken = refreshTokenResponse?.authenticationResult?.idToken,
            accessToken = refreshTokenResponse?.authenticationResult?.accessToken,
            refreshToken = refreshTokenResponse?.authenticationResult?.refreshToken,
            expiration = Instant.now().plus(expiresIn.seconds).epochSeconds
        )

        return AmplifyCredential.UserPool(cognitoUserPoolTokens)
    }
}
