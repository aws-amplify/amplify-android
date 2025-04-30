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
package com.amplifyframework.storage

import com.amplifyframework.annotations.InternalAmplifyApi

/**
 * Resolves identityId to be injected into String for StoragePath.
 */
typealias IdentityIdPathResolver = (identityId: String) -> String

/**

 * StoragePath is a wrapper class that provides the ability to resolve transfer paths at the time of transfer.
 */
abstract class StoragePath {

    companion object {

        /**
         * Generate a StoragePath from String.
         *
         * @param path A full transfer path in String format
         * @return path
         */
        @JvmStatic
        fun fromString(path: String): StoragePath = StringStoragePath(path)

        /**
         * Generate a StoragePath from IdentityIdPathResolver.
         *
         * @param identityIdPathResolver resolves the StoragePath with the ability to inject identityId's at the time
         * of transfer.
         * @return path
         */
        @JvmStatic
        fun fromIdentityId(identityIdPathResolver: IdentityIdPathResolver): StoragePath =
            IdentityIdProvidedStoragePath(identityIdPathResolver)
    }
}

/**
 * StoragePath that was created with the full String path.
 */
data class StringStoragePath internal constructor(private val path: String) : StoragePath() {

    @InternalAmplifyApi
    fun resolvePath() = path
}

/**
 * StoragePath that is resolved by providing the identityId.
 */
data class IdentityIdProvidedStoragePath internal constructor(
    private val identityIdPathResolver: IdentityIdPathResolver
) : StoragePath() {

    @InternalAmplifyApi
    fun resolvePath(identityId: String): String = identityIdPathResolver.invoke(identityId)
}
