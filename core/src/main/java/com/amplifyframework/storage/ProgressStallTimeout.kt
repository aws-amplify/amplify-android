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
package com.amplifyframework.storage

/**
 * Strategy for cancelling uploads when progress stops advancing.
 *
 * Configure a default on the storage plugin configuration and optionally override per upload via the
 * upload options (`StorageUploadFileOptions` / `StorageUploadInputStreamOptions` implementations).
 *
 * When enabled, the upload is cancelled and the `onError` callback receives a [StorageException]
 * whose `cause` is a [ProgressStallTimeoutException] if progress does not advance within the
 * configured interval. The default is [Disabled], which preserves existing behavior.
 */
sealed class ProgressStallTimeout {

    /**
     * Duration in seconds used by the stall timer, or `0` when disabled.
     */
    abstract val secondsForStallTimer: Long

    /**
     * Do not cancel uploads when progress stalls.
     *
     * Named `Disabled` (not `None`) so that, when used as a nullable option, it does not collide
     * with "option not supplied, defer to plugin default" semantics.
     */
    object Disabled : ProgressStallTimeout() {
        override val secondsForStallTimer: Long = 0
    }

    /**
     * Cancel the upload if progress does not advance within this interval.
     *
     * Values of `0` or less are treated the same as [Disabled] — the stall timer is not started.
     *
     * @property seconds Stall interval in seconds.
     */
    data class Interval(val seconds: Long) : ProgressStallTimeout() {
        override val secondsForStallTimer: Long = if (seconds > 0) seconds else 0
    }

    companion object {
        /**
         * Factory for [Disabled], convenient for Java consumers.
         */
        @JvmStatic
        fun disabled(): ProgressStallTimeout = Disabled

        /**
         * Factory for [Interval], convenient for Java consumers.
         *
         * @param seconds Stall interval in seconds.
         */
        @JvmStatic
        fun interval(seconds: Long): ProgressStallTimeout = Interval(seconds)
    }
}
