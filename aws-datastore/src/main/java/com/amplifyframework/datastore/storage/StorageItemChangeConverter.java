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

package com.amplifyframework.datastore.storage;

import androidx.annotation.NonNull;

import com.amplifyframework.core.model.Model;
import com.amplifyframework.datastore.DataStoreException;

/**
 * A component which is able to convert from {@link StorageItemChange} and
 * {@link StorageItemChangeRecord}, and vice-versa.
 */
public interface StorageItemChangeConverter {
    /**
     * Converts a {@link StorageItemChange} into a {@link StorageItemChangeRecord}.
     * @param change A storage item change instance, to be made into a record
     * @return A Record corresponding to the storage item change.
     * @param <T> Type of item being kept in the StorageItemChange.
     */
    @NonNull
    <T extends Model> StorageItemChangeRecord toRecord(@NonNull StorageItemChange<T> change);

    /**
     * Converts a {@link StorageItemChangeRecord} into a {@link StorageItemChange}.
     * @param record Record to convert into a storage item change
     * @param <T> Type of item represented inside of the change record
     * @return A {@link StorageItemChange} representation of provided record
     * @throws DataStoreException If unable to perform the conversion
     */
    @NonNull
    <T extends Model> StorageItemChange<T> fromRecord(@NonNull StorageItemChangeRecord record)
            throws DataStoreException;
}
