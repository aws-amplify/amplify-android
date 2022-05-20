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

/**
 * Executes a state transition when a precondition is fulfilled or when an event is received.
 *
 * StateMachineEvents are resolved as follows:
 *
 * - Traverse the State tree depth-first
 * - Resolve each leaf State (that is, each State that has no substates) using the current state and incoming event.
 * Resolver returns a `StateResolution` that contains both a new State, and a set of zero or more side `Effects`/`Actions`.
 * - The parent State assigns the new substate value to the appropriate property, and appends the returned Effects to
 * the list of Effects to be returned in the parent State's own `StateResolution`
 * - Each inner node resolves its own attributes by evaluating the new values of its substates, the current values of
 * its own properties, and the triggering StateMachineEvent
 * - The inner node appends zero or more Effects to the list of effects to be performed
 * - The inner node returns its new values (which are the new local values plus the new values of all substates), and
 * list of Effects (which are the effects requested by all substates, plus the effects requested by local state
 * resolution) in a `StateResolution`
 * - The process continues up to the "root" State
 * - The State Machine stores the new composite state as the new state of the System
 * - The State Machine dispatches Effects for resolution and execution
 */
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
