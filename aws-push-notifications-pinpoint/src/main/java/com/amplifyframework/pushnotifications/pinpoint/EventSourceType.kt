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

import com.amplifyframework.core.Amplify
import com.amplifyframework.notifications.pushnotifications.NotificationPayload
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json

internal class EventSourceType private constructor(
    eventSourcePrefix: String = "",
    val eventSourceIdAttributeKey: String = "",
    val eventSourceActivityAttributeKey: String = "",
    internal val attributeParser: EventSourceAttributeParser = EventSourceAttributeParser()
) {
    val eventTypeOpened = "$eventSourcePrefix.$AWS_EVENT_TYPE_OPENED"
    private val eventTypeReceivedBackground = "$eventSourcePrefix.$AWS_EVENT_TYPE_RECEIVED_BACKGROUND"
    private val eventTypeReceivedForeground = "$eventSourcePrefix.$AWS_EVENT_TYPE_RECEIVED_FOREGROUND"

    companion object {
        private val LOG = Amplify.Logging.forNamespace("amplify:aws-push-notifications-pinpoint")
        private const val CAMPAIGN_EVENT_SOURCE_PREFIX = "_campaign"
        private const val JOURNEY_EVENT_SOURCE_PREFIX = "_journey"
        private const val AWS_EVENT_TYPE_OPENED = "opened_notification"
        private const val AWS_EVENT_TYPE_RECEIVED_FOREGROUND = "received_foreground"
        private const val AWS_EVENT_TYPE_RECEIVED_BACKGROUND = "received_background"

        fun getEventSourceType(payload: NotificationPayload): EventSourceType {
            return if (payload.rawData.containsKey(PushNotificationsConstants.PINPOINT_CAMPAIGN_CAMPAIGN_ID)) {
                EventSourceType(
                    CAMPAIGN_EVENT_SOURCE_PREFIX,
                    PushNotificationsConstants.CAMPAIGN_ID,
                    PushNotificationsConstants.CAMPAIGN_ACTIVITY_ID,
                    CampaignAttributeParser()
                )
            } else if (payload.rawData.containsKey(PushNotificationsConstants.PINPOINT_PREFIX) &&
                payload.rawData[PushNotificationsConstants.PINPOINT_PREFIX]!!.contains("\"journey\"".toRegex())
            ) {
                EventSourceType(
                    JOURNEY_EVENT_SOURCE_PREFIX,
                    PushNotificationsConstants.JOURNEY_ID,
                    PushNotificationsConstants.JOURNEY_ACTIVITY_ID,
                    JourneyAttributeParser()
                )
            } else {
                EventSourceType()
            }
        }
    }

    fun getEventTypeReceived(isAppInForeground: Boolean) = if (isAppInForeground)
        eventTypeReceivedForeground
    else
        eventTypeReceivedBackground

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
        open fun parseAttributes(payload: NotificationPayload): Map<String, String> = emptyMap()
    }

    private class CampaignAttributeParser : EventSourceAttributeParser() {
        override fun parseAttributes(payload: NotificationPayload) = payload.rawData.filter {
            it.key.contains(PushNotificationsConstants.CAMPAIGN_PREFIX)
        }.mapKeys {
            it.key.replace(PushNotificationsConstants.CAMPAIGN_PREFIX, "")
        }
    }

    private class JourneyAttributeParser : EventSourceAttributeParser() {
        override fun parseAttributes(payload: NotificationPayload) = payload
            .rawData[PushNotificationsConstants.PINPOINT_PREFIX]?.let {
            try {
                Json.decodeFromString<Map<String, Map<String, String>>>(it)[PushNotificationsConstants.JOURNEY]
            } catch (e: Exception) {
                LOG.error("Error parsing journey attribute", e)
                null
            }
        } ?: mapOf()
    }
}
