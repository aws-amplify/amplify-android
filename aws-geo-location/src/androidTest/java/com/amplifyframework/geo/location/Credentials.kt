/*
 * Copyright 2021 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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

package com.amplifyframework.geo.location

import android.content.Context
import com.amplifyframework.core.Resources

/**
 * Utility to load Cognito user credentials from a json document.
 * The document must be formatted as following:
 *
 * <pre>
 * {
 *   "username": "username123",
 *   "password": "kool@pass22"
 * }
 * </pre>
 */
object Credentials {
    private const val DEFAULT_ID = "credentials" // credentials.json
    private var credentials: Pair<String, String>? = null

    fun load(context: Context, identifier: String = DEFAULT_ID): Pair<String, String> {
        if (credentials == null) {
            credentials = synchronized(this) {
                val json = Resources.readJsonResource(context, identifier)
                val username = json.getString("username")
                val password = json.getString("password")
                username to password
            }
        }
        return credentials!!
    }
}