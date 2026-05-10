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
import com.amplifyframework.auth.cognito.exceptions.service.InvalidAccountTypeException
import com.amplifyframework.auth.cognito.getCognitoSession
import com.amplifyframework.auth.cognito.isValid
import com.amplifyframework.auth.cognito.toAuthException
import com.amplifyframework.auth.exceptions.ConfigurationException
import com.amplifyframework.auth.exceptions.InvalidStateException
import com.amplifyframework.auth.exceptions.NotAuthorizedException
import com.amplifyframework.auth.exceptions.ServiceException
import com.amplifyframework.auth.exceptions.SessionExpiredException
import com.amplifyframework.auth.exceptions.SignedOutException
import com.amplifyframework.auth.options.AuthFetchSessionOptions
import com.amplifyframework.auth.plugins.core.AuthHubEventEmitter
import com.amplifyframework.statemachine.codegen.data.AmplifyCredential
import com.amplifyframework.statemachine.codegen.errors.SessionError
import com.amplifyframework.statemachine.codegen.events.AuthorizationEvent
import com.amplifyframework.statemachine.codegen.states.AuthorizationState
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.flow.onSubscription

internal class FetchAuthSessionUseCase(
    private val stateMachine: AuthStateMachine,
    private val emitter: AuthHubEventEmitter = AuthHubEventEmitter()
) {
    suspend fun execute(options: AuthFetchSessionOptions = AuthFetchSessionOptions.defaults()): AWSCognitoAuthSession =
        execute(userId = null, options = options)

    /**
     * Multi-user fetch: returns the session for [userId] from [AuthStateRepo].
     *
     * When [userId] is null, behaves identically to the no-arg overload (active user / single-user
     * legacy path).
     *
     * When [userId] is non-null, reads that user's persisted state, refreshes per-user if needed,
     * and dispatches refresh / federation events scoped to [userId] so [AuthStateRepo] tracks the
     * refreshed credential against the right user.
     */
    suspend fun execute(
        userId: String?,
        options: AuthFetchSessionOptions = AuthFetchSessionOptions.defaults()
    ): AWSCognitoAuthSession {
        val forceRefresh = options.forceRefresh
        val currentState = if (userId.isNullOrEmpty()) {
            stateMachine.getCurrentState()
        } else {
            stateMachine.getStateForUser(userId)
        }

        return when (val authZState = currentState.authZState) {
            is AuthorizationState.Configured -> {
                waitForSession(AuthorizationEvent(AuthorizationEvent.EventType.FetchUnAuthSession), userId)
            }
            is AuthorizationState.SessionEstablished -> {
                val credential = authZState.amplifyCredential
                if (credential.isValid() && !forceRefresh) {
                    // Return existing credential
                    credential.getCognitoSession()
                } else {
                    // Refresh session
                    val event = getRefreshSessionEvent(credential, userId)
                    waitForSession(event, userId)
                }
            }
            is AuthorizationState.Error -> {
                when (val error = authZState.exception) {
                    is SessionError -> waitForSession(getRefreshSessionEvent(error.amplifyCredential, userId), userId)
                    else -> throw InvalidStateException()
                }
            }
            else -> throw InvalidStateException()
        }
    }

    private fun getRefreshSessionEvent(credential: AmplifyCredential, userId: String? = null): AuthorizationEvent =
        if (credential is AmplifyCredential.IdentityPoolFederated) {
            AuthorizationEvent(
                AuthorizationEvent.EventType.StartFederationToIdentityPool(
                    credential.federatedToken,
                    credential.identityId,
                    credential,
                    userId = userId
                )
            )
        } else {
            AuthorizationEvent(AuthorizationEvent.EventType.RefreshSession(credential, userId = userId))
        }

    private suspend fun waitForSession(event: AuthorizationEvent, userId: String? = null): AWSCognitoAuthSession =
        stateMachine.state
            .onSubscription {
                if (userId.isNullOrEmpty()) stateMachine.send(event) else stateMachine.send(event, userId)
            }
            .drop(1)
            .mapNotNull { authState ->
                when (val authZState = authState.authZState) {
                    is AuthorizationState.SessionEstablished -> {
                        authZState.amplifyCredential.getCognitoSession()
                    }
                    is AuthorizationState.Error -> {
                        when (val error = authZState.exception) {
                            is SessionError -> {
                                when (val innerException = error.exception) {
                                    is SignedOutException -> {
                                        error.amplifyCredential.getCognitoSession(innerException)
                                    }
                                    is SessionExpiredException -> {
                                        emitter.sendHubEvent(AuthChannelEventName.SESSION_EXPIRED.toString())
                                        error.amplifyCredential.getCognitoSession(innerException)
                                    }
                                    is ServiceException -> {
                                        error.amplifyCredential.getCognitoSession(innerException)
                                    }
                                    is NotAuthorizedException -> {
                                        error.amplifyCredential.getCognitoSession(innerException)
                                    }
                                    else -> {
                                        val errorResult = innerException.toAuthException("Fetch auth session failed.")
                                        error.amplifyCredential.getCognitoSession(errorResult)
                                    }
                                }
                            }
                            is ConfigurationException -> {
                                val errorResult = InvalidAccountTypeException(error)
                                AmplifyCredential.Empty.getCognitoSession(errorResult)
                            }
                            else -> {
                                val errorResult = error.toAuthException("Fetch auth session failed.")
                                AmplifyCredential.Empty.getCognitoSession(errorResult)
                            }
                        }
                    }
                    else -> null
                }
            }.first()
}
