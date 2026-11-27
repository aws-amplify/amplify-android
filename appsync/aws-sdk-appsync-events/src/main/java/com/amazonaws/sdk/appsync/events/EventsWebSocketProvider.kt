/*
 * Copyright 2025 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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

package com.amazonaws.sdk.appsync.events

import com.amazonaws.sdk.appsync.core.AppSyncAuthorizer
import com.amazonaws.sdk.appsync.core.LoggerProvider
import java.util.concurrent.atomic.AtomicReference
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.cancel
import kotlinx.coroutines.isActive
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient

internal class EventsWebSocketProvider(
    private val eventsEndpoints: EventsEndpoints,
    private val authorizer: AppSyncAuthorizer,
    private val okHttpClient: OkHttpClient,
    private val json: Json,
    private val loggerProvider: LoggerProvider?,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
) {
    private val mutex = Mutex()
    private val connectionResultReference = AtomicReference<Result<EventsWebSocket>?>(null)
    private val connectionInProgressReference = AtomicReference<Deferred<Result<EventsWebSocket>>?>(null)

    // A connection attempt belongs to the provider, not to whichever caller happens to trigger it, so it is
    // launched on this scope rather than the caller's. That way cancelling one caller cannot cancel an attempt
    // that other callers are awaiting. The scope is cancelled by [close] and recreated for the next attempt.
    private var connectionScope = newConnectionScope()

    val existingWebSocket: EventsWebSocket?
        get() = connectionResultReference.get()?.getOrNull()

    suspend fun getConnectedWebSocket(): EventsWebSocket = getConnectedWebSocketResult().getOrThrow()

    private suspend fun getConnectedWebSocketResult(): Result<EventsWebSocket> {
        // If a connection is already established, return it.
        openConnectionResultOrNull()?.let { return it }

        // If an attempt is already in flight, join it without taking the lock or blocking it.
        connectionInProgressReference.get()?.takeUnless { it.isCompleted }?.let { return it.await() }

        // Otherwise resolve which attempt to await under the lock, then await it outside the lock so the
        // network connect does not hold the mutex against other callers.
        val deferredConnection = mutex.withLock {
            openConnectionResultOrNull()?.let { return it }

            connectionInProgressReference.get()?.takeUnless { it.isCompleted } ?: startConnection()
        }

        return deferredConnection.await()
    }

    private fun startConnection(): Deferred<Result<EventsWebSocket>> {
        if (!connectionScope.isActive) {
            connectionScope = newConnectionScope()
        }
        return connectionScope.async {
            // Record the result from within the attempt so it is stored even if the caller that triggered
            // the attempt is cancelled while awaiting it. Completion happens after this runs, so callers that
            // observe the attempt as completed also observe the stored result.
            attemptConnection().also { connectionResultReference.set(it) }
        }.also { connectionInProgressReference.set(it) }
    }

    /**
     * Returns the established connection result if one is open, otherwise clears any closed connection and
     * returns null. Must be called while holding [mutex] when a null result may lead to a new attempt.
     */
    private fun openConnectionResultOrNull(): Result<EventsWebSocket>? {
        val existingResult = connectionResultReference.get()
        val existingWebSocket = existingResult?.getOrNull() ?: return null
        return if (existingWebSocket.isClosed) {
            connectionResultReference.set(null)
            null
        } else {
            existingResult
        }
    }

    /**
     * Cancels any in-progress connection attempt and releases the provider's connection scope. A subsequent
     * call to [getConnectedWebSocket] starts a fresh attempt on a new scope.
     */
    fun close() {
        connectionScope.cancel()
        connectionInProgressReference.set(null)
    }

    private fun newConnectionScope() = CoroutineScope(ioDispatcher + SupervisorJob())

    private suspend fun attemptConnection(): Result<EventsWebSocket> = try {
        val eventsWebSocket = EventsWebSocket(
            eventsEndpoints,
            authorizer,
            okHttpClient,
            json,
            loggerProvider,
            ioDispatcher
        )
        eventsWebSocket.connect()
        Result.success(eventsWebSocket)
    } catch (e: Exception) {
        Result.failure(e)
    }
}
