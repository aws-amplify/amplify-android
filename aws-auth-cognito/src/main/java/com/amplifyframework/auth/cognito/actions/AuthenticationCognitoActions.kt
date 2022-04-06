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
import com.amplifyframework.statemachine.Action
import com.amplifyframework.statemachine.codegen.actions.AuthenticationActions
import com.amplifyframework.statemachine.codegen.data.SignInMethod
import com.amplifyframework.statemachine.codegen.data.SignedInData
import com.amplifyframework.statemachine.codegen.data.SignedOutData
import com.amplifyframework.statemachine.codegen.events.AuthEvent
import com.amplifyframework.statemachine.codegen.events.AuthenticationEvent
import com.amplifyframework.statemachine.codegen.events.SRPEvent
import com.amplifyframework.statemachine.codegen.events.SignOutEvent
import java.util.Date

object AuthenticationCognitoActions : AuthenticationActions {
    override fun configureAuthenticationAction(event: AuthenticationEvent.EventType.Configure) =
        Action { dispatcher, environment ->
            val userPoolTokens = event.storedCredentials?.cognitoUserPoolTokens
            val authenticationEvent = userPoolTokens?.let {
                val signedInData = SignedInData("", "", Date(), SignInMethod.SRP, it)
                AuthenticationEvent(AuthenticationEvent.EventType.InitializedSignedIn(signedInData))
            } ?: AuthenticationEvent(AuthenticationEvent.EventType.InitializedSignedOut(SignedOutData()))
            dispatcher.send(authenticationEvent)
            dispatcher.send(AuthEvent(AuthEvent.EventType.ConfiguredAuthentication(event.configuration)))
        }

    override fun initiateSRPSignInAction(event: AuthenticationEvent.EventType.SignInRequested) =
        Action { dispatcher, environment ->
            with(event) {
                val srpEvent = username?.run {
                    password?.run { SRPEvent(SRPEvent.EventType.InitiateSRP(username, password)) }
                } ?: AuthenticationEvent(
                    AuthenticationEvent.EventType.ThrowError(
                        AuthException("Sign in failed.", "username or password empty")
                    )
                )
                dispatcher.send(srpEvent)
            }
        }

    override fun initiateSignOutAction(
        event: AuthenticationEvent.EventType.SignOutRequested,
        signedInData: SignedInData
    ) = Action { dispatcher, environment ->
        if (event.isGlobalSignOut) {
            dispatcher.send(SignOutEvent(SignOutEvent.EventType.SignOutGlobally(signedInData)))
        } else {
            dispatcher.send(
                SignOutEvent(
                    SignOutEvent.EventType.SignOutLocally(
                        signedInData,
                        isGlobalSignOut = false,
                        invalidateTokens = false
                    )
                )
            )
        }
    }
}
