/*
 * Copyright 2024 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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

class AmplifyV2KeyValueRepositoryProvider(private val context: Context) : KeyValueRepositoryProvider {
    // Keep repositories in memory so we aren't continually re-opening
    private val keyValueRepositories = mutableMapOf<String, KeyValueRepository>()

    @Synchronized
    override fun get(repositoryIdentifier: String): KeyValueRepository {
        return keyValueRepositories.getOrPut(repositoryIdentifier) {
            EncryptedKeyValueRepository(context, repositoryIdentifier)
        }
    }
}
