package com.amplifyframework.recordcache

import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

sealed class FlushStrategy {
    data class Interval(
        /**
         * The interval between automatic flush operations.
         *
         * Shorter intervals mean more frequent API calls but lower latency.
         * Longer intervals reduce API calls but increase the time before
         * records are sent.
         *
         * Defaults to 30 seconds.
         */
        val interval: Duration = 30.seconds
    ) : FlushStrategy()
}
