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
 * A result of a remove operation on the Storage category.
 */
public final class StorageRemoveResult implements Result {
    private final String key;

    private StorageRemoveResult(String key) {
        this.key = key;
    }

    /**
     * Creates a StorageRemoveResult from a storage key.
     * @param key The key of the storage item that was removed
     * @return A storage remove result describing key
     */
    @NonNull
    public static StorageRemoveResult fromKey(@NonNull String key) {
        return new StorageRemoveResult(Objects.requireNonNull(key));
    }

    /**
     * Gets the key of the item that was removed from storage.
     * @return Key for item that was removed from storage
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
        if (!(thatObject instanceof StorageRemoveResult)) {
            return false;
        }
        final StorageRemoveResult that = (StorageRemoveResult) thatObject;
        return (this.key.equals(that.getKey()));
    }
}
