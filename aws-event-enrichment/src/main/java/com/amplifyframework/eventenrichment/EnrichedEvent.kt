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
package com.amplifyframework.eventenrichment

import com.amplifyframework.eventenrichment.metadata.AppMetadata
import com.amplifyframework.eventenrichment.metadata.DeviceMetadata
import com.amplifyframework.eventenrichment.metadata.SdkMetadata
import com.amplifyframework.eventenrichment.session.Session
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonObject

/**
 * Version of the analytics event envelope schema, emitted as `event_version`.
 * "3.1" identifies the layout of the envelope so downstream consumers can tell
 * which schema they are parsing. Bump only when the envelope structure changes;
 * independent of the package version.
 */
private const val EVENT_VERSION = "3.1"

/**
 * An analytics event enriched with device, app, session, and SDK metadata.
 *
 * Use [toJson] to serialize to the structured analytics JSON envelope.
 *
 * @param eventId Unique event identifier (UUID). Not emitted in the envelope.
 * @param eventType Type of the event.
 * @param eventTimestamp Milliseconds since epoch when the event was recorded.
 * @param session Session active at the time of recording.
 * @param attributes Merged attributes (globals + per-event).
 * @param metrics Merged metrics (globals + per-event).
 * @param device Device metadata.
 * @param app Application metadata.
 * @param sdk SDK metadata.
 * @param clientId Persistent client/device identifier.
 * @param userId Optional user identifier.
 */
data class EnrichedEvent(
    val eventId: String,
    val eventType: String,
    val eventTimestamp: Long,
    val session: Session,
    val attributes: Map<String, String>,
    val metrics: Map<String, Double>,
    val device: DeviceMetadata,
    val app: AppMetadata,
    val sdk: SdkMetadata,
    val clientId: String,
    val userId: String? = null
) {
    /** Serializes to the analytics event envelope as a JSON string. */
    fun toJson(): String {
        val obj = buildJsonObject {
            put("event_type", eventType)
            put("event_timestamp", eventTimestamp)
            // On-device enrichment has no server ingestion step, so this
            // reflects client-side arrival and mirrors event_timestamp.
            // Retained for envelope compatibility.
            put("arrival_timestamp", eventTimestamp)
            put("event_version", EVENT_VERSION)
            putJsonObject("application") {
                put("app_id", app.appId)
                app.packageName?.let { put("package_name", it) }
                app.versionName?.let { put("version_name", it) }
                app.versionCode?.let { put("version_code", it) }
                app.title?.let { put("title", it) }
                putJsonObject("sdk") {
                    put("name", sdk.name)
                    put("version", sdk.version)
                }
            }
            putJsonObject("client") {
                put("client_id", clientId)
                userId?.let { put("user_id", it) }
            }
            putJsonObject("device") {
                val hasPlatform = device.platform != null || device.platformVersion != null
                if (hasPlatform) {
                    putJsonObject("platform") {
                        device.platform?.let { put("name", it) }
                        device.platformVersion?.let { put("version", it) }
                    }
                }
                device.manufacturer?.let { put("make", it) }
                device.model?.let { put("model", it) }
                device.locale?.let {
                    putJsonObject("locale") { put("code", it) }
                }
            }
            putJsonObject("session") {
                put("id", session.id)
                put("start_timestamp", session.startTimestamp)
                session.stopTimestamp?.let { put("stop_timestamp", it) }
                session.duration?.let { put("duration", it) }
            }
            if (attributes.isNotEmpty()) {
                putJsonObject("attributes") {
                    attributes.forEach { (key, value) -> put(key, value) }
                }
            }
            if (metrics.isNotEmpty()) {
                putJsonObject("metrics") {
                    metrics.forEach { (key, value) -> put(key, value) }
                }
            }
        }
        return Json.encodeToString(kotlinx.serialization.json.JsonObject.serializer(), obj)
    }
}
