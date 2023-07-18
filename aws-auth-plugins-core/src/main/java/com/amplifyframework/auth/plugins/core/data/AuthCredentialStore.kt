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

package com.amplifyframework.auth.plugins.core.data

import android.content.Context
import com.amplifyframework.core.store.EncryptedKeyValueRepository
import com.amplifyframework.core.store.InMemoryKeyValueRepository
import com.amplifyframework.core.store.KeyValueRepository

internal class AuthCredentialStore(
    context: Context,
    keyValueStoreIdentifierSuffix: String,
    isPersistenceEnabled: Boolean
) {
    private val keyValueStoreIdentifier = "com.amplify.credentialStore.$keyValueStoreIdentifierSuffix"

    private val keyValue: KeyValueRepository = if (isPersistenceEnabled) {
        EncryptedKeyValueRepository(context, keyValueStoreIdentifier)
    } else {
        InMemoryKeyValueRepository()
    }

    fun put(key: String, value: String) = keyValue.put(key, value)
    fun get(key: String): String? = keyValue.get(key)
    fun remove(key: String) = keyValue.remove(key)
    fun removeAll() = keyValue.removeAll()
}
