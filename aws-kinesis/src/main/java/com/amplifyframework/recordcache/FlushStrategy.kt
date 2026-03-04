package com.amplifyframework.recordcache

import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

/**
 * Strategy for automatically flushing cached records to Kinesis.
 */
sealed class FlushStrategy {
    /**
     * Flush records at regular time intervals.
     *
     * @param interval Time between automatic flush operations
     */
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

    /**
     * Disable automatic flushing. Records must be flushed manually by calling flush().
     */
    data object None : FlushStrategy()
}
