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

class AnyResolver<StateType : State, ResolverType : StateMachineResolver<StateType>>(
    val resolver: ResolverType
) :
    StateMachineResolver<StateType> {

    override var defaultState: StateType = resolver.defaultState

    override fun resolve(oldState: StateType, event: StateMachineEvent): StateResolution<StateType> =
        resolver.resolve(oldState, event)
}
