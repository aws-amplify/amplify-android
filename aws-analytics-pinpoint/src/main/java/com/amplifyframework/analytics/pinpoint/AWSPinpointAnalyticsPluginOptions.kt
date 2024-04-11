/*
 * Copyright 2024 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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

package com.amplifyframework.analytics.pinpoint

import com.amplifyframework.analytics.pinpoint.AWSPinpointAnalyticsPluginConfiguration.DEFAULT_AUTO_FLUSH_INTERVAL

/**
 * Options that can be specified to fine-tune the behavior of the Pinpoint Analytics Plugin.
 */
data class AWSPinpointAnalyticsPluginOptions internal constructor(
    /**
     * The interval between sends of queued analytics events, in milliseconds
     */
    val autoFlushEventsInterval: Long
) {
    companion object {
        /**
         * Create a new [Builder] instance
         */
        @JvmStatic
        fun builder() = Builder()

        /**
         * Create an [AWSPinpointAnalyticsPluginOptions] instance
         */
        @JvmSynthetic
        operator fun invoke(func: Builder.() -> Unit) = Builder().apply(func).build()

        internal fun defaults() = builder().build()
    }

    /**
     * Builder API for constructing [AWSPinpointAnalyticsPluginOptions] instances
     */
    class Builder internal constructor() {
        /**
         * Set the interval between sends of queed analytics events, in milliseconds
         */
        var autoFlushEventsInterval: Long = DEFAULT_AUTO_FLUSH_INTERVAL
            @JvmSynthetic set

        /**
         * Set the interval between sends of queed analytics events, in milliseconds
         */
        fun autoFlushEventsInterval(value: Long) = apply { autoFlushEventsInterval = value }

        internal fun build() = AWSPinpointAnalyticsPluginOptions(
            autoFlushEventsInterval = autoFlushEventsInterval
        )
    }
}
