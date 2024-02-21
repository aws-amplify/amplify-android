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

import androidx.annotation.NonNull;
import androidx.core.util.ObjectsCompat;

import java.util.Objects;

/**
 * A result of a remove operation on the Storage category.
 */
public final class StorageRemoveResult {
    private final String path;
    private final String key;

    /**
     * Creates a new StorageRemoveResult.
     * Although this has public access, it is intended for internal use and should not be used directly by host
     * applications. The behavior of this may change without warning.
     * @param path The path of the storage item that was removed
     * @param key The key of the storage item that was removed
     */
    public StorageRemoveResult(String path, String key) {
        this.path = path;
        this.key = key;
    }

    /**
     * Creates a StorageRemoveResult from a storage key.
     * @deprecated This method should not be used and will result in an incorrect path that
     * shows the key value instead of the full path.
     * @param key The key of the storage item that was removed
     * @return A storage remove result describing key
     */
    @Deprecated
    @NonNull
    public static StorageRemoveResult fromKey(@NonNull String key) {
        return new StorageRemoveResult(
                Objects.requireNonNull(key),
                Objects.requireNonNull(key)
        );
    }

    /**
     * Gets the path of the item that was removed from storage.
     * @return Path for item that was removed from storage
     */
    @NonNull
    public String getPath() {
        return path;
    }

    /**
     * Gets the key of the item that was removed from storage.
     * @deprecated Will be replaced by path because transfer operations that use StoragePath do
     * not have a concept of a "key". Will return the full path if StoragePath was used.
     * @return Key for item that was removed from storage
     */
    @Deprecated
    @NonNull
    public String getKey() {
        return key;
    }

    @Override
    public boolean equals(Object thatObject) {
        if (this == thatObject) {
            return true;
        }
        if (thatObject == null || getClass() != thatObject.getClass()) {
            return false;
        }

        StorageRemoveResult that = (StorageRemoveResult) thatObject;

        return ObjectsCompat.equals(path, that.path) && ObjectsCompat.equals(key, that.key);
    }

    @Override
    public int hashCode() {
        return Objects.hash(path, key);
    }
}
