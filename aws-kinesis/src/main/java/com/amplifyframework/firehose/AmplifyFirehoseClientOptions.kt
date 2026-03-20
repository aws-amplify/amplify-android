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
package com.amplifyframework.firehose

import aws.sdk.kotlin.services.firehose.FirehoseClient
import com.amplifyframework.recordcache.FlushStrategy
import kotlin.time.Duration.Companion.seconds

private const val DEFAULT_CACHE_SIZE_LIMIT_IN_BYTES = 5L * 1024 * 1024

/**
 * Configuration options for [AmplifyFirehoseClient].
 *
 * @param cacheMaxBytes Maximum size of the local cache in bytes (default: 5MB)
 * @param maxRetries Maximum number of retry attempts for failed records (default: 5)
 * @param flushStrategy Strategy for automatic flushing of cached records
 */
data class AmplifyFirehoseClientOptions internal constructor(
    val cacheMaxBytes: Long,
    val maxRetries: Int,
    val flushStrategy: FlushStrategy,
    val configureClient: FirehoseClientConfigurationProvider? = null
) {
    companion object {
        /**
         * Creates a new builder for configuring [AmplifyFirehoseClientOptions].
         *
         * @return A new builder instance with default values
         */
        @JvmStatic
        fun builder() = Builder()

        @JvmSynthetic
        operator fun invoke(func: Builder.() -> Unit) = Builder().apply(func).build()

        /**
         * Creates [AmplifyFirehoseClientOptions] with default values.
         *
         * @return Options with default values
         */
        @JvmStatic
        fun defaults() = builder().build()
    }

    /**
     * Builder for [AmplifyFirehoseClientOptions].
     */
    class Builder internal constructor() {
        var cacheMaxBytes: Long = DEFAULT_CACHE_SIZE_LIMIT_IN_BYTES
            @JvmSynthetic set

        var maxRetries: Int = 5
            @JvmSynthetic set

        var flushStrategy: FlushStrategy = FlushStrategy.Interval(30.seconds)
            @JvmSynthetic set

        var configureClient: FirehoseClientConfigurationProvider? = null
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
         * Sets a custom configuration provider for the underlying [FirehoseClient].
         *
         * @param value Configuration provider, or null to use defaults
         * @return This builder instance
         */
        fun configureClient(value: FirehoseClientConfigurationProvider?) = apply { configureClient = value }

        /**
         * Configures the underlying [FirehoseClient] using a DSL-style lambda.
         *
         * Example (Kotlin):
         * ```kotlin
         * val options = AmplifyFirehoseClientOptions {
         *     maxRetries = 5
         *     configureClient {
         *         retryStrategy {
         *             maxAttempts = 10
         *         }
         *     }
         * }
         * ```
         *
         * @param value Lambda with receiver on [FirehoseClient.Config.Builder]
         * @return This builder instance
         */
        @JvmSynthetic
        fun configureClient(value: FirehoseClient.Config.Builder.() -> Unit) = apply {
            configureClient = FirehoseClientConfigurationProvider { it.value() }
        }

        /**
         * Builds the [AmplifyFirehoseClientOptions] with configured values.
         *
         * @return Configured options instance
         */
        fun build() = AmplifyFirehoseClientOptions(
            cacheMaxBytes,
            maxRetries,
            flushStrategy,
            configureClient
        )
    }
}
