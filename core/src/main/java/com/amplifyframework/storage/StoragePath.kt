package com.amplifyframework.storage

/**
 * @param identityId lambda function returning path for operation
 */
typealias IdentityIdPathResolver = (identityId: String) -> String

sealed class StoragePath {

    data class StringStoragePath(val path: String) : StoragePath()

    data class IdentityIdProvidedStoragePath(
        val identityIdPathResolver: IdentityIdPathResolver
    ) : StoragePath() {
        fun resolvePath(identityId: String): String {
            return identityIdPathResolver.invoke(identityId)
        }
    }

    companion object {
        @JvmStatic
        operator fun invoke(path: String): StoragePath = StringStoragePath(path)

        @JvmStatic
        operator fun invoke(identityIdPathResolver: IdentityIdPathResolver): StoragePath =
            IdentityIdProvidedStoragePath(identityIdPathResolver)
    }
}
