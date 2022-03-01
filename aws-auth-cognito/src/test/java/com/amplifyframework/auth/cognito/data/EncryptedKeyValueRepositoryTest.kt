package com.amplifyframework.auth.cognito.data

import android.content.Context
import android.content.SharedPreferences
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.Mockito
import org.mockito.Mockito.atMost
import org.mockito.Mockito.times
import org.mockito.junit.MockitoJUnitRunner

@RunWith(MockitoJUnitRunner::class)
class EncryptedKeyValueRepositoryTest {

    // Testing using partial mock as the sut is basically a wrapper
    // of EncryptedSharedPreferences which is created statically, making it difficult to mock or stub
    @Mock
    lateinit var repository: EncryptedKeyValueRepository

    @Mock
    lateinit var context: Context

    @Mock
    lateinit var mockPrefs: SharedPreferences

    @Mock
    lateinit var mockPrefsEditor: SharedPreferences.Editor

    companion object {
        private const val TEST_KEY = "test Data"
        private const val TEST_VAL = "test Val"
    }

    @Before
    fun setup() {
        Mockito.`when`(repository.getSharedPreferences()).thenReturn(mockPrefs)
        Mockito.`when`(mockPrefs.edit()).thenReturn(mockPrefsEditor)
    }

    @Test
    fun testPut() {
        Mockito.`when`(repository.put(Mockito.anyString(), Mockito.anyString())).thenCallRealMethod()
        repository.put(TEST_KEY, TEST_VAL)
        Mockito.verify(mockPrefsEditor, times(1)).putString(TEST_KEY, TEST_VAL)
        Mockito.verify(mockPrefsEditor, times(1)).apply()
    }

    @Test
    fun testGet() {
        Mockito.`when`(repository.get(Mockito.anyString())).thenCallRealMethod()
        repository.get(TEST_KEY)
        Mockito.verify(mockPrefs, times(1)).getString(TEST_KEY, null)
    }

    @Test
    fun testRemove() {
        Mockito.`when`(repository.remove(Mockito.anyString())).thenCallRealMethod()
        repository.remove(TEST_KEY)
        Mockito.verify(mockPrefsEditor, times(1)).remove(TEST_KEY)
        Mockito.verify(mockPrefsEditor, times(1)).apply()
    }
}