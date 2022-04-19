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

typealias ActionClosure = suspend (EventDispatcher, Environment) -> Unit
typealias BoundActionClosure<T> = suspend T.(String?, EventDispatcher) -> Unit

interface Action {
    val id: String
        get() = this.javaClass.simpleName

    suspend fun execute(dispatcher: EventDispatcher, environment: Environment)

    companion object {
        fun basic(id: String, block: ActionClosure) = BasicAction(id, block)

        inline operator fun invoke(name: String? = null, crossinline block: ActionClosure) = object : Action {
            override val id = name ?: super.id
            override suspend fun execute(dispatcher: EventDispatcher, environment: Environment) {
                block(dispatcher, environment)
            }
        }

        inline operator fun <EnvType : Environment> invoke(
            name: String? = null,
            crossinline block: BoundActionClosure<EnvType>
        ) = object : Action {
            override val id = name ?: super.id
            override suspend fun execute(dispatcher: EventDispatcher, environment: Environment) {
                val safeEnv = environment as EnvType
                safeEnv.block(id, dispatcher)
            }
        }
    }
}

class BasicAction(override var id: String, val block: ActionClosure) : Action {
    override suspend fun execute(dispatcher: EventDispatcher, environment: Environment) {
        block(dispatcher, environment)
    }
}
