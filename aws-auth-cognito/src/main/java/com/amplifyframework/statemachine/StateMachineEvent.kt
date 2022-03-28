package com.amplifyframework.statemachine

import java.util.Date
import java.util.UUID

interface StateMachineEvent {
    val id: String
        get() = UUID.randomUUID().toString()

    val type: String
    val time: Date?
}
