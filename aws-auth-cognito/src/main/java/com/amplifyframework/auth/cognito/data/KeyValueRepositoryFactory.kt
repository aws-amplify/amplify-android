package com.amplifyframework.auth.cognito.data

import android.content.Context

class KeyValueRepositoryFactory {
    fun create(context: Context, keyValueRepoID: String, persistenceEnabled: Boolean): KeyValueRepository {
        return when {
            persistenceEnabled -> EncryptedKeyValueRepository(context, keyValueRepoID)
            else -> InMemoryKeyValueRepository()
        }
    }
}
