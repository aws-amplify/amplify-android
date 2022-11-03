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

import com.amplifyframework.auth.AuthSession
import com.amplifyframework.auth.cognito.AuthStateMachine
import com.amplifyframework.auth.cognito.exceptions.service.InvalidAccountTypeException
import com.amplifyframework.auth.cognito.getCognitoSession
import com.amplifyframework.auth.cognito.isValid
import com.amplifyframework.auth.exceptions.ConfigurationException
import com.amplifyframework.auth.exceptions.InvalidStateException
import com.amplifyframework.auth.exceptions.SessionExpiredException
import com.amplifyframework.auth.exceptions.SignedOutException
import com.amplifyframework.auth.exceptions.UnknownException
import com.amplifyframework.auth.options.AuthFetchSessionOptions
import com.amplifyframework.statemachine.codegen.data.AmplifyCredential
import com.amplifyframework.statemachine.codegen.data.AuthConfiguration
import com.amplifyframework.statemachine.codegen.errors.SessionError
import com.amplifyframework.statemachine.codegen.events.AuthorizationEvent
import com.amplifyframework.statemachine.codegen.states.AuthorizationState

internal class FetchAuthSessionRequest(val options: AuthFetchSessionOptions)

internal class FetchAuthSessionTask(
    private val authStateMachine: AuthStateMachine,
    val configuration: AuthConfiguration,
    val request: FetchAuthSessionRequest
) : AuthTask<AuthSession> {
    override suspend fun validateStates(): AuthSession {
        val forceRefresh = request.options.forceRefresh
        val authState = authStateMachine.getCurrentStateAsync().await()
        when (val authZState = authState.authZState) {
            is AuthorizationState.Configured -> {
                authStateMachine.send(AuthorizationEvent(AuthorizationEvent.EventType.FetchUnAuthSession))
                return execute()
            }
            is AuthorizationState.SessionEstablished -> {
                val credential = authZState.amplifyCredential
                if (!credential.isValid() || forceRefresh) {
                    if (credential is AmplifyCredential.IdentityPoolFederated) {
                        authStateMachine.send(
                            AuthorizationEvent(
                                AuthorizationEvent.EventType.StartFederationToIdentityPool(
                                    credential.federatedToken,
                                    credential.identityId,
                                    credential
                                )
                            )
                        )
                    } else {
                        authStateMachine.send(
                            AuthorizationEvent(AuthorizationEvent.EventType.RefreshSession(credential))
                        )
                    }
                    return execute()
                } else return credential.getCognitoSession()
            }
            is AuthorizationState.Error -> {
                val error = authZState.exception
                if (error is SessionError) {
                    val amplifyCredential = error.amplifyCredential
                    if (amplifyCredential is AmplifyCredential.IdentityPoolFederated) {
                        authStateMachine.send(
                            AuthorizationEvent(
                                AuthorizationEvent.EventType.StartFederationToIdentityPool(
                                    amplifyCredential.federatedToken,
                                    amplifyCredential.identityId,
                                    amplifyCredential
                                )
                            )
                        )
                    } else {
                        authStateMachine.send(
                            AuthorizationEvent(AuthorizationEvent.EventType.RefreshSession(amplifyCredential))
                        )
                    }
                    return execute()
                } else {
                    throw InvalidStateException()
                }
            }
            else -> throw InvalidStateException()
        }
    }

    override suspend fun execute(): AuthSession {
        val channel = authStateMachine.listenAsync()

        for (authState in channel) {
            when (val authZState = authState.authZState) {
                is AuthorizationState.SessionEstablished -> {
                    channel.close()
                    return authZState.amplifyCredential.getCognitoSession()
                }
                is AuthorizationState.Error -> {
                    channel.close()
                    return when (val error = authZState.exception) {
                        is SessionError -> {
                            when (error.exception) {
                                is SignedOutException -> {
                                    error.amplifyCredential.getCognitoSession(error.exception)
                                }
                                is SessionExpiredException -> {
                                    AmplifyCredential.Empty.getCognitoSession(error.exception)
                                    //                                        sendHubEvent(AuthChannelEventName.SESSION_EXPIRED.toString())
                                }
                                else -> {
                                    val errorResult = UnknownException("Fetch auth session failed.", error)
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
                else -> Unit
            }
        }
        throw InvalidStateException()
    }

    override suspend fun invoke(): AuthSession {
        return validateStates()
    }
}
