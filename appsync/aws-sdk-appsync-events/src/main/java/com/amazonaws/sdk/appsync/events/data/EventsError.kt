/*
 * Copyright 2025 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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
package com.amazonaws.sdk.appsync.events.data

import com.amazonaws.sdk.appsync.events.data.WebSocketMessage.Received
import kotlinx.serialization.Serializable

@Serializable
internal data class EventsErrors(val errors: List<EventsError>) : Received()

internal fun EventsErrors.toEventsException(fallbackMessage: String? = null): EventsException {
    return errors.firstOrNull()?.toEventsException(fallbackMessage) ?: EventsException.unknown(fallbackMessage)
}

@Serializable
internal data class EventsError(val errorType: String, val message: String? = null)

// fallback message is only used if WebSocketError didn't provide a message
internal fun EventsError.toEventsException(fallbackMessage: String? = null): EventsException {
    val message = this.message ?: fallbackMessage
    return when (errorType) {
        "UnauthorizedException" -> UnauthorizedException(message)
        "BadRequestException" -> BadRequestException(message)
        "MaxSubscriptionsReachedError" -> MaxSubscriptionsReachedException(message)
        "LimitExceededError" -> RateLimitExceededException(message)
        "ResourceNotFound" -> ResourceNotFoundException(message)
        "UnsupportedOperation" -> UnsupportedOperationException(message)
        "InvalidInputError" -> InvalidInputException(message)
        else -> EventsException(message = "$errorType: $message")
    }
}
