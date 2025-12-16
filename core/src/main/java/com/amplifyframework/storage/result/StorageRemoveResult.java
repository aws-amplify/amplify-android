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

import com.amplifyframework.annotations.InternalAmplifyApi;

import java.util.Objects;

/**
 * A result of a remove operation on the Storage category.
 */
public final class StorageRemoveResult {
    private final String path;
    private final String key;
    private final boolean isFolder;
    private final int totalFiles;
    private final int deletedCount;
    private final int failedCount;

    /**
     * Creates a new StorageRemoveResult for single file operations.
     * Although this has public access, it is intended for internal use and should not be used directly by host
     * applications. The behavior of this may change without warning.
     * @param path The path of the storage item that was removed
     * @param key The key of the storage item that was removed
     */
    @InternalAmplifyApi
    public StorageRemoveResult(String path, String key) {
        this(path, key, false, 1, 1, 0);
    }

    /**
     * Creates a new StorageRemoveResult for folder operations.
     * Although this has public access, it is intended for internal use and should not be used directly by host
     * applications. The behavior of this may change without warning.
     * @param path The path of the storage item that was removed
     * @param key The key of the storage item that was removed
     * @param isFolder Whether this was a folder operation
     * @param totalFiles Total number of files processed
     * @param deletedCount Number of files successfully deleted
     * @param failedCount Number of files that failed to delete
     */
    @InternalAmplifyApi
    public StorageRemoveResult(String path, String key, boolean isFolder, int totalFiles, int deletedCount, int failedCount) {
        this.path = path;
        this.key = key;
        this.isFolder = isFolder;
        this.totalFiles = totalFiles;
        this.deletedCount = deletedCount;
        this.failedCount = failedCount;
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

    /**
     * Gets whether this was a folder operation.
     * @return true if this was a folder operation, false for single file
     */
    public boolean isFolder() {
        return isFolder;
    }

    /**
     * Gets the total number of files processed in the operation.
     * For single file operations, this will be 1.
     * @return total number of files processed
     */
    public int getTotalFiles() {
        return totalFiles;
    }

    /**
     * Gets the number of files successfully deleted.
     * @return number of files successfully deleted
     */
    public int getDeletedCount() {
        return deletedCount;
    }

    /**
     * Gets the number of files that failed to delete.
     * @return number of files that failed to delete
     */
    public int getFailedCount() {
        return failedCount;
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

        return ObjectsCompat.equals(path, that.path) && 
               ObjectsCompat.equals(key, that.key) &&
               isFolder == that.isFolder &&
               totalFiles == that.totalFiles &&
               deletedCount == that.deletedCount &&
               failedCount == that.failedCount;
    }

    @Override
    public int hashCode() {
        return Objects.hash(path, key, isFolder, totalFiles, deletedCount, failedCount);
    }
}
