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
    fun resolvePath(identityId: String): String {
        return identityIdPathResolver.invoke(identityId)
    }
}
