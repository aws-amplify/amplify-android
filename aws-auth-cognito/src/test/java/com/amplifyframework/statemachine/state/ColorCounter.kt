package com.amplifyframework.statemachine.state

import com.amplifyframework.statemachine.*

data class ColorCounter(val color: Color, val counter: Counter, val hasTriggered: Boolean) :
    State {
    override val type = "${color.type}.${counter.type}"

    class Resolver : StateMachineResolver<ColorCounter> {
        override val defaultState = ColorCounter(Color.red, Counter(0), false)

        override fun resolve(oldState: ColorCounter, event: StateMachineEvent): StateResolution<ColorCounter> {
            var builder = Builder(oldState)
            var actions: MutableList<Action> = mutableListOf()
            when (event) {
                is Counter.Event -> {
                    val resolution = Counter.Resolver().resolve(oldState.counter, event)
                    builder.counter = resolution.newState
                    actions = resolution.actions.toMutableList()
                }
                is Color.Event -> {
                    val resolution = Color.Resolver().resolve(oldState.color, event)
                    builder.color = resolution.newState
                    actions = resolution.actions.toMutableList()
                }
                else -> StateResolution.from(defaultState)
            }
            resolveHasTriggered(builder)
            return StateResolution(builder.build(), actions)
        }

        private fun resolveHasTriggered(builder: Builder) {
            if (builder.color == Color.yellow && builder.counter.value == 2) {
                builder.hasTriggered = true
            }
        }
    }

    class Builder(
        var color: Color = Color.red,
        var counter: Counter = Counter(0),
        var hasTriggered: Boolean = false
    ) :
        com.amplifyframework.statemachine.Builder<ColorCounter> {
        constructor(from: ColorCounter) : this(from.color, from.counter, from.hasTriggered)

        override fun build() = ColorCounter(color, counter, hasTriggered)
    }
}