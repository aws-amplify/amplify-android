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

package com.amplifyframework.auth.cognito.util

import com.amplifyframework.auth.cognito.AuthStateMachine
import com.amplifyframework.auth.cognito.helpers.UserPoolSignInHelper
import com.amplifyframework.statemachine.StateMachineEvent
import com.amplifyframework.statemachine.codegen.states.AuthenticationState
import com.amplifyframework.statemachine.codegen.states.AuthorizationState
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.flow.onSubscription

internal suspend fun AuthStateMachine.sendEventAndGetSignInResult(event: StateMachineEvent) = state
    .onSubscription { send(event) }
    .drop(1) // Ignore current state
    .mapNotNull { authState ->
        val authNState = authState.authNState
        val authZState = authState.authZState
        when {
            authNState is AuthenticationState.Error -> throw authNState.exception
            authNState is AuthenticationState.SigningIn -> UserPoolSignInHelper.checkNextStep(
                signInState = authNState.signInState
            )
            authNState is AuthenticationState.SignedIn &&
                authZState is AuthorizationState.SessionEstablished -> UserPoolSignInHelper.signedInResult()
            else -> null
        }
    }.first()
