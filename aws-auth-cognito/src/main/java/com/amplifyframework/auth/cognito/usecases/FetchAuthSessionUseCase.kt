/*
 * Copyright 2024 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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
import com.amplifyframework.auth.cognito.AWSCognitoAuthSession
import com.amplifyframework.auth.cognito.AuthStateMachine
import com.amplifyframework.auth.cognito.getCognitoSession
import com.amplifyframework.auth.cognito.isValid
import com.amplifyframework.auth.cognito.exceptions.service.InvalidAccountTypeException
import com.amplifyframework.auth.exceptions.ConfigurationException
import com.amplifyframework.auth.exceptions.InvalidStateException
import com.amplifyframework.auth.exceptions.NotAuthorizedException
import com.amplifyframework.auth.exceptions.ServiceException
import com.amplifyframework.auth.exceptions.SessionExpiredException
import com.amplifyframework.auth.exceptions.SignedOutException
import com.amplifyframework.auth.exceptions.UnknownException
import com.amplifyframework.auth.options.AuthFetchSessionOptions
import com.amplifyframework.auth.plugins.core.AuthHubEventEmitter
import com.amplifyframework.statemachine.codegen.data.AmplifyCredential
import com.amplifyframework.statemachine.codegen.errors.SessionError
import com.amplifyframework.statemachine.codegen.events.AuthorizationEvent
import com.amplifyframework.statemachine.codegen.states.AuthorizationState
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.onSubscription

internal class FetchAuthSessionUseCase(
    private val stateMachine: AuthStateMachine,
    private val emitter: AuthHubEventEmitter = AuthHubEventEmitter()
) {
    suspend fun execute(
        options: AuthFetchSessionOptions = AuthFetchSessionOptions.defaults()
    ): AWSCognitoAuthSession {
        val forceRefresh = options.forceRefresh
        val currentState = stateMachine.getCurrentState()
        val authZState = currentState.authZState

        when (authZState) {
            is AuthorizationState.Configured -> {
                return waitForSession(
                    AuthorizationEvent(AuthorizationEvent.EventType.FetchUnAuthSession)
                )
            }
            is AuthorizationState.SessionEstablished -> {
                val credential = authZState.amplifyCredential
                if (credential.isValid() && !forceRefresh) {
                    return credential.getCognitoSession() as AWSCognitoAuthSession
                }
                val event = if (credential is AmplifyCredential.IdentityPoolFederated) {
                    AuthorizationEvent(
                        AuthorizationEvent.EventType.StartFederationToIdentityPool(
                            credential.federatedToken,
                            credential.identityId,
                            credential
                        )
                    )
                } else {
                    AuthorizationEvent(AuthorizationEvent.EventType.RefreshSession(credential))
                }
                return waitForSession(event)
            }
            is AuthorizationState.Error -> {
                val error = authZState.exception
                if (error is SessionError) {
                    val amplifyCredential = error.amplifyCredential
                    val event = if (amplifyCredential is AmplifyCredential.IdentityPoolFederated) {
                        AuthorizationEvent(
                            AuthorizationEvent.EventType.StartFederationToIdentityPool(
                                amplifyCredential.federatedToken,
                                amplifyCredential.identityId,
                                amplifyCredential
                            )
                        )
                    } else {
                        AuthorizationEvent(AuthorizationEvent.EventType.RefreshSession(amplifyCredential))
                    }
                    return waitForSession(event)
                } else {
                    throw InvalidStateException()
                }
            }
            else -> throw InvalidStateException()
        }
    }

    private suspend fun waitForSession(event: AuthorizationEvent): AWSCognitoAuthSession {
        return stateMachine.state
            .onSubscription { stateMachine.send(event) }
            .drop(1)
            .mapNotNull { authState ->
                when (val authZState = authState.authZState) {
                    is AuthorizationState.SessionEstablished -> {
                        authZState.amplifyCredential.getCognitoSession() as AWSCognitoAuthSession
                    }
                    is AuthorizationState.Error -> {
                        when (val error = authZState.exception) {
                            is SessionError -> {
                                when (val innerException = error.exception) {
                                    is SignedOutException -> {
                                        error.amplifyCredential.getCognitoSession(innerException) as AWSCognitoAuthSession
                                    }
                                    is SessionExpiredException -> {
                                        emitter.sendHubEvent(AuthChannelEventName.SESSION_EXPIRED.toString())
                                        error.amplifyCredential.getCognitoSession(innerException) as AWSCognitoAuthSession
                                    }
                                    is ServiceException -> {
                                        error.amplifyCredential.getCognitoSession(innerException) as AWSCognitoAuthSession
                                    }
                                    is NotAuthorizedException -> {
                                        error.amplifyCredential.getCognitoSession(innerException) as AWSCognitoAuthSession
                                    }
                                    else -> {
                                        val errorResult = UnknownException(
                                            "Fetch auth session failed.",
                                            innerException
                                        )
                                        error.amplifyCredential.getCognitoSession(errorResult) as AWSCognitoAuthSession
                                    }
                                }
                            }
                            is ConfigurationException -> {
                                val errorResult = InvalidAccountTypeException(error)
                                AmplifyCredential.Empty.getCognitoSession(errorResult) as AWSCognitoAuthSession
                            }
                            else -> {
                                val errorResult = UnknownException("Fetch auth session failed.", error)
                                AmplifyCredential.Empty.getCognitoSession(errorResult) as AWSCognitoAuthSession
                            }
                        }
                    }
                    else -> null
                }
            }.first()
    }
}
