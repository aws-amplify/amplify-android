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

/**
 * Options for the identify endpoint, mirroring the backend contract's
 * `options` object.
 *
 * Device registration is expressed here rather than through a separate route:
 * set [address] (the push token), [deviceId], [channelType], and optionally
 * [platform] / [appVersion].
 *
 * @param userAttributes Additional string user attributes (key → list of values)
 * @param address Push token / endpoint address
 * @param deviceId Stable device identifier (the unique key for upsert)
 * @param channelType The push channel for the registered device
 * @param platform Client OS platform
 * @param appVersion App version
 * @param optOut Opt-out preference (accepted but no backend effect)
 */
data class IdentifyUserOptions(
    val userAttributes: Map<String, List<String>>? = null,
    val address: String? = null,
    val deviceId: String? = null,
    val channelType: ChannelType? = null,
    val platform: String? = null,
    val appVersion: String? = null,
    val optOut: OptOut? = null
) {
    /** Whether every field is null/empty (nothing to send). */
    val isEmpty: Boolean get() = toJson().isEmpty()

    /** Serializes to the endpoint's `options` shape, omitting null fields. */
    fun toJson(): Map<String, Any> = buildMap {
        userAttributes?.takeIf { it.isNotEmpty() }?.let { put("userAttributes", it) }
        address?.let { put("address", it) }
        deviceId?.let { put("deviceId", it) }
        channelType?.let { put("channelType", it.value) }
        platform?.let { put("platform", it) }
        appVersion?.let { put("appVersion", it) }
        optOut?.let { put("optOut", it.value) }
    }
}

/**
 * Push opt-out preference. Accepted for API compatibility but has no effect
 * on the current backend contract.
 */
enum class OptOut(val value: String) {
    ALL("ALL"),
    NONE("NONE")
}
