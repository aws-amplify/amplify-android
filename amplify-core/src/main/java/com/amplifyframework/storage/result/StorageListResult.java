/*
 * Copyright 2019 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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

package com.amplifyframework.storage.result;

import androidx.annotation.Nullable;

import com.amplifyframework.core.async.Result;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;

/**
 * A result of an list operation on the Storage category.
 */
public final class StorageListResult implements Result {
    private final List<Item> items;

    private StorageListResult(List<Item> items) {
        this.items = items;
    }

    /**
     * Factory method to construct a storage list result from a list of items.
     * @param items A possibly null, possibly empty list of items
     * @return A new immutable instance of StorageListResult
     */
    public static StorageListResult fromItems(@Nullable List<Item> items) {
        final List<Item> safeItems = new ArrayList<>();
        if (items != null) {
            safeItems.addAll(items);
        }
        return new StorageListResult(Collections.unmodifiableList(safeItems));
    }

    /**
     * Gets the items retrieved by the list API.
     * @return List of items that were returned by the Storage category's list API(s).
     */
    public List<Item> getItems() {
        return items;
    }

    /**
     * Used to store the data on each item in a storage path.
     */
    public static final class Item {
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
        public Item(String key, long size, Date lastModified, String eTag, Object pluginResults) {
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
        public Date getLastModified() {
            return lastModified;
        }

        /**
         * Get an MD5 hash of the object.
         * @return An MD5 hash of the object
         */
        public String getETag() {
            return eTag;
        }

        /**
         * Get additional results specific to the plugin.
         * @return Additional results specific to the plugin.
         */
        public Object getPluginResults() {
            return pluginResults;
        }
    }
}

