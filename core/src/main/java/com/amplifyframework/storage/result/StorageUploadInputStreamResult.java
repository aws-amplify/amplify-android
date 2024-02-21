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
 * The result of a InputStream upload operation in the Storage category.
 */
public final class StorageUploadInputStreamResult extends StorageUploadResult {

    /**
     * Creates a new StorageUploadFileResult from a storage item path.
     * Although this has public access, it is intended for internal use and should not be used directly by host
     * applications. The behavior of this may change without warning.
     * @param path Path for an item that was uploaded successfully
     * @param key Key for an item that was uploaded successfully
     */
    public StorageUploadInputStreamResult(String path, String key) {
        super(path, key);
    }

    /**
     * Creates a new StorageUploadFileResult from a storage item key.
     * @deprecated This method should not be used and will result in an incorrect path that
     * shows the key value instead of the full path.
     * @param key Key for an item that was uploaded successfully
     * @return A storage upload result containing the item key
     */
    @Deprecated
    @NonNull
    public static StorageUploadInputStreamResult fromKey(@NonNull String key) {
        return new StorageUploadInputStreamResult(
                Objects.requireNonNull(key),
                Objects.requireNonNull(key)
        );
    }

    @Override
    @SuppressWarnings("deprecation")
    public boolean equals(Object thatObject) {
        if (this == thatObject) {
            return true;
        }
        if (thatObject == null || getClass() != thatObject.getClass()) {
            return false;
        }

        StorageUploadInputStreamResult that = (StorageUploadInputStreamResult) thatObject;

        return ObjectsCompat.equals(super.getKey(), that.getKey()) &&
                ObjectsCompat.equals(super.getPath(), this.getPath());
    }

    @Override
    public int hashCode() {
        return super.hashCode();
    }
}
