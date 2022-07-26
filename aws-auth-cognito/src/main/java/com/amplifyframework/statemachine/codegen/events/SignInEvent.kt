package com.amplifyframework.statemachine.codegen.events

import com.amplifyframework.statemachine.StateMachineEvent
import java.util.Date

class SignInEvent(val eventType: EventType, override val time: Date? = null) : StateMachineEvent {
    sealed class EventType {
        data class InitiateSignInWithSRP(val username: String, val password: String) : EventType()
        data class SignedIn(val id: String = "") : EventType()
        data class ReceivedSMSChallenge(val id: String = "") : EventType()
        data class ThrowError(val exception: Exception) : EventType()
    }

    override val type: String = eventType.javaClass.simpleName
}
