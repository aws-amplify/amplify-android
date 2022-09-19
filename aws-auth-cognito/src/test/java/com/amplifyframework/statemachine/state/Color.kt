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
