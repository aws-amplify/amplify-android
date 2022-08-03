package com.amplifyframework.statemachine.codegen.events

import com.amplifyframework.statemachine.StateMachineEvent
import java.util.Date

class SignInChallengeEvent(val eventType: EventType, override val time: Date? = null) : StateMachineEvent {
    sealed class EventType {
        data class WaitForAnswer(val id: String = "") : EventType()
        data class VerifyChallengeAnswer(val challengeParameters: Map<String, String>) : EventType()
        data class Verified(val id: String = "") : EventType()
    }

    override val type: String = eventType.javaClass.simpleName
}
