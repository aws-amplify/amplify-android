package com.amplifyframework.kinesis

import aws.sdk.kotlin.services.kinesis.KinesisClient
import com.amplifyframework.recordcache.FlushStrategy
import kotlin.time.Duration.Companion.seconds

private const val DEFAULT_CACHE_SIZE_LIMIT_IN_BYTES = 500L * 1024 * 1024

/**
 * Configuration options for [AmplifyKinesisClient].
 *
 * @param cacheMaxBytes Maximum size of the local cache in bytes (default: 500MB)
 * @param maxRetries Maximum number of retry attempts for failed records (default: 5)
 * @param flushStrategy Strategy for automatic flushing of cached records
 */
data class AmplifyKinesisClientOptions internal constructor(
    val cacheMaxBytes: Long,
    val maxRetries: Int,
    val flushStrategy: FlushStrategy,
    val configureClient: KinesisClientConfigurationProvider? = null
) {
    companion object {
        /**
         * Creates a new builder for configuring [AmplifyKinesisClientOptions].
         *
         * @return A new builder instance with default values
         */
        @JvmStatic
        fun builder() = Builder()

        @JvmSynthetic
        operator fun invoke(func: Builder.() -> Unit) = Builder().apply(func).build()

        /**
         * Creates [AmplifyKinesisClientOptions] with default values.
         *
         * @return Options with default values
         */
        @JvmStatic
        fun defaults() = builder().build()
    }

    /**
     * Builder for [AmplifyKinesisClientOptions].
     */
    class Builder internal constructor() {
        var cacheMaxBytes: Long = DEFAULT_CACHE_SIZE_LIMIT_IN_BYTES
            @JvmSynthetic set

        var maxRetries: Int = 5
            @JvmSynthetic set

        var flushStrategy: FlushStrategy = FlushStrategy.Interval(30.seconds)
            @JvmSynthetic set

        var configureClient: KinesisClientConfigurationProvider? = null
            @JvmSynthetic private set

        /**
         * Sets the maximum cache size in bytes.
         *
         * @param value Maximum cache size in bytes
         * @return This builder instance
         */
        fun cacheMaxBytes(value: Long) = apply { cacheMaxBytes = value }

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

        /**
         * Sets a custom configuration provider for the underlying [KinesisClient].
         *
         * @param value Configuration provider, or null to use defaults
         * @return This builder instance
         */
        fun configureClient(value: KinesisClientConfigurationProvider?) = apply { configureClient = value }

        /**
         * Configures the underlying [KinesisClient] using a DSL-style lambda.
         *
         * Example (Kotlin):
         * ```kotlin
         * val options = AmplifyKinesisClientOptions {
         *     maxRetries = 5
         *     configureClient {
         *         retryStrategy {
         *             maxAttempts = 10
         *         }
         *     }
         * }
         * ```
         *
         * @param value Lambda with receiver on [KinesisClient.Config.Builder]
         * @return This builder instance
         */
        @JvmSynthetic
        fun configureClient(value: KinesisClient.Config.Builder.() -> Unit) = apply {
            configureClient = KinesisClientConfigurationProvider { it.value() }
        }

        /**
         * Builds the [AmplifyKinesisClientOptions] with configured values.
         *
         * @return Configured options instance
         */
        fun build() = AmplifyKinesisClientOptions(
            cacheMaxBytes,
            maxRetries,
            flushStrategy,
            configureClient
        )
    }
}
