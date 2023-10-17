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
import java.util.Date

internal class SignInChallengeEvent(val eventType: EventType, override val time: Date? = null) : StateMachineEvent {
    sealed class EventType {
        data class WaitForAnswer(val challenge: AuthChallenge, val hasNewResponse: Boolean = false) : EventType()
        data class VerifyChallengeAnswer(val answer: String, val metadata: Map<String, String>) : EventType()

        data class RetryVerifyChallengeAnswer(
            val answer: String,
            val metadata: Map<String, String>,
            val authChallenge: AuthChallenge
        ) : EventType()
        data class FinalizeSignIn(val accessToken: String) : EventType()
        data class Verified(val id: String = "") : EventType()
        data class ThrowError(
            val exception: Exception,
            val challenge: AuthChallenge,
            val hasNewResponse: Boolean = false
        ) : EventType()
    }

    override val type: String = eventType.javaClass.simpleName
}
