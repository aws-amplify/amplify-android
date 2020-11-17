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

import com.amplifyframework.core.Amplify;
import com.amplifyframework.core.Consumer;
import com.amplifyframework.hub.HubChannel;
import com.amplifyframework.hub.HubEvent;
import com.amplifyframework.storage.StorageChannelEventName;
import com.amplifyframework.storage.StorageException;
import com.amplifyframework.storage.operation.StorageUploadInputStreamOperation;
import com.amplifyframework.storage.result.StorageTransferProgress;
import com.amplifyframework.storage.result.StorageUploadFileResult;
import com.amplifyframework.storage.s3.CognitoAuthProvider;
import com.amplifyframework.storage.s3.ServerSideEncryption;
import com.amplifyframework.storage.s3.request.AWSS3StorageUploadRequest;
import com.amplifyframework.storage.s3.service.StorageService;
import com.amplifyframework.storage.s3.utils.S3Keys;

import com.amazonaws.mobileconnectors.s3.transferutility.TransferListener;
import com.amazonaws.mobileconnectors.s3.transferutility.TransferObserver;
import com.amazonaws.mobileconnectors.s3.transferutility.TransferState;
import com.amazonaws.services.s3.model.ObjectMetadata;

import java.io.IOException;
import java.io.InputStream;
import java.util.Objects;

/**
 * An operation to upload an InputStream from AWS S3.
 */
public final class AWSS3StorageUploadInputStreamOperation
        extends StorageUploadInputStreamOperation<AWSS3StorageUploadRequest<InputStream>> {
    private final StorageService storageService;
    private final CognitoAuthProvider cognitoAuthProvider;
    private final Consumer<StorageTransferProgress> onProgress;
    private final Consumer<StorageUploadFileResult> onSuccess;
    private final Consumer<StorageException> onError;
    private TransferObserver transferObserver;

    /**
     * Constructs a new AWSS3StorageUploadInputStreamOperation.
     * @param storageService S3 client wrapper
     * @param cognitoAuthProvider Interface to retrieve AWS specific auth information
     * @param request upload request parameters
     * @param onProgress Notified upon advancements in upload progress
     * @param onSuccess Will be notified when results of upload are available
     * @param onError Notified when upload fails with an error
     */
    public AWSS3StorageUploadInputStreamOperation(
            @NonNull StorageService storageService,
            @NonNull CognitoAuthProvider cognitoAuthProvider,
            @NonNull AWSS3StorageUploadRequest<InputStream> request,
            @NonNull Consumer<StorageTransferProgress> onProgress,
            @NonNull Consumer<StorageUploadFileResult> onSuccess,
            @NonNull Consumer<StorageException> onError
    ) {
        super(Objects.requireNonNull(request));
        this.storageService = Objects.requireNonNull(storageService);
        this.cognitoAuthProvider = cognitoAuthProvider;
        this.onProgress = Objects.requireNonNull(onProgress);
        this.onSuccess = Objects.requireNonNull(onSuccess);
        this.onError = Objects.requireNonNull(onError);
        this.transferObserver = null;
    }

    @SuppressLint("SyntheticAccessor")
    @Override
    public void start() {
        // Only start if it hasn't already been started
        if (transferObserver != null) {
            return;
        }

        String currentIdentityId;

        try {
            currentIdentityId = cognitoAuthProvider.getIdentityId();
        } catch (StorageException exception) {
            onError.accept(exception);
            return;
        }

        String serviceKey = S3Keys.createServiceKey(
                getRequest().getAccessLevel(),
                getRequest().getTargetIdentityId() != null
                        ? getRequest().getTargetIdentityId()
                        : currentIdentityId,
                getRequest().getKey()
        );

        // Grab the inputStream to upload...
        InputStream inputStream = getRequest().getLocal();

        // Set up the metadata
        ObjectMetadata objectMetadata = new ObjectMetadata();
        objectMetadata.setUserMetadata(getRequest().getMetadata());
        objectMetadata.setContentType(getRequest().getContentType());

        ServerSideEncryption storageServerSideEncryption = getRequest().getServerSideEncryption();
        if (!ServerSideEncryption.NONE.equals(storageServerSideEncryption)) {
            objectMetadata.setSSEAlgorithm(storageServerSideEncryption.getName());
        }

        // Upload!
        try {
            transferObserver = storageService.uploadInputStream(serviceKey, inputStream, objectMetadata);
            transferObserver.setTransferListener(new UploadTransferListener());
        } catch (IOException ioException) {
            onError.accept(new StorageException(
                    "Issue uploading inputStream.",
                    ioException,
                    "See included exception for more details and suggestions to fix."
            ));
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

    @SuppressLint("SyntheticAccessor")
    private final class UploadTransferListener implements TransferListener {
        @Override
        public void onStateChanged(int transferId, TransferState state) {
            Amplify.Hub.publish(HubChannel.STORAGE,
                    HubEvent.create(StorageChannelEventName.UPLOAD_STATE, state.name()));
            switch (state) {
                case COMPLETED:
                    onSuccess.accept(StorageUploadFileResult.fromKey(getRequest().getKey()));
                    return;
                case FAILED:
                    onError.accept(new StorageException(
                            "Storage upload operation was interrupted.",
                            "Please verify that you have a stable internet connection."
                    ));
                    return;
                default:
                    // no-op;
            }
        }

        @Override
        public void onProgressChanged(int transferId, long bytesCurrent, long bytesTotal) {
            onProgress.accept(new StorageTransferProgress(bytesCurrent, bytesTotal));
        }

        @Override
        public void onError(int transferId, Exception exception) {
            Amplify.Hub.publish(HubChannel.STORAGE,
                    HubEvent.create(StorageChannelEventName.UPLOAD_ERROR, exception));
            onError.accept(new StorageException(
                    "Something went wrong with your AWS S3 Storage upload file operation",
                    exception,
                    "See attached exception for more information and suggestions"
            ));
        }
    }
}
