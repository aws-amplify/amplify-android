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

import aws.sdk.kotlin.services.cognitoidentityprovider.model.*
import com.amplifyframework.auth.cognito.AuthEnvironment
import com.amplifyframework.auth.cognito.data.SignedOutData
import com.amplifyframework.auth.cognito.events.AuthenticationEvent
import com.amplifyframework.auth.cognito.events.SignOutEvent
import com.amplifyframework.statemachine.Action
import com.amplifyframework.statemachine.codegen.actions.SignOutActions

object SignOutCognitoActions : SignOutActions {
    override fun localSignOutAction(event: SignOutEvent.EventType.SignOutLocally) =
        Action { dispatcher, environment ->
            dispatcher.send(SignOutEvent(SignOutEvent.EventType.SignedOutSuccess(event.signedInData)))
            dispatcher.send(
                AuthenticationEvent(
                    AuthenticationEvent.EventType.InitializedSignedOut(
                        SignedOutData(event.signedInData.username)
                    )
                )
            )
            //TODO: handle failure - SignedOutFailure
        }

    override fun globalSignOutAction(event: SignOutEvent.EventType.SignOutGlobally) =
        Action { dispatcher, environment ->
            val env = (environment as AuthEnvironment)
            runCatching {
                env.cognitoAuthService.cognitoIdentityProviderClient?.globalSignOut(
                    GlobalSignOutRequest {
                        this.accessToken = env.accessToken as String
                    }
                )
            }.onSuccess {
                dispatcher.send(
                    SignOutEvent(SignOutEvent.EventType.RevokeToken(event.signedInData))
                )
            }.onFailure {
                dispatcher.send(
                    SignOutEvent(
                        SignOutEvent.EventType.SignOutLocally(
                            event.signedInData,
                            isGlobalSignOut = false,
                            invalidateTokens = false
                        )
                    )
                )
            }
        }

    override fun revokeTokenAction(event: SignOutEvent.EventType.RevokeToken) =
        Action { dispatcher, environment ->
            val env = (environment as AuthEnvironment)
            env.configuration.runCatching {
                env.cognitoAuthService.cognitoIdentityProviderClient?.revokeToken(RevokeTokenRequest {
                    clientId = userPool?.appClient
                    clientSecret = userPool?.appClientSecret
                    token = event.signedInData.cognitoUserPoolTokens.refreshToken
                })
            }.also {
                dispatcher.send(
                    SignOutEvent(
                        SignOutEvent.EventType.SignOutLocally(
                            event.signedInData,
                            isGlobalSignOut = false,
                            invalidateTokens = false
                        )
                    )
                )
            }
        }
}