/*
 * Copyright 2025 Amazon.com, Inc. or its affiliates. All Rights Reserved.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License").
 *  You may not use this file except in compliance with the License.
 *  A copy of the License is located at
 *
 *   http://aws.amazon.com/apache2.0
 *
 *  or in the "license" file accompanying this file. This file is distributed
 *  on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either
 *  express or implied. See the License for the specific language governing
 *  permissions and limitations under the License.
 */

package com.amplifyframework.aws.appsync.events

import com.amplifyframework.aws.appsync.core.AppSyncAuthorizer
import com.amplifyframework.aws.appsync.core.util.Logger
import java.util.concurrent.atomic.AtomicReference
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient

internal class EventsWebSocketProvider(
    private val eventsEndpoints: EventsEndpoints,
    private val authorizer: AppSyncAuthorizer,
    private val okHttpClient: OkHttpClient,
    private val json: Json,
    private val logger: Logger?
) {
    private val mutex = Mutex()
    private val _connectResult = AtomicReference<Result<EventsWebSocket>?>(null)
    private val _connectionInProgress = AtomicReference<Deferred<Result<EventsWebSocket>>?>(null)

    fun getExistingWebSocket(): EventsWebSocket? = _connectResult.get()?.getOrNull()

    suspend fun getConnectedWebSocket(): EventsWebSocket = getConnectedWebSocketResult().getOrThrow()

    private suspend fun getConnectedWebSocketResult(): Result<EventsWebSocket> = coroutineScope {
        // If connection is already established, return it
        mutex.withLock {
            val existingResult = _connectResult.get()
            val existingWebSocket = existingResult?.getOrNull()
            if (existingWebSocket != null) {
                if (existingWebSocket.isClosed.get()) {
                    _connectResult.set(null)
                } else {
                    return@coroutineScope existingResult
                }
            }
        }

        val deferredInProgressConnection = _connectionInProgress.get()
        if (deferredInProgressConnection != null && !deferredInProgressConnection.isCompleted) {
            return@coroutineScope deferredInProgressConnection.await()
        }

        mutex.withLock {
            val existingResultInLock = _connectResult.get()
            val existingWebSocket = existingResultInLock?.getOrNull()
            if (existingWebSocket != null) {
                if (existingWebSocket.isClosed.get()) {
                    _connectResult.set(null)
                } else {
                    return@coroutineScope existingResultInLock
                }
            }

            val deferredInProgressConnectionInLock = _connectionInProgress.get()
            if (deferredInProgressConnectionInLock != null && !deferredInProgressConnectionInLock.isCompleted) {
                return@coroutineScope deferredInProgressConnectionInLock.await()
            }

            val newDeferredInProgressConnection = async { attemptConnection() }
            _connectionInProgress.set(newDeferredInProgressConnection)
            val connectionResult = newDeferredInProgressConnection.await()
            _connectResult.set(connectionResult)
            _connectionInProgress.set(null)
            connectionResult
        }
    }

    private suspend fun attemptConnection(): Result<EventsWebSocket> {
        return try {
            val eventsWebSocket = EventsWebSocket(
                eventsEndpoints,
                authorizer,
                okHttpClient,
                json,
                logger
            )
            eventsWebSocket.connect()
            Result.success(eventsWebSocket)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}