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
package com.amplifyframework.cloudwatch

import aws.sdk.kotlin.services.cloudwatchlogs.CloudWatchLogsClient
import com.amplifyframework.annotations.ExperimentalAmplifyApi
import com.amplifyframework.foundation.config.SdkClientConfigurationProvider

/** Provides custom configuration for the underlying [CloudWatchLogsClient]. */
typealias CloudWatchLogsClientConfigurationProvider =
    SdkClientConfigurationProvider<CloudWatchLogsClient.Config.Builder>

private const val DEFAULT_LOCAL_STORE_MAX_SIZE_IN_MB = 5

/**
 * Configuration options for [AmplifyCloudWatchClient].
 *
 * @param logGroupName The CloudWatch log group to send log events to
 * @param localStoreMaxSizeInMB Maximum size of the on-device log store, in megabytes (default: 5)
 * @param flushStrategy Strategy for automatically flushing cached log entries
 * @param loggingConstraints Constraints controlling which messages are captured
 * @param configureClient Optional customization of the underlying [CloudWatchLogsClient]
 */
@ExperimentalAmplifyApi
data class AmplifyCloudWatchClientOptions internal constructor(
    val logGroupName: String,
    val localStoreMaxSizeInMB: Int,
    val flushStrategy: FlushStrategy,
    val loggingConstraints: LoggingConstraints,
    val configureClient: CloudWatchLogsClientConfigurationProvider? = null
) {
    companion object {
        /**
         * Creates a new builder for configuring [AmplifyCloudWatchClientOptions].
         *
         * @return A new builder instance with default values
         */
        @JvmStatic
        fun builder() = Builder()

        @JvmSynthetic
        operator fun invoke(func: Builder.() -> Unit) = Builder().apply(func).build()
    }

    /**
     * Builder for [AmplifyCloudWatchClientOptions].
     *
     * [logGroupName] is required and must be set before [build] is called.
     */
    class Builder internal constructor() {
        var logGroupName: String? = null
            @JvmSynthetic set

        var localStoreMaxSizeInMB: Int = DEFAULT_LOCAL_STORE_MAX_SIZE_IN_MB
            @JvmSynthetic set

        var flushStrategy: FlushStrategy = FlushStrategy.Interval()
            @JvmSynthetic set

        var loggingConstraints: LoggingConstraints = LoggingConstraints()
            @JvmSynthetic set

        var configureClient: CloudWatchLogsClientConfigurationProvider? = null
            @JvmSynthetic private set

        /** Sets the CloudWatch log group to send log events to. Required. */
        fun logGroupName(value: String) = apply { logGroupName = value }

        /** Sets the maximum size of the on-device log store, in megabytes. */
        fun localStoreMaxSizeInMB(value: Int) = apply { localStoreMaxSizeInMB = value }

        /** Sets the strategy for automatically flushing cached log entries. */
        fun flushStrategy(value: FlushStrategy) = apply { flushStrategy = value }

        /** Sets the constraints controlling which messages are captured. */
        fun loggingConstraints(value: LoggingConstraints) = apply { loggingConstraints = value }

        /** Sets a custom configuration provider for the underlying [CloudWatchLogsClient]. */
        fun configureClient(value: CloudWatchLogsClientConfigurationProvider?) = apply { configureClient = value }

        /**
         * Configures the underlying [CloudWatchLogsClient] using a DSL-style lambda.
         *
         * @param value Lambda with receiver on [CloudWatchLogsClient.Config.Builder]
         * @return This builder instance
         */
        @JvmSynthetic
        fun configureClient(value: CloudWatchLogsClient.Config.Builder.() -> Unit) = apply {
            configureClient = CloudWatchLogsClientConfigurationProvider { it.value() }
        }

        /**
         * Builds the [AmplifyCloudWatchClientOptions] with the configured values.
         *
         * @return Configured options instance
         * @throws IllegalArgumentException if [logGroupName] was not set
         */
        fun build(): AmplifyCloudWatchClientOptions {
            require(localStoreMaxSizeInMB > 0) {
                "localStoreMaxSizeInMB must be greater than 0, was $localStoreMaxSizeInMB"
            }
            return AmplifyCloudWatchClientOptions(
                requireNotNull(logGroupName) { "logGroupName is required" },
                localStoreMaxSizeInMB,
                flushStrategy,
                loggingConstraints,
                configureClient
            )
        }
    }
}
