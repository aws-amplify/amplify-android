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

package com.amplifyframework.statemachine.state

import com.amplifyframework.statemachine.Action
import com.amplifyframework.statemachine.Environment
import com.amplifyframework.statemachine.State
import com.amplifyframework.statemachine.StateMachine
import com.amplifyframework.statemachine.StateMachineEvent
import com.amplifyframework.statemachine.StateMachineResolver
import com.amplifyframework.statemachine.StateResolution
import java.util.Date

internal class CounterStateMachine(
    resolver: StateMachineResolver<Counter>,
    environment: CounterEnvironment
) :
    StateMachine<Counter, CounterEnvironment>(resolver, environment) {
    constructor() : this(Counter.Resolver(), CounterEnvironment.empty)

    companion object {
        fun logging() = CounterStateMachine(Counter.Resolver().logging(), CounterEnvironment.empty)
    }
}

internal data class Counter(val value: Int) : State {
    override val type = "Counter"

    class Event(
        override val id: String,
        val eventType: EventType,
        override val time: Date? = null,
        val data: Any? = null
    ) : StateMachineEvent {

        sealed class EventType {
            object Increment : EventType()
            object Decrement : EventType()
            data class AdjustBy(val value: Int) : EventType()
            data class Set(val value: Int) : EventType()
            data class IncrementAndDoActions(val actions: List<Action>) : EventType()
        }

        override val type = when (eventType) {
            EventType.Increment -> "increment"
            EventType.Decrement -> "decrement"
            is EventType.AdjustBy -> "adjustBy.${eventType.value}"
            is EventType.Set -> "set.${eventType.value}"
            is EventType.IncrementAndDoActions -> "incrementAndDoActions"
        }
    }

    class Resolver : StateMachineResolver<Counter> {
        override val defaultState = Counter(0)

        override fun resolve(oldState: Counter, event: StateMachineEvent): StateResolution<Counter> {
            val resolution = when (event) {
                event as? Event -> resolveState(oldState, event)
                else -> StateResolution(defaultState)
            }
            return resolution
        }

        companion object {
            fun resolveState(oldState: Counter, event: Event): StateResolution<Counter> {
                val actions: MutableList<Action> = mutableListOf()
                var newValue = oldState.value
                when (event.eventType) {
                    is Event.EventType.Increment -> newValue += 1
                    is Event.EventType.Decrement -> newValue = -1
                    is Event.EventType.AdjustBy -> newValue += event.eventType.value
                    is Event.EventType.Set -> newValue = event.eventType.value
                    is Event.EventType.IncrementAndDoActions -> {
                        newValue += 1
                        actions.addAll(event.eventType.actions)
                    }
                }
                val newState = Counter(newValue)
                return StateResolution(newState, actions)
            }
        }
    }
}

class CounterEnvironment : Environment {
    companion object {
        val empty = CounterEnvironment()
    }
}
