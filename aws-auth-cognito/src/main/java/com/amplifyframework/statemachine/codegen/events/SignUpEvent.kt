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

import com.amplifyframework.auth.options.AuthResendSignUpCodeOptions
import com.amplifyframework.auth.options.AuthSignUpOptions
import com.amplifyframework.statemachine.StateMachineEvent
import com.amplifyframework.statemachine.codegen.data.SignedUpData
import java.util.Date

class SignUpEvent(
    val eventType: EventType,
    override val time: Date? = null,
) : StateMachineEvent {
    sealed class EventType {
        data class InitiateSignUp(
            val username: String,
            val password: String,
            val options: AuthSignUpOptions
        ) : EventType()

        data class ConfirmSignUp(val username: String, val confirmationCode: String) : EventType()
        data class ResendSignUpCode(
            val username: String,
            val options: AuthResendSignUpCodeOptions
        ) : EventType()

        data class ResendSignUpCodeSuccess(val signedUpData: SignedUpData) : EventType()
        data class ResendSignUpCodeFailure(val exception: Exception) : EventType()
        data class InitiateSignUpSuccess(val signedUpData: SignedUpData) : EventType()
        data class InitiateSignUpFailure(val exception: Exception) : EventType()
        data class ConfirmSignUpSuccess(val id: String = "") : EventType()
        data class ConfirmSignUpFailure(val exception: Exception) : EventType()
    }

    override val type = eventType.toString()
}
