package com.amplifyframework.storage

import com.amplifyframework.annotations.InternalAmplifyApi

typealias IdentityIdPathResolver = (identityId: String) -> String

abstract class StoragePath {

    data class StringStoragePath(private val path: String) : StoragePath() {

        @InternalAmplifyApi
        fun resolvePath() = path
    }

    data class IdentityIdProvidedStoragePath(
        private val identityIdPathResolver: IdentityIdPathResolver
    ) : StoragePath() {

        @InternalAmplifyApi
        fun resolvePath(identityId: String): String {
            return identityIdPathResolver.invoke(identityId)
        }
    }

    companion object {
        @JvmStatic
        fun fromString(path: String): StoragePath = StringStoragePath(path)

        @JvmStatic
        fun withIdentityId(identityIdPathResolver: IdentityIdPathResolver): StoragePath =
            IdentityIdProvidedStoragePath(identityIdPathResolver)
    }
}
