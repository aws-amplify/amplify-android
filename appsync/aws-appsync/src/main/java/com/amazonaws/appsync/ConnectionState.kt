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

import com.amplifyframework.api.ApiException

/**
 * The connection state of the client's shared WebSocket. Replaces Hub events from V2.
 * Exposed via [AmplifyAppSyncClient.events].
 */
sealed class ConnectionState {
    /** A WebSocket connection is being established. */
    data object Connecting : ConnectionState()

    /** The WebSocket connection is established and ready. */
    data object Connected : ConnectionState()

    /**
     * No active WebSocket connection.
     * @param cause The reason for disconnection, or null if this is a clean shutdown
     *   (no subscriptions, client closed, or not yet connected).
     */
    data class Disconnected(val cause: ApiException? = null) : ConnectionState()
}
