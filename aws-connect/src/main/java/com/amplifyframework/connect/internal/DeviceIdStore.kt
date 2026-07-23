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
package com.amplifyframework.connect.internal

import android.content.Context
import android.content.SharedPreferences
import java.util.UUID

/**
 * Persists a stable device identifier in SharedPreferences using the shared
 * Amplify device ID contract.
 *
 * The device ID is a random UUID used as a UNIQUE key in Customer Profiles to
 * deduplicate device registrations across token refreshes. It is stored in a
 * plain (unencrypted) SharedPreferences file because a device UUID is an
 * identifier, not a credential.
 *
 * The storage key and file name are shared with the event enrichment client
 * so that a single device resolves to one ID across all Amplify packages.
 * Whichever client initializes first generates the UUID; subsequent clients
 * read the existing value.
 */
internal class DeviceIdStore(context: Context) {

    private val prefs: SharedPreferences = context.applicationContext
        .getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)

    /**
     * Returns the persisted device ID, or generates and persists a new one.
     */
    fun getOrCreate(): String {
        val existing = prefs.getString(DEVICE_ID_KEY, null)
        if (!existing.isNullOrEmpty()) return existing
        val newId = UUID.randomUUID().toString()
        prefs.edit().putString(DEVICE_ID_KEY, newId).apply()
        return newId
    }

    /**
     * Returns the current device ID without generating a new one.
     * @return The device ID, or null if none is persisted.
     */
    fun get(): String? = prefs.getString(DEVICE_ID_KEY, null)

    /**
     * Clears the persisted device ID.
     */
    fun clear() {
        prefs.edit().remove(DEVICE_ID_KEY).apply()
    }

    internal companion object {
        /**
         * SharedPreferences file name for the persistent device ID.
         *
         * Shared with the event enrichment client. Do not change without
         * coordinating the cross-package contract.
         */
        const val PREFERENCES_NAME = "com.amplifyframework.device_id"

        /**
         * SharedPreferences key for the persistent device ID.
         *
         * Shared with the event enrichment client. Do not change without
         * coordinating the cross-package contract.
         */
        const val DEVICE_ID_KEY = "com.amplifyframework.device_id"
    }
}
