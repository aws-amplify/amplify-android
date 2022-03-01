package com.amplifyframework.statemachine

interface EffectExecutor {
    fun execute(actions: List<Action>, eventDispatcher: EventDispatcher, environment: Environment)
}