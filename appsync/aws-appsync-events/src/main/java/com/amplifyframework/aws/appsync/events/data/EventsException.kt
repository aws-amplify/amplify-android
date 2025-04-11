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

/**
 * Base class for exceptions thrown in Events library
 *
 * @param message of the exception.
 * @param cause of the exception.
 * @param recoverySuggestion recommendation to resolve exception.
 */
open class EventsException internal constructor(
    message: String,
    cause: Throwable? = null,
    val recoverySuggestion: String? = null
) : Exception(message, cause) {

    internal companion object {
        internal fun unknown(message: String? = null): EventsException {
            return EventsException(
                message = message ?: "An unknown error occurred",
                recoverySuggestion = "This is not expected to occur. Contact AWS"
            )
        }
    }
}

/**
 * Thrown when failing to connect to Events WebSocket.
 */
class ConnectException internal constructor(cause: Throwable?) : EventsException(
    message = "Failed to connect to the Events Api",
    cause = cause,
    recoverySuggestion = "See the underlying exception for cause"
)

/**
 * Thrown when call is unauthorized.
 */
class UnauthorizedException internal constructor(message: String? = null) : EventsException(
    message = message ?: "You are not authorized to make this call",
    recoverySuggestion = "Check your authorizer and Event configuration values and try again"
)

/**
 * Thrown when connection is unexpectedly closed.
 */
class ConnectionClosedException internal constructor(cause: Throwable? = null) : EventsException(
    message = "The websocket connection was closed",
    cause = cause,
    recoverySuggestion = "Check your internet connection and try again"
)

/**
 * Thrown when rate limit is exceeded.
 */
internal class RateLimitExceededException internal constructor() : EventsException(
    message = "Rate limit exceeded",
    recoverySuggestion = "Try again later"
)

/**
 * Thrown when operation is unsupported.
 */
internal class UnsupportedOperationException internal constructor() : EventsException(
    message = "WebSocket did not understand the operation",
    recoverySuggestion = "This is not expected to occur. Contact AWS"
)

/**
 * Thrown when resource is not found.
 */
internal class ResourceNotFoundException internal constructor() : EventsException(
    message = "Resource not found",
    recoverySuggestion = "Check Event configuration values and try again"
)

/**
 * Thrown when hitting max subscription limit.
 */
class MaxSubscriptionsReachedException internal constructor(throwable: Throwable) : EventsException(
    message = "Max number of subscriptions reached",
    recoverySuggestion = "Unsubscribe from existing channels before attempting to subscribe."
)

/**
 * Thrown when attempting to send too many events or invalid request.
 */
class BadRequestException internal constructor() : EventsException(
    message = "Input exceeded 5 event limit",
    recoverySuggestion = "Submit 5 events or less."
)

internal class UserClosedConnectionException internal constructor() : EventsException(
    message = "The websocket connection was closed normally"
)
