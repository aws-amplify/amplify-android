package com.amplifyframework.storage.s3.extensions

import com.amplifyframework.auth.AuthCredentialsProvider
import com.amplifyframework.storage.IdentityIdProvidedStoragePath
import com.amplifyframework.storage.StorageException
import com.amplifyframework.storage.StoragePath
import com.amplifyframework.storage.StoragePathValidationException
import com.amplifyframework.storage.StringStoragePath

suspend fun StoragePath.toS3ServiceKey(authCredentialsProvider: AuthCredentialsProvider): String {
    val stringPath = when (this) {
        is StringStoragePath -> {
            resolvePath()
        }
        is IdentityIdProvidedStoragePath -> {
            val identityId = try {
                authCredentialsProvider.getIdentityId()
            } catch (e: Exception) {
                throw StorageException(
                    "Failed to fetch identity ID",
                    e,
                    "See included exception for more details and suggestions to fix."
                )
            }
            resolvePath(identityId)
        }

        else -> {
            throw StoragePathValidationException.unsupportedStoragePathException()
        }
    }

    if (stringPath.startsWith("/") || stringPath.isEmpty()) {
        throw StoragePathValidationException.invalidStoragePathException()
    }

    return stringPath
}
