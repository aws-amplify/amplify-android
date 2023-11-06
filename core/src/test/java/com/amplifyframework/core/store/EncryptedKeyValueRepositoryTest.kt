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
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.Mockito
import org.mockito.Mockito.anyString
import org.mockito.Mockito.times
import org.mockito.junit.MockitoJUnitRunner
import java.security.GeneralSecurityException

@RunWith(MockitoJUnitRunner::class)
class EncryptedKeyValueRepositoryTest {

    // Testing using partial mock as the sut is basically a wrapper
    // of EncryptedSharedPreferences which is created statically, making it difficult to mock or stub
    @Mock
    internal lateinit var repository: EncryptedKeyValueRepository

    @Mock
    lateinit var context: Context

    @Mock
    lateinit var mockPrefs: SharedPreferences

    @Mock
    lateinit var mockPrefsEditor: SharedPreferences.Editor

    companion object {
        private const val TEST_KEY = "test Data"
        private const val TEST_VAL = "test Val"
        private const val TEST_STRING = "test"
    }

    @Before
    fun setup() {
        Mockito.`when`(repository.sharedPreferences).thenReturn(mockPrefs)
        Mockito.`when`(repository.editor).thenReturn(mockPrefsEditor)
    }

    @Test
    fun testPut() {
        Mockito.`when`(repository.put(anyString(), anyString())).thenCallRealMethod()
        repository.put(TEST_KEY, TEST_VAL)
        Mockito.verify(mockPrefsEditor, times(1)).putString(TEST_KEY, TEST_VAL)
        Mockito.verify(mockPrefsEditor, times(1)).apply()
    }

    @Test
    fun testGet() {
        Mockito.`when`(repository.get(anyString())).thenCallRealMethod()
        repository.get(TEST_KEY)
        Mockito.verify(mockPrefs, times(1)).getString(TEST_KEY, null)
    }

    @Test
    fun testRemove() {
        Mockito.`when`(repository.remove(anyString())).thenCallRealMethod()
        repository.remove(TEST_KEY)
        Mockito.verify(mockPrefsEditor, times(1)).remove(TEST_KEY)
        Mockito.verify(mockPrefsEditor, times(1)).apply()
    }

    @Test
    fun testCorruptionOfMasterKey(){
        Mockito.`when`(repository.createEncryptedSharedPreferences(TEST_KEY)).thenThrow(GeneralSecurityException())
        Mockito.`when`(repository.getInstallationIdentifier(context, TEST_STRING)).thenReturn(TEST_STRING)
        Mockito.`when`(repository.get(anyString())).thenCallRealMethod()
        repository.get(TEST_KEY)
        Mockito.verify(mockPrefs, times(1)).getString(TEST_KEY, null)
    }
}
