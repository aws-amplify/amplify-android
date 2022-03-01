package com.amplifyframework.statemachine

interface EventDispatcher {
    fun send(event: StateMachineEvent)
}