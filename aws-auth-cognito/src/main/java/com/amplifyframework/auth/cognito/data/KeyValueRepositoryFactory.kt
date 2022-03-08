package com.amplifyframework.auth.cognito.data

import android.content.Context
import com.amplifyframework.auth.cognito.data.AWSCognitoLegacyCredentialStore.Companion.APP_LOCAL_CACHE
import com.amplifyframework.auth.cognito.data.AWSCognitoLegacyCredentialStore.Companion.AWS_KEY_VALUE_STORE_NAMESPACE_IDENTIFIER

class KeyValueRepositoryFactory {
    fun create(context: Context, keyValueRepoID: String, persistenceEnabled: Boolean = true): KeyValueRepository {
        return when (keyValueRepoID) {
            AWSCognitoAuthCredentialStore.awsKeyValueStoreIdentifier ->
                when {
                    persistenceEnabled -> EncryptedKeyValueRepository(context, keyValueRepoID)
                    else -> InMemoryKeyValueRepository()
                }
            AWS_KEY_VALUE_STORE_NAMESPACE_IDENTIFIER, APP_LOCAL_CACHE ->
                LegacyKeyValueRepository(context, keyValueRepoID)
            else -> InMemoryKeyValueRepository()
        }
    }
}
