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

import com.amplifyframework.auth.cognito.AuthEnvironment
import com.amplifyframework.statemachine.Action
import com.amplifyframework.statemachine.codegen.actions.AuthorizationActions
import com.amplifyframework.statemachine.codegen.data.AmplifyCredential
import com.amplifyframework.statemachine.codegen.data.LoginsMapProvider
import com.amplifyframework.statemachine.codegen.data.SignedInData
import com.amplifyframework.statemachine.codegen.events.AuthEvent
import com.amplifyframework.statemachine.codegen.events.AuthorizationEvent
import com.amplifyframework.statemachine.codegen.events.FetchAuthSessionEvent
import com.amplifyframework.statemachine.codegen.events.RefreshSessionEvent

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

    override fun initializeFetchUnAuthSession() =
        Action<AuthEnvironment>("InitFetchAuthSession") { id, dispatcher ->
            logger?.verbose("$id Starting execution")
            val evt = configuration.identityPool?.let {
                FetchAuthSessionEvent(FetchAuthSessionEvent.EventType.FetchIdentity(LoginsMapProvider.UnAuthLogins()))
            } ?: AuthorizationEvent(AuthorizationEvent.EventType.ThrowError(Exception("Identity Pool not configured.")))
            logger?.verbose("$id Sending event ${evt.type}")
            dispatcher.send(evt)
        }

    override fun initializeFetchAuthSession(signedInData: SignedInData) =
        Action<AuthEnvironment>("InitFetchAuthSession") { id, dispatcher ->
            logger?.verbose("$id Starting execution")
            val evt = when {
                configuration.userPool == null -> AuthorizationEvent(
                    AuthorizationEvent.EventType.ThrowError(Exception("User Pool not configured."))
                )
                configuration.identityPool == null -> AuthorizationEvent(
                    AuthorizationEvent.EventType.ThrowError(Exception("Identity Pool not configured."))
                )
                else -> {
                    val logins = LoginsMapProvider.CognitoUserPoolLogins(
                        configuration.userPool.region,
                        configuration.userPool.poolId,
                        signedInData.cognitoUserPoolTokens.idToken!!
                    )
                    FetchAuthSessionEvent(FetchAuthSessionEvent.EventType.FetchIdentity(logins))
                }
            }
            logger?.verbose("$id Sending event ${evt.type}")
            dispatcher.send(evt)
        }

    override fun initiateRefreshSessionAction(amplifyCredential: AmplifyCredential) =
        Action<AuthEnvironment>("InitiateRefreshSession") { id, dispatcher ->
            logger?.verbose("$id Starting execution")
            val evt = when (amplifyCredential) {
                is AmplifyCredential.UserPoolData -> RefreshSessionEvent(
                    RefreshSessionEvent.EventType.RefreshUserPoolTokens(amplifyCredential.signedInData)
                )
                is AmplifyCredential.IdentityPool -> RefreshSessionEvent(
                    RefreshSessionEvent.EventType.RefreshUnAuthSession(LoginsMapProvider.UnAuthLogins())
                )
                else -> AuthorizationEvent(
                    AuthorizationEvent.EventType.ThrowError(Exception("Credentials empty, cannot refresh."))
                )
            }
            logger?.verbose("$id Sending event ${evt.type}")
            dispatcher.send(evt)
        }
}
