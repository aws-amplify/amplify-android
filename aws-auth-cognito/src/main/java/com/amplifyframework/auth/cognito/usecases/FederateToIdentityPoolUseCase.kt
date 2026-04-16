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

import com.amplifyframework.auth.AWSCredentials
import com.amplifyframework.auth.AWSTemporaryCredentials
import com.amplifyframework.auth.AuthProvider
import com.amplifyframework.auth.cognito.AWSCognitoAuthChannelEventName
import com.amplifyframework.auth.cognito.AuthStateMachine
import com.amplifyframework.auth.cognito.helpers.identityProviderName
import com.amplifyframework.auth.cognito.options.FederateToIdentityPoolOptions
import com.amplifyframework.auth.cognito.result.FederateToIdentityPoolResult
import com.amplifyframework.auth.cognito.toAuthException
import com.amplifyframework.auth.exceptions.InvalidStateException
import com.amplifyframework.auth.exceptions.UnknownException
import com.amplifyframework.auth.plugins.core.AuthHubEventEmitter
import com.amplifyframework.statemachine.codegen.data.AmplifyCredential
import com.amplifyframework.statemachine.codegen.data.FederatedToken
import com.amplifyframework.statemachine.codegen.errors.SessionError
import com.amplifyframework.statemachine.codegen.events.AuthorizationEvent
import com.amplifyframework.statemachine.codegen.states.AuthState
import com.amplifyframework.statemachine.codegen.states.AuthenticationState
import com.amplifyframework.statemachine.codegen.states.AuthorizationState
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.flow.onSubscription

internal class FederateToIdentityPoolUseCase(
    private val stateMachine: AuthStateMachine,
    private val emitter: AuthHubEventEmitter = AuthHubEventEmitter()
) {
    suspend fun execute(
        providerToken: String,
        authProvider: AuthProvider,
        options: FederateToIdentityPoolOptions? = null
    ): FederateToIdentityPoolResult {
        val authState = stateMachine.getCurrentState()

        val authNState = authState.authNState
        val authZState = authState.authZState

        if (authState !is AuthState.Configured ||
            !isValidAuthNState(authNState) ||
            !isValidAuthZState(authZState)
        ) {
            throw InvalidStateException("Federation could not be completed.")
        }

        val existingCredential = when (authZState) {
            is AuthorizationState.SessionEstablished -> authZState.amplifyCredential
            is AuthorizationState.Error -> {
                (authZState.exception as? SessionError)?.amplifyCredential
            }
            else -> null
        }

        val event = AuthorizationEvent(
            AuthorizationEvent.EventType.StartFederationToIdentityPool(
                token = FederatedToken(providerToken, authProvider.identityProviderName),
                identityId = options?.developerProvidedIdentityId,
                existingCredential
            )
        )

        return stateMachine.state
            .onSubscription { stateMachine.send(event) }
            .drop(1) // Ignore current state
            .mapNotNull { state ->
                val newAuthNState = state.authNState
                val newAuthZState = state.authZState
                when {
                    newAuthNState is AuthenticationState.FederatedToIdentityPool &&
                        newAuthZState is AuthorizationState.SessionEstablished -> {
                        parseResult(newAuthZState)
                    }
                    newAuthNState is AuthenticationState.Error &&
                        newAuthZState is AuthorizationState.Error -> {
                        throw newAuthZState.exception.toAuthException("Federation could not be completed.")
                    }
                    else -> null
                }
            }.first()
    }

    private fun parseResult(authZState: AuthorizationState.SessionEstablished): FederateToIdentityPoolResult {
        val credential = authZState.amplifyCredential as? AmplifyCredential.IdentityPoolFederated
        val identityId = credential?.identityId
        val awsCredentials = credential?.credentials
        val temporaryAwsCredentials = AWSCredentials.createAWSCredentials(
            awsCredentials?.accessKeyId,
            awsCredentials?.secretAccessKey,
            awsCredentials?.sessionToken,
            awsCredentials?.expiration
        ) as? AWSTemporaryCredentials

        if (identityId != null && temporaryAwsCredentials != null) {
            val result = FederateToIdentityPoolResult(
                credentials = temporaryAwsCredentials,
                identityId = identityId
            )
            emitter.sendHubEvent(AWSCognitoAuthChannelEventName.FEDERATED_TO_IDENTITY_POOL.toString())
            return result
        } else {
            throw UnknownException(
                message = "Unable to parse credentials to expected output."
            )
        }
    }

    private fun isValidAuthNState(authNState: AuthenticationState?): Boolean = when (authNState) {
        is AuthenticationState.SignedOut,
        is AuthenticationState.Error,
        is AuthenticationState.NotConfigured,
        is AuthenticationState.FederatedToIdentityPool -> true
        else -> false
    }

    private fun isValidAuthZState(authZState: AuthorizationState?): Boolean = when (authZState) {
        is AuthorizationState.Configured,
        is AuthorizationState.SessionEstablished,
        is AuthorizationState.Error -> true
        else -> false
    }
}
