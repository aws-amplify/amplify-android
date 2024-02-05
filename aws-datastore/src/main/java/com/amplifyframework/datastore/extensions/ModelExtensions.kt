package com.amplifyframework.datastore.extensions

import com.amplifyframework.core.model.Model

/**
 * This method returns the primary key that was used on the ModelMetadata table for the given model.
 * The returned value should only be used to construct the lookup sqlite key, and is not a value used by AppSync
 * @return the primary key that was used on the ModelMetadata table for the given model
 */
internal fun Model.getMetadataSQLitePrimaryKey(): String {
    return "$modelName|$primaryKeyString"
}
