package com.amplifyframework.statemachine

import kotlinx.coroutines.*
import java.util.*

typealias StateChangeListenerToken = UUID
typealias OnSubscribedCallback = () -> Unit

/**
 * State Machine operates on data/enum classes of State implementation and
 * Resolvers of type State.
 * @implements EventDispatcher
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

    private val operationQueue = newFixedThreadPoolContext(1, "Single threaded dispatcher")
    private val exceptionHandler = CoroutineExceptionHandler { _, exception ->
        println("CoroutineExceptionHandler got $exception")
    }
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

    fun listen(listener: (StateType) -> Unit, onSubscribe: OnSubscribedCallback?): StateChangeListenerToken {
        val token = UUID.randomUUID()
        GlobalScope.launch(stateMachineScope) {
            addSubscription(token, listener, onSubscribe)
        }
        return token
    }

    fun cancel(token: StateChangeListenerToken) {
        pendingCancellations.add(token)
        GlobalScope.launch(stateMachineScope) {
            removeSubscription(token)
        }
    }

    fun getCurrentState(completion: (StateType) -> Unit) {
        GlobalScope.launch(stateMachineScope) {
            completion(currentState)
        }
    }

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

    private fun removeSubscription(token: StateChangeListenerToken) {
        pendingCancellations.remove(token)
        subscribers.remove(token)
    }

    override fun send(event: StateMachineEvent) {
        GlobalScope.launch(stateMachineScope) {
            process(event)
        }
    }

    private fun notifySubscribers(
        subscriber: Map.Entry<StateChangeListenerToken, (StateType) -> Unit>,
        newState: StateType
    ): Boolean {
        val token = subscriber.key
        if (pendingCancellations.contains(token)) return false
        subscriber.value(newState)
        return true
    }

    private fun process(event: StateMachineEvent) {
        val resolution = resolver.resolve(currentState, event)
        if (currentState != resolution.newState) {
            currentState = resolution.newState
            val subscribersToRemove = subscribers.filter {
                !notifySubscribers(it, resolution.newState)
            }
            subscribersToRemove.forEach {
                subscribers.remove(it.key)
            }
        }
        execute(resolution.actions)
    }

    private fun execute(actions: List<Action>) {
        executor.execute(actions, this, environment)
    }
}
