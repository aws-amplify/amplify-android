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
import com.amplifyframework.auth.cognito.exceptions.configuration.InvalidOauthConfigurationException
import com.amplifyframework.auth.cognito.helpers.JWTParser
import com.amplifyframework.statemachine.Action
import com.amplifyframework.statemachine.codegen.actions.SignOutActions
import com.amplifyframework.statemachine.codegen.data.DeviceMetadata
import com.amplifyframework.statemachine.codegen.data.GlobalSignOutErrorData
import com.amplifyframework.statemachine.codegen.data.HostedUIErrorData
import com.amplifyframework.statemachine.codegen.data.RevokeTokenErrorData
import com.amplifyframework.statemachine.codegen.data.SignInMethod
import com.amplifyframework.statemachine.codegen.data.SignedOutData
import com.amplifyframework.statemachine.codegen.events.AuthenticationEvent
import com.amplifyframework.statemachine.codegen.events.SignOutEvent

internal object SignOutCognitoActions : SignOutActions {
    override fun hostedUISignOutAction(event: SignOutEvent.EventType.InvokeHostedUISignOut) =
        Action<AuthEnvironment>("HostedUISignOut") { id, dispatcher ->
            logger.verbose("$id Starting execution")
            try {
                if (hostedUIClient == null) throw InvalidOauthConfigurationException()
                hostedUIClient.launchWebViewSignOut()
            } catch (e: Exception) {
                logger.warn("Failed to sign out web ui.", e)
                val hostedUIErrorData = HostedUIErrorData(hostedUIClient?.createSignOutUri()?.toString(), e)
                val evt = if (event.signOutData.globalSignOut) {
                    SignOutEvent(SignOutEvent.EventType.SignOutGlobally(event.signedInData, hostedUIErrorData))
                } else {
                    SignOutEvent(SignOutEvent.EventType.RevokeToken(event.signedInData, hostedUIErrorData))
                }
                logger.verbose("$id Sending event ${evt.type}")
                dispatcher.send(evt)
            }
        }

    override fun localSignOutAction(event: SignOutEvent.EventType.SignOutLocally) =
        Action<AuthEnvironment>("LocalSignOut") { id, dispatcher ->
            logger.verbose("$id Starting execution")
            val evt = SignOutEvent(
                SignOutEvent.EventType.SignedOutSuccess(
                    SignedOutData(
                        lastKnownUsername = event.signedInData?.username,
                        hostedUIErrorData = event.hostedUIErrorData,
                        globalSignOutErrorData = event.globalSignOutErrorData,
                        revokeTokenErrorData = event.revokeTokenErrorData
                    )
                )
            )
            logger.verbose("$id Sending event ${evt.type}")
            dispatcher.send(evt)
        }

    override fun globalSignOutAction(event: SignOutEvent.EventType.SignOutGlobally) =
        Action<AuthEnvironment>("GlobalSignOut") { id, dispatcher ->
            logger.verbose("$id Starting execution")
            val accessToken = event.signedInData.cognitoUserPoolTokens.accessToken
            val evt = try {
                cognitoAuthService.cognitoIdentityProviderClient?.globalSignOut(
                    GlobalSignOutRequest { this.accessToken = accessToken }
                )
                SignOutEvent(
                    SignOutEvent.EventType.RevokeToken(event.signedInData, hostedUIErrorData = event.hostedUIErrorData)
                )
            } catch (e: Exception) {
                logger.warn("Failed to sign out globally.", e)
                val globalSignOutErrorData = GlobalSignOutErrorData(
                    accessToken = accessToken,
                    error = e
                )
                SignOutEvent(
                    SignOutEvent.EventType.SignOutGloballyError(
                        signedInData = event.signedInData,
                        hostedUIErrorData = event.hostedUIErrorData,
                        globalSignOutErrorData = globalSignOutErrorData
                    )
                )
            }
            logger.verbose("$id Sending event ${evt.type}")
            dispatcher.send(evt)
        }

    override fun revokeTokenAction(event: SignOutEvent.EventType.RevokeToken) =
        Action<AuthEnvironment>("RevokeTokens") { id, dispatcher ->
            logger.verbose("$id Starting execution")
            val accessToken = event.signedInData.cognitoUserPoolTokens.accessToken
            val refreshToken = event.signedInData.cognitoUserPoolTokens.refreshToken
            val evt = try {
                // Check for "origin_jti" claim in access token, else skip revoking
                if (accessToken?.let { JWTParser.hasClaim(it, "origin_jti") } == true) {
                    cognitoAuthService.cognitoIdentityProviderClient?.revokeToken(
                        RevokeTokenRequest {
                            clientId = configuration.userPool?.appClient
                            clientSecret = configuration.userPool?.appClientSecret
                            token = refreshToken
                        }
                    )
                    SignOutEvent(SignOutEvent.EventType.SignOutLocally(event.signedInData, event.hostedUIErrorData))
                } else {
                    logger.debug("Access Token does not contain `origin_jti` claim. Skip revoking tokens.")
                    val error = RevokeTokenErrorData(
                        refreshToken = refreshToken,
                        error = Exception("Access Token does not contain `origin_jti` claim. Skip revoking tokens.")
                    )

                    SignOutEvent(
                        SignOutEvent.EventType.SignOutLocally(
                            signedInData = event.signedInData,
                            hostedUIErrorData = event.hostedUIErrorData,
                            globalSignOutErrorData = event.globalSignOutErrorData,
                            revokeTokenErrorData = error
                        )
                    )
                }
            } catch (e: Exception) {
                logger.warn("Failed to revoke tokens.", e)
                val error = RevokeTokenErrorData(
                    refreshToken = refreshToken,
                    error = e
                )

                SignOutEvent(
                    SignOutEvent.EventType.SignOutLocally(
                        signedInData = event.signedInData,
                        hostedUIErrorData = event.hostedUIErrorData,
                        globalSignOutErrorData = event.globalSignOutErrorData,
                        revokeTokenErrorData = error
                    )
                )
            }
            logger.verbose("$id Sending event ${evt.type}")
            dispatcher.send(evt)
        }

    override fun buildRevokeTokenErrorAction(event: SignOutEvent.EventType.SignOutGloballyError) =
        Action<AuthEnvironment>("BuildRevokeTokenError") { id, dispatcher ->
            logger.verbose("$id Starting execution")

            val error = RevokeTokenErrorData(
                refreshToken = event.signedInData.cognitoUserPoolTokens.refreshToken,
                error = Exception("RevokeToken not attempted because GlobalSignOut failed.")
            )

            val evt = SignOutEvent(
                SignOutEvent.EventType.SignOutLocally(
                    signedInData = event.signedInData,
                    hostedUIErrorData = event.hostedUIErrorData,
                    globalSignOutErrorData = event.globalSignOutErrorData,
                    revokeTokenErrorData = error
                )
            )

            logger.verbose("$id Sending event ${evt.type}")
            dispatcher.send(evt)
        }

    override fun userCancelledAction(event: SignOutEvent.EventType.UserCancelled) =
        Action<AuthEnvironment>("UserCancelledSignOut") { id, dispatcher ->
            logger.verbose("$id Starting execution")
            val evt = AuthenticationEvent(
                AuthenticationEvent.EventType.CancelSignOut(event.signedInData, DeviceMetadata.Empty)
            )
            logger.verbose("$id Sending event ${evt.type}")
            dispatcher.send(evt)
        }
}
