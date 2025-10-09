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

import com.amplifyframework.auth.AuthChannelEventName
import com.amplifyframework.auth.AuthFactorType
import com.amplifyframework.auth.cognito.AuthConfiguration
import com.amplifyframework.auth.cognito.AuthStateMachine
import com.amplifyframework.auth.cognito.CognitoAuthExceptionConverter.Companion.toAuthException
import com.amplifyframework.auth.cognito.exceptions.configuration.InvalidUserPoolConfigurationException
import com.amplifyframework.auth.cognito.options.AWSCognitoAuthSignInOptions
import com.amplifyframework.auth.cognito.options.AuthFlowType
import com.amplifyframework.auth.cognito.util.sendEventAndGetSignInResult
import com.amplifyframework.auth.exceptions.InvalidStateException
import com.amplifyframework.auth.options.AuthSignInOptions
import com.amplifyframework.auth.plugins.core.AuthHubEventEmitter
import com.amplifyframework.auth.result.AuthSignInResult
import com.amplifyframework.statemachine.codegen.data.SignInData
import com.amplifyframework.statemachine.codegen.events.AuthenticationEvent
import com.amplifyframework.statemachine.codegen.states.AuthState
import com.amplifyframework.statemachine.codegen.states.AuthenticationState
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.mapNotNull

internal class SignInUseCase(
    private val stateMachine: AuthStateMachine,
    private val configuration: AuthConfiguration,
    private val hubEmitter: AuthHubEventEmitter = AuthHubEventEmitter()
) {
    suspend fun execute(
        username: String?,
        password: String?,
        options: AuthSignInOptions = AuthSignInOptions.defaults()
    ): AuthSignInResult {
        val signInData = getSignInData(username = username, password = password, options = options)

        // Make sure we can sign in
        waitForStateThatAllowsSignIn()

        val event = AuthenticationEvent(AuthenticationEvent.EventType.SignInRequested(signInData))
        val result = stateMachine.sendEventAndGetSignInResult(event)

        if (result.isSignedIn) {
            hubEmitter.sendHubEvent(AuthChannelEventName.SIGNED_IN.toString())
        }

        return result
    }

    private suspend fun waitForStateThatAllowsSignIn(): AuthState {
        val authState = stateMachine.state.mapNotNull { authState ->
            when (val authNState = authState.authNState) {
                is AuthenticationState.NotConfigured -> throw InvalidUserPoolConfigurationException()
                is AuthenticationState.SignedOut, is AuthenticationState.Configured -> authState
                is AuthenticationState.SigningOut -> null
                is AuthenticationState.SigningIn -> {
                    // Cancel the sign in
                    stateMachine.send(AuthenticationEvent(AuthenticationEvent.EventType.CancelSignIn()))
                    null
                }
                is AuthenticationState.Error -> throw authNState.exception.toAuthException("Sign in failed.")
                else -> throw InvalidStateException()
            }
        }.first()
        return authState
    }

    private fun getSignInData(username: String?, password: String?, options: AuthSignInOptions): SignInData {
        val cognitoOptions = options as? AWSCognitoAuthSignInOptions ?: AWSCognitoAuthSignInOptions.builder().build()
        return when (cognitoOptions.authFlowType ?: configuration.authFlowType) {
            AuthFlowType.USER_SRP_AUTH -> SignInData.SRPSignInData(
                username = username,
                password = password,
                metadata = cognitoOptions.metadata,
                authFlowType = AuthFlowType.USER_SRP_AUTH
            )
            AuthFlowType.CUSTOM_AUTH, AuthFlowType.CUSTOM_AUTH_WITHOUT_SRP -> SignInData.CustomAuthSignInData(
                username = username,
                metadata = cognitoOptions.metadata
            )
            AuthFlowType.CUSTOM_AUTH_WITH_SRP -> SignInData.CustomSRPAuthSignInData(
                username = username,
                password = password,
                metadata = cognitoOptions.metadata
            )
            AuthFlowType.USER_PASSWORD_AUTH -> SignInData.MigrationAuthSignInData(
                username = username,
                password = password,
                metadata = cognitoOptions.metadata,
                authFlowType = AuthFlowType.USER_PASSWORD_AUTH
            )
            AuthFlowType.USER_AUTH -> when (cognitoOptions.preferredFirstFactor) {
                AuthFactorType.PASSWORD -> SignInData.MigrationAuthSignInData(
                    username = username,
                    password = password,
                    metadata = cognitoOptions.metadata,
                    authFlowType = AuthFlowType.USER_AUTH
                )
                AuthFactorType.PASSWORD_SRP -> SignInData.SRPSignInData(
                    username = username,
                    password = password,
                    metadata = cognitoOptions.metadata,
                    authFlowType = AuthFlowType.USER_AUTH
                )
                else -> SignInData.UserAuthSignInData(
                    username = username,
                    preferredChallenge = cognitoOptions.preferredFirstFactor,
                    callingActivity = cognitoOptions.callingActivity,
                    metadata = cognitoOptions.metadata
                )
            }
        }
    }
}
