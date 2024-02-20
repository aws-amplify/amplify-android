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
 * The result of a file upload operation in the Storage category.
 */
public final class StorageUploadFileResult extends StorageUploadResult {

    private StorageUploadFileResult(String path, String key) {
        super(path, key);
    }

    /**
     * Creates a new StorageUploadFileResult from a storage item key.
     * @deprecated This method should not be used since path will be incorrect.
     * @param key Key for an item that was uploaded successfully
     * @return A storage upload result containing the item key
     */
    @Deprecated
    @NonNull
    public static StorageUploadFileResult fromKey(@NonNull String key) {
        return new StorageUploadFileResult(
                Objects.requireNonNull(key),
                Objects.requireNonNull(key)
        );
    }

    /**
     * Creates a new StorageUploadFileResult from a storage item path.
     * Although this has public access, it is intended for internal use and should not be used directly by host
     * applications. The behavior of this may change without warning.
     * * @param path Path for an item that was uploaded successfully
     * @return A storage upload result containing the item path
     */
    @NonNull
    public static StorageUploadFileResult fromPath(@NonNull String path) {
        return new StorageUploadFileResult(
                Objects.requireNonNull(path),
                Objects.requireNonNull(path)
        );
    }

    /**
     * Creates a new StorageUploadFileResult from a storage item path.
     * @deprecated This method is temporary to internally support older transfer methods that
     * we will additionally add path for.
     * Although this has public access, it is intended for internal use and should not be used directly by host
     * applications. The behavior of this may change without warning.
     * * @param path Path for an item that was uploaded successfully
     * @return A storage upload result containing the item path
     */
    @Deprecated
    @NonNull
    public static StorageUploadFileResult fromPathAndKey(@NonNull String path, @NonNull String key) {
        return new StorageUploadFileResult(
                Objects.requireNonNull(path),
                Objects.requireNonNull(key)
        );
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

        return ObjectsCompat.equals(super.getKey(), that.getKey()) &&
                ObjectsCompat.equals(super.getPath(), that.getPath());
    }

    @Override
    public int hashCode() {
        return super.hashCode();
    }
}
