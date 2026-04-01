/*
 * Copyright 2025 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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

package com.amplifyframework.auth.cognito.usecases

import android.content.Intent
import android.net.Uri
import com.amplifyframework.auth.cognito.AuthEnvironment
import com.amplifyframework.auth.cognito.AuthStateMachine
import com.amplifyframework.auth.cognito.exceptions.service.HostedUISignOutException
import com.amplifyframework.auth.cognito.exceptions.service.UserCancelledException
import com.amplifyframework.auth.cognito.helpers.authLogger
import com.amplifyframework.statemachine.codegen.data.HostedUIErrorData
import com.amplifyframework.statemachine.codegen.data.SignInMethod
import com.amplifyframework.statemachine.codegen.events.HostedUIEvent
import com.amplifyframework.statemachine.codegen.events.SignOutEvent
import com.amplifyframework.statemachine.codegen.states.AuthenticationState
import com.amplifyframework.statemachine.codegen.states.SignOutState

internal class WebUiSignInResponseUseCase(
    private val stateMachine: AuthStateMachine,
    private val authEnvironment: AuthEnvironment
) {
    private val logger = authLogger()

    suspend fun execute(intent: Intent?) {
        val authState = stateMachine.getCurrentState()
        val callbackUri = intent?.data
        val event = when (val authNState = authState.authNState) {
            is AuthenticationState.SigningOut -> {
                (authNState.signOutState as? SignOutState.SigningOutHostedUI)?.let { signOutState ->
                    getSignOutEvent(callbackUri, signOutState)
                }
            }
            is AuthenticationState.SigningIn -> {
                getHostedUiEvent(callbackUri)
            }
            else -> {
                logger.warn(
                    "Received handleWebUIResponse but ignoring because the user is not currently signing in " +
                        "or signing out"
                )
                null
            }
        }
        event?.let { stateMachine.send(it) }
    }

    private fun getSignOutEvent(callbackUri: Uri?, signOutState: SignOutState.SigningOutHostedUI) =
        if (callbackUri == null &&
            !signOutState.bypassCancel &&
            signOutState.signedInData.signInMethod !=
            SignInMethod.ApiBased(SignInMethod.ApiBased.AuthType.UNKNOWN)
        ) {
            SignOutEvent(SignOutEvent.EventType.UserCancelled(signOutState.signedInData))
        } else {
            val hostedUIErrorData = if (callbackUri == null) {
                HostedUIErrorData(
                    url = authEnvironment.hostedUIClient?.createSignOutUri()?.toString(),
                    error = HostedUISignOutException(authEnvironment.hostedUIClient != null)
                )
            } else {
                null
            }
            if (signOutState.globalSignOut) {
                SignOutEvent(
                    SignOutEvent.EventType.SignOutGlobally(
                        signOutState.signedInData,
                        hostedUIErrorData
                    )
                )
            } else {
                SignOutEvent(
                    SignOutEvent.EventType.RevokeToken(
                        signOutState.signedInData,
                        hostedUIErrorData
                    )
                )
            }
        }

    private fun getHostedUiEvent(callbackUri: Uri?) = if (callbackUri == null) {
        HostedUIEvent(
            HostedUIEvent.EventType.ThrowError(
                UserCancelledException(
                    "The user cancelled the sign-in attempt, so it did not complete.",
                    "To recover: catch this error, and show the sign-in screen again."
                )
            )
        )
    } else {
        HostedUIEvent(HostedUIEvent.EventType.FetchToken(callbackUri))
    }
}
