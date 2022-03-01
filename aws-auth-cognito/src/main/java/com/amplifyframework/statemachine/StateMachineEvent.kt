package com.amplifyframework.statemachine

import java.util.*

interface StateMachineEvent {
    val id: String
        get() = UUID.randomUUID().toString()

    val type: String
    val time: Date?
}