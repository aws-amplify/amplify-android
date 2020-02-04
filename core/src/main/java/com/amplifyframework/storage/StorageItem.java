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

package com.amplifyframework.storage;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.util.ObjectsCompat;

import java.util.Date;

/**
 * Used to store the data on each item in a storage.
 */
public final class StorageItem {
    private final String key;
    private final long size;
    private final Date lastModified;
    private final String eTag;
    private final Object pluginResults;

    /**
     * Object to represent an item listing in Storage.
     * @param key The unique identifier of the object in storage.
     * @param size Size in bytes of the object
     * @param lastModified The date the Object was Last Modified
     * @param eTag The entity tag is an MD5 hash of the object.
     *             ETag reflects only changes to the contents of an object, not its metadata.
     * @param pluginResults Additional results specific to the plugin.
     */
    public StorageItem(
            @NonNull String key,
            long size,
            @NonNull Date lastModified,
            @NonNull String eTag,
            @Nullable Object pluginResults
    ) {
        this.key = key;
        this.size = size;
        this.lastModified = lastModified;
        this.eTag = eTag;
        this.pluginResults = pluginResults;
    }

    /**
     * Get unique identifier of the object in storage.
     * @return Unique identifier of the object in storage.
     */
    @NonNull
    public String getKey() {
        return key;
    }

    /**
     * Get size in bytes of the object.
     * @return Size in bytes of the object
     */
    public long getSize() {
        return size;
    }

    /**
     * Get the date the Object was Last Modified.
     * @return The date the Object was Last Modified
     */
    @NonNull
    public Date getLastModified() {
        return lastModified;
    }

    /**
     * Get an MD5 hash of the object.
     * @return An MD5 hash of the object
     */
    @NonNull
    public String getETag() {
        return eTag;
    }

    /**
     * Get additional results specific to the plugin.
     * @return Additional results specific to the plugin.
     */
    @Nullable
    public Object getPluginResults() {
        return pluginResults;
    }

    @Override
    @NonNull
    public String toString() {
        return "StorageItem{" +
                "key='" + key + '\'' +
                ", size=" + size +
                ", lastModified=" + lastModified.toString() +
                ", eTag='" + eTag + '\'' +
                ", pluginResults=" + pluginResults +
                '}';
    }

    @Override
    public boolean equals(@Nullable Object obj) {
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof StorageItem)) {
            return false;
        }

        StorageItem that = (StorageItem) obj;
        if (!ObjectsCompat.equals(key, that.key)) {
            return false;
        }
        if (size != that.size) {
            return false;
        }
        if (!ObjectsCompat.equals(lastModified, that.lastModified)) {
            return false;
        }
        if (!ObjectsCompat.equals(eTag, that.eTag)) {
            return false;
        }
        return ObjectsCompat.equals(pluginResults, that.pluginResults);
    }

    @Override
    @SuppressWarnings("MagicNumber")
    public int hashCode() {
        int result = key.hashCode();
        result = 31 * result + (int) size;
        result = 31 * result + lastModified.hashCode();
        result = 31 * result + eTag.hashCode();
        result = 31 * result + (pluginResults != null ? pluginResults.hashCode() : 0);
        return result;
    }
}
