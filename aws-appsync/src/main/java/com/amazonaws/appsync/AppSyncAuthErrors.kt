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
package com.amazonaws.appsync

import com.amplifyframework.api.graphql.GraphQLResponse
import com.amplifyframework.datastore.appsync.AppSyncExtensions

/**
 * Recognises the errors that mean "this identity was rejected", as opposed to errors that would recur
 * with any identity. Only the former is worth retrying with a different auth mode.
 *
 * Both the HTTP and WebSocket paths need this, and both defer the classification to
 * [AppSyncExtensions] rather than maintaining a list of error-type strings here.
 */

/** Whether any error on this response is an authorization failure. */
internal fun GraphQLResponse<*>.hasUnauthorizedError(): Boolean = errors.any { error ->
    val extensions = error.extensions
    !extensions.isNullOrEmpty() && AppSyncExtensions(extensions).isUnauthorizedErrorType
}

/** Whether any of these WebSocket errors is an authorization failure. */
internal fun List<AppSyncWebSocketMessage.WireError>.hasUnauthorizedError(): Boolean = any { error ->
    error.errorType?.let { AppSyncExtensions(it, null, null).isUnauthorizedErrorType } == true
}

/**
 * Whether any of these errors says the API's concurrent-subscription limit was reached.
 *
 * Checked against the wire string directly rather than through [AppSyncExtensions], whose error-type
 * enum does not model this case.
 *
 * Deliberately narrower than [hasRateLimitError]: the two limits are released by different actions, so
 * they must not be folded together.
 */
internal fun List<AppSyncWebSocketMessage.WireError>.hasSubscriptionLimitError(): Boolean =
    any { it.errorType == MAX_SUBSCRIPTIONS_REACHED }

/**
 * Whether any of these errors says the API's request rate limit was exceeded.
 *
 * A cap on how fast requests may arrive, not on how many subscriptions may be open, so the remedy is to
 * back off rather than to release anything.
 */
internal fun List<AppSyncWebSocketMessage.WireError>.hasRateLimitError(): Boolean =
    any { it.errorType == LIMIT_EXCEEDED }

/** Whether any error on this response says the API's request rate limit was exceeded. */
internal fun GraphQLResponse<*>.hasRateLimitError(): Boolean =
    errors.any { it.extensions?.get(ERROR_TYPE_KEY) == LIMIT_EXCEEDED }

private const val MAX_SUBSCRIPTIONS_REACHED = "MaxSubscriptionsReachedError"
private const val LIMIT_EXCEEDED = "LimitExceededError"
private const val ERROR_TYPE_KEY = "errorType"

/**
 * Converts wire errors into the response type callers already handle, so an exception carrying them can
 * be inspected the same way a query's errors are. The classification is preserved under `extensions`,
 * which is where a GraphQL response carries it.
 */
internal fun List<AppSyncWebSocketMessage.WireError>.toGraphQLErrors(): List<GraphQLResponse.Error> = map {
    GraphQLResponse.Error(
        it.message,
        null,
        null,
        it.errorType?.let { type -> mapOf("errorType" to type) } ?: emptyMap()
    )
}
