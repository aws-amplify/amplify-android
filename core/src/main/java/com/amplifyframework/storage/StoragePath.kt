package com.amplifyframework.storage

import com.amplifyframework.annotations.InternalAmplifyApi

typealias IdentityIdPathResolver = (identityId: String) -> String

abstract class StoragePath {

    companion object {
        @JvmStatic
        fun fromString(path: String): StoragePath = StringStoragePath(path)

        @JvmStatic
        fun fromIdentityId(identityIdPathResolver: IdentityIdPathResolver): StoragePath =
            IdentityIdProvidedStoragePath(identityIdPathResolver)
    }
}

data class StringStoragePath internal constructor(private val path: String) : StoragePath() {

    @InternalAmplifyApi
    fun resolvePath() = path
}

data class IdentityIdProvidedStoragePath internal constructor(
    private val identityIdPathResolver: IdentityIdPathResolver
) : StoragePath() {

    @InternalAmplifyApi
    fun resolvePath(identityId: String): String {
        return identityIdPathResolver.invoke(identityId)
    }
}
