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

package com.amplifyframework.statemachine.codegen.events

import com.amplifyframework.auth.options.AuthSignInOptions
import com.amplifyframework.statemachine.StateMachineEvent
import com.amplifyframework.statemachine.codegen.data.AmplifyCredential
import com.amplifyframework.statemachine.codegen.data.AuthConfiguration
import com.amplifyframework.statemachine.codegen.data.SignedInData
import com.amplifyframework.statemachine.codegen.data.SignedOutData
import java.util.Date

class AuthenticationEvent(val eventType: EventType, override val time: Date? = null) :
    StateMachineEvent {
    sealed class EventType {
        data class Configure(
            val configuration: AuthConfiguration,
            val storedCredentials: AmplifyCredential?
        ) : EventType()

        object Configured : EventType()
        data class InitializedSignedIn(val signedInData: SignedInData) : EventType()
        data class InitializedSignedOut(val signedOutData: SignedOutData) : EventType()
        data class SignInRequested(
            val username: String?,
            val password: String?,
            val options: AuthSignInOptions
        ) : EventType()

        data class SignInCompleted(val signedInData: SignedInData) : EventType()

        data class SignOutRequested(
            val isGlobalSignOut: Boolean = false,
            val invalidateTokens: Boolean = true
        ) : EventType()

        data class CancelSignIn(val id: String = "") : EventType()
        data class ResetSignUp(val id: String = "") : EventType()
        data class ThrowError(val exception: Exception) : EventType()
    }

    override val type: String = eventType.javaClass.simpleName
}
