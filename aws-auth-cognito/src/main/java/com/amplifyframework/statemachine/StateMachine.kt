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

import java.util.UUID
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.newSingleThreadContext
import kotlinx.coroutines.withContext

internal typealias OnSubscribedCallback = () -> Unit

internal class StateChangeListenerToken private constructor(val uuid: UUID) {
    constructor() : this(UUID.randomUUID())
    override fun equals(other: Any?) = other is StateChangeListenerToken && other.uuid == uuid
    override fun hashCode() = uuid.hashCode()
}

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
 * @param concurrentQueue event queue or thread pool for effect executor and subscription callback
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

    // The current state of the state machine. Consumers can collect or read the current state from the read-only StateFlow
    private val _state = MutableStateFlow(initialState ?: resolver.defaultState)
    val state = _state.asStateFlow()

    // Private accessor for the current state. Although this is thread-safe to access/mutate, we still want to limit
    // read/write to the single-threaded stateMachineContext for consistency
    private var currentState: StateType
        get() = _state.value
        set(value) {
            _state.value = value
        }

    // Manage consistency of internal state machine state and limits invocation of listeners to a minimum of one at a time.
    @OptIn(ExperimentalCoroutinesApi::class, DelicateCoroutinesApi::class)
    private val stateMachineContext = SupervisorJob() + newSingleThreadContext("StateMachineContext")
    private val stateMachineScope = CoroutineScope(stateMachineContext)

    // weak wrapper ??
    private val subscribers: MutableMap<StateChangeListenerToken, (StateType) -> Unit> = mutableMapOf()

    // atomic value ??
    private val pendingCancellations: MutableSet<StateChangeListenerToken> = mutableSetOf()

    /**
     * Start listening to state changes updates. Asynchronously invoke listener on a background queue with the current state.
     * Both `listener` and `onSubscribe` will be invoked on a background queue.
     * @param listener listener to be invoked on state changes
     * @param onSubscribe callback to invoke when subscription is complete
     * @return token that can be used to unsubscribe the listener
     */
    @Deprecated("Collect from state flow instead")
    fun listen(token: StateChangeListenerToken, listener: (StateType) -> Unit, onSubscribe: OnSubscribedCallback?) {
        stateMachineScope.launch {
            addSubscription(token, listener, onSubscribe)
        }
    }

    /**
     * Stop listening to state changes updates. Register a pending cancellation if a new event comes in between the time
     * `cancel` is called and the time the pending cancellation is processed, the event will not be dispatched to the listener.
     * @param token identifies the listener to be removed
     */
    @Deprecated("Collect from state flow instead")
    fun cancel(token: StateChangeListenerToken) {
        pendingCancellations.add(token)
        stateMachineScope.launch {
            removeSubscription(token)
        }
    }

    /**
     * Invoke `completion` with the current state
     * @param completion callback to invoke with the current state
     */
    @Deprecated("Use suspending version instead")
    fun getCurrentState(completion: (StateType) -> Unit) {
        stateMachineScope.launch {
            completion(currentState)
        }
    }

    /**
     * Get the current state, dispatching to the state machine context for the read.
     */
    suspend fun getCurrentState() = withContext(stateMachineContext) { currentState }

    /**
     * Register a listener.
     * @param token token, which will be retained in the subscribers map
     * @param listener listener to invoke when the state has changed
     * @param onSubscribe callback to invoke when subscription is complete
     */
    private fun addSubscription(
        token: StateChangeListenerToken,
        listener: (StateType) -> Unit,
        onSubscribe: OnSubscribedCallback?
    ) {
        if (pendingCancellations.contains(token)) return
        val currentState = this.currentState
        subscribers[token] = listener
        onSubscribe?.invoke()
        stateMachineScope.launch(dispatcherQueue) {
            listener.invoke(currentState)
        }
    }

    /**
     * Unregister a listener.
     * @param token token of the listener to remove
     */
    private fun removeSubscription(token: StateChangeListenerToken) {
        pendingCancellations.remove(token)
        subscribers.remove(token)
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
     * Notify all the listeners with the new state.
     * @param subscriber pair containing the subscriber token and listener
     * @param newState new state to be sent
     * @return true if the subscriber was notified, false if the token was null or a cancellation was pending
     */
    private fun notifySubscribers(
        subscriber: Map.Entry<StateChangeListenerToken, (StateType) -> Unit>,
        newState: StateType
    ): Boolean {
        val token = subscriber.key
        if (pendingCancellations.contains(token)) return false
        subscriber.value(newState)
        return true
    }

    /**
     * Resolver mutates the state based on current state and incoming event, and returns resolution with new state and
     * effects. If the state machine's state after resolving is not equal to the state before the event, update the
     * state machine's state and invoke listeners with the new state. Regardless of whether the state is new or not,
     * the state machine will execute any effects from the event resolution process.
     * @param event event to apply on current state for resolution
     */
    private fun process(event: StateMachineEvent) {
        val resolution = resolver.resolve(currentState, event)
        if (currentState != resolution.newState) {
            currentState = resolution.newState
            val subscribersToRemove = subscribers.filter { !notifySubscribers(it, resolution.newState) }
            subscribersToRemove.forEach { subscribers.remove(it.key) }
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
