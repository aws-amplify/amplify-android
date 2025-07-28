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

package com.amplifyframework.auth.cognito.data

import android.content.Context
import android.content.SharedPreferences
import android.util.Base64
import android.util.Log
import androidx.core.content.edit
import com.amplifyframework.core.store.KeyValueRepository
import com.amplifyframework.statemachine.codegen.errors.CredentialStoreError
import java.nio.charset.Charset
import java.security.Key
import java.security.SecureRandom
import java.security.spec.AlgorithmParameterSpec
import javax.crypto.Cipher
import javax.crypto.spec.GCMParameterSpec

internal class LegacyKeyValueRepository(
    context: Context,
    private val sharedPreferencesName: String,
    private var isPersistenceEnabled: Boolean = true
) : KeyValueRepository {

    // TODO
    // Ported Implementation of AWS Java SDK. Needs cleanup for things that are not needed for migration.
    // And if EncryptedSharedPrefs provides satisfactory encryption as this one does.

    companion object {
        private const val CIPHER_AES_GCM_NOPADDING = "AES/GCM/NoPadding"
        private const val CIPHER_AES_GCM_NOPADDING_IV_LENGTH_IN_BYTES = 12
        private const val CIPHER_AES_GCM_NOPADDING_TAG_LENGTH_LENGTH_IN_BITS = 128
        private const val CHARSET_NAME = "UTF-8"

        /**
         * The data key in SharedPreferences will have this suffix.
         *
         * For example: if the key to access data is "accessKey", the SharedPreferences
         * key will be "accessKey.encrypted"
         */
        private const val SHARED_PREFERENCES_DATA_IDENTIFIER_SUFFIX = ".encrypted"

        /**
         * The IV used to encrypt data will be stored under the key
         * "accessKey.encrypted.iv"
         */
        private const val SHARED_PREFERENCES_IV_SUFFIX = ".iv"

        /**
         * The version of the KeyValueStore will be stored under the key
         * "accessKey.encrypted.keyvaluestoreversion"
         */
        private const val SHARED_PREFERENCES_STORE_VERSION_SUFFIX = ".keyvaluestoreversion"
        private const val AWS_KEY_VALUE_STORE_VERSION_1_KEY_STORE_ALIAS_FOR_AES_SUFFIX = ".aesKeyStoreAlias"
        private const val AWS_KEY_VALUE_STORE_VERSION = 1
    }

    private val secureRandom: SecureRandom = SecureRandom()
    private var sharedPreferencesForData: SharedPreferences

    private val cacheFactory: MutableMap<String, MutableMap<String, String>> = mutableMapOf()

    // In-memory store operates on the key passed in and does not use the suffixes.
    private val cache: MutableMap<String, String>

    init {
        cache = getCacheForKey(sharedPreferencesName)
        this.sharedPreferencesForData = context.getSharedPreferences(
            sharedPreferencesName,
            Context.MODE_PRIVATE
        )
    }

    private fun getCacheForKey(key: String): MutableMap<String, String> = cacheFactory.getOrPut(key) { mutableMapOf() }

    /**
     * Store the key and value pair in memory and in Shared Preferences
     * @param dataKey key to the
     * @param value Value
     */
    override fun put(dataKey: String, value: String?) {
        // Irrespective of persistence is enabled or not, store in memory.
        value?.also { cache[dataKey] = it }

        if (!isPersistenceEnabled) {
            return
        }

        if (value == null) {
            // Null value -> Removing data, IV and version from Shared Prefs.
            remove(dataKey)
            return
        }

        // dataKey becomes dataKey.encrypted
        val dataKeyInPersistentStore: String = getDataKeyUsedInPersistentStore(dataKey)

        val encryptionKeyAlias: String = getEncryptionKeyAlias()

        val encryptionKey: Result<Key> = retrieveEncryptionKey(encryptionKeyAlias).onFailure {
            generateEncryptionKey(encryptionKeyAlias)
        }.onFailure {
            return
        }

        try {
            // Encrypt
            val iv: ByteArray = generateInitializationVector()
            val base64EncodedEncryptedString: String? = encrypt(
                encryptionKey.getOrThrow(),
                GCMParameterSpec(CIPHER_AES_GCM_NOPADDING_TAG_LENGTH_LENGTH_IN_BITS, iv),
                value
            )

            val base64EncodedIV: String = Base64.encodeToString(iv, Base64.DEFAULT)
                ?: throw Exception("Error in Base64 encoding the IV for dataKey = $dataKey")

            // Persist
            sharedPreferencesForData.edit {
                putString(dataKeyInPersistentStore, base64EncodedEncryptedString) // data
                    .putString(
                        "$dataKeyInPersistentStore$SHARED_PREFERENCES_IV_SUFFIX",
                        base64EncodedIV
                    ) // IV
                    .putInt(
                        "$dataKeyInPersistentStore$SHARED_PREFERENCES_STORE_VERSION_SUFFIX",
                        AWS_KEY_VALUE_STORE_VERSION
                    ) // KeyValueStore Version
            }
        } catch (ex: Exception) {
            // TODO Log  Error
            //    ("Error in storing value for dataKey = " + dataKey +
            //            ". This data has not been stored in the persistent store."), ex.toString()
        }
    }

    @Synchronized
    override fun get(dataKey: String): String? {
        if (!isPersistenceEnabled) {
            return cache[dataKey]
        }

        val dataKeyInPersistentStore = getDataKeyUsedInPersistentStore(dataKey)

        val encryptionKeyAlias = getEncryptionKeyAlias()

        val decryptionKey = retrieveEncryptionKey(encryptionKeyAlias).onFailure {
            return null
        }

        if (!sharedPreferencesForData.contains(dataKeyInPersistentStore)) {
            return null
        }

        return try {
            // If the version of data stored mismatches with the version of the store,
            // return null.
            val keyValueStoreVersion =
                sharedPreferencesForData.getString(
                    dataKeyInPersistentStore + SHARED_PREFERENCES_STORE_VERSION_SUFFIX,
                    "-1"
                )?.toInt()
            if (keyValueStoreVersion != AWS_KEY_VALUE_STORE_VERSION) {
                // TODO : Log ("The version of the data read from SharedPreferences for " + dataKey + " does not match the version of the store.")
                return null
            }

            // Read from the SharedPreferences and decrypt
            val encryptedData = sharedPreferencesForData.getString(dataKeyInPersistentStore, null)
            val decryptedDataInString: String? = decrypt(
                decryptionKey.getOrThrow(),
                getInitializationVector(dataKeyInPersistentStore),
                encryptedData
            )

            // Update the in-memory cache after read from disk.
            decryptedDataInString?.also { cache[dataKey] = it }
        } catch (ex: java.lang.Exception) {
            // TODO Log error in retrieval

            // Remove the dataKey and its associated value if there is an exception in decryption
            remove(dataKey)
            null
        }
    }

    @Throws(java.lang.Exception::class)
    private fun getInitializationVector(keyOfDataInSharedPreferences: String): AlgorithmParameterSpec {
        val keyOfIV = keyOfDataInSharedPreferences + SHARED_PREFERENCES_IV_SUFFIX
        if (!sharedPreferencesForData.contains(keyOfIV)) {
            throw java.lang.Exception(
                "Initialization vector for $keyOfDataInSharedPreferences is missing from the SharedPreferences."
            )
        }
        val initializationVectorInString = sharedPreferencesForData.getString(keyOfIV, null)
            ?: throw java.lang.Exception(
                "Cannot read the initialization vector for $keyOfDataInSharedPreferences from SharedPreferences."
            )
        val base64DecodedIV: ByteArray = Base64.decode(initializationVectorInString, Base64.DEFAULT)
        if (base64DecodedIV.isEmpty()) {
            throw java.lang.Exception(
                "Cannot base64 decode the initialization vector for " +
                    "$keyOfDataInSharedPreferences read from SharedPreferences."
            )
        }
        return GCMParameterSpec(CIPHER_AES_GCM_NOPADDING_TAG_LENGTH_LENGTH_IN_BITS, base64DecodedIV)
    }

    private fun decrypt(decryptionKey: Key, ivSpec: AlgorithmParameterSpec, encryptedData: String?): String? = try {
        val encryptedDecodedData: ByteArray = Base64.decode(encryptedData, Base64.DEFAULT)
        val cipher = Cipher.getInstance(CIPHER_AES_GCM_NOPADDING)
        cipher.init(Cipher.DECRYPT_MODE, decryptionKey, ivSpec)
        val decryptedData = cipher.doFinal(encryptedDecodedData)
        String(decryptedData, Charset.forName(CHARSET_NAME))
    } catch (ex: java.lang.Exception) {
        // TODO Log Error in decrypting data
        null
    }

    private fun generateInitializationVector(): ByteArray {
        val initializationVector = ByteArray(CIPHER_AES_GCM_NOPADDING_IV_LENGTH_IN_BYTES)
        secureRandom.nextBytes(initializationVector)
        return initializationVector
    }

    private fun getEncryptionKeyAlias() =
        sharedPreferencesName + AWS_KEY_VALUE_STORE_VERSION_1_KEY_STORE_ALIAS_FOR_AES_SUFFIX

    @Synchronized
    override fun remove(dataKey: String) {
        // Irrespective of persistence is enabled or not, mutate in memory.
        cache.remove(dataKey)
        if (isPersistenceEnabled) {
            val keyUsedInPersistentStore: String = getDataKeyUsedInPersistentStore(dataKey)
            sharedPreferencesForData.edit {
                remove(keyUsedInPersistentStore)
                    .remove(keyUsedInPersistentStore + SHARED_PREFERENCES_IV_SUFFIX)
                    .remove(keyUsedInPersistentStore + SHARED_PREFERENCES_STORE_VERSION_SUFFIX)
            }
        }
    }

    private fun getDataKeyUsedInPersistentStore(key: String): String = "$key$SHARED_PREFERENCES_DATA_IDENTIFIER_SUFFIX"

    @Synchronized
    private fun retrieveEncryptionKey(encryptionKeyAlias: String): Result<Key> = LegacyKeyProvider
        .retrieveKey(encryptionKeyAlias)
        .onFailure {
            LegacyKeyProvider.deleteKey(encryptionKeyAlias)
            Result.failure<Key>(
                CredentialStoreError(
                    "Key cannot be retrieved. " +
                        "Deleting the encryption key identified by the keyAlias: $encryptionKeyAlias"
                )
            )
        }

    @Synchronized
    private fun generateEncryptionKey(encryptionKeyAlias: String): Result<Key> =
        LegacyKeyProvider.generateKey(encryptionKeyAlias)

    private fun encrypt(encryptionKey: Key, ivSpec: AlgorithmParameterSpec, data: String): String? = try {
        val cipher = Cipher.getInstance(CIPHER_AES_GCM_NOPADDING)
        cipher.init(Cipher.ENCRYPT_MODE, encryptionKey, ivSpec)

        val encryptedData = cipher.doFinal(data.toByteArray(charset(CHARSET_NAME)))

        // TODO : Check the correct flags
        Base64.encodeToString(encryptedData, Base64.DEFAULT)
    } catch (ex: Exception) {
        Log.e("Error in encrypting data. ", ex.toString())
        null
    }
}
