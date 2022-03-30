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

import java.security.Key
import java.security.KeyStore
import java.security.KeyStoreSpi
import java.security.Provider
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.MockedStatic
import org.mockito.Mockito
import org.mockito.junit.MockitoJUnitRunner

@RunWith(MockitoJUnitRunner::class)
class LegacyKeyProviderTest {

    private val androidKeyStoreAlias = "AndroidKeyStore"
    private val testKeyAlias = "Test Key"

    // This is mocked using MockStatic and needs to be closed
    private val mockedStaticKeyStore: MockedStatic<KeyStore> = Mockito.mockStatic(
        KeyStore::class.java
    )

    @Mock
    private lateinit var mockKeyStore: FakeKeyStore

    @Mock
    private lateinit var mockKey: Key

    @Before
    fun setUp() {
        mockedStaticKeyStore.`when`<KeyStore> { KeyStore.getInstance(Mockito.anyString()) }.thenReturn(
            mockKeyStore
        )
    }

    @After
    fun tearDown() {
        mockedStaticKeyStore.close()
    }

    @Test
    fun `retrieve key fails if key does not exist for key alias`() {
        val key = LegacyKeyProvider.retrieveKey(testKeyAlias)

        assertTrue { key.isFailure }
        assertEquals(
            "Key does not exists for the keyAlias: $testKeyAlias in $androidKeyStoreAlias",
            (key.exceptionOrNull()?.message)
        )
    }

    @Test
    fun `retrieve key fails if keyStore returns null`() {
        Mockito.`when`(mockKeyStore.containsAlias(testKeyAlias)).thenReturn(true)
        Mockito.`when`(mockKeyStore.getKey(testKeyAlias, null)).thenReturn(null)

        val key = LegacyKeyProvider.retrieveKey(testKeyAlias)
        assertTrue { key.isFailure }
        assertEquals(
            "Key is null even though the keyAlias: " +
                testKeyAlias + " is present in " + androidKeyStoreAlias,
            key.exceptionOrNull()?.message
        )
    }

    @Test
    fun `retrieve key returns key from key store`() {
        Mockito.`when`(mockKeyStore.containsAlias(testKeyAlias)).thenReturn(true)
        Mockito.`when`(mockKeyStore.getKey(testKeyAlias, null)).thenReturn(mockKey)

        val key = LegacyKeyProvider.retrieveKey(testKeyAlias)
        assertTrue { key.isSuccess }
        assertEquals(key.getOrNull(), mockKey)
    }

    private class FakeKeyStore(keyStoreSpi: KeyStoreSpi?, provider: Provider?, type: String?) :
        KeyStore(keyStoreSpi, provider, type)
}
