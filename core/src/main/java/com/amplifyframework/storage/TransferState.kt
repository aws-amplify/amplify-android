/**
 * Copyright 2015-2019 Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License").
 * You may not use this file except in compliance with the License.
 * A copy of the License is located at
 * <p>
 * http://aws.amazon.com/apache2.0
 * <p>
 * or in the "license" file accompanying this file. This file is distributed
 * on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either
 * express or implied. See the License for the specific language governing
 * permissions and limitations under the License.
 */

package com.amplifyframework.storage

/**
 * The current state of a transfer. A transfer is initially in WAITING state
 * when added. It will turn into IN_PROGRESS once it starts. Customers can pause
 * or cancel the transfer when needed and turns it into PAUSED or CANCELED state
 * respectively. Finally the transfer will either succeed as COMPLETED or fail
 * as FAILED. The other enum values are internal use only.
 */
enum class TransferState {
    /**
     * This state represents a transfer that has been queued, but has not yet
     * started
     */
    WAITING,

    /**
     * This state represents a transfer that is currently uploading or
     * downloading data
     */
    IN_PROGRESS,

    /**
     * This state represents a transfer that is paused
     */
    PAUSED,

    /**
     * This state represents a transfer that has been resumed and queued for
     * execution, but has not started to actively transfer data
     */
    RESUMED_WAITING,

    /**
     * This state represents a transfer that is completed
     */
    COMPLETED,

    /**
     * This state represents a transfer that is canceled
     */
    CANCELED,

    /**
     * This state represents a transfer that has failed
     */
    FAILED,

    /**
     * This state represents a transfer that is a completed part of a multi-part
     * upload. This state is primarily used internally and there should be no
     * need to use this state.
     */
    PART_COMPLETED,

    /**
     * This state represents a transfer that has been requested to cancel, but
     * the service processing transfers has not yet fulfilled this request. This
     * state is primarily used internally and there should be no need to use
     * this state.
     */
    PENDING_CANCEL,

    /**
     * This state represents a transfer that has been requested to pause by the
     * client, but the service processing transfers has not yet fulfilled this
     * request. This state is primarily used internally and there should be no
     * need to use this state.
     */
    PENDING_PAUSE,

    /**
     * This is an internal value used to detect if the current transfer is in an
     * unknown state
     */
    UNKNOWN;

    companion object {
        @JvmStatic
        fun getState(state: String): TransferState {
            return try {
                valueOf(state)
            } catch (exception: IllegalArgumentException) {
                UNKNOWN
            }
        }

        @JvmStatic
        fun isStarted(state: TransferState?) = setOf(
            WAITING,
            IN_PROGRESS,
            RESUMED_WAITING
        ).contains(state)

        @JvmStatic
        fun isInTerminalState(state: TransferState?) =
            setOf(COMPLETED, CANCELED, FAILED).contains(state)

        @JvmStatic
        fun isPaused(state: TransferState?) =
            setOf(PENDING_PAUSE, PAUSED).contains(state)

        @JvmStatic
        fun isCancelled(state: TransferState?) =
            setOf(PENDING_CANCEL, CANCELED).contains(state)
    }
}
