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
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.newFixedThreadPoolContext

internal typealias StateChangeListenerToken = UUID
internal typealias OnSubscribedCallback = () -> Unit

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
    executor: EffectExecutor? = null,
    concurrentQueue: CoroutineDispatcher? = null,
    initialState: StateType? = null
) : EventDispatcher {
    private val resolver = resolver.eraseToAnyResolver()
    private val executor: EffectExecutor
    private var currentState = initialState ?: resolver.defaultState

    private val dispatcherQueue: CoroutineDispatcher

    /**
     * Manage consistency of internal state machine state and limits invocation of listeners to a minimum of one at a time.
     */
    private val operationQueue = newFixedThreadPoolContext(1, "Single threaded dispatcher")
    private val exceptionHandler = CoroutineExceptionHandler { _, exception ->
        println("CoroutineExceptionHandler got $exception")
    }

    /**
     * TODO: add coroutine exception handler if required.
     */
    private val stateMachineScope = Job() + operationQueue // + exceptionHandler

    // weak wrapper ??
    private val subscribers: MutableMap<StateChangeListenerToken, (StateType) -> Unit>

    // atomic value ??
    private val pendingCancellations: MutableSet<StateChangeListenerToken>

    init {
        val resolvedQueue = concurrentQueue ?: Dispatchers.Default
        dispatcherQueue = resolvedQueue
        this.executor = executor ?: ConcurrentEffectExecutor(resolvedQueue)

        subscribers = mutableMapOf()
        pendingCancellations = mutableSetOf()
    }

    /**
     * Start listening to state changes updates. Asynchronously invoke listener on a background queue with the current state.
     * Both `listener` and `onSubscribe` will be invoked on a background queue.
     * @param listener listener to be invoked on state changes
     * @param onSubscribe callback to invoke when subscription is complete
     * @return token that can be used to unsubscribe the listener
     */
    fun listen(listener: (StateType) -> Unit, onSubscribe: OnSubscribedCallback?): StateChangeListenerToken {
        val token = UUID.randomUUID()
        GlobalScope.launch(stateMachineScope) {
            addSubscription(token, listener, onSubscribe)
        }
        return token
    }

    /**
     * Stop listening to state changes updates. Register a pending cancellation if a new event comes in between the time
     * `cancel` is called and the time the pending cancellation is processed, the event will not be dispatched to the listener.
     * @param token identifies the listener to be removed
     */
    fun cancel(token: StateChangeListenerToken) {
        pendingCancellations.add(token)
        GlobalScope.launch(stateMachineScope) {
            removeSubscription(token)
        }
    }

    /**
     * Invoke `completion` with the current state
     * @param completion callback to invoke with the current state
     */
    fun getCurrentState(completion: (StateType) -> Unit) {
        GlobalScope.launch(stateMachineScope) {
            completion(currentState)
        }
    }

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
        GlobalScope.launch(dispatcherQueue) {
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
        GlobalScope.launch(stateMachineScope) {
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
