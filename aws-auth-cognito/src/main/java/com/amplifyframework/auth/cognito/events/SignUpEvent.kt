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

import aws.sdk.kotlin.services.cognitoidentityprovider.model.ConfirmSignUpResponse
import aws.sdk.kotlin.services.cognitoidentityprovider.model.SignUpResponse
import com.amplifyframework.auth.options.AuthSignUpOptions
import com.amplifyframework.statemachine.StateMachineEvent
import java.util.*

class SignUpEvent(val eventType: EventType, override val time: Date? = null,
) : StateMachineEvent {
    sealed class EventType {
        data class InitiateSignUp(
            val username: String,
            val password: String,
            val options: AuthSignUpOptions
        ) : EventType()
        data class ConfirmSignUp(val username: String, val confirmationCode: String) : EventType()
        data class InitiateSignUpSuccess(val username: String, val signUpResponse: SignUpResponse?) : EventType()
        data class InitiateSignUpFailure(val exception: Exception) : EventType()
        data class ConfirmSignUpSuccess(val confirmSignupResponse: ConfirmSignUpResponse?) : EventType()
        data class ConfirmSignUpFailure(val exception: Exception) : EventType()
    }

    override val type = eventType.toString()
}