/*
 * Copyright 2025 Amazon.com, Inc. or its affiliates. All Rights Reserved.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License").
 *  You may not use this file except in compliance with the License.
 *  A copy of the License is located at
 *
 *   http://aws.amazon.com/apache2.0
 *
 *  or in the "license" file accompanying this file. This file is distributed
 *  on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either
 *  express or implied. See the License for the specific language governing
 *  permissions and limitations under the License.
 */

package com.amplifyframework.aws.appsync.events

import com.amplifyframework.aws.appsync.core.AppSyncAuthorizer
import com.amplifyframework.aws.appsync.core.AppSyncRequest
import com.amplifyframework.aws.appsync.events.data.EventsErrors
import com.amplifyframework.aws.appsync.events.data.EventsException
import com.amplifyframework.aws.appsync.events.data.PublishResult
import com.amplifyframework.aws.appsync.events.data.toEventsException
import com.amplifyframework.aws.appsync.events.utils.HeaderKeys
import com.amplifyframework.aws.appsync.events.utils.HeaderValues
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import okhttp3.HttpUrl
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody

internal class RestClient(
    private val url: HttpUrl,
    private val okHttpClient: OkHttpClient,
    private val json: Json
) {

    suspend fun post(channelName: String, authorizer: AppSyncAuthorizer, event: JsonElement): PublishResult {
        return post(channelName, authorizer, events = listOf(event))
    }

    suspend fun post(channelName: String, authorizer: AppSyncAuthorizer, events: List<JsonElement>): PublishResult {
        return try {
            executePost(channelName, authorizer, events)
        } catch (exception: Exception) {
            PublishResult.Failure(exception.toEventsException())
        }
    }

    @Throws(Exception::class)
    internal suspend fun executePost(
        channelName: String,
        authorizer: AppSyncAuthorizer,
        events: List<JsonElement>
    ): PublishResult.Response {
        val postBody = JsonObject(
            content = mapOf(
                "channel" to JsonPrimitive(channelName),
                "events" to JsonArray(events.map { JsonPrimitive(it.toString()) })
            )
        ).toString()

        val preAuthRequest = Request.Builder().apply {
            url(url)
            addHeader(HeaderKeys.ACCEPT, HeaderValues.ACCEPT_APPLICATION_JSON)
            addHeader(HeaderKeys.CONTENT_TYPE, HeaderValues.CONTENT_TYPE_APPLICATION_JSON)
            addHeader(HeaderKeys.HOST, url.host)
            addHeader(HeaderKeys.USER_AGENT, HeaderValues.USER_AGENT)
            addHeader(HeaderKeys.X_AMZ_USER_AGENT, HeaderValues.USER_AGENT)
            post(postBody.toRequestBody(HeaderValues.CONTENT_TYPE_APPLICATION_JSON.toMediaType()))
        }.build()

        val authHeaders = authorizer.getAuthorizationHeaders(object : AppSyncRequest {
            override val method: AppSyncRequest.HttpMethod
                get() = AppSyncRequest.HttpMethod.POST
            override val url: String
                get() = preAuthRequest.url.toString()
            override val headers: Map<String, String>
                get() = preAuthRequest.headers.toMap()
            override val body: String
                get() = postBody
        })

        val authRequest = preAuthRequest.newBuilder().apply {
            authHeaders.forEach {
                header(it.key, it.value)
            }
        }.build()

        val result = okHttpClient.newCall(authRequest).execute()
        val body = result.body.string()
        return if (result.isSuccessful) {
            json.decodeFromString<PublishResult.Response>(body)
        } else {
            throw try {
                val errors = json.decodeFromString<EventsErrors>(body)
                errors.toEventsException("Failed to post event(s)")
            } catch (e: Exception) {
                EventsException.unknown("Failed to post event(s)", e)
            }
        }
    }
}
