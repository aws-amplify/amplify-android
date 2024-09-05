/*
 * Copyright 2024 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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

package com.amplifyframework.apollo.appsync

import com.apollographql.apollo.api.ApolloRequest
import com.apollographql.apollo.api.http.HttpRequest

/**
 * Interface for classes that provide different types of authorization for AppSync. AppSync supports various auth
 * modes, including API Key, Cognito User Pools, OIDC, Lambda-based authorization, and IAM policies. Implementations
 * of this interface can be used to provide the specific headers and payloads needed for the auth mode being used.
 */
interface AppSyncAuthorizer {
    /**
     * Return the headers to append to HTTP requests (e.g. mutations and queries)
     * @param request The HttpRequest being sent
     * @return A map of header names to values
     */
    suspend fun getHttpAuthorizationHeaders(request: HttpRequest): Map<String, String>

    /**
     * Return the headers to append to the web socket connection request when establishing a subscription.
     * @param endpoint The [AppSyncEndpoint] for this subscription
     * @return A map of header names to values
     */
    suspend fun getWebsocketConnectionHeaders(endpoint: AppSyncEndpoint): Map<String, String>

    /**
     * Return the payload value to append to each message sent over the websocket.
     * @param endpoint The [AppSyncEndpoint] for this subscription
     * @param request The [ApolloRequest] being sent
     * @return A map of header names to values
     */
    suspend fun getWebSocketSubscriptionPayload(
        endpoint: AppSyncEndpoint,
        request: ApolloRequest<*>
    ): Map<String, String>
}
