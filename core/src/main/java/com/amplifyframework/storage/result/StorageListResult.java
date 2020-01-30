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
import com.amplifyframework.storage.StorageItem;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * A result of an list operation on the Storage category.
 */
public final class StorageListResult implements Result {
    private final List<StorageItem> items;

    private StorageListResult(List<StorageItem> items) {
        this.items = items;
    }

    /**
     * Factory method to construct a storage list result from a list of items.
     * @param items A possibly null, possibly empty list of items
     * @return A new immutable instance of StorageListResult
     */
    @NonNull
    public static StorageListResult fromItems(@Nullable List<StorageItem> items) {
        final List<StorageItem> safeItems = new ArrayList<>();
        if (items != null) {
            safeItems.addAll(items);
        }
        return new StorageListResult(Collections.unmodifiableList(safeItems));
    }

    /**
     * Gets the items retrieved by the list API.
     * @return List of items that were returned by the Storage category's list API(s).
     */
    @NonNull
    public List<StorageItem> getItems() {
        return items;
    }
}
