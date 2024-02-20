package com.amplifyframework.storage

/**
 * Used as Lambda function which provides ownerId to help with returning custom path
 * @param ownerId provided ownerId if available
 * @return path for storage operation
 */
typealias OwnerIdPathResolver = (ownerId: String?) -> String

/**
 * @param ownerIdPathResolver lambda function returning path for operation
 */
class StoragePath (internal val ownerIdPathResolver: OwnerIdPathResolver) {

    /**
     * @param path path for operation
     */
    constructor(path: String) : this({ _ -> path })
}
