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
