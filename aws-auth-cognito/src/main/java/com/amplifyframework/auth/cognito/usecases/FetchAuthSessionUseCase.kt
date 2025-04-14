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
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.mapNotNull

internal class FetchAuthSessionUseCase(
    private val stateMachine: AuthStateMachine,
    private val emitter: AuthHubEventEmitter = AuthHubEventEmitter()
) {
    suspend fun execute(options: AuthFetchSessionOptions = AuthFetchSessionOptions.defaults()): AWSCognitoAuthSession =
        when (val authZState = stateMachine.getCurrentState().authZState) {
            is AuthorizationState.Configured -> {
                stateMachine.send(AuthorizationEvent(AuthorizationEvent.EventType.FetchUnAuthSession))
                listenForSessionEstablished()
            }
            is AuthorizationState.SessionEstablished -> {
                val credential = authZState.amplifyCredential
                if (!credential.isValid() || options.forceRefresh) {
                    sendRefreshSessionEvent(credential)
                    listenForSessionEstablished()
                } else {
                    credential.getCognitoSession()
                }
            }
            is AuthorizationState.Error -> {
                val error = authZState.exception
                if (error is SessionError) {
                    sendRefreshSessionEvent(error.amplifyCredential)
                    listenForSessionEstablished()
                } else {
                    throw InvalidStateException()
                }
            }
            else -> throw InvalidStateException()
        }

    private fun sendRefreshSessionEvent(credential: AmplifyCredential) {
        if (credential is AmplifyCredential.IdentityPoolFederated) {
            stateMachine.send(
                AuthorizationEvent(
                    AuthorizationEvent.EventType.StartFederationToIdentityPool(
                        credential.federatedToken,
                        credential.identityId,
                        credential
                    )
                )
            )
        } else {
            stateMachine.send(AuthorizationEvent(AuthorizationEvent.EventType.RefreshSession(credential)))
        }
    }

    private suspend fun listenForSessionEstablished(): AWSCognitoAuthSession {
        val session = stateMachine.stateTransitions.mapNotNull { authState ->
            when (val authZState = authState.authZState) {
                is AuthorizationState.SessionEstablished -> authZState.amplifyCredential.getCognitoSession()
                is AuthorizationState.Error -> {
                    when (val error = authZState.exception) {
                        is SessionError -> {
                            when (val innerException = error.exception) {
                                is SignedOutException -> error.amplifyCredential.getCognitoSession(innerException)
                                is ServiceException -> error.amplifyCredential.getCognitoSession(innerException)
                                is NotAuthorizedException -> error.amplifyCredential.getCognitoSession(innerException)
                                is SessionExpiredException -> {
                                    emitter.sendHubEvent(AuthChannelEventName.SESSION_EXPIRED.toString())
                                    error.amplifyCredential.getCognitoSession(innerException)
                                }
                                else -> {
                                    val errorResult = UnknownException("Fetch auth session failed.", innerException)
                                    error.amplifyCredential.getCognitoSession(errorResult)
                                }
                            }
                        }
                        is ConfigurationException -> {
                            val errorResult = InvalidAccountTypeException(error)
                            AmplifyCredential.Empty.getCognitoSession(errorResult)
                        }
                        else -> {
                            val errorResult = UnknownException("Fetch auth session failed.", error)
                            AmplifyCredential.Empty.getCognitoSession(errorResult)
                        }
                    }
                }
                else -> null // no-op
            }
        }.first()

        return session
    }
}
