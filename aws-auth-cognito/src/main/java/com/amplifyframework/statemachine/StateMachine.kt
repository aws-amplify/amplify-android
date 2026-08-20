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

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.newSingleThreadContext
import kotlinx.coroutines.withContext

/**
 * Model, mutate and process effects of a system as a finite state automaton. It consists of:
 * State - which represents the current state of the system
 * Resolver - a mechanism for mutating state in response to events and returning side effects called Actions
 * Listener - which accepts and enqueues incoming events
 * StateChangedListeners - which are notified whenever the state changes
 * EffectExecutor - which resolves and executes side Effects/Actions
 * @implements EventDispatcher
 * @param resolver responsible for mutating state based on incoming events
 * @param environment holds system specific environment info accessible to Effects/Actions
 * @param executor responsible for invoking effects
 * @param initialState starting state of the system (resolver default state will be used if omitted)
 */
internal open class StateMachine<StateType : State, EnvironmentType : Environment>(
    resolver: StateMachineResolver<StateType>,
    val environment: EnvironmentType,
    private val dispatcherQueue: CoroutineDispatcher = Dispatchers.Default,
    private val executor: EffectExecutor = ConcurrentEffectExecutor(dispatcherQueue),
    initialState: StateType? = null
) : EventDispatcher {
    private val resolver = resolver.eraseToAnyResolver()

    // The current state of the state machine. We use a SharedFlow instead of a StateFlow so that emitted states are
    // not conflated, and all emitted states are received by subscribers
    private val _state = MutableSharedFlow<StateType>(
        replay = 1,
        extraBufferCapacity = 10,
        onBufferOverflow = BufferOverflow.DROP_OLDEST
    ).apply {
        tryEmit(initialState ?: resolver.defaultState)
    }
    val state = _state.asSharedFlow()

    // Manage consistency of internal state machine state and limits invocation of listeners to a minimum of one at a time.
    @OptIn(ExperimentalCoroutinesApi::class, DelicateCoroutinesApi::class)
    private val stateMachineContext = SupervisorJob() + newSingleThreadContext("StateMachineContext")
    private val stateMachineScope = CoroutineScope(stateMachineContext)

    /**
     * Get the current state, dispatching to the state machine context for the read.
     */
    suspend fun getCurrentState() = withContext(stateMachineContext) { state.first() }

    private fun setCurrentState(newState: StateType) {
        _state.tryEmit(newState)
    }

    /**
     * Send `event` to the StateMachine for resolution, and applies any effects and new states returned from the resolution.
     * @param event event to send to the system
     */
    override fun send(event: StateMachineEvent) {
        stateMachineScope.launch {
            process(event)
        }
    }

    /**
     * Resolver mutates the state based on current state and incoming event, and returns resolution with new state and
     * effects. If the state machine's state after resolving is not equal to the state before the event, update the
     * state machine's state and invoke listeners with the new state. Regardless of whether the state is new or not,
     * the state machine will execute any effects from the event resolution process.
     * @param event event to apply on current state for resolution
     */
    private suspend fun process(event: StateMachineEvent) {
        val currentState = getCurrentState()
        val resolution = resolver.resolve(currentState, event)
        if (currentState != resolution.newState) {
            setCurrentState(resolution.newState)
        }
        execute(resolution.actions)
    }

    /**
     * Execute resolution side effects asynchronously.
     */
    private fun execute(actions: List<Action>) {
        executor.execute(actions, this, environment)
    }
}
