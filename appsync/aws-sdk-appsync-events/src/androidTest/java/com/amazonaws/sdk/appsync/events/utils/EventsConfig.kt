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
package com.amazonaws.sdk.appsync.events.utils

import android.content.Context
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

internal fun getEventsConfig(context: Context): EventsConfig {
    val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
    }
    return try {
        val inputStream = context.resources.openRawResource(
            context.resources.getIdentifier("amplify_outputs", "raw", context.packageName)
        )
        val jsonString = inputStream.bufferedReader().use { it.readText() }
        json.decodeFromString<AmplifyConfig>(jsonString).customConfig.eventsConfig
    } catch (e: Exception) {
        throw IllegalStateException("Failed to load events configuration", e)
    }
}

@Serializable
internal data class AmplifyConfig(
    @SerialName("custom") val customConfig: CustomConfig
)

@Serializable
@SerialName("custom")
internal data class CustomConfig(
    @SerialName("events") val eventsConfig: EventsConfig
)

@Serializable
@SerialName("events")
internal data class EventsConfig(
    val url: String,
    @SerialName("aws_region") val awsRegion: String,
    @SerialName("default_authorization_type")val defaultAuthorizationType: String,
    @SerialName("api_key") val apiKey: String
)
