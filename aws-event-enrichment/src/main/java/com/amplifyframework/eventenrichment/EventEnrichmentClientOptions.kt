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
 */
data class EventEnrichmentClientOptions internal constructor(
    val autoSessionTracking: Boolean,
    val sessionTimeout: Duration
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

        /** Sets whether sessions are tracked automatically from the app lifecycle. */
        fun autoSessionTracking(value: Boolean) = apply { autoSessionTracking = value }

        /** Sets the background timeout before a new session starts. */
        fun sessionTimeout(value: Duration) = apply { sessionTimeout = value }

        /** Builds the [EventEnrichmentClientOptions] with configured values. */
        fun build() = EventEnrichmentClientOptions(
            autoSessionTracking = autoSessionTracking,
            sessionTimeout = sessionTimeout
        )
    }
}
