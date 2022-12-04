/*
 *  Copyright 2022 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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

package com.amplifyframework.geo.location.database

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV
import androidx.security.crypto.EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
import androidx.security.crypto.MasterKeys
import java.io.File
import java.util.UUID
import net.zetetic.database.sqlcipher.SQLiteDatabase

const val passphraseKey = "passphrase"

internal class GeoDatabase(private val context: Context) {

    internal val sharedPreferences: SharedPreferences by lazy {
        EncryptedSharedPreferences.create(
            "geodb.${getInstallationIdentifier(context)}",
            MasterKeys.getOrCreate(MasterKeys.AES256_GCM_SPEC),
            context,
            AES256_SIV,
            AES256_GCM
        )
    }

    private val database by lazy {
        val path = context.getDatabasePath("awsgeo.db")
        val db = SQLiteDatabase.openOrCreateDatabase(path, getDatabasePassphrase(), null, null)
        LocationTable.onCreate(db, 1)
        db
    }

    val locationDao by lazy { LocationDao(database) }

    private fun getInstallationIdentifier(context: Context): String {
        val identifierFile = File(context.noBackupFilesDir, "geodb.installationIdentifier")
        val previousIdentifier = getExistingInstallationIdentifier(identifierFile)
        return previousIdentifier ?: createInstallationIdentifier(identifierFile)
    }

    private fun getExistingInstallationIdentifier(identifierFile: File): String? {
        return if (identifierFile.exists()) {
            val identifier = identifierFile.readText()
            identifier.ifBlank { null }
        } else {
            null
        }
    }

    private fun createInstallationIdentifier(identifierFile: File): String {
        val newIdentifier = UUID.randomUUID().toString()
        try {
            identifierFile.writeText(newIdentifier)
        } catch (e: Exception) {
            // Failed to write identifier to file, session will be forced to be in memory
        }
        return newIdentifier
    }

    private fun getDatabasePassphrase(): String {
        return sharedPreferences.getString(passphraseKey, null) ?: UUID.randomUUID().toString().also { passphrase ->
            sharedPreferences.edit().putString(passphraseKey, passphrase).apply()
        }
    }
}
