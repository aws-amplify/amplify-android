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

package com.amplifyframework.core.store

import android.content.Context
import android.content.SharedPreferences
import androidx.annotation.VisibleForTesting
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV
import androidx.security.crypto.EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
import androidx.security.crypto.MasterKeys
import java.io.File
import java.util.UUID

class EncryptedKeyValueRepository(
    private val context: Context,
    private val sharedPreferencesName: String,
) : KeyValueRepository {

    @VisibleForTesting
    internal val sharedPreferences: SharedPreferences by lazy {
        EncryptedSharedPreferences.create(
            "$sharedPreferencesName.${getInstallationIdentifier(context, sharedPreferencesName)}",
            MasterKeys.getOrCreate(MasterKeys.AES256_GCM_SPEC),
            context,
            AES256_SIV,
            AES256_GCM
        )
    }

    @VisibleForTesting
    internal val editor: SharedPreferences.Editor by lazy {
        sharedPreferences.edit()
    }

    override fun put(dataKey: String, value: String?) {
        with(editor) {
            putString(dataKey, value)
            apply()
        }
    }

    override fun get(dataKey: String): String? = sharedPreferences.getString(dataKey, null)

    override fun remove(dataKey: String) {
        with(editor) {
            remove(dataKey)
            apply()
        }
    }

    /**
     * EncryptedSharedPreferences may have been backed up by the application, but will be unreadable due to the
     * KeyStore record being lost. To prevent an unreadable EncryptedSharedPreferences, we append a suffix to the name
     * with a UUID created in the noBackupFilesDir
     */
    @Synchronized
    private fun getInstallationIdentifier(context: Context, keyValueRepoID: String): String {
        val identifierFile = File(context.noBackupFilesDir, "$keyValueRepoID.installationIdentifier")
        val previousIdentifier = getExistingInstallationIdentifier(identifierFile)

        return previousIdentifier ?: createInstallationIdentifier(identifierFile)
    }

    /**
     * Gets the existing installation identifier (if exists)
     */
    private fun getExistingInstallationIdentifier(identifierFile: File): String? {
        return if (identifierFile.exists()) {
            val identifier = identifierFile.readText()
            identifier.ifBlank { null }
        } else {
            null
        }
    }

    /**
     * Creates a new installation identifier for the install
     */
    private fun createInstallationIdentifier(identifierFile: File): String {
        val newIdentifier = UUID.randomUUID().toString()
        try {
            identifierFile.writeText(newIdentifier)
        } catch (e: Exception) {
            // Failed to write identifier to file, session will be forced to be in memory
        }
        return newIdentifier
    }
}
