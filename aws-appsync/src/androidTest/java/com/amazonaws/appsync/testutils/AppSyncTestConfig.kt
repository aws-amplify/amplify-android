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
package com.amazonaws.appsync.testutils

import android.content.Context
import org.json.JSONObject

/**
 * The deployed API's coordinates, read from the `amplify_outputs` raw resource.
 *
 * @param endpoint The GraphQL endpoint URL.
 * @param region The API's AWS region.
 * @param apiKey The API key, absent when the deployed API does not issue one.
 */
internal data class AppSyncTestConfig(val endpoint: String, val region: String, val apiKey: String?)

/**
 * A Cognito user to sign in as, read from the `credentials` raw resource.
 */
internal data class AppSyncTestCredentials(val username: String, val password: String)

/**
 * Reads the deployed API's coordinates, or returns null when the resource is absent or does not
 * describe a GraphQL API.
 *
 * Returning null rather than throwing is what lets the tests skip: the resource is gitignored, so it
 * is missing on any machine that has not provisioned a backend, including CI.
 */
internal fun readAppSyncConfig(context: Context): AppSyncTestConfig? {
    val outputs = readRawJson(context, "amplify_outputs") ?: return null
    val data = outputs.optJSONObject("data") ?: return null
    val endpoint = data.optString("url").takeIf { it.isNotBlank() } ?: return null
    val region = data.optString("aws_region").takeIf { it.isNotBlank() } ?: return null
    return AppSyncTestConfig(
        endpoint = endpoint,
        region = region,
        apiKey = data.optString("api_key").takeIf { it.isNotBlank() }
    )
}

/**
 * Reads the last user in the `credentials` resource, or null when the resource is absent.
 */
internal fun readAppSyncCredentials(context: Context): AppSyncTestCredentials? {
    val root = readRawJson(context, "credentials") ?: return null
    val users = root.optJSONArray("credentials") ?: return null
    if (users.length() == 0) return null
    val user = users.optJSONObject(users.length() - 1) ?: return null
    val username = user.optString("username").takeIf { it.isNotBlank() } ?: return null
    val password = user.optString("password").takeIf { it.isNotBlank() } ?: return null
    return AppSyncTestCredentials(username, password)
}

private fun readRawJson(context: Context, name: String): JSONObject? {
    val id = context.resources.getIdentifier(name, "raw", context.packageName)
    if (id == 0) return null
    return try {
        val text = context.resources.openRawResource(id).bufferedReader().use { it.readText() }
        JSONObject(text)
    } catch (error: Exception) {
        null
    }
}
