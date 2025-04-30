/*
 * Copyright 2025 Amazon.com, Inc. or its affiliates. All Rights Reserved.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License").
 *  You may not use this file except in compliance with the License.
 *  A copy of the License is located at
 *
 *   http://aws.amazon.com/apache2.0
 *
 *  or in the "license" file accompanying this file. This file is distributed
 *  on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either
 *  express or implied. See the License for the specific language governing
 *  permissions and limitations under the License.
 */
package com.amplifyframework.aws.appsync.events.data

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Sealed Result type of an event(s) publish call.
 * PublishResult.Success = The publish call was successfully processed by the service
 * PublishResult.Failure = The publish call did not succeed
 *
 */
sealed class PublishResult {

    /**
     * Represents a successful response, which may contain both
     * successful and failed events. A Success case indicates the publish
     * itself succeeded, not that all events were processed successfully.
     *
     * @property successfulEvents list of events successfully processed by AWS AppSync.
     * @property failedEvents list of events that AWS AppSync failed to process.
     * @property status of the publish call.
     *  Successful = All events published successfully
     *  Failed = All events failed to publish
     *  PartialSuccess = Mix of successful and failed events. Check event indexes to determine individual states.     */
    @Serializable
    data class Response internal constructor(
        @SerialName("successful") val successfulEvents: List<SuccessfulEvent>,
        @SerialName("failed") val failedEvents: List<FailedEvent>
    ) : PublishResult() {

        /**
         * Contains identifying information of an event AWS AppSync failed to process.
         */
        sealed class Status {
            data object Successful : Status()
            data object Failed : Status()
            data object PartialSuccess : Status()
        }

        val status: Status
            get() = when {
                successfulEvents.isNotEmpty() && failedEvents.isNotEmpty() -> Status.PartialSuccess
                failedEvents.isNotEmpty() -> Status.Failed
                else -> Status.Successful
            }
    }

    /**
     * Represents a failed response where the publish was not successful
     */
    data class Failure internal constructor(val error: EventsException) : PublishResult()
}

/**
 * Contains identifying information of a successfully processed event.
 *
 * @property identifier identifier of event used for logging purposes.
 * @property index of the event as it was sent in the publish.
 */
@Serializable
data class SuccessfulEvent internal constructor(
    val identifier: String,
    val index: Int
)

/**
 * Contains identifying information of an event AWS AppSync failed to process.
 *
 * @property identifier identifier of event used for logging purposes.
 * @property index of the event as it was sent in the publish.
 * @property errorCode for the failed event.
 * @property errorMessage for the failed event.
 */
@Serializable
data class FailedEvent internal constructor(
    val identifier: String,
    val index: Int,
    @SerialName("code") val errorCode: Int? = null,
    @SerialName("message") val errorMessage: String? = null
)
