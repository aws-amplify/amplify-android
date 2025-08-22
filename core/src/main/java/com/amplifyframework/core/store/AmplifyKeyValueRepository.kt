/*
 * Copyright 2025 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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
import com.amplifyframework.annotations.InternalAmplifyApi

@InternalAmplifyApi
class AmplifyKeyValueRepository(
    private val context: Context,
    private val sharedPreferencesName: String
) : KeyValueRepository {

    // We attempt to get an encrypted persistent repository, but if that fails, use an in memory one instead.
    private val repository: KeyValueRepository by lazy {
        try {
            EncryptedKeyValueRepository(context, sharedPreferencesName).also {
                // This attempts to open EncryptedSharedPrefs. If it opens, we are good to use.
                it.sharedPreferences
            }
        } catch (exception: Exception) {
            // We crashed attempting to open EncryptedKeyValueRepository, use In-Memory Instead.
            InMemoryKeyValueRepositoryProvider.getKeyValueRepository(sharedPreferencesName)
        }
    }

    override fun get(dataKey: String): String? = repository.get(dataKey)

    override fun put(dataKey: String, value: String?) = repository.put(dataKey, value)

    override fun remove(dataKey: String) = repository.remove(dataKey)

    override fun removeAll() = repository.removeAll()
}
