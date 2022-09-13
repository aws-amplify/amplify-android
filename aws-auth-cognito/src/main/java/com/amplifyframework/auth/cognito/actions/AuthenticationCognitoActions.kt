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

import com.amplifyframework.auth.AuthException
import com.amplifyframework.auth.cognito.AuthEnvironment
import com.amplifyframework.auth.cognito.options.AuthFlowType
import com.amplifyframework.statemachine.Action
import com.amplifyframework.statemachine.codegen.actions.AuthenticationActions
import com.amplifyframework.statemachine.codegen.data.AmplifyCredential
import com.amplifyframework.statemachine.codegen.data.SignInMethod
import com.amplifyframework.statemachine.codegen.data.SignedInData
import com.amplifyframework.statemachine.codegen.data.SignedOutData
import com.amplifyframework.statemachine.codegen.events.AuthEvent
import com.amplifyframework.statemachine.codegen.events.AuthenticationEvent
import com.amplifyframework.statemachine.codegen.events.SignInEvent
import com.amplifyframework.statemachine.codegen.events.SignOutEvent
import java.util.Date

object AuthenticationCognitoActions : AuthenticationActions {
    override fun configureAuthenticationAction(event: AuthenticationEvent.EventType.Configure) =
        Action<AuthEnvironment>("ConfigureAuthN") { id, dispatcher ->
            logger.verbose("$id Starting execution")
            val evt = when (val credentials = event.storedCredentials) {
                is AmplifyCredential.UserPool -> {
                    val signedInData = SignedInData("", "", Date(), SignInMethod.SRP, credentials.tokens)
                    AuthenticationEvent(AuthenticationEvent.EventType.InitializedSignedIn(signedInData))
                }
                is AmplifyCredential.UserAndIdentityPool -> {
                    val signedInData = SignedInData("", "", Date(), SignInMethod.SRP, credentials.tokens)
                    AuthenticationEvent(AuthenticationEvent.EventType.InitializedSignedIn(signedInData))
                }
                else -> AuthenticationEvent(AuthenticationEvent.EventType.InitializedSignedOut(SignedOutData()))
            }
            logger.verbose("$id Sending event ${evt.type}")
            dispatcher.send(evt)

            val authEvent = AuthEvent(
                AuthEvent.EventType.ConfiguredAuthentication(event.configuration, event.storedCredentials)
            )
            logger.verbose("$id Sending event ${authEvent.type}")
            dispatcher.send(authEvent)
        }

    override fun initiateSignInAction(event: AuthenticationEvent.EventType.SignInRequested) =
        Action<AuthEnvironment>("InitiateSignInAction") { id, dispatcher ->
            logger.verbose("$id Starting execution")

            val signInType = event.signInType ?: AuthFlowType.USER_SRP_AUTH.toString()

            when (AuthFlowType.valueOf(signInType)) {
                AuthFlowType.USER_SRP_AUTH -> {
                    val evt = event.username?.run {
                        event.password?.run {
                            SignInEvent(
                                SignInEvent.EventType.InitiateSignInWithSRP(event.username, event.password)
                            )
                        }
                    } ?: AuthenticationEvent(
                        AuthenticationEvent.EventType.ThrowError(
                            AuthException(
                                "Sign in failed.",
                                "username or password empty"
                            )
                        )
                    )

                    logger.verbose("$id Sending event ${evt.type}")
                    dispatcher.send(evt)
                }
                AuthFlowType.CUSTOM_AUTH -> {
                    val evt = event.username?.run {
                        SignInEvent(
                            SignInEvent.EventType.InitiateSignInWithCustom(
                                event.username,
                                event.password,
                                event.options
                            )
                        )
                    } ?: AuthenticationEvent(
                        AuthenticationEvent.EventType.ThrowError(
                            AuthException(
                                "Sign in failed.",
                                "username can not be empty"
                            )
                        )
                    )

                    logger.verbose("$id Sending event ${evt.type}")
                    dispatcher.send(evt)
                }
                AuthFlowType.USER_PASSWORD_AUTH -> TODO()
            }
        }

    override fun initiateSignOutAction(
        event: AuthenticationEvent.EventType.SignOutRequested,
        signedInData: SignedInData?
    ) = Action<AuthEnvironment>("InitSignOut") { id, dispatcher ->
        logger.verbose("$id Starting execution")

        val evt = when {
            signedInData != null && event.isGlobalSignOut -> {
                SignOutEvent(SignOutEvent.EventType.SignOutGlobally(signedInData))
            }
            signedInData != null && !event.isGlobalSignOut -> {
                SignOutEvent(SignOutEvent.EventType.RevokeToken(signedInData))
            }
            else -> SignOutEvent(SignOutEvent.EventType.SignOutLocally(signedInData))
        }
        logger.verbose("$id Sending event ${evt.type}")
        dispatcher.send(evt)
    }
}
