/*
 * Copyright 2022 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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

package com.amplifyframework.auth.cognito.data

import android.content.Context
import android.content.SharedPreferences
import androidx.annotation.VisibleForTesting
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV
import androidx.security.crypto.EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
import androidx.security.crypto.MasterKeys

class EncryptedKeyValueRepository(
    private val context: Context,
    private val sharedPreferencesName: String,
) : KeyValueRepository {

    override fun put(dataKey: String, value: String?) {
        with(getSharedPreferences().edit()) {
            putString(dataKey, value)
            apply()
        }
    }

    override fun get(dataKey: String): String? = getSharedPreferences().getString(dataKey, null)

    override fun remove(dataKey: String) {
        with(getSharedPreferences().edit()) {
            remove(dataKey)
            apply()
        }
    }

    @VisibleForTesting
    fun getSharedPreferences(): SharedPreferences {
        return EncryptedSharedPreferences.create(
            sharedPreferencesName,
            MasterKeys.getOrCreate(MasterKeys.AES256_GCM_SPEC),
            context,
            AES256_SIV,
            AES256_GCM
        )
    }
}
