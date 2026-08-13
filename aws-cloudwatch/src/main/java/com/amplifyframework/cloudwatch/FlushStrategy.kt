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

import com.amplifyframework.annotations.ExperimentalAmplifyApi
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

/**
 * Strategy for flushing cached log events to CloudWatch.
 *
 * There is intentionally no "none" strategy — automatic flushing is turned off via
 * [AmplifyCloudWatchClient.disable] instead.
 */
@ExperimentalAmplifyApi
sealed class FlushStrategy {
    /**
     * Automatically flush at a regular interval.
     *
     * @param interval Time between automatic flush operations. Defaults to 60 seconds.
     */
    data class Interval(val interval: Duration = 60.seconds) : FlushStrategy()
}
