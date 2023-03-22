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

package com.amplifyframework.statemachine

import java.util.logging.ConsoleHandler
import java.util.logging.Level
import java.util.logging.Logger

internal class LoggingStateMachineResolver<StateType : State, ResolverType : StateMachineResolver<StateType>>(
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
