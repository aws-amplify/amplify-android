package com.amplifyframework.statemachine

class StateResolution<T : State>(val newState: T, val actions: List<Action> = listOf()) {
    companion object {
        fun <T : State> from(_state: T): StateResolution<T> = StateResolution(_state)
    }
}