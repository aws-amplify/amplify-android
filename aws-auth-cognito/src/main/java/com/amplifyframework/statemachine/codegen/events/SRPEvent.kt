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
import java.util.Date

internal class SRPEvent(val eventType: EventType, override val time: Date? = null) :
    StateMachineEvent {
    sealed class EventType {
        data class InitiateSRP(
            val username: String,
            val password: String,
            val metadata: Map<String, String>
        ) : EventType()
        data class InitiateSRPWithCustom(
            val username: String,
            val password: String,
            val metadata: Map<String, String>
        ) : EventType()
        data class RespondPasswordVerifier(
            val challengeParameters: Map<String, String>,
            val metadata: Map<String, String>,
            val session: String?
        ) : EventType()

        data class ThrowAuthError(val exception: Exception) : EventType()
        data class CancelSRPSignIn(val id: String = "") : EventType()
        data class ThrowPasswordVerifierError(val exception: Exception) : EventType()
        data class Reset(val id: String = "") : EventType()
    }

    override val type: String = eventType.javaClass.simpleName
}
