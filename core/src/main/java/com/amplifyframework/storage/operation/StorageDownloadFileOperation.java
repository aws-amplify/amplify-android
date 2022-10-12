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

package com.amplifyframework.storage.operation;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.amplifyframework.core.Consumer;
import com.amplifyframework.storage.StorageException;
import com.amplifyframework.storage.result.StorageDownloadFileResult;
import com.amplifyframework.storage.result.StorageTransferProgress;

import java.util.UUID;

/**
 * Base operation type for download file behavior on the Storage category.
 *
 * @param <R> type of the request object
 */
public abstract class StorageDownloadFileOperation<R> extends StorageTransferOperation<R, StorageDownloadFileResult> {

    /**
     * Constructs a new StorageDownloadFileOperation.
     * @param amplifyOperationRequest The request object of the operation
     */
    public StorageDownloadFileOperation(@Nullable R amplifyOperationRequest) {
        this(amplifyOperationRequest, UUID.randomUUID().toString(), null, null, null);
    }

    /**
     * Constructs a new StorageDownloadFileOperation.
     * @param amplifyOperationRequest The request object of the operation
     * @param transferId Unique identifier for tracking in local device queue
     * @param onProgress Notified upon advancements in upload progress
     * @param onSuccess Will be notified when results of upload are available
     * @param onError Notified when upload fails with an error
     */
    protected StorageDownloadFileOperation(
            @Nullable R amplifyOperationRequest,
            @NonNull String transferId,
            @Nullable Consumer<StorageTransferProgress> onProgress,
            @Nullable Consumer<StorageDownloadFileResult> onSuccess,
            @Nullable Consumer<StorageException> onError
    ) {
        super(amplifyOperationRequest, transferId, onProgress, onSuccess, onError);
    }

    /**
     * Provide a Consumer to receive successful transfer result.
     *
     * @param onSuccess Consumer which provides a successful transfer result
     */
    @Override
    public void setOnSuccess(@Nullable Consumer<StorageDownloadFileResult> onSuccess) {
        super.setOnSuccess(onSuccess);
    }
}

