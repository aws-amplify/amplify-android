/*
 * Copyright 2021 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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

package com.amplifyframework.storage.s3.configuration

import com.amplifyframework.core.Consumer
import com.amplifyframework.storage.StorageAccessLevel
import com.amplifyframework.storage.StorageException
import com.amplifyframework.storage.s3.CognitoAuthProvider
import com.amplifyframework.storage.s3.utils.S3Keys

/**
 * Resolves the final prefix prepended to the S3 key for a given request.
 **/
interface AWSS3PluginPrefixResolver {

    /**
     * Resolves the final prefix prepended to the S3 key
     * @param accessLevel Storage access level of the request
     * @param targetIdentity Identity ID of the user
     * @param onSuccess success callback with the resolved prefix string
     * @param onError error callback with storage excetion
     */
    fun resolvePrefix(
        accessLevel: StorageAccessLevel,
        targetIdentity: String?,
        onSuccess: Consumer<String>,
        onError: Consumer<StorageException>
    )
}

/**
 * Default prefix resolver based on the storage access level.
 **/
internal class StorageAccessLevelAwarePrefixResolver(
    private val cognitoAuthProvider: CognitoAuthProvider
) :
    AWSS3PluginPrefixResolver {

    override fun resolvePrefix(
        accessLevel: StorageAccessLevel,
        targetIdentity: String?,
        onSuccess: Consumer<String>,
        onError: Consumer<StorageException>
    ) {
        val identityId = runCatching {
            cognitoAuthProvider.identityId
        }
        when {
            identityId.isSuccess -> {
                val resultIdentityId = targetIdentity ?: identityId.getOrThrow()
                onSuccess.accept(S3Keys.getAccessLevelPrefix(accessLevel, resultIdentityId))
            }
            else -> {
                onError.accept(identityId.exceptionOrNull() as StorageException)
            }
        }
    }
}
