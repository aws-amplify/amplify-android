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

package com.amplifyframework.auth.cognito.events

import com.amplifyframework.auth.cognito.data.*
import com.amplifyframework.auth.options.AuthSignInOptions
import com.amplifyframework.auth.options.AuthSignUpOptions
import com.amplifyframework.statemachine.StateMachineEvent
import java.util.*

class AuthenticationEvent(val eventType: EventType, override val time: Date? = null) :
    StateMachineEvent {
    sealed class EventType {
        data class Configure(
            val configuration: AuthConfiguration,
            val storedCredentials: AmplifyCredential?
        ) : EventType()
        data class InitializedSignedIn(val signedInData: SignedInData) : EventType()
        data class InitializedSignedOut(val signedOutData: SignedOutData) : EventType()
        data class SignUpRequested(
            val username: String?,
            val password: String?,
            val options: AuthSignUpOptions
        ) : EventType()

        data class ConfirmSignUpRequested(
            val username: String,
            val confirmationCode: String,
        ) : EventType()

        data class SignInRequested(
            val username: String?,
            val password: String?,
            val options: AuthSignInOptions
        ) : EventType()

        data class SignOutRequested(
            val isGlobalSignOut: Boolean = false,
            val invalidateTokens: Boolean = true
        ) : EventType()

        data class CancelSignIn(val id: String = "") : EventType()
        data class CancelSignUp(val username: String) : EventType()
        data class ThrowError(val exception: Exception) : EventType()
    }

    override val type = eventType.toString()
}