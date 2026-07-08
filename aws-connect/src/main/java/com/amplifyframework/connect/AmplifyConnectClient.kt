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
package com.amplifyframework.connect

import androidx.annotation.VisibleForTesting
import com.amplifyframework.connect.internal.IdentifyUserService
import com.amplifyframework.foundation.logging.AmplifyLogging
import com.amplifyframework.foundation.logging.Logger
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonObject

/**
 * A standalone, online-only client for the Amazon Connect Customer Profiles
 * identify endpoint (a backend Lambda fronted by an HTTP API).
 *
 * The client is a thin authorized POST: it resolves the caller's session
 * through an injected [ConnectCredentialsProvider] and sends the request to
 * the authenticated or guest route accordingly. Device registration is folded
 * into [identifyUser] via [IdentifyUserOptions] — there is no separate device
 * route in the backend contract.
 *
 * ## Deferred (Phase 2)
 *
 * Network calls happen immediately and surface [ConnectNetworkException] when
 * offline. The offline queue, connectivity drain, and retry/backoff are not
 * implemented here.
 */
class AmplifyConnectClient(
    configuration: ConnectClientConfiguration,
    private val credentialsProvider: ConnectCredentialsProvider,
    private val platform: String? = null,
    private val appVersion: String? = null
) {
    @VisibleForTesting
    internal var service: IdentifyUserService = IdentifyUserService(
        endpoint = configuration.endpoint,
        region = configuration.region
    )

    @VisibleForTesting
    internal constructor(
        configuration: ConnectClientConfiguration,
        credentialsProvider: ConnectCredentialsProvider,
        platform: String?,
        appVersion: String?,
        service: IdentifyUserService
    ) : this(configuration, credentialsProvider, platform, appVersion) {
        this.service = service
    }
    private val logger: Logger = AmplifyLogging.logger<AmplifyConnectClient>()

    /**
     * Sends user information to the Customer Profiles endpoint.
     *
     * Routes to the authenticated endpoint (bearer token, keyed on the Cognito
     * `sub`) when the session has a user-pool token, otherwise to the guest
     * endpoint (SigV4 `execute-api`, keyed on the Identity Pool `identityId`).
     *
     * [userId] is optional; it is stored only as an attribute and is never the
     * identity key. Device registration and merge-on-sign-in are expressed via
     * [options].
     *
     * @param userProfile User profile attributes to send
     * @param userId Optional application-level user identifier
     * @param options Device registration and merge-on-sign-in options
     * @throws ConnectNotSignedInException if no auth material is available
     * @throws ConnectNetworkException on transport failure
     * @throws AmplifyConnectException for endpoint errors
     */
    suspend fun identifyUser(userProfile: UserProfile, userId: String? = null, options: IdentifyUserOptions? = null) {
        val session = credentialsProvider.fetchSession()
        val body = buildJsonObject {
            userId?.let { put("userId", it) }
            putJsonObject("userProfile") {
                userProfile.name?.let { put("name", it) }
                userProfile.email?.let { put("email", it) }
                userProfile.phoneNumber?.let { put("phoneNumber", it) }
                userProfile.plan?.let { put("plan", it) }
                userProfile.customAttributes?.takeIf { it.isNotEmpty() }?.let { attrs ->
                    putJsonObject("customAttributes") {
                        attrs.forEach { (k, v) -> put(k, v) }
                    }
                }
            }
            if (options != null && !options.isEmpty) {
                putJsonObject("options") {
                    options.toJson().forEach { (k, v) ->
                        put(k, toJsonElement(v))
                    }
                }
            }
        }
        service.identify(session = session, body = body.toString())
        logger.info { "identifyUser sent (${if (session.isAuthenticated) "authenticated" else "guest"})" }
    }

    @Suppress("UNCHECKED_CAST")
    private fun toJsonElement(value: Any): JsonElement = when (value) {
        is String -> JsonPrimitive(value)
        is Number -> JsonPrimitive(value)
        is Boolean -> JsonPrimitive(value)
        is Map<*, *> -> buildJsonObject {
            (value as Map<String, Any>).forEach { (k, v) -> put(k, toJsonElement(v)) }
        }
        is List<*> -> kotlinx.serialization.json.JsonArray(
            value.map { toJsonElement(it ?: JsonNull) }
        )
        else -> JsonPrimitive(value.toString())
    }
}
