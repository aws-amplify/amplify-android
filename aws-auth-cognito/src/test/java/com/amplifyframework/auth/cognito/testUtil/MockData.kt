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

package com.amplifyframework.auth.cognito.testUtil

import aws.sdk.kotlin.services.cognitoidentityprovider.model.AuthenticationResultType
import com.amplifyframework.statemachine.codegen.states.AuthState
import com.amplifyframework.statemachine.codegen.states.AuthenticationState
import com.amplifyframework.statemachine.codegen.states.AuthorizationState
import com.amplifyframework.statemachine.codegen.states.SignUpState
import kotlin.time.Duration.Companion.minutes

internal fun authState(
    authNState: AuthenticationState? = null,
    authZState: AuthorizationState? = null,
    authSignUpState: SignUpState? = null
) = AuthState.Configured(
    authNState = authNState,
    authZState = authZState,
    authSignUpState = authSignUpState
)

internal fun authenticationResult(
    accessToken: String? = "accessToken",
    idToken: String? = "idToken",
    refreshToken: String? = "refreshToken",
    expiresIn: Int = 60.minutes.inWholeSeconds.toInt()
) = AuthenticationResultType {
    this.accessToken = accessToken
    this.idToken = idToken
    this.refreshToken = refreshToken
    this.expiresIn = expiresIn
}
