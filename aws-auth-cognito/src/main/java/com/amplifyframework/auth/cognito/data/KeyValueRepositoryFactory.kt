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
import com.amplifyframework.auth.cognito.data.AWSCognitoAuthCredentialStore.Companion.awsKeyValueStoreIdentifier
import com.amplifyframework.auth.cognito.data.AWSCognitoLegacyCredentialStore.Companion.APP_DEVICE_INFO_CACHE
import com.amplifyframework.auth.cognito.data.AWSCognitoLegacyCredentialStore.Companion.APP_TOKENS_INFO_CACHE
import com.amplifyframework.auth.cognito.data.AWSCognitoLegacyCredentialStore.Companion.AWS_KEY_VALUE_STORE_NAMESPACE_IDENTIFIER
import com.amplifyframework.auth.cognito.data.AWSCognitoLegacyCredentialStore.Companion.AWS_MOBILE_CLIENT_PROVIDER
import com.amplifyframework.core.store.EncryptedKeyValueRepository
import com.amplifyframework.core.store.InMemoryKeyValueRepository
import com.amplifyframework.core.store.KeyValueRepository

internal class KeyValueRepositoryFactory {
    fun create(context: Context, keyValueRepoID: String, persistenceEnabled: Boolean = true): KeyValueRepository {
        return when {
            keyValueRepoID == awsKeyValueStoreIdentifier -> when {
                persistenceEnabled -> EncryptedKeyValueRepository(context, keyValueRepoID)
                else -> InMemoryKeyValueRepository()
            }
            keyValueRepoID == AWS_KEY_VALUE_STORE_NAMESPACE_IDENTIFIER ||
                keyValueRepoID == APP_TOKENS_INFO_CACHE ||
                keyValueRepoID == AWS_MOBILE_CLIENT_PROVIDER ||
                keyValueRepoID.startsWith(APP_DEVICE_INFO_CACHE) ->
                LegacyKeyValueRepository(context, keyValueRepoID)
            else -> InMemoryKeyValueRepository()
        }
    }
}
