/*
 * Copyright 2020 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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
import java.util.Date

/**
 * Used to store the data on each item in a storage.
 * @param path The unique path of the object in storage.
 * @param key The unique identifier of the object in storage.
 * @param size Size in bytes of the object
 * @param lastModified The date the Object was Last Modified
 * @param eTag The entity tag is an MD5 hash of the object.
 * ETag reflects only changes to the contents of an object, not its metadata.
 * @param pluginResults Additional results specific to the plugin.
 */
data class StorageItem @InternalAmplifyApi constructor(
    /**
     * Get unique path of the object in storage.
     * @return Unique path of the object in storage.
     */
    val path: String,
    /**
     * Get unique identifier of the object in storage.
     * @return Unique identifier of the object in storage.
     */
    @Deprecated("Migrate to using path instead")
    val key: String,
    /**
     * Get size in bytes of the object.
     * @return Size in bytes of the object
     */
    val size: Long,
    /**
     * Get the date the Object was Last Modified.
     * @return The date the Object was Last Modified
     */
    val lastModified: Date,
    /**
     * Get an MD5 hash of the object.
     * @return An MD5 hash of the object
     */
    val eTag: String,
    /**
     * Get additional results specific to the plugin.
     * @return Additional results specific to the plugin.
     */
    val pluginResults: Any?
) {

    /**
     * Object to represent an item listing in Storage.
     * @param key The unique identifier of the object in storage.
     * @param size Size in bytes of the object
     * @param lastModified The date the Object was Last Modified
     * @param eTag The entity tag is an MD5 hash of the object.
     * ETag reflects only changes to the contents of an object, not its metadata.
     * @param pluginResults Additional results specific to the plugin.
     */
    @Deprecated("Stop using this constructor in favor of constructor with path")
    constructor(
        key: String,
        size: Long,
        lastModified: Date,
        eTag: String,
        pluginResults: Any?
    ) : this(key, key, size, lastModified, eTag, pluginResults)
}
