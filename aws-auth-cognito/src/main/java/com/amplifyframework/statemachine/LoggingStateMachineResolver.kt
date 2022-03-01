package com.amplifyframework.statemachine

import java.util.logging.ConsoleHandler
import java.util.logging.Level
import java.util.logging.Logger

class LoggingStateMachineResolver<StateType : State, ResolverType : StateMachineResolver<StateType>>(
    private val resolver: ResolverType,
    logger: Logger? = null,
    private val level: Level = Level.INFO
) :
    StateMachineResolver<StateType> {

    private var logger = logger ?: makeDefaultLogger()

    override val defaultState = resolver.defaultState

    companion object {
        // TODO: reification - create logger of name T::class.java.name
        fun makeDefaultLogger(): Logger {
            val logger = Logger.getLogger(this.toString())
            val handler = ConsoleHandler()
            handler.level = Level.ALL
            logger.level = Level.ALL
            logger.addHandler(handler)
            logger.useParentHandlers = false
            return logger
        }
    }

    override fun resolve(oldState: StateType, event: StateMachineEvent): StateResolution<StateType> {
        val resolution = resolver.resolve(oldState, event)
        logger.log(level, oldState.toString())
        logger.log(level, event.type)
        logger.log(level, resolution.newState.toString())
        return resolution
    }
}