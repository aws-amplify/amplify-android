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
 * [AppSyncExtensions] so the client agrees with the API plugin on which error types count rather than
 * maintaining its own list of magic strings.
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
 * TODO: `LimitExceededError` is a sibling meaning a request-rate limit rather than a subscription
 *  count. It needs different recovery advice, so it is not folded in here.
 */
internal fun List<AppSyncWebSocketMessage.WireError>.hasSubscriptionLimitError(): Boolean =
    any { it.errorType == MAX_SUBSCRIPTIONS_REACHED }

private const val MAX_SUBSCRIPTIONS_REACHED = "MaxSubscriptionsReachedError"
