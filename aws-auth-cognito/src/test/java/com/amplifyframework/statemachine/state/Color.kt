package com.amplifyframework.statemachine.state

import java.util.Date

enum class Color : com.amplifyframework.statemachine.State {
    red, green, blue, yellow;

    override val type = name

    companion object {
        var next = Event.next
    }

    enum class Event : com.amplifyframework.statemachine.StateMachineEvent {
        next;

        override val id: String
            get() = "Color.Event.${this.type}"
        override val type = name
        override val time: Date? = null
    }

    class Resolver : com.amplifyframework.statemachine.StateMachineResolver<Color> {
        override val defaultState = red

        override fun resolve(
            oldState: Color,
            event: com.amplifyframework.statemachine.StateMachineEvent
        ): com.amplifyframework.statemachine.StateResolution<Color> {
            val index = Color.values().indexOf(oldState)
            val newIndex = (index + 1) % Color.values().size
            val newState = Color.values()[newIndex]
            return com.amplifyframework.statemachine.StateResolution.from(newState)
        }
    }
}
