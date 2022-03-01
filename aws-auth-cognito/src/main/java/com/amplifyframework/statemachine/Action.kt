package com.amplifyframework.statemachine

typealias ActionClosure = (EventDispatcher, Environment) -> Unit

interface Action {
    val id: String
        get() = this.javaClass.name

    suspend fun execute(dispatcher: EventDispatcher, environment: Environment)

    companion object {
        fun basic(id: String, closure: ActionClosure) = BasicAction(id, closure)
    }
}

class BasicAction(override var id: String, val closure: ActionClosure) : Action {
    override suspend fun execute(dispatcher: EventDispatcher, environment: Environment) {
        closure(dispatcher, environment)
    }
}