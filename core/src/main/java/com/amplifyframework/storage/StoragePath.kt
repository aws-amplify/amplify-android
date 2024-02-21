package com.amplifyframework.storage

import androidx.annotation.WorkerThread

/**
 * Used as Lambda function which provides ownerId to help with returning custom path
 * @return path for storage operation
 */
typealias OwnerIdPathResolver = (ownerId: String?) -> String

/**
 * @param ownerIdPathResolver lambda function returning path for operation
 */
class StoragePath(private val ownerIdPathResolver: OwnerIdPathResolver) {

    /**
     * @param path path for operation
     */
    constructor(path: String) : this({ _ -> path })

    @WorkerThread
    fun resolve(ownerId: String? = null): String {
        return ownerIdPathResolver.invoke(ownerId)
    }
}
