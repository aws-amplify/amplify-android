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

import java.net.SocketException
import java.net.SocketTimeoutException
import java.net.UnknownHostException

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
        internal fun unknown(message: String? = null, cause: Throwable? = null): EventsException = EventsException(
            message = message ?: "An unknown error occurred",
            cause = cause,
            recoverySuggestion = if (cause != null) "This is not expected to occur. Contact AWS" else null
        )
    }

    override fun toString(): String = "${javaClass.simpleName} {message=$message" +
        (cause?.let { ", cause=$cause" } ?: "") +
        (recoverySuggestion?.let { ", recoverSuggestion=$recoverySuggestion" } ?: "") +
        "}"

    override fun hashCode(): Int {
        var result = super.hashCode()
        result = 31 * result + (message?.hashCode() ?: 0)
        result = 31 * result + (cause?.hashCode() ?: 0)
        result = 31 * result + (recoverySuggestion?.hashCode() ?: 0)
        return result
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is EventsException) return false

        return message == other.message &&
            cause == other.cause &&
            recoverySuggestion == other.recoverySuggestion
    }
}

internal fun Exception.toEventsException(): EventsException = when (this) {
    is EventsException -> this
    is UnknownHostException, is SocketTimeoutException, is SocketException -> NetworkException(throwable = this)
    else -> EventsException.unknown(cause = this)
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
class RateLimitExceededException internal constructor(message: String?) : EventsException(
    message = message ?: "Rate limit exceeded",
    recoverySuggestion = "Try again later"
)

/**
 * Thrown when operation is unsupported.
 */
class UnsupportedOperationException internal constructor(message: String?) : EventsException(
    message = message ?: "WebSocket did not understand the operation",
    recoverySuggestion = "This is not expected to occur. Contact AWS"
)

/**
 * Thrown when resource is not found.
 */
class ResourceNotFoundException internal constructor(message: String?) : EventsException(
    message = message ?: "Namespace not found",
    recoverySuggestion = "Check resource values and try again"
)

/**
 * Thrown when hitting max subscription limit.
 */
class MaxSubscriptionsReachedException internal constructor(message: String?) : EventsException(
    message = message ?: "Max number of subscriptions reached",
    recoverySuggestion = "Unsubscribe from existing channels before attempting to subscribe."
)

/**
 * Thrown when attempting to send too many events or invalid request.
 */
class BadRequestException internal constructor(message: String?) : EventsException(
    message = message ?: "An unknown error occurred"
)

/**
 * Thrown when attempting to send too many events or invalid request over websocket.
 */
class InvalidInputException internal constructor(message: String?) : EventsException(
    message = message ?: "An unknown error occurred"
)

/**
 * Thrown when we detect a failure in the network.
 * See the cause for the underlying error.
 */
class NetworkException internal constructor(throwable: Throwable) : EventsException(
    message = "Network error",
    cause = throwable,
    recoverySuggestion = "Check your internet connection and try again. See the cause for more details."
)

/**
 * An internal exception that is not provided to the customer.
 * We use this exception so that we can differentiate between a connection being closed unexpectedly or by the user.
 * If the connection is closed by the user, we catch this exception and don't propagate it to the customer.
 */
internal class UserClosedConnectionException internal constructor() : EventsException(
    message = "The websocket connection was closed normally"
)
