/*
 * Copyright 2026 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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
package com.amazonaws.appsync

import kotlinx.coroutines.Deferred
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * Hands out the client's single shared WebSocket, opening it on first use.
 *
 * The connection is lazy: a client that never subscribes never opens a socket. It is shared because
 * AppSync multiplexes every subscription over one connection, so opening one per subscription would
 * both waste connections and hit the API's connection limit sooner.
 *
 * Concurrency is the whole point of this class. Two coroutines calling `subscribe()` at the same
 * moment must not open two sockets, and a caller arriving while a connection is still being
 * established must wait for that attempt rather than starting a second one.
 *
 * A failed attempt is **not** cached: the next caller retries. A closed connection is discarded and
 * replaced, which is what lets a client recover after `MaxSubscriptionsReached` or an idle timeout
 * without the caller managing connection state.
 *
 * @param connectionFactory Creates a socket. Injectable so tests need no real endpoint.
 */
internal class AppSyncWebSocketProvider(
    private val connectionFactory: () -> AppSyncWebSocket
) {
    private val mutex = Mutex()
    private var connected: AppSyncWebSocket? = null
    private var inProgress: Deferred<Result<AppSyncWebSocket>>? = null

    /** The live socket, or null if none has been established. Never opens one. */
    val existing: AppSyncWebSocket?
        get() = connected?.takeUnless { it.isClosed }

    /**
     * Returns the shared socket, connecting if necessary.
     *
     * @throws AppSyncException if the connection attempt fails. The failure is not cached.
     */
    suspend fun connection(): AppSyncWebSocket = coroutineScope {
        // Fast path: a live connection needs no lock contention.
        existing?.let { return@coroutineScope it }

        // Join an attempt already running, without holding the lock while it completes — otherwise a
        // slow connect would serialize every waiting subscriber behind the mutex.
        inProgress?.takeUnless { it.isCompleted }?.let {
            return@coroutineScope it.await().getOrElse { error -> throw error }
        }

        val attempt = mutex.withLock {
            // Re-check under the lock: another coroutine may have finished between the checks above
            // and acquiring it. Without this, both would start an attempt.
            existing?.let { return@coroutineScope it }
            inProgress?.takeUnless { it.isCompleted }
                ?: async { attemptConnection() }.also { inProgress = it }
        }

        val result = attempt.await()

        mutex.withLock {
            if (inProgress === attempt) inProgress = null
            result.getOrNull()?.let { connected = it }
        }

        result.getOrElse { error -> throw error }
    }

    /**
     * Closes the shared socket, if there is one, and forgets it.
     *
     * @param cause Why it is being closed, or null for a clean shutdown.
     */
    suspend fun close(cause: AppSyncException? = null) {
        val socket = mutex.withLock {
            connected.also {
                connected = null
                inProgress = null
            }
        }
        socket?.disconnect(cause)
    }

    private suspend fun attemptConnection(): Result<AppSyncWebSocket> = try {
        Result.success(connectionFactory().also { it.connect() })
    } catch (error: Exception) {
        // Surfaced as a typed failure so every caller joined to this attempt sees the same reason.
        Result.failure(AppSyncException.from(error))
    }
}
