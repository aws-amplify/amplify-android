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

package com.amplifyframework.storage.result;

import androidx.annotation.NonNull;
import androidx.core.util.ObjectsCompat;

import java.util.Objects;

/**
 * The result of an upload operation in the Storage category.
 */
public class StorageUploadResult {
    private final String key;

    StorageUploadResult(String key) {
        this.key = key;
    }

    /**
     * Creates a new StorageUploadResult from a storage item key.
     * @param key Key for an item that was uploaded successfully
     * @return A storage upload result containing the item key
     */
    @NonNull
    public static StorageUploadResult fromKey(@NonNull String key) {
        return new StorageUploadResult(Objects.requireNonNull(key));
    }

    /**
     * Gets the key for the item was successfully uploaded.
     * @return Key for item that was uploaded
     */
    @NonNull
    public String getKey() {
        return key;
    }

    /**
     * Compare StorageUploadResult with another Object.
     * @param thatObject the Object be compared with
     * @return true if equals, else false
     */
    @Override
    public boolean equals(Object thatObject) {
        if (this == thatObject) {
            return true;
        }
        if (thatObject == null || getClass() != thatObject.getClass()) {
            return false;
        }

        StorageUploadResult that = (StorageUploadResult) thatObject;

        return ObjectsCompat.equals(key, that.key);
    }

    /**
     * Get hashCode.
     * @return hashCode
     */
    @Override
    public int hashCode() {
        return key != null ? key.hashCode() : 0;
    }
}
