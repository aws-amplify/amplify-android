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

import androidx.annotation.Nullable;

/**
 * Represents the current amount of progress that has made during a storage transfer operation.
 */
public final class StorageTransferProgress {
    private final long currentBytes;
    private final long totalBytes;

    /**
     * Creates a new StorageTransferProgress instance.
     * @param currentBytes The number of bytes that have been transferred so far
     * @param totalBytes The total number of bytes that are expected to transfer
     */
    public StorageTransferProgress(long currentBytes, long totalBytes) {
        this.currentBytes = currentBytes;
        this.totalBytes = totalBytes;
    }

    /**
     * Gets the current number of bytes that have been transferred.
     * This is number greater than 0, and less than or equal to the value of
     * {@link #getTotalBytes()}. For example, if 5 bytes have been transferred,
     * and 10 bytes are expected to transfer, this value is 5.
     * @return The current number of bytes that have been transferred
     */
    public long getCurrentBytes() {
        return currentBytes;
    }

    /**
     * Gets the total number of bytes that are expected to transfer. When this number
     * of bytes has transferred, the transfer is complete. This value is greater than or
     * equal to the value of {@link #getCurrentBytes()}.
     * @return The total number of bytes that are expected to transfer.
     */
    public long getTotalBytes() {
        return totalBytes;
    }

    /**
     * Gets the fraction of the transfer that has been completed, so far. This is a value
     * greater than or equal to 0, and less than or equal to 1. When the value is 0, the transfer
     * has not started. When the value is 1, the transfer is complete. If 5 bytes have been transferred,
     * and 10 bytes are expected, this value is (5.0f/10.0f) = 0.5f.
     * @return Fraction of transfer that has been completed, a value between 0 and 1, inclusive.
     */
    public double getFractionCompleted() {
        return ((double) currentBytes) / totalBytes;
    }

    @Override
    public boolean equals(@Nullable Object thatObject) {
        if (this == thatObject) {
            return true;
        }
        if (thatObject == null || getClass() != thatObject.getClass()) {
            return false;
        }
        StorageTransferProgress that = (StorageTransferProgress) thatObject;
        if (getCurrentBytes() != that.getCurrentBytes()) {
            return false;
        }
        return getTotalBytes() == that.getTotalBytes();
    }

    @Override
    public int hashCode() {
        int result = (int) (getCurrentBytes() ^ (getCurrentBytes() >>> 32));
        result = 31 * result + (int) (getTotalBytes() ^ (getTotalBytes() >>> 32));
        return result;
    }

    @Override
    public String toString() {
        return "StorageTransferProgress{" +
            "currentBytes=" + getCurrentBytes() +
            ", totalBytes=" + getTotalBytes() +
            ", fractionCompleted=" + getFractionCompleted() +
            '}';
    }
}
