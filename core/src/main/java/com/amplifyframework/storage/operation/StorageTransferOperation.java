/*
 * Copyright 2022 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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
import com.amplifyframework.core.async.AmplifyOperation;
import com.amplifyframework.core.async.Cancelable;
import com.amplifyframework.core.async.Resumable;
import com.amplifyframework.core.category.CategoryType;
import com.amplifyframework.storage.StorageException;
import com.amplifyframework.storage.TransferState;
import com.amplifyframework.storage.result.StorageTransferProgress;
import com.amplifyframework.storage.result.StorageTransferResult;

/**
 * Base operation type for any transfer behavior on the Storage category.
 *
 * @param <R> type of the request object
 * @param <T> type of success transfer result
 */
public abstract class StorageTransferOperation<R, T extends StorageTransferResult>
    extends AmplifyOperation<R> implements Resumable, Cancelable {

    /**
     * Consumer that notifies transfer progress.
     */
    @Nullable
    private Consumer<StorageTransferProgress> onProgress;

    /**
     * Consumer that notifies transfer success.
     */
    @Nullable
    private Consumer<T> onSuccess;

    /**
     * Consumer that notifies transfer error.
     */
    @Nullable
    private Consumer<StorageException> internalOnError;

    /**
     * Unique identifier for the transfer.
     */
    @NonNull
    private final String transferId;

    /**
     * Current @{@link TransferState}.
     */
    @NonNull
    private TransferState transferState;

    /**
     * StorageException that caused the failure.
     */
    @Nullable
    private StorageException error;

    /**
     * Constructs a new AmplifyOperation.
     *
     * @param amplifyOperationRequest The request object of the operation
     * @param transferId              Unique identifier for tracking in local device queue
     * @param onProgress              Notified upon advancements in upload progress
     * @param onSuccess               Will be notified when results of upload are available
     * @param onError                 Notified when upload fails with an error
     */
    protected StorageTransferOperation(
        @Nullable R amplifyOperationRequest,
        @NonNull String transferId,
        @Nullable Consumer<StorageTransferProgress> onProgress,
        @Nullable Consumer<T> onSuccess,
        @Nullable Consumer<StorageException> onError
    ) {
        super(CategoryType.STORAGE, amplifyOperationRequest);
        this.transferId = transferId;
        this.onProgress = onProgress;
        this.onSuccess = onSuccess;
        this.internalOnError = new InternalOnError(onError);
    }

    /**
     * Gets a unique identifier of a transfer operation held in the device queue.
     * Holding on to this id allows you to later query for the queued operation.
     *
     * @return device queue transfer id
     */
    @NonNull
    public String getTransferId() {
        return transferId;
    }

    /**
     * Sets current transfer state.
     *
     * @param transferState set current transfer state
     */
    protected void setTransferState(@NonNull TransferState transferState) {
        this.transferState = transferState;
    }

    /**
     * Gets current transfer state.
     *
     * @return TransferState
     */
    @NonNull
    public abstract TransferState getTransferState();

    /**
     * Provide a Consumer to receive transfer progress updates.
     *
     * @param onProgress Consumer which provides incremental progress updates
     */
    public void setOnProgress(@Nullable Consumer<StorageTransferProgress> onProgress) {
        this.onProgress = onProgress;
    }

    /**
     * Provide a Consumer to receive transfer progress updates.
     *
     * @return Consumer to receive transfer progress updates.
     */
    protected Consumer<StorageTransferProgress> getOnProgress() {
        return this.onProgress;
    }

    /**
     * Provide a Consumer to receive successful transfer result.
     *
     * @param onSuccess Consumer which provides a successful transfer result
     */
    public void setOnSuccess(@Nullable Consumer<T> onSuccess) {
        this.onSuccess = onSuccess;
    }

    /**
     * Provide a Consumer to receive successful transfer result.
     *
     * @return onSuccess Consumer which provides a successful transfer result
     */
    protected Consumer<T> getOnSuccess() {
        return this.onSuccess;
    }

    /**
     * Provide a Consumer to receive transfer errors.
     *
     * @param onError Consumer which provides transfer errors
     */
    public void setOnError(@Nullable Consumer<StorageException> onError) {
        if (onError == null) {
            this.internalOnError = null;
            return;
        }
        this.internalOnError = new InternalOnError(onError);
        if (getTransferState() == TransferState.FAILED) {
            if (error == null) {
                error = new StorageException(
                    "Something went wrong with your AWS S3 Storage transfer operation",
                    new UnknownError("Reason unknown"),
                    "Please re-queue the operation"
                );
            }
            internalOnError.accept(error);
        }
    }

    /**
     * Provide a Consumer to receive transfer errors.
     *
     * @return onError Consumer which provides transfer errors
     */
    protected Consumer<StorageException> getOnError() {
        return this.internalOnError;
    }

    /**
     *
     * clears All listeners attached to this operation.
     *
     */
    public void clearAllListeners() {
        onProgress = null;
        onSuccess = null;
        internalOnError = null;
    }

    /**
     * Provide the exception which caused the failure.
     *
     * @return exception that caused the failure.
     */
    @NonNull
    public StorageException getError() {
        return error;
    }

    /**
     * Set the exception which caused the failure.
     *
     * @param error that caused the failure.
     */
    protected void setError(@NonNull StorageException error) {
        this.error = error;
    }

    /**
     * Internal class to persist the error locally.
     *
     */
    private class InternalOnError implements Consumer<StorageException> {
        private final Consumer<StorageException> onError;

        InternalOnError(Consumer<StorageException> onError) {
            this.onError = onError;
        }

        @Override
        public void accept(@NonNull StorageException value) {
            setError(value);
            if (onError != null) {
                onError.accept(value);
            }
        }
    }
}
