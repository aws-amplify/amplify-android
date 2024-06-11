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
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV
import androidx.security.crypto.EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
import androidx.security.crypto.MasterKeys
import com.amplifyframework.core.store.EncryptedKeyValueRepository.Companion.amplifyIdentifierPrefix
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkAll
import io.mockk.verify
import java.io.File
import java.security.GeneralSecurityException
import java.security.KeyStore
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class EncryptedKeyValueRepositoryTest {

    // Use a temporary folder for writing the installation identifier
    @get:Rule
    val folder = TemporaryFolder()

    private val context = mockk<Context>(relaxed = true)
    private val editor = mockk<SharedPreferences.Editor>(relaxed = true)
    private val sharedPreferences = mockk<SharedPreferences> {
        every { edit() } returns editor
    }
    private val keystore = mockk<KeyStore>(relaxed = true)

    private val defaultMasterKeySpec = mockk<KeyGenParameterSpec> {
        every { keystoreAlias } returns "default_master_key"
    }
    private val amplifyMasterKeySpec = mockk<KeyGenParameterSpec> {
        every { keystoreAlias } returns "amplify_master_key"
    }

    @Before
    fun setup() {
        mockkStatic(EncryptedSharedPreferences::class)
        mockkStatic(KeyStore::class)
        mockkStatic(MasterKeys::class)
        every { KeyStore.getInstance("AndroidKeyStore") } returns keystore

        every { MasterKeys.getOrCreate(defaultMasterKeySpec) } returns "masterKey"
        every { MasterKeys.getOrCreate(amplifyMasterKeySpec) } returns "amplifyKey"

        folder.create()
    }

    @After
    fun teardown() {
        unmockkAll()
    }

    @Test
    fun `gets a value from a preferences`() {
        val installationFile = folder.newFile()
        installationFile.writeText("abcdef")

        setupSharedPreferences("abcdef")
        every { sharedPreferences.getString("foo", null) } returns "bar"

        val repository = createRepository(installationFile)
        val result = repository.get("foo")
        assertEquals("bar", result)
    }

    @Test
    fun `puts a value into preferences`() {
        val installationFile = folder.newFile()
        installationFile.writeText("abcdef")

        setupSharedPreferences("abcdef")

        val repository = createRepository(installationFile)
        repository.put("foo", "bar")

        verify {
            editor.putString("foo", "bar")
            editor.apply()
        }
    }

    @Test
    fun `deletes a value from preferences`() {
        val installationFile = folder.newFile()
        installationFile.writeText("abcdef")

        setupSharedPreferences("abcdef")

        val repository = createRepository(installationFile)
        repository.remove("foo")

        verify {
            editor.remove("foo")
            editor.apply()
        }
    }

    @Test
    fun `removes all values from preferences`() {
        val installationFile = folder.newFile()
        installationFile.writeText("abcdef")

        setupSharedPreferences("abcdef")

        val repository = createRepository(installationFile)
        repository.removeAll()

        verify {
            editor.clear()
            editor.apply()
        }
    }

    @Test
    fun `gets a value from a keystore created with amplify master key`() {
        val installationFile = folder.newFile()
        installationFile.writeText(amplifyIdentifier("abcdef"))

        setupSharedPreferences(amplifyIdentifier("abcdef"))
        every { sharedPreferences.getString("foo", null) } returns "bar"

        val repository = createRepository(installationFile)
        val result = repository.get("foo")
        assertEquals("bar", result)
    }

    @Test
    fun `uses the amplify master key when creating new repositories`() {
    }

    @Test
    fun `recreates the repository with the amplify key if the default master key is corrupted`() {
        val installationFile = folder.newFile()
        installationFile.writeText("abcdef")
        setupSharedPreferences()

        every { MasterKeys.getOrCreate(defaultMasterKeySpec) } throws GeneralSecurityException("error")

        val repository = createRepository(installationFile)
        repository.put("foo", "bar")

        // Verify encrypted preferences are using the amplify key
        verify {
            EncryptedSharedPreferences.create(
                match { it.startsWith("test.$amplifyIdentifierPrefix") },
                "amplifyKey",
                any(),
                any(),
                any()
            )
        }
    }

    @Test
    fun `updates the installation identifier if the default master key is corrupted`() {
        val installationFile = folder.newFile()
        installationFile.writeText("abcdef")
        setupSharedPreferences()

        every { MasterKeys.getOrCreate(defaultMasterKeySpec) } throws GeneralSecurityException("error")

        val repository = createRepository(installationFile)
        repository.put("foo", "bar")

        // As a side effect the installation identifier should have been updated to the amplify-specific version
        val identifier = installationFile.readText()
        assertTrue(identifier.startsWith(amplifyIdentifierPrefix))
    }

    @Test
    fun `deletes the shared preferences if the default master key is corrupted`() {
        val installationFile = folder.newFile()
        installationFile.writeText("abcdef")
        setupSharedPreferences()

        every { MasterKeys.getOrCreate(defaultMasterKeySpec) } throws GeneralSecurityException("error")

        val repository = createRepository(installationFile)
        repository.put("foo", "bar")

        verify {
            context.deleteSharedPreferences("test.abcdef")
        }
    }

    @Test
    fun `deletes the amplify master key if it's corrupted`() {
        val installationFile = folder.newFile()
        installationFile.writeText(amplifyIdentifier("abcdef"))
        setupSharedPreferences()

        every { MasterKeys.getOrCreate(amplifyMasterKeySpec) }.throws(GeneralSecurityException("error1"))
            .andThenThrows(GeneralSecurityException("error2"))
            .andThenThrows(GeneralSecurityException("error3"))
            .andThen("amplifyKey2")

        val repository = createRepository(installationFile)
        repository.put("foo", "bar")

        verify {
            keystore.deleteEntry("amplify_master_key")
        }
    }

    private fun setupSharedPreferences(identifier: String? = null) {
        every {
            EncryptedSharedPreferences.create(
                if (identifier == null) any() else "test.$identifier",
                when {
                    identifier == null -> any()
                    identifier.startsWith(amplifyIdentifierPrefix) -> "amplifyKey"
                    else -> "masterKey"
                },
                context,
                AES256_SIV,
                AES256_GCM
            )
        } returns sharedPreferences
    }
    private fun amplifyIdentifier(identifier: String) = "$amplifyIdentifierPrefix$identifier"

    private fun createRepository(installationFile: File = folder.newFile()): EncryptedKeyValueRepository {
        return EncryptedKeyValueRepository(
            context,
            "test",
            defaultMasterKeySpec,
            amplifyMasterKeySpec
        ) { _, _ -> installationFile }
    }
}
