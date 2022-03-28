package com.amplifyframework.statemachine

typealias ActionClosure = (EventDispatcher, Environment) -> Unit

interface Action {
    val id: String
        get() = this.javaClass.name

    suspend fun execute(dispatcher: EventDispatcher, environment: Environment)

    companion object {
        fun basic(id: String, block: ActionClosure) = BasicAction(id, block)

        inline operator fun invoke(
            crossinline block: suspend (EventDispatcher, Environment) -> Unit
        ): Action {
            return object : Action {
                override suspend fun execute(
                    dispatcher: EventDispatcher,
                    environment: Environment
                ) {
                    block(dispatcher, environment)
                }
            }
        }
    }
}

class BasicAction(override var id: String, val block: ActionClosure) : Action {
    override suspend fun execute(dispatcher: EventDispatcher, environment: Environment) {
        block(dispatcher, environment)
    }
}
