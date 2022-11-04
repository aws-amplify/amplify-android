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

package com.amplifyframework.auth.cognito.operations

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

internal interface Task<T> {
    suspend operator fun invoke(): T

    companion object {
        operator fun <T> invoke(block: suspend () -> T) = object : Task<T> {
            override suspend fun invoke() = block()
        }
    }
}

private data class Message<T>(val task: Task<T>, val job: CompletableDeferred<T>)

internal class TaskQueue {
    private val job = Job()
    private val scope = CoroutineScope(job)
    private val channel = Channel<Message<*>>()

    init {
        scope.launch {
            for (msg in channel) {
                if (isActive) {
                    if (msg.job.isCompleted) continue

                    try {
                        val result = withContext((msg as Message<Any?>).job) { msg.task() }
                        msg.job.complete(result)
                    } catch (e: Exception) {
                        msg.job.completeExceptionally(e)
                    }
                }
            }
        }
    }

    suspend fun <T> sync(block: suspend () -> T): T {
        return syncTask { Task { block() } }
    }

    suspend fun <T> syncTask(task: () -> Task<T>): T {
        val job = CompletableDeferred<T>(this.job)
        channel.send(Message(task.invoke(), job))
        return job.await()
    }
}
