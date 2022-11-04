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

package com.amplifyframework.auth.cognito.operations

import com.amplifyframework.auth.AuthChannelEventName
import com.amplifyframework.auth.cognito.AuthStateMachine
import com.amplifyframework.auth.cognito.CognitoAuthExceptionConverter
import com.amplifyframework.auth.cognito.options.AWSCognitoAuthSignOutOptions
import com.amplifyframework.auth.cognito.result.AWSCognitoAuthSignOutResult
import com.amplifyframework.auth.cognito.result.GlobalSignOutError
import com.amplifyframework.auth.cognito.result.HostedUIError
import com.amplifyframework.auth.cognito.result.RevokeTokenError
import com.amplifyframework.auth.exceptions.InvalidStateException
import com.amplifyframework.auth.options.AuthSignOutOptions
import com.amplifyframework.auth.result.AuthSignOutResult
import com.amplifyframework.statemachine.codegen.data.AuthConfiguration
import com.amplifyframework.statemachine.codegen.data.SignOutData
import com.amplifyframework.statemachine.codegen.events.AuthenticationEvent
import com.amplifyframework.statemachine.codegen.states.AuthenticationState
import com.amplifyframework.statemachine.codegen.states.AuthorizationState

internal class AWSCognitoAuthSignOutRequest(val options: AuthSignOutOptions, val sendHubEvent: Boolean = true)

internal class AWSCognitoAuthSignOutTask(
    authStateMachine: AuthStateMachine,
    configuration: AuthConfiguration,
    val request: AWSCognitoAuthSignOutRequest
) : AuthTask<AuthSignOutResult>(authStateMachine, configuration) {
    override suspend fun validateStates(): AuthSignOutResult {
        val authState = authStateMachine.getCurrentStateAsync().await()
        return when (authState.authNState) {
            is AuthenticationState.NotConfigured -> AWSCognitoAuthSignOutResult.CompleteSignOut
            // Continue sign out and clear auth or guest credentials
            is AuthenticationState.SignedIn, is AuthenticationState.SignedOut -> {
                // Send SignOut event here instead of OnSubscribedCallback handler to ensure we do not fire
                // onComplete immediately, which would happen if calling signOut while signed out
                sendSignOutEvent()
                listenAndComplete()
            }
            is AuthenticationState.FederatedToIdentityPool -> AWSCognitoAuthSignOutResult.FailedSignOut(
                InvalidStateException(
                    "The user is currently federated to identity pool. " +
                        "You must call clearFederationToIdentityPool to clear credentials."
                )
            )
            else -> AWSCognitoAuthSignOutResult.FailedSignOut(InvalidStateException())
        }
    }

    private fun sendSignOutEvent() {
        val event = AuthenticationEvent(
            AuthenticationEvent.EventType.SignOutRequested(
                SignOutData(
                    request.options.isGlobalSignOut,
                    (request.options as? AWSCognitoAuthSignOutOptions)?.browserPackage
                )
            )
        )
        authStateMachine.send(event)
    }

    override suspend fun listenAndComplete(): AuthSignOutResult {
        val channel = authStateMachine.listenAsync()
        for (authState in channel) {
            val authNState = authState.authNState
            val authZState = authState.authZState
            when {
                authNState is AuthenticationState.SignedOut && authZState is AuthorizationState.Configured -> {
                    channel.close()
                    if (request.sendHubEvent) {
                        sendHubEvent(AuthChannelEventName.SIGNED_OUT.toString())
                    }
                    val signedOutData = authNState.signedOutData
                    return if (signedOutData.hasError) {
                        AWSCognitoAuthSignOutResult.PartialSignOut(
                            hostedUIError = signedOutData.hostedUIErrorData?.let { HostedUIError(it) },
                            globalSignOutError = signedOutData.globalSignOutErrorData?.let { GlobalSignOutError(it) },
                            revokeTokenError = signedOutData.revokeTokenErrorData?.let { RevokeTokenError(it) }
                        )
                    } else AWSCognitoAuthSignOutResult.CompleteSignOut
                }
                authNState is AuthenticationState.Error -> {
                    channel.close()
                    return AWSCognitoAuthSignOutResult.FailedSignOut(
                        CognitoAuthExceptionConverter.lookup(authNState.exception, "Sign out failed.")
                    )
                }
            }
        }
        throw InvalidStateException()
    }

    override suspend fun invoke(): AuthSignOutResult {
        return validateStates()
    }
}
