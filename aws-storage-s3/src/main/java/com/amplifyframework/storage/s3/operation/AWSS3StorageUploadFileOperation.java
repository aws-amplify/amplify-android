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

package com.amplifyframework.storage.s3.operation;

import android.annotation.SuppressLint;
import androidx.annotation.NonNull;

import com.amplifyframework.core.Consumer;
import com.amplifyframework.storage.StorageException;
import com.amplifyframework.storage.operation.StorageUploadFileOperation;
import com.amplifyframework.storage.result.StorageUploadFileResult;
import com.amplifyframework.storage.s3.request.AWSS3StorageUploadFileRequest;
import com.amplifyframework.storage.s3.service.StorageService;
import com.amplifyframework.storage.s3.utils.S3RequestUtils;

import com.amazonaws.mobileconnectors.s3.transferutility.TransferListener;
import com.amazonaws.mobileconnectors.s3.transferutility.TransferObserver;
import com.amazonaws.mobileconnectors.s3.transferutility.TransferState;

import java.io.File;
import java.util.Objects;

/**
 * An operation to upload a file from AWS S3.
 */
public final class AWSS3StorageUploadFileOperation extends StorageUploadFileOperation<AWSS3StorageUploadFileRequest> {
    private final StorageService storageService;
    private final Consumer<StorageUploadFileResult> onSuccess;
    private final Consumer<StorageException> onError;
    private TransferObserver transferObserver;
    private File file;

    /**
     * Constructs a new AWSS3StorageUploadFileOperation.
     * @param storageService S3 client wrapper
     * @param request upload request parameters
     * @param onSuccess Will be notified when results of upload are available
     * @param onError Notified when upload fails with an error
     */
    public AWSS3StorageUploadFileOperation(
            @NonNull StorageService storageService,
            @NonNull AWSS3StorageUploadFileRequest request,
            @NonNull Consumer<StorageUploadFileResult> onSuccess,
            @NonNull Consumer<StorageException> onError
    ) {
        super(Objects.requireNonNull(request));
        this.storageService = Objects.requireNonNull(storageService);
        this.onSuccess = Objects.requireNonNull(onSuccess);
        this.onError = Objects.requireNonNull(onError);
        this.transferObserver = null;
        this.file = null;
    }

    @SuppressLint("SyntheticAccessor")
    @Override
    public void start() {
        // Only start if it hasn't already been started
        if (transferObserver == null) {

            String serviceKey = S3RequestUtils.getServiceKey(
                    getRequest().getAccessLevel(),
                    getRequest().getTargetIdentityId(),
                    getRequest().getKey()
            );
            this.file = new File(getRequest().getLocal()); //TODO: Add error handling if path is invalid

            try {
                if (getRequest().getMetadata().isEmpty()) {
                    transferObserver = storageService.uploadFile(serviceKey, file);
                } else {
                    transferObserver = storageService.uploadFile(serviceKey, file, getRequest().getMetadata());
                }

            } catch (Exception exception) {
                onError.accept(new StorageException(
                    "Issue uploading file",
                    exception,
                    "See included exception for more details and suggestions to fix."
                ));
                return;
            }

            transferObserver.setTransferListener(new TransferListener() {
                @Override
                public void onStateChanged(int transferId, TransferState state) {
                    // TODO: dispatch event to hub
                    if (TransferState.COMPLETED == state) {
                        onSuccess.accept(StorageUploadFileResult.fromKey(getRequest().getKey()));
                    }
                }

                @SuppressWarnings("checkstyle:MagicNumber")
                @Override
                public void onProgressChanged(int transferId, long bytesCurrent, long bytesTotal) {
                    @SuppressWarnings("unused")
                    int percentage = (int) (bytesCurrent / bytesTotal * 100);
                    // TODO: dispatch event to hub
                }

                @Override
                public void onError(int transferId, Exception exception) {
                    onError.accept(new StorageException(
                        "Something went wrong with your AWS S3 Storage upload file operation",
                        exception,
                        "See attached exception for more information and suggestions"
                    ));
                }
            });
        }
    }

    @Override
    public void cancel() {
        if (transferObserver != null) {
            try {
                storageService.cancelTransfer(transferObserver);
            } catch (Exception exception) {
                onError.accept(new StorageException(
                    "Something went wrong while attempting to cancel your AWS S3 Storage upload file operation",
                    exception,
                    "See attached exception for more information and suggestions"
                ));
            }
        }
    }

    @Override
    public void pause() {
        if (transferObserver != null) {
            try {
                storageService.pauseTransfer(transferObserver);
            } catch (Exception exception) {
                onError.accept(new StorageException(
                    "Something went wrong while attempting to pause your AWS S3 Storage upload file operation",
                    exception,
                    "See attached exception for more information and suggestions"
                ));
            }
        }
    }

    @Override
    public void resume() {
        if (transferObserver != null) {
            try {
                storageService.resumeTransfer(transferObserver);
            } catch (Exception exception) {
                onError.accept(new StorageException(
                    "Something went wrong while attempting to resume your AWS S3 Storage upload file operation",
                    exception,
                    "See attached exception for more information and suggestions"
                ));
            }
        }
    }
}
