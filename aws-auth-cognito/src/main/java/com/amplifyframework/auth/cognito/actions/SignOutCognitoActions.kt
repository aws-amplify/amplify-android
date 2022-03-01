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
import com.amplifyframework.statemachine.Environment
import com.amplifyframework.statemachine.EventDispatcher
import com.amplifyframework.statemachine.codegen.actions.SignOutActions

object SignOutCognitoActions : SignOutActions {
    override fun localSignOutAction(event: SignOutEvent.EventType.SignOutLocally) =
        object : Action {
            override suspend fun execute(dispatcher: EventDispatcher, environment: Environment) {
                dispatcher.send(SignOutEvent(SignOutEvent.EventType.SignedOutSuccess(event.signedInData)))
                dispatcher.send(AuthenticationEvent(AuthenticationEvent.EventType.InitializedSignedOut(
                    SignedOutData(event.signedInData.username)
                )))
                //TODO: handle failure
            }
        }

    override fun globalSignOutAction(event: SignOutEvent.EventType.SignOutGlobally) =
        object : Action {
            override suspend fun execute(dispatcher: EventDispatcher, environment: Environment) {
                val env = (environment as AuthEnvironment)
                env.cognitoIdentityProviderClient.globalSignOut(
                    GlobalSignOutRequest {
                        this.accessToken = env.accessToken as String
                    }
                )
                dispatcher.send(
                    SignOutEvent(SignOutEvent.EventType.RevokeToken(event.signedInData))
                )
                //TODO: handle failure
            }
        }

    override fun revokeTokenAction(event: SignOutEvent.EventType.RevokeToken) = object : Action {
        override suspend fun execute(dispatcher: EventDispatcher, environment: Environment) {
            val env = (environment as AuthEnvironment)
            env.configuration.run {
                env.cognitoIdentityProviderClient.revokeToken(RevokeTokenRequest {
                    clientId = userPool?.appClient
                    clientSecret = userPool?.appClientSecret
                    token = event.signedInData.cognitoUserPoolTokens.refreshToken
                })
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
}