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

import com.amplifyframework.auth.cognito.data.AuthenticationError
import com.amplifyframework.auth.cognito.data.SignedInData
import com.amplifyframework.statemachine.StateMachineEvent
import java.util.*

class SignOutEvent(
    val eventType: EventType, override val time: Date? = null,
) : StateMachineEvent {
    sealed class EventType {
        data class SignOutLocally(
            val signedInData: SignedInData,
            val isGlobalSignOut: Boolean,
            val invalidateTokens: Boolean
        ) : EventType()

        data class SignOutGlobally(val signedInData: SignedInData) : EventType()
        data class SignedOutSuccess(val signedInData: SignedInData) : EventType()
        data class SignedOutFailure(val error: AuthenticationError) : EventType()
        data class RevokeToken(val signedInData: SignedInData) : EventType()
    }

    override val type = eventType.toString()
}