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

package com.amplifyframework.datastore.storage;

import androidx.annotation.NonNull;

import com.amplifyframework.AmplifyException;
import com.amplifyframework.core.model.Model;
import com.amplifyframework.datastore.DataStoreException;
import com.amplifyframework.datastore.DataStoreItemChange;

/**
 * Utility to map {@link StorageItemChange}s to the customer-visible type, {@link DataStoreItemChange}.
 */
public final class ItemChangeMapper {
    private ItemChangeMapper() {}

    /**
     * Converts an {@link StorageItemChange} into an {@link DataStoreItemChange}.
     *
     * @param storageItemChange A storage item change
     * @param <T>               Type of data that was changed in the storage layer
     * @return A data store item change representing the change in storage layer
     * @throws DataStoreException On failure to map corresponding fields for provided data
     */
    public static <T extends Model> DataStoreItemChange<T> map(
            @NonNull StorageItemChange<T> storageItemChange) throws DataStoreException {
        return DataStoreItemChange.<T>builder()
            .initiator(map(storageItemChange.initiator()))
            .item(storageItemChange.item())
            .itemClass(storageItemChange.itemClass())
            .type(map(storageItemChange.type()))
            .uuid(storageItemChange.changeId().toString())
            .build();
    }

    private static DataStoreItemChange.Initiator map(StorageItemChange.Initiator initiator)
            throws DataStoreException {
        switch (initiator) {
            case SYNC_ENGINE:
                return DataStoreItemChange.Initiator.REMOTE;
            case DATA_STORE_API:
                return DataStoreItemChange.Initiator.LOCAL;
            default:
                throw new DataStoreException(
                    "Unknown initiator of storage change: " + initiator,
                    AmplifyException.TODO_RECOVERY_SUGGESTION
                );
        }
    }

    private static DataStoreItemChange.Type map(StorageItemChange.Type type)
            throws DataStoreException {
        switch (type) {
            case DELETE:
                return DataStoreItemChange.Type.DELETE;
            case UPDATE:
                return DataStoreItemChange.Type.UPDATE;
            case CREATE:
                return DataStoreItemChange.Type.CREATE;
            default:
                throw new DataStoreException(
                    "Unknown type of storage change: " + type,
                    AmplifyException.TODO_RECOVERY_SUGGESTION
                );
        }
    }
}
