package com.amplifyframework.statemachine

import java.util.logging.Level
import java.util.logging.Logger

interface StateMachineResolver<StateType : State> {
    val defaultState: StateType
    fun resolve(oldState: StateType, event: StateMachineEvent): StateResolution<StateType>

    fun logging(
        logger: Logger? = null,
        level: Level = Level.FINE
    ): LoggingStateMachineResolver<StateType, StateMachineResolver<StateType>> =
        LoggingStateMachineResolver(this, logger, level)

    fun eraseToAnyResolver(): AnyResolver<StateType, *> {
        val anyResolver = this as? AnyResolver<StateType, *>
        return anyResolver ?: AnyResolver(this)
    }
}

class AnyResolver<StateType : State, ResolverType : StateMachineResolver<StateType>>(val resolver: ResolverType) :
    StateMachineResolver<StateType> {

    override var defaultState: StateType = resolver.defaultState

    override fun resolve(oldState: StateType, event: StateMachineEvent): StateResolution<StateType> =
        resolver.resolve(oldState, event)
}