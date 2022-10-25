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

import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import com.amplifyframework.statemachine.codegen.errors.CredentialStoreError
import java.security.Key
import java.security.KeyStore
import javax.crypto.KeyGenerator

internal object LegacyKeyProvider {
    private const val AES_KEY_ALGORITHM = "AES"
    private const val CIPHER_AES_GCM_NOPADDING_KEY_LENGTH_IN_BITS = 256
    private const val ANDROID_KEY_STORE_NAME = "AndroidKeyStore"

    fun generateKey(keyAlias: String): Result<Key> {
        val keyStore = KeyStore.getInstance(ANDROID_KEY_STORE_NAME)
        keyStore.load(null)

        if (keyStore.containsAlias(keyAlias)) {
            return Result.failure(
                CredentialStoreError(
                    "Key already exists for the keyAlias: $keyAlias in $ANDROID_KEY_STORE_NAME"
                )
            )
        }

        val parameterSpec =
            KeyGenParameterSpec.Builder(
                keyAlias,
                KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
            )
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setKeySize(CIPHER_AES_GCM_NOPADDING_KEY_LENGTH_IN_BITS)
                .setRandomizedEncryptionRequired(false)
                .build()

        val generator = KeyGenerator.getInstance(AES_KEY_ALGORITHM, ANDROID_KEY_STORE_NAME)
        generator.init(parameterSpec)

        return Result.success(generator.generateKey())
    }

    fun retrieveKey(keyAlias: String): Result<Key> {
        val keyStore = KeyStore.getInstance(ANDROID_KEY_STORE_NAME)
        keyStore.load(null)

        if (!keyStore.containsAlias(keyAlias)) {
            val message = "Key does not exists for the keyAlias: $keyAlias in $ANDROID_KEY_STORE_NAME"
            return Result.failure(CredentialStoreError(message))
        }

        val key: Key? = keyStore.getKey(keyAlias, null)
        return if (key != null) {
            Result.success(key)
        } else {
            val message = "Key is null even though the keyAlias: " +
                keyAlias + " is present in " + ANDROID_KEY_STORE_NAME
            Result.failure(CredentialStoreError(message))
        }
    }

    fun deleteKey(keyAlias: String) {
        val keyStore = KeyStore.getInstance(ANDROID_KEY_STORE_NAME)
        keyStore.load(null)

        keyStore.deleteEntry(keyAlias)
    }
}
