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

import aws.sdk.kotlin.services.cognitoidentityprovider.model.GlobalSignOutRequest
import aws.sdk.kotlin.services.cognitoidentityprovider.model.RevokeTokenRequest
import com.amplifyframework.auth.cognito.AuthEnvironment
import com.amplifyframework.auth.cognito.helpers.JWTParser
import com.amplifyframework.statemachine.Action
import com.amplifyframework.statemachine.codegen.actions.SignOutActions
import com.amplifyframework.statemachine.codegen.data.SignedOutData
import com.amplifyframework.statemachine.codegen.events.SignOutEvent

object SignOutCognitoActions : SignOutActions {
    override fun hostedUISignOutAction(event: SignOutEvent.EventType.InvokeHostedUISignOut) =
        Action<AuthEnvironment>("HostedUISignOut") { id, dispatcher ->
            logger?.verbose("$id Starting execution")
            try {
                if (hostedUIClient == null) throw Exception() // TODO: More detailed exception
                hostedUIClient.launchCustomTabsSignOut(event.signOutData.browserPackage)
            } catch (e: Exception) {

            }
        }


    override fun localSignOutAction(event: SignOutEvent.EventType.SignOutLocally) =
        Action<AuthEnvironment>("LocalSignOut") { id, dispatcher ->
            logger?.verbose("$id Starting execution")
            val evt = SignOutEvent(SignOutEvent.EventType.SignedOutSuccess(SignedOutData(event.signedInData.username)))
            logger?.verbose("$id Sending event ${evt.type}")
            dispatcher.send(evt)
        }

    override fun globalSignOutAction(event: SignOutEvent.EventType.SignOutGlobally) =
        Action<AuthEnvironment>("GlobalSignOut") { id, dispatcher ->
            logger?.verbose("$id Starting execution")
            try {
                cognitoAuthService.cognitoIdentityProviderClient?.globalSignOut(
                    GlobalSignOutRequest { this.accessToken = event.signedInData.cognitoUserPoolTokens.accessToken }
                )
            } catch (e: Exception) {
                logger?.warn("Failed to sign out globally.", e)
            }
            val evt = SignOutEvent(SignOutEvent.EventType.RevokeToken(event.signedInData))
            logger?.verbose("$id Sending event ${evt.type}")
            dispatcher.send(evt)
        }

    override fun revokeTokenAction(event: SignOutEvent.EventType.RevokeToken) =
        Action<AuthEnvironment>("RevokeTokens") { id, dispatcher ->
            logger?.verbose("$id Starting execution")
            try {
                // Check for "origin_jti" claim in access token, else skip revoking
                val accessToken = event.signedInData.cognitoUserPoolTokens.accessToken
                if (accessToken?.let { JWTParser.hasClaim(it, "origin_jti") } == true) {
                    cognitoAuthService.cognitoIdentityProviderClient?.revokeToken(
                        RevokeTokenRequest {
                            clientId = configuration.userPool?.appClient
                            clientSecret = configuration.userPool?.appClientSecret
                            token = event.signedInData.cognitoUserPoolTokens.refreshToken
                        }
                    )
                } else {
                    logger?.debug("Access Token does not contain `origin_jti` claim. Skip revoking tokens.")
                }
            } catch (e: Exception) {
                logger?.warn("Failed to revoke tokens.", e)
            }
            val evt = SignOutEvent(SignOutEvent.EventType.SignOutLocally(event.signedInData))
            logger?.verbose("$id Sending event ${evt.type}")
            dispatcher.send(evt)
        }
}
