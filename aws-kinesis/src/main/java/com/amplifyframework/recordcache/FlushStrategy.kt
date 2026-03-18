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
