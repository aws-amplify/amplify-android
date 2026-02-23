package com.amplifyframework.kinesis

import com.amplifyframework.recordcache.FlushStrategy
import kotlin.time.Duration.Companion.seconds

private const val DEFAULT_CACHE_SIZE_LIMIT_IN_BYTES = 500L * 1024 * 1024

/**
 * Configuration options for [KinesisDataStreams].
 *
 * @param cacheMaxBytes Maximum size of the local cache in bytes (default: 500MB)
 * @param maxRecords Maximum number of records to cache before forcing a flush (default: 500)
 * @param maxRetries Maximum number of retry attempts for failed records (default: 5)
 * @param flushStrategy Strategy for automatic flushing of cached records
 */
data class KinesisDataStreamsOptions internal constructor(
    val cacheMaxBytes: Long,
    val maxRecords: Int,
    val maxRetries: Int,
    val flushStrategy: FlushStrategy,
    val configureClient: KinesisClientConfigurationProvider? = null
) {
    companion object {
        /**
         * Creates a new builder for configuring [KinesisDataStreamsOptions].
         *
         * @return A new builder instance with default values
         */
        @JvmStatic
        fun builder() = Builder()

        @JvmSynthetic
        operator fun invoke(func: Builder.() -> Unit) = Builder().apply(func).build()

        /**
         * Creates [KinesisDataStreamsOptions] with default values.
         *
         * @return Options with default values
         */
        @JvmStatic
        fun defaults() = builder().build()
    }

    /**
     * Builder for [KinesisDataStreamsOptions].
     */
    class Builder internal constructor() {
        var cacheMaxBytes: Long = DEFAULT_CACHE_SIZE_LIMIT_IN_BYTES
            @JvmSynthetic set

        var maxRecords: Int = 500
            @JvmSynthetic set

        var maxRetries: Int = 5
            @JvmSynthetic set

        var flushStrategy: FlushStrategy = FlushStrategy.Interval(30.seconds)
            @JvmSynthetic set

        var configureClient: KinesisClientConfigurationProvider? = null
            @JvmSynthetic set

        /**
         * Sets the maximum cache size in bytes.
         *
         * @param value Maximum cache size in bytes
         * @return This builder instance
         */
        fun cacheMaxBytes(value: Long) = apply { cacheMaxBytes = value }

        /**
         * Sets the maximum number of records to cache.
         *
         * @param value Maximum number of records
         * @return This builder instance
         */
        fun maxRecords(value: Int) = apply { maxRecords = value }

        /**
         * Sets the maximum number of retry attempts.
         *
         * @param value Maximum retry attempts
         * @return This builder instance
         */
        fun maxRetries(value: Int) = apply { maxRetries = value }

        /**
         * Sets the flush strategy for automatic record flushing.
         *
         * @param value Flush strategy
         * @return This builder instance
         */
        fun flushStrategy(value: FlushStrategy) = apply { flushStrategy = value }
        fun configureClient(value: KinesisClientConfigurationProvider?) = apply { configureClient = value }

        /**
         * Builds the [KinesisDataStreamsOptions] with configured values.
         *
         * @return Configured options instance
         */
        fun build() = KinesisDataStreamsOptions(cacheMaxBytes, maxRecords, maxRetries, flushStrategy, configureClient)
    }
}
