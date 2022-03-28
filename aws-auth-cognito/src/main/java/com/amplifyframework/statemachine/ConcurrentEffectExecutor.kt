package com.amplifyframework.statemachine

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

class ConcurrentEffectExecutor(private val dispatcherQueue: CoroutineDispatcher) : EffectExecutor {
    override fun execute(
        actions: List<Action>,
        eventDispatcher: EventDispatcher,
        environment: Environment
    ) {
        actions.forEach { action ->
            GlobalScope.launch(dispatcherQueue) {
                action.execute(eventDispatcher, environment)
            }
        }
    }
}
