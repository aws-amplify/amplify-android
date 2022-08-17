/*
 * Copyright 2022 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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
package com.amplifyframework.analytics.pinpoint.models

import java.util.UUID
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/*
* Internal representation of Pinpoint Event
* */

@Serializable
internal class PinpointEvent constructor(
    val eventId: String = UUID.randomUUID().toString(),
    val eventType: String,
    val attributes: Map<String, String>,
    val metrics: Map<String, Double>,
    val sdkInfo: SDKInfo,
    val pinpointSession: PinpointSession,
    val eventTimestamp: Long,
    val uniqueId: String,
    val androidAppDetails: AndroidAppDetails,
    val androidDeviceDetails: AndroidDeviceDetails
) {

    init {
        // TODO("Add documentation")
        require(eventType.length <= 50)
    }

    companion object {
        fun fromJsonString(jsonString: String): PinpointEvent {
            return Json.decodeFromString<PinpointEvent>(jsonString)
        }
    }

    fun toJsonString(): String {
        return Json.encodeToString(this)
    }
}
