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
package com.amazonaws.appsync

/**
 * Wraps the authorizer(s) that the client uses. Supports both single-auth (one authorizer for
 * all requests) and multi-auth (multiple authorizers, selected based on model `@auth` rules
 * or per-request overrides).
 */
sealed class AppSyncAuthorization {

    /**
     * Single authorizer used for all requests.
     * @param authorizer The authorizer to use.
     */
    data class Single(
        val authorizer: AppSyncClientAuthorizer
    ) : AppSyncAuthorization()

    /**
     * Multiple authorizers. The client selects the appropriate one based on model `@auth` rules
     * or per-request auth mode overrides. Falls back to [defaultAuthMode] when no rule matches.
     *
     * @param defaultAuthMode The auth mode to use when no per-request override or model rule applies.
     * @param authorizers The list of authorizers. Each authorizer's [AppSyncClientAuthorizer.authMode]
     *   determines which auth mode it handles. Duplicate auth modes are not allowed.
     */
    data class Multi(
        val defaultAuthMode: AppSyncAuthMode,
        val authorizers: List<AppSyncClientAuthorizer>
    ) : AppSyncAuthorization() {
        init {
            val modes = authorizers.map { it.authMode }
            require(modes.distinct().size == modes.size) {
                "Duplicate auth modes in authorizers list: ${modes.groupBy { it }.filter { it.value.size > 1 }.keys}"
            }
            require(authorizers.any { it.authMode == defaultAuthMode }) {
                "No authorizer provided for the default auth mode: $defaultAuthMode"
            }
        }
    }

    /**
     * Resolves the authorizer for a given [AppSyncAuthMode].
     * @return The matching authorizer, or null if not found.
     */
    internal fun authorizerFor(mode: AppSyncAuthMode): AppSyncClientAuthorizer? = when (this) {
        is Single -> authorizer.takeIf { it.authMode == mode }
        is Multi -> authorizers.firstOrNull { it.authMode == mode }
    }

    /**
     * Returns the default authorizer.
     */
    internal val defaultAuthorizer: AppSyncClientAuthorizer
        get() = when (this) {
            is Single -> authorizer
            is Multi -> authorizers.first { it.authMode == this.defaultAuthMode }
        }

    /**
     * Returns the default auth mode.
     */
    internal fun resolveDefaultAuthMode(): AppSyncAuthMode = when (this) {
        is Single -> authorizer.authMode
        is Multi -> defaultAuthMode
    }
}
