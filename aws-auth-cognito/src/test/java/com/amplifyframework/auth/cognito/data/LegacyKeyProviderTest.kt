package com.amplifyframework.auth.cognito.data

import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.MockedStatic
import org.mockito.Mockito
import org.mockito.junit.MockitoJUnitRunner
import java.security.Key
import java.security.KeyStore
import java.security.KeyStoreSpi
import java.security.Provider
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@RunWith(MockitoJUnitRunner::class)
class LegacyKeyProviderTest {

    private val androidKeyStoreAlias = "AndroidKeyStore"
    private val testKeyAlias = "Test Key"

    // This is mocked using MockStatic and needs to be closed
    private val mockedStaticKeyStore: MockedStatic<KeyStore> = Mockito.mockStatic(KeyStore::class.java)

    @Mock
    private lateinit var mockKeyStore: FakeKeyStore

    @Mock
    private lateinit var mockKey: Key

    @Before
    fun setUp() {
        mockedStaticKeyStore.`when`<KeyStore> { KeyStore.getInstance(Mockito.anyString()) }.thenReturn(mockKeyStore)
    }

    @After
    fun tearDown() {
        mockedStaticKeyStore.close()
    }

    @Test
    fun `retrieve key fails if key does not exist for key alias`() {
        val key = LegacyKeyProvider.retrieveKey(testKeyAlias)

        assertTrue { key is Result.Failure }
        assertEquals((key as Result.Failure).message,
            "Key does not exists for the keyAlias: $testKeyAlias in $androidKeyStoreAlias")
    }

    @Test
    fun `retrieve key fails if keyStore returns null`() {
        Mockito.`when`(mockKeyStore.containsAlias(testKeyAlias)).thenReturn(true)
        Mockito.`when`(mockKeyStore.getKey(testKeyAlias, null)).thenReturn(null)

        val key = LegacyKeyProvider.retrieveKey(testKeyAlias)
        assertTrue { key is Result.Failure }
        assertEquals((key as Result.Failure).message,
            "Key is null even though the keyAlias: " +
                    testKeyAlias + " is present in " + androidKeyStoreAlias)
    }

    @Test
    fun `retrieve key returns key from key store`() {
        Mockito.`when`(mockKeyStore.containsAlias(testKeyAlias)).thenReturn(true)
        Mockito.`when`(mockKeyStore.getKey(testKeyAlias, null)).thenReturn(mockKey)

        val key = LegacyKeyProvider.retrieveKey(testKeyAlias)
        assertTrue { key is Result.Success }
        assertEquals((key as Result.Success).value, mockKey)
    }

    private class FakeKeyStore(keyStoreSpi: KeyStoreSpi?, provider: Provider?, type: String?) :
        KeyStore(keyStoreSpi, provider, type)
}