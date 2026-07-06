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
package com.amplifyframework.eventenrichment

import android.app.Application
import android.content.Context
import androidx.annotation.VisibleForTesting
import com.amplifyframework.annotations.InternalAmplifyApi
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
import com.amplifyframework.eventenrichment.session.SessionManager
import com.amplifyframework.eventenrichment.session.SessionState
import com.amplifyframework.foundation.logging.AmplifyLogging
import com.amplifyframework.foundation.logging.Logger
import com.amplifyframework.foundation.result.Result
import java.time.Instant
import java.util.UUID

/**
 * Client for recording enriched analytics events.
 *
 * Collects device, app, session, and SDK metadata and produces [EnrichedEvent]
 * instances that serialize to a structured analytics JSON envelope. The client
 * is transport-agnostic; provide an [EventSink] to forward events to Kinesis,
 * Firehose, or any custom destination.
 *
 * The client auto-resolves the device metadata (via [AndroidDeviceMetadataProvider])
 * and the persistent client id (via [SharedPreferencesClientIdProvider], using a
 * key shared across Amplify packages) from the supplied [Context]. Callers only
 * provide the [appId] and [SdkMetadata].
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
@OptIn(InternalAmplifyApi::class)
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
    private val generateEventId: () -> String
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
        appMetadata = resolveAppMetadata(appId, appMetadata),
        deviceMetadata = deviceMetadataProvider.getDeviceMetadata(),
        sdkMetadata = sdkMetadata,
        clientId = clientIdProvider.getClientId(),
        sink = sink,
        sessionManager = SessionManager(appId = appId, sessionTimeout = options.sessionTimeout),
        autoSessionTracking = options.autoSessionTracking,
        application = context.applicationContext as? Application,
        clock = Instant::now,
        generateEventId = { UUID.randomUUID().toString() }
    )

    private val logger: Logger = AmplifyLogging.logger<EventEnrichmentClient>()

    /**
     * The session manager for this client. Exposed for advanced integrations
     * and platform lifecycle observers.
     */
    @get:VisibleForTesting
    val sessionManager: SessionManager = sessionManager

    private var lifecycleObserver: LifecycleObserver? = null

    @Volatile
    private var userId: String? = null

    private val globalFields = GlobalFieldsManager()

    @Volatile
    private var closed = false

    /** Whether the client has been closed. */
    val isClosed: Boolean
        get() = closed

    init {
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
        if (closed) return Result.Failure(EventEnrichmentClosedException())

        // A stopped session is still exposed by the manager for inspection, so
        // start a fresh one instead of stamping the stopped session (which
        // carries a stop_timestamp) onto a new event.
        if (sessionManager.session == null || sessionManager.state == SessionState.STOPPED) {
            sessionManager.startSession()
        }

        val mergedAttributes = globalFields.attributes + attributes
        val mergedMetrics = globalFields.metrics + metrics

        val event = EnrichedEvent(
            eventId = generateEventId(),
            eventType = eventType,
            eventTimestamp = clock().toEpochMilli(),
            session = requireNotNull(sessionManager.session) { "Session must be active while recording" },
            attributes = mergedAttributes,
            metrics = mergedMetrics,
            device = deviceMetadata,
            app = appMetadata,
            sdk = sdkMetadata,
            clientId = clientId,
            userId = userId
        )

        sink?.send(event)
        logger.verbose { "Recorded event: $eventType" }
        return Result.Success(event)
    }

    /** Starts a new session manually. */
    fun startSession() = sessionManager.startSession()

    /** Stops the current session. */
    fun stopSession() = sessionManager.stopSession()

    /** Called when the app moves to the background. */
    fun handleAppPaused() = sessionManager.handleAppPaused()

    /** Called when the app returns to the foreground. */
    fun handleAppResumed() = sessionManager.handleAppResumed()

    /** Sets the user identifier stamped on subsequent events. */
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
     * Releases resources and stops session tracking. The client cannot be
     * reused after closing.
     */
    fun close() {
        closed = true
        lifecycleObserver?.dispose()
        lifecycleObserver = null
        // Stop the session to record its end, then drop it so no stale session
        // is readable after close.
        sessionManager.stopSession()
        sessionManager.clearSession()
        logger.info { "Client closed" }
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
