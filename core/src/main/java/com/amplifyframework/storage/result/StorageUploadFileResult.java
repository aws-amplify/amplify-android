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

import com.amplifyframework.core.async.Result;

import java.util.Objects;

/**
 * The result of a file upload operation in the Storage category.
 */
public final class StorageUploadFileResult implements Result {
    private final String key;

    private StorageUploadFileResult(String key) {
        this.key = key;
    }

    /**
     * Creates a new StorageUploadFileResult from a storage item key.
     * @param key Key for an item that was uploaded successfully
     * @return A storage upload result containing the item key
     */
    @NonNull
    public static StorageUploadFileResult fromKey(@NonNull String key) {
        return new StorageUploadFileResult(Objects.requireNonNull(key));
    }

    /**
     * Gets the key for the item was successfully uploaded.
     * @return Key for item that was uploaded
     */
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

        StorageUploadFileResult that = (StorageUploadFileResult) thatObject;

        return ObjectsCompat.equals(key, that.key);
    }

    @Override
    public int hashCode() {
        return key != null ? key.hashCode() : 0;
    }
}
