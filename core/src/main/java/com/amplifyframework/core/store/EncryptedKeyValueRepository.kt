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
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import androidx.annotation.VisibleForTesting
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV
import androidx.security.crypto.EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
import androidx.security.crypto.MasterKeys
import com.amplifyframework.core.Amplify
import java.io.File
import java.security.KeyStore
import java.util.UUID

class EncryptedKeyValueRepository @VisibleForTesting constructor(
    private val context: Context,
    private val sharedPreferencesName: String,
    private val defaultMasterKeySpec: KeyGenParameterSpec,
    private val amplifyMasterKeySpec: KeyGenParameterSpec,
    private val fileFactory: (dir: File, fileName: String) -> File
) : KeyValueRepository {

    constructor(context: Context, sharedPreferencesName: String) : this(
        context = context,
        sharedPreferencesName = sharedPreferencesName,
        defaultMasterKeySpec = getDefaultMasterKeySpec(),
        amplifyMasterKeySpec = getAmplifyMasterKeySpec(),
        fileFactory = { dir, fileName -> File(dir, fileName) }
    )

    private val sharedPreferences by lazy { getOrCreateSharedPreferences() }

    override fun put(dataKey: String, value: String?) = edit { putString(dataKey, value) }
    override fun get(dataKey: String): String? = sharedPreferences.getString(dataKey, null)
    override fun remove(dataKey: String) = edit { remove(dataKey) }
    override fun removeAll() = edit { clear() }

    private inline fun edit(crossinline block: SharedPreferences.Editor.() -> Unit) = with(sharedPreferences.edit()) {
        block()
        apply()
    }

    private fun getOrCreateSharedPreferences(): SharedPreferences {
        val identifier = getInstallationIdentifier()
        return if (identifier.startsWith(amplifyIdentifierPrefix)) {
            // This repository was encrypted with the amplify master key
            openKeystoreWithAmplifyMasterKey(identifier)
        } else {
            // This repository was encrypted with the default master key
            openKeystoreWithDefaultMasterKey(identifier)
        }
    }

    private fun openKeystoreWithAmplifyMasterKey(identifier: String): SharedPreferences {
        var amplifyMasterKey = getMasterKeyOrNull(amplifyMasterKeySpec)
        if (amplifyMasterKey == null) {
            logger.warn("Unable to retrieve Amplify master key. Deleting invalid master key and creating new one")
            deleteAmplifyMasterKey()
            amplifyMasterKey = getMasterKeyOrThrow(amplifyMasterKeySpec)
        }

        val fileName = getSharedPrefsFileName(identifier)

        // Return the shared preferences if we can
        getSharedPreferencesOrNull(fileName, amplifyMasterKey)?.let { return it }

        logger.warn("Cannot retrieve preferences encrypted with amplify master key. Deleting and recreating.")
        deleteSharedPreferences(fileName)
        return getSharedPreferencesOrThrow(fileName, amplifyMasterKey)
    }

    private fun openKeystoreWithDefaultMasterKey(identifier: String): SharedPreferences {
        // Try to open the encrypted preferences using the default master key
        getMasterKeyOrNull(defaultMasterKeySpec)?.let { defaultMasterKey ->
            val fileName = getSharedPrefsFileName(identifier)
            getSharedPreferencesOrNull(fileName, defaultMasterKey)?.let { return it }
        }

        logger.warn("Cannot retrieve preferences encrypted with default master key. Deleting and recreating.")
        // Delete the existing shared preferences file
        deleteSharedPreferences(getSharedPrefsFileName(identifier))
        // Create a new identifier with the amplify prefix
        val newIdentifier = createInstallationIdentifier(getInstallationFile())
        // Use the amplify master key to create the new shared preferences
        return openKeystoreWithAmplifyMasterKey(newIdentifier)
    }

    private fun getSharedPreferencesOrNull(fileName: String, key: String) = try {
        getSharedPreferencesOrThrow(fileName = fileName, key = key)
    } catch (e: Exception) { null }

    private fun getSharedPreferencesOrThrow(fileName: String, key: String): SharedPreferences =
        EncryptedSharedPreferences.create(
            fileName,
            key,
            context,
            AES256_SIV,
            AES256_GCM
        )

    private fun deleteSharedPreferences(fileName: String) = context.deleteSharedPreferences(fileName)

    private fun deleteAmplifyMasterKey() = KeyStore.getInstance("AndroidKeyStore").run {
        load(null)
        deleteEntry(amplifyMasterKeySpec.keystoreAlias)
    }

    private fun getMasterKeyOrNull(spec: KeyGenParameterSpec): String? {
        // Getting the Master Key should succeed, but keystore bugs in some OEM implementations mean that
        // the master key occasionally becomes corrupted on some devices. We make multiple attempts to ensure that
        // the error is not transient.
        repeat(3) { attempt ->
            try {
                return getMasterKeyOrThrow(spec)
            } catch (e: Exception) {
                logger.warn("Unable to retrieve master key, attempt ${attempt + 1} / 3", e)
            }
        }
        return null
    }

    private fun getMasterKeyOrThrow(spec: KeyGenParameterSpec) = MasterKeys.getOrCreate(spec)

    private fun getSharedPrefsFileName(installationIdentifier: String) =
        "$sharedPreferencesName.$installationIdentifier"

    private fun getInstallationFile() = fileFactory(
        context.noBackupFilesDir,
        "$sharedPreferencesName.installationIdentifier"
    )

    /**
     * EncryptedSharedPreferences may have been backed up by the application, but will be unreadable due to the
     * KeyStore record being lost. To prevent an unreadable EncryptedSharedPreferences, we append a suffix to the name
     * with a UUID created in the noBackupFilesDir
     */
    @Synchronized
    private fun getInstallationIdentifier(): String {
        val identifierFile = getInstallationFile()
        val previousIdentifier = getExistingInstallationIdentifier(identifierFile)
        return previousIdentifier ?: createInstallationIdentifier(identifierFile)
    }

    /**
     * Gets the existing installation identifier (if exists)
     */
    private fun getExistingInstallationIdentifier(identifierFile: File) = if (identifierFile.exists()) {
        identifierFile.readText().ifBlank { null }
    } else {
        null
    }

    /**
     * Creates a new installation identifier for the install
     */
    private fun createInstallationIdentifier(identifierFile: File) =
        "$amplifyIdentifierPrefix${UUID.randomUUID()}".also {
            writeInstallationIdentifier(identifierFile, it)
        }

    /**
     * Writes installation identifier to disk
     */
    private fun writeInstallationIdentifier(identifierFile: File, identifier: String) {
        try {
            identifierFile.writeText(identifier)
        } catch (e: Exception) {
            // Failed to write identifier to file, session will be forced to be in memory
        }
    }

    internal companion object {
        private val logger = Amplify.Logging.forNamespace(EncryptedKeyValueRepository::class.simpleName!!)

        private fun getDefaultMasterKeySpec() = MasterKeys.AES256_GCM_SPEC

        // We create our own KeyGenParameterSpec that is exactly like MasterKeys.AES256_GCM_SPEC except with a different
        // alias. This allows us to safely delete this key should it become corrupted without potentially impacting any
        // other part of the customer's application.
        private fun getAmplifyMasterKeySpec() = KeyGenParameterSpec.Builder(
            "amplify_master_key",
            KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
        ).setBlockModes(KeyProperties.BLOCK_MODE_GCM)
            .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
            .setKeySize(256)
            .build()

        // This prefix is used to identify repositories encrypted with the amplifyMasterKey instead of the
        // defaultMasterKey
        @VisibleForTesting internal const val amplifyIdentifierPrefix = "__amplify__"
    }
}
