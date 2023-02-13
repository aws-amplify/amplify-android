/*
 * Copyright 2023 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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

package com.amplifyframework.pushnotifications.pinpoint

import android.util.Log
import com.amplifyframework.pushnotifications.pinpoint.utils.NotificationPayload
import com.amplifyframework.pushnotifications.pinpoint.utils.PushNotificationsConstants
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json

class EventSourceType private constructor(
    private var eventSourceIdAttributeKey: String,
    private var eventSourceActivityAttributeKey: String,
    private var attributeParser: EventSourceAttributeParser
) {

    companion object {
        private val CAMPAIGN = EventSourceType(
            PushNotificationsConstants.PINPOINT_CAMPAIGN_CAMPAIGN_ID,
            PushNotificationsConstants.PINPOINT_CAMPAIGN_CAMPAIGN_ACTIVITY_ID,
            CampaignAttributeParser()
        )

        private val JOURNEY = EventSourceType(
            PushNotificationsConstants.JOURNEY_ID,
            PushNotificationsConstants.JOURNEY_ACTIVITY_ID,
            JourneyAttributeParser()
        )

        private val UNKNOWN = EventSourceType(
            "",
            "",
            EventSourceAttributeParser()
        )

        fun getEventSourceType(payload: NotificationPayload): EventSourceType {
            return if (payload.rawData.containsKey(PushNotificationsConstants.PINPOINT_CAMPAIGN_CAMPAIGN_ACTIVITY_ID)) {
                CAMPAIGN
            } else if (payload.rawData.containsKey(PushNotificationsConstants.PINPOINT_PREFIX) &&
                payload.rawData[PushNotificationsConstants.PINPOINT_PREFIX]!!.contains("\"journey\"".toRegex())
            ) {
                JOURNEY
            } else {
                UNKNOWN
            }
        }
    }

    fun getEventSourceIdAttributeKey(): String {
        return eventSourceIdAttributeKey
    }

    fun getEventSourceActivityAttributeKey(): String {
        return eventSourceActivityAttributeKey
    }

    internal fun getAttributeParser(): EventSourceAttributeParser {
        return attributeParser
    }

    /**
     * Campaign attributes are send from Pinpoint flattened
     * For example:
     * "pinpoint.campaign.campaign_id"
     * Journey attributes come in JSON format
     * This class just seeks to abstract some of that away
     * from the logic that handles the notification and
     * also provides a default implementation that returns
     * an empty map.
     */
    internal open class EventSourceAttributeParser {
        open fun parseAttributes(payload: NotificationPayload): Map<String, String> {
            return emptyMap()
        }
    }

    private class CampaignAttributeParser : EventSourceAttributeParser() {
        override fun parseAttributes(payload: NotificationPayload): Map<String, String> {
            val result: HashMap<String, String> = HashMap()
            val campaignAttributes = payload.rawData.filter {
                it.key.contains(PushNotificationsConstants.CAMPAIGN_PREFIX)
            }
            for ((key, value) in campaignAttributes) {
                // Remove campaign prefix and include it in the attributes
                val sanitizedKey = key.replace(PushNotificationsConstants.CAMPAIGN_PREFIX, "")
                result[sanitizedKey] = value
            }
            return result
        }
    }

    private class JourneyAttributeParser : EventSourceAttributeParser() {
        override fun parseAttributes(payload: NotificationPayload): Map<String, String> {
            val result: HashMap<String, String> = HashMap()
            val pinpointAttribute = payload.rawData[PushNotificationsConstants.PINPOINT_PREFIX] ?: return result
            try {
                val journeyAttribute = Json.decodeFromString<Map<String, Map<String, String>>>(pinpointAttribute)
                val journeyAttributes = journeyAttribute[PushNotificationsConstants.JOURNEY_ATTRIBUTE_KEY]
                if (journeyAttributes != null) {
                    for ((key, value) in journeyAttributes) {
                        result[key] = value
                    }
                }
                return result
            } catch (e: Exception) {
                Log.e("PushNotificationsService", e.toString())
            }
            return result
        }
    }
}
