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
import androidx.annotation.Nullable;

import com.amplifyframework.core.async.Result;

import java.util.Objects;

/**
 * A result of a put operation on the Storage category.
 */
public final class StoragePutResult implements Result {
    private final String key;

    private StoragePutResult(String key) {
        this.key = key;
    }

    /**
     * Creates a new StoragePutResult from a storage item key.
     * @param key Key for an item that was put successfully
     * @return A storage put result containing the item key
     */
    @NonNull
    public static StoragePutResult fromKey(@NonNull String key) {
        return new StoragePutResult(Objects.requireNonNull(key));
    }

    /**
     * Gets the key for the item was successfully put.
     * @return Key for item that was put
     */
    @NonNull
    public String getKey() {
        return key;
    }

    @Override
    public int hashCode() {
        return key.hashCode();
    }

    @Override
    public boolean equals(@Nullable Object thatObject) {
        if (!(thatObject instanceof StoragePutResult)) {
            return false;
        }
        final StoragePutResult that = (StoragePutResult) thatObject;
        return this.key.equals(that.getKey());
    }
}
