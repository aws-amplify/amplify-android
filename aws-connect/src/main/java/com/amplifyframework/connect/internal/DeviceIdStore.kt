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
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKeys
import java.util.UUID

/**
 * Persists a stable device identifier in EncryptedSharedPreferences.
 * The deviceId is used as a UNIQUE key in Customer Profiles to deduplicate
 * device registrations across token refreshes.
 */
internal class DeviceIdStore(context: Context) {

    private val prefs: SharedPreferences

    init {
        val masterKeyAlias = MasterKeys.getOrCreate(MasterKeys.AES256_GCM_SPEC)

        prefs = EncryptedSharedPreferences.create(
            PREFS_FILE_NAME,
            masterKeyAlias,
            context.applicationContext,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    }

    /**
     * Returns the persisted device ID, or generates and persists a new one.
     */
    fun getOrCreate(): String = prefs.getString(KEY_DEVICE_ID, null) ?: run {
        val newId = UUID.randomUUID().toString()
        prefs.edit().putString(KEY_DEVICE_ID, newId).apply()
        newId
    }

    /**
     * Returns the current device ID without generating a new one.
     * @return The device ID, or null if none is persisted.
     */
    fun get(): String? = prefs.getString(KEY_DEVICE_ID, null)

    /**
     * Clears the persisted device ID.
     */
    fun clear() {
        prefs.edit().remove(KEY_DEVICE_ID).apply()
    }

    internal companion object {
        const val PREFS_FILE_NAME = "com.amplifyframework.connect.device"
        const val KEY_DEVICE_ID = "device_id"
    }
}
