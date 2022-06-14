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

/**
 * Description of the discrete status of a system that the State Machine models.
 *
 * ### Properties of a State
 *
 * States are immutable, mutually-exclusive, trees of value attributes:
 * - **Immutable**: States are not directly mutable. Instead, new states are resolved by applying a State Resolver
 * against the current value of the state and a new `StateMachineEvent` to return a new `State` value.
 * - **Mutually exclusive**: A system's State is exactly equivalent to the set of values that compose it. Thus, if two
 * State values have identical properties, they are themselves identical. From a practical standpoint, this means States
 * have value, not reference, semantics.
 * - **Trees**: Each State has its own set of attributes, and zero or more substates. The "local" attributes of a State
 * may be derived from the values of its substates, or they may evolve independently in response to Events. A State may
 * have at most one "parent" State.
 */
interface State {
    val type: String
        get() = this.javaClass.simpleName

    /**
     * States must be enums or data classes, or implement the below members.
     */
    override fun equals(other: Any?): Boolean
    override fun toString(): String
    override fun hashCode(): Int
}
