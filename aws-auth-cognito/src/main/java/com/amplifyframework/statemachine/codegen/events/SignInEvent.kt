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

import com.amplifyframework.statemachine.StateMachineEvent
import com.amplifyframework.statemachine.codegen.data.AuthChallenge
import com.amplifyframework.statemachine.codegen.data.DeviceMetaData
import com.amplifyframework.statemachine.codegen.data.SignInData
import java.util.Date

class SignInEvent(val eventType: EventType, override val time: Date? = null) : StateMachineEvent {
    sealed class EventType {
        data class InitiateSignInWithSRP(val username: String, val password: String) : EventType()
        data class InitiateSignInWithCustom(
            val username: String,
            val password: String?,
            val metadata: Map<String, String>
        ) : EventType()
        data class InitiateHostedUISignIn(val hostedUISignInData: SignInData.HostedUISignInData) : EventType()
        data class SignedIn(val id: String = "") : EventType()
        data class ConfirmDevice(val deviceMetaData: DeviceMetaData) : EventType()
        data class FinalizeSignIn(val id: String = "") : EventType()
        data class ReceivedChallenge(val challenge: AuthChallenge) : EventType()
        data class ThrowError(val exception: Exception) : EventType()
    }

    override val type: String = eventType.javaClass.simpleName
}
