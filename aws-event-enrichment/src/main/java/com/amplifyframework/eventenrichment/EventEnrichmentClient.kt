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
@file:OptIn(kotlin.time.ExperimentalTime::class)

package com.amplifyframework.eventenrichment

import android.app.Application
import android.content.Context
import androidx.annotation.VisibleForTesting
import com.amplifyframework.annotations.ExperimentalAmplifyApi
import com.amplifyframework.eventenrichment.clientid.ClientIdProvider
import com.amplifyframework.eventenrichment.clientid.SharedPreferencesClientIdProvider
import com.amplifyframework.eventenrichment.exception.EventEnrichmentClosedException
import com.amplifyframework.eventenrichment.exception.EventEnrichmentException
import com.amplifyframework.eventenrichment.lifecycle.AndroidLifecycleObserver
import com.amplifyframework.eventenrichment.lifecycle.LifecycleObserver
import com.amplifyframework.eventenrichment.metadata.AndroidDeviceMetadataProvider
import com.amplifyframework.eventenrichment.metadata.AppMetadata
import com.amplifyframework.eventenrichment.metadata.DeviceMetadata
import com.amplifyframework.eventenrichment.metadata.DeviceMetadataProvider
import com.amplifyframework.eventenrichment.metadata.SdkMetadata
import com.amplifyframework.eventenrichment.session.CoroutineTimeoutScheduler
import com.amplifyframework.eventenrichment.session.Session
import com.amplifyframework.eventenrichment.session.SessionBoundary
import com.amplifyframework.eventenrichment.session.SessionBoundaryListener
import com.amplifyframework.eventenrichment.session.SessionManager
import com.amplifyframework.foundation.logging.AmplifyLogging
import com.amplifyframework.foundation.logging.Logger
import com.amplifyframework.foundation.result.Result
import java.util.UUID
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock
import kotlin.time.Clock
import kotlin.time.Instant
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.launch

/**
 * Client for recording enriched analytics events.
 *
 * Collects device, app, session, and SDK metadata and produces [EnrichedEvent]
 * instances that serialize to a structured analytics JSON envelope. The client
 * is transport-agnostic; provide an [EventSink] to forward events to Kinesis,
 * Firehose, or any custom destination. Session starts and stops are sent to
 * the sink as `_session.start` and `_session.stop` events. Every event goes
 * through one queue, so the sink receives them one at a time, in the order they
 * happened, on a background dispatcher; exceptions it throws are caught and
 * logged.
 *
 * The client auto-resolves the device metadata and the persistent client id
 * (using a key shared across Amplify packages) from the supplied [Context].
 * Callers only provide the [appId] and [SdkMetadata].
 *
 * Example usage:
 * ```kotlin
 * val client = EventEnrichmentClient(
 *     context = applicationContext,
 *     appId = "my-app-id",
 *     sdkMetadata = SdkMetadata(name = "amplify-android", version = "2.0.0")
 * )
 *
 * val result = client.record("button_clicked")
 * if (result is Result.Success) println(result.data.toJson())
 *
 * client.close()
 * ```
 *
 * @param context Android context used to resolve device metadata, the client
 *   id, and app lifecycle callbacks.
 * @param appId Application identifier stamped on every event.
 * @param sdkMetadata SDK metadata stamped on every event.
 * @param options Configuration options with sensible defaults.
 * @param sink Optional transport for enriched events.
 * @param appMetadata Optional full application metadata. When provided, its
 *   [AppMetadata.appId] must equal [appId].
 */
@ExperimentalAmplifyApi
class EventEnrichmentClient @VisibleForTesting internal constructor(
    private val appMetadata: AppMetadata,
    private val deviceMetadata: DeviceMetadata,
    private val sdkMetadata: SdkMetadata,
    private val clientId: String,
    private val sink: EventSink?,
    sessionManager: SessionManager,
    private val autoSessionTracking: Boolean,
    private val application: Application?,
    private val clock: () -> Instant,
    private val generateEventId: () -> String,
    private val emitSessionEvents: Boolean = true,
    initialUserId: String? = null,
    initialGlobalAttributes: Map<String, String> = emptyMap(),
    initialGlobalMetrics: Map<String, Double> = emptyMap(),
    private val lock: ReentrantLock = ReentrantLock(),
    private val scope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Default),
    private val sinkDispatcher: CoroutineDispatcher = Dispatchers.IO
) {
    /**
     * Creates a client that auto-resolves device metadata and the client id
     * from [context].
     */
    @JvmOverloads
    constructor(
        context: Context,
        appId: String,
        sdkMetadata: SdkMetadata,
        options: EventEnrichmentClientOptions = EventEnrichmentClientOptions.defaults(),
        sink: EventSink? = null,
        appMetadata: AppMetadata? = null,
        deviceMetadataProvider: DeviceMetadataProvider = AndroidDeviceMetadataProvider(),
        clientIdProvider: ClientIdProvider = SharedPreferencesClientIdProvider(context)
    ) : this(
        context = context,
        appId = appId,
        sdkMetadata = sdkMetadata,
        options = options,
        sink = sink,
        appMetadata = appMetadata,
        deviceMetadataProvider = deviceMetadataProvider,
        clientIdProvider = clientIdProvider,
        lock = ReentrantLock(),
        scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    )

    /**
     * Bridging constructor that shares one [lock] and one [scope] between the
     * client and the [SessionManager]'s timeout scheduler.
     */
    private constructor(
        context: Context,
        appId: String,
        sdkMetadata: SdkMetadata,
        options: EventEnrichmentClientOptions,
        sink: EventSink?,
        appMetadata: AppMetadata?,
        deviceMetadataProvider: DeviceMetadataProvider,
        clientIdProvider: ClientIdProvider,
        lock: ReentrantLock,
        scope: CoroutineScope
    ) : this(
        appMetadata = resolveAppMetadata(appId, appMetadata),
        deviceMetadata = deviceMetadataProvider.getDeviceMetadata(),
        sdkMetadata = sdkMetadata,
        clientId = clientIdProvider.getClientId(),
        sink = sink,
        sessionManager = SessionManager(
            appId = appId,
            sessionTimeout = options.sessionTimeout,
            lock = lock,
            scheduler = CoroutineTimeoutScheduler(scope)
        ),
        autoSessionTracking = options.autoSessionTracking,
        application = context.applicationContext as? Application,
        clock = Clock.System::now,
        generateEventId = { UUID.randomUUID().toString() },
        emitSessionEvents = options.emitSessionEvents,
        initialUserId = options.initialUserId,
        initialGlobalAttributes = options.initialGlobalAttributes,
        initialGlobalMetrics = options.initialGlobalMetrics,
        lock = lock,
        scope = scope
    )

    private val logger: Logger = AmplifyLogging.logger<EventEnrichmentClient>()

    /**
     * The session manager for this client. Exposed for advanced integrations
     * and platform lifecycle observers.
     */
    internal val sessionManager: SessionManager = sessionManager

    private var lifecycleObserver: LifecycleObserver? = null

    @Volatile
    private var userId: String? = initialUserId

    private val globalFields = GlobalFieldsManager().apply {
        initialGlobalAttributes.forEach(::addAttribute)
        initialGlobalMetrics.forEach(::addMetric)
    }

    @Volatile
    private var closed = false

    // Single ordered queue for every sink send. Producers enqueue under the lock.
    private val sendQueue = Channel<EnrichedEvent>(Channel.UNLIMITED)

    /** Whether the client has been closed. */
    val isClosed: Boolean
        get() = closed

    init {
        this.sessionManager.boundaryListener = SessionBoundaryListener(::onSessionBoundary)
        scope.launch(sinkDispatcher) {
            for (event in sendQueue) deliver(event)
            // close() closed the queue and everything queued has been sent.
            scope.cancel()
        }
        if (autoSessionTracking) {
            this.sessionManager.startSession()
            application?.let {
                lifecycleObserver = AndroidLifecycleObserver(it, this.sessionManager)
            }
        }
    }

    /**
     * Records an event and returns the enriched result.
     *
     * Returns [Result.Failure] with [EventEnrichmentClosedException] if the
     * client has been closed. Attributes are String-valued and metrics are
     * Double-valued; no length or count caps are applied.
     *
     * When a [sink] is configured, the event is queued behind earlier events,
     * including the start of a session this call begins, and sent
     * asynchronously on [sinkDispatcher]; any exception the sink throws is
     * caught and logged as a warning.
     *
     * @param eventType Type of the event.
     * @param attributes Per-event attributes merged over the global attributes.
     * @param metrics Per-event metrics merged over the global metrics.
     */
    @JvmOverloads
    fun record(
        eventType: String,
        attributes: Map<String, String> = emptyMap(),
        metrics: Map<String, Double> = emptyMap()
    ): Result<EnrichedEvent, EventEnrichmentException> {
        // Serialized against close() and session mutations via the shared lock,
        // so the queue order matches the call order. The sink runs off the lock.
        val event = lock.withLock {
            if (closed) return Result.Failure(EventEnrichmentClosedException())
            buildEvent(eventType, sessionManager.ensureActiveSession(), attributes, metrics).also(::enqueue)
        }
        logger.verbose { "Recorded event: $eventType" }
        return Result.Success(event)
    }

    /** Starts a new session manually. No-op once the client is closed. */
    fun startSession() = ifOpen { sessionManager.startSession() }

    /**
     * Stops the current session. No-op if already stopped or once the client is
     * closed. Returning to the foreground does not start a new session after
     * this; [record] or [startSession] does.
     */
    fun stopSession() = ifOpen { sessionManager.stopSession() }

    /** Called when the app moves to the background. No-op once the client is closed. */
    fun handleAppPaused() = ifOpen { sessionManager.handleAppPaused() }

    /** Called when the app returns to the foreground. No-op once the client is closed. */
    fun handleAppResumed() = ifOpen { sessionManager.handleAppResumed() }

    /**
     * Sets the user identifier stamped on subsequent events. Use
     * [EventEnrichmentClientOptions.initialUserId] to have the launch session
     * event carry it.
     */
    fun setUserId(userId: String?) {
        this.userId = userId
    }

    /** Adds a global attribute stamped on every subsequent event. */
    fun addGlobalAttribute(key: String, value: String) = globalFields.addAttribute(key, value)

    /** Removes a global attribute. */
    fun removeGlobalAttribute(key: String) = globalFields.removeAttribute(key)

    /** Adds a global metric stamped on every subsequent event. */
    fun addGlobalMetric(key: String, value: Double) = globalFields.addMetric(key, value)

    /** Removes a global metric. */
    fun removeGlobalMetric(key: String) = globalFields.removeMetric(key)

    /**
     * Releases resources and stops session tracking. The final `_session.stop`
     * is queued behind every event already queued, and nothing is sent after
     * it. Returns without waiting for those sends; the client's coroutine scope
     * is cancelled once they finish. The client cannot be reused after closing.
     */
    fun close() {
        // Serialized against record() via the shared lock so a concurrent
        // record cannot observe a half-closed client.
        lock.withLock {
            closed = true
            lifecycleObserver?.dispose()
            lifecycleObserver = null
            // Stop the session to record its end, then drop it so no stale
            // session is readable after close.
            sessionManager.stopSession()
            sessionManager.clearSession()
            // The consumer sends what is queued, then cancels the scope.
            sendQueue.close()
        }
        logger.info { "Client closed" }
    }

    // Called inside the session lock, so boundaries queue in transition order.
    private fun onSessionBoundary(boundary: SessionBoundary, session: Session) {
        if (!emitSessionEvents) return
        enqueue(buildEvent(boundary.eventType, session))
    }

    private fun buildEvent(
        eventType: String,
        session: Session,
        attributes: Map<String, String> = emptyMap(),
        metrics: Map<String, Double> = emptyMap()
    ) = EnrichedEvent(
        eventId = generateEventId(),
        eventType = eventType,
        eventTimestamp = clock().toEpochMilliseconds(),
        session = session,
        attributes = globalFields.attributes + attributes,
        metrics = globalFields.metrics + metrics,
        device = deviceMetadata,
        app = appMetadata,
        sdk = sdkMetadata,
        clientId = clientId,
        userId = userId
    )

    // Must be called under the lock. Fails silently once close() closed the queue.
    private fun enqueue(event: EnrichedEvent) {
        if (sink != null) sendQueue.trySend(event)
    }

    private fun deliver(event: EnrichedEvent) {
        try {
            sink?.send(event)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Throwable) {
            // Caught broadly so one failing send cannot stop the queue.
            logger.warn(e) { "Event sink failed for event: ${event.eventType}" }
        }
    }

    // Held under the shared lock so a session call cannot run after close().
    private inline fun ifOpen(block: () -> Unit) {
        lock.withLock { if (!closed) block() }
    }

    private companion object {
        /**
         * Resolves the effective [AppMetadata], asserting that a caller-supplied
         * [AppMetadata.appId] agrees with [appId] when both are provided.
         */
        fun resolveAppMetadata(appId: String, appMetadata: AppMetadata?): AppMetadata {
            if (appMetadata != null) {
                require(appMetadata.appId == appId) {
                    "appMetadata.appId (\"${appMetadata.appId}\") does not match the appId " +
                        "\"$appId\". When both are provided they must be the same value."
                }
                return appMetadata
            }
            return AppMetadata(appId = appId)
        }
    }
}
