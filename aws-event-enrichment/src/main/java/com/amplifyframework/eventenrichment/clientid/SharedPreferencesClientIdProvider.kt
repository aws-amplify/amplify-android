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
package com.amplifyframework.eventenrichment.clientid

import android.content.Context
import java.util.UUID

/**
 * Provides a persistent client/device identifier for event enrichment.
 *
 * Implement this to supply a custom identifier. The default
 * [SharedPreferencesClientIdProvider] reads or creates a UUID persisted in
 * SharedPreferences under a key shared with other Amplify clients.
 */
fun interface ClientIdProvider {
    /** Returns the persistent client ID, creating one if it does not exist. */
    fun getClientId(): String
}

/**
 * Reads or creates a persistent client ID from SharedPreferences.
 *
 * Uses read-or-create semantics: if a non-empty value exists at
 * [CLIENT_ID_STORAGE_KEY], it is returned. Otherwise a new random UUID is
 * generated, persisted, and returned.
 *
 * The storage key [CLIENT_ID_STORAGE_KEY] is a cross-package integration
 * contract shared with the Connect client so that a device gets ONE id
 * everywhere. Whichever client initializes first generates the UUID; the other
 * reads it.
 *
 * @param context Android context used to access SharedPreferences.
 */
class SharedPreferencesClientIdProvider(context: Context) : ClientIdProvider {
    private val preferences = context.applicationContext
        .getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)

    override fun getClientId(): String {
        val existing = preferences.getString(CLIENT_ID_STORAGE_KEY, null)
        if (!existing.isNullOrEmpty()) return existing
        val id = UUID.randomUUID().toString()
        preferences.edit().putString(CLIENT_ID_STORAGE_KEY, id).apply()
        return id
    }

    companion object {
        /**
         * SharedPreferences key for the persistent client/device ID.
         *
         * Shared with the Connect client so a device resolves to a single id
         * across Amplify packages. Do not change without coordinating the
         * cross-package contract.
         */
        const val CLIENT_ID_STORAGE_KEY = "com.amplifyframework.device_id"

        /** Name of the SharedPreferences store backing the client id. */
        const val PREFERENCES_NAME = "com.amplifyframework.device_id"
    }
}
