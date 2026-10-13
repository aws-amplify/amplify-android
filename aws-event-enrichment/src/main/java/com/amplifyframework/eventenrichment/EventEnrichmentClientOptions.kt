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

import com.amplifyframework.annotations.ExperimentalAmplifyApi
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

private val DEFAULT_SESSION_TIMEOUT = 5.seconds

/**
 * Configuration options for [EventEnrichmentClient].
 *
 * @param autoSessionTracking Whether to automatically start a session at
 *   construction and drive it from the app lifecycle. When `true` (the
 *   default), a session starts as soon as the client is constructed and follows
 *   app foreground/background transitions. When `false`, no session is started
 *   at construction; the first `record()` call lazily starts one, and manual
 *   session and lifecycle calls still work. It only opts out of eagerly
 *   starting a session up front.
 * @param sessionTimeout Duration the app can remain backgrounded before a new
 *   session starts. Defaults to 5 seconds.
 * @param initialUserId User id stamped on events from construction, including
 *   the launch `_session.start`. Defaults to null.
 * @param initialGlobalAttributes Global attributes applied at construction,
 *   so the launch `_session.start` carries them. Defaults to empty.
 * @param initialGlobalMetrics Global metrics applied at construction, so the
 *   launch `_session.start` carries them. Defaults to empty.
 */
@ExperimentalAmplifyApi
data class EventEnrichmentClientOptions internal constructor(
    val autoSessionTracking: Boolean,
    val sessionTimeout: Duration,
    val initialUserId: String?,
    val initialGlobalAttributes: Map<String, String>,
    val initialGlobalMetrics: Map<String, Double>
) {
    companion object {
        /** Creates a new builder for configuring [EventEnrichmentClientOptions]. */
        @JvmStatic
        fun builder() = Builder()

        @JvmSynthetic
        operator fun invoke(func: Builder.() -> Unit) = Builder().apply(func).build()

        /** Creates [EventEnrichmentClientOptions] with default values. */
        @JvmStatic
        fun defaults() = builder().build()
    }

    /** Builder for [EventEnrichmentClientOptions]. */
    class Builder internal constructor() {
        var autoSessionTracking: Boolean = true
            @JvmSynthetic set

        var sessionTimeout: Duration = DEFAULT_SESSION_TIMEOUT
            @JvmSynthetic set

        var initialUserId: String? = null
            @JvmSynthetic set

        var initialGlobalAttributes: Map<String, String> = emptyMap()
            @JvmSynthetic set

        var initialGlobalMetrics: Map<String, Double> = emptyMap()
            @JvmSynthetic set

        /** Sets whether sessions are tracked automatically from the app lifecycle. */
        fun autoSessionTracking(value: Boolean) = apply { autoSessionTracking = value }

        /** Sets the background timeout before a new session starts. */
        fun sessionTimeout(value: Duration) = apply { sessionTimeout = value }

        /** Sets the user id stamped on events from construction. */
        fun initialUserId(value: String?) = apply { initialUserId = value }

        /** Sets the global attributes applied at construction. */
        fun initialGlobalAttributes(value: Map<String, String>) = apply { initialGlobalAttributes = value }

        /** Sets the global metrics applied at construction. */
        fun initialGlobalMetrics(value: Map<String, Double>) = apply { initialGlobalMetrics = value }

        /** Builds the [EventEnrichmentClientOptions] with configured values. */
        fun build() = EventEnrichmentClientOptions(
            autoSessionTracking = autoSessionTracking,
            sessionTimeout = sessionTimeout,
            initialUserId = initialUserId,
            initialGlobalAttributes = initialGlobalAttributes,
            initialGlobalMetrics = initialGlobalMetrics
        )
    }
}
