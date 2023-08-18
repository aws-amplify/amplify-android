/*
 * Copyright 2023 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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

package com.amplifyframework.util

import java.util.concurrent.Callable
import java.util.concurrent.CompletableFuture
import java.util.concurrent.ExecutorService
import java.util.concurrent.Future
import java.util.concurrent.TimeUnit

/**
 * An implementation of [ExecutorService] that can run tasks synchronously either automatically or on demand
 */
class MockExecutorService(var autoRunTasks: Boolean = true) : ExecutorService {
    val queue = mutableListOf<Runnable>()
    var numTasksQueued = 0

    override fun execute(command: Runnable) { submit(command) }

    override fun <T : Any> submit(task: Callable<T>): Future<T> {
        val future = CompletableFuture<T>()
        queueTask {
            future.complete(task.call())
        }
        return future
    }

    override fun <T : Any> submit(task: Runnable, result: T): Future<T> {
        val future = CompletableFuture<T>()
        queueTask {
            task.run()
            future.complete(result)
        }
        return future
    }
    override fun submit(task: Runnable): Future<*> {
        val future = CompletableFuture<Unit>()
        queueTask {
            task.run()
            future.complete(Unit)
        }
        return future
    }

    // Remaining API is not implemented
    override fun shutdown() {}
    override fun shutdownNow(): List<Runnable> = queue
    override fun isShutdown() = false
    override fun isTerminated() = false
    override fun awaitTermination(timeout: Long, unit: TimeUnit) = false
    override fun <T : Any> invokeAll(tasks: MutableCollection<out Callable<T>>) = emptyList<Future<T>>()
    override fun <T : Any> invokeAll(
        tasks: MutableCollection<out Callable<T>>,
        timeout: Long,
        unit: TimeUnit
    ) = emptyList<Future<T>>()
    override fun <T : Any> invokeAny(tasks: MutableCollection<out Callable<T>>): T? = null
    override fun <T : Any> invokeAny(
        tasks: MutableCollection<out Callable<T>>,
        timeout: Long,
        unit: TimeUnit?
    ): T? = null

    fun runNext() {
        val task = queue.removeFirstOrNull()
        task?.run()
    }

    private fun queueTask(runnable: Runnable) {
        numTasksQueued++
        if (autoRunTasks) {
            runnable.run()
        } else {
            queue.add(runnable)
        }
    }
}
