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
import com.amplifyframework.storage.operation.StorageDownloadFileOperation;
import com.amplifyframework.storage.result.StorageDownloadFileResult;
import com.amplifyframework.storage.result.StorageTransferProgress;
import com.amplifyframework.storage.s3.CognitoAuthProvider;
import com.amplifyframework.storage.s3.request.AWSS3StorageDownloadFileRequest;
import com.amplifyframework.storage.s3.service.StorageService;
import com.amplifyframework.storage.s3.utils.S3Keys;

import com.amazonaws.mobileconnectors.s3.transferutility.TransferListener;
import com.amazonaws.mobileconnectors.s3.transferutility.TransferObserver;
import com.amazonaws.mobileconnectors.s3.transferutility.TransferState;

import java.io.File;

/**
 * An operation to download a file from AWS S3.
 */
public final class AWSS3StorageDownloadFileOperation
        extends StorageDownloadFileOperation<AWSS3StorageDownloadFileRequest> {
    private final StorageService storageService;
    private final CognitoAuthProvider cognitoAuthProvider;
    private final Consumer<StorageTransferProgress> onProgress;
    private final Consumer<StorageDownloadFileResult> onSuccess;
    private final Consumer<StorageException> onError;
    private TransferObserver transferObserver;
    private File file;

    /**
     * Constructs a new AWSS3StorageDownloadFileOperation.
     * @param storageService S3 client wrapper
     * @param cognitoAuthProvider Interface to retrieve AWS specific auth information
     * @param request download request parameters
     * @param onProgress Notified upon advancements in download progress
     * @param onSuccess Notified when download results are available
     * @param onError Notified upon download error
     */
    public AWSS3StorageDownloadFileOperation(
            @NonNull StorageService storageService,
            @NonNull CognitoAuthProvider cognitoAuthProvider,
            @NonNull AWSS3StorageDownloadFileRequest request,
            @NonNull Consumer<StorageTransferProgress> onProgress,
            @NonNull Consumer<StorageDownloadFileResult> onSuccess,
            @NonNull Consumer<StorageException> onError
    ) {
        super(request);
        this.storageService = storageService;
        this.cognitoAuthProvider = cognitoAuthProvider;
        this.onProgress = onProgress;
        this.onSuccess = onSuccess;
        this.onError = onError;
        this.transferObserver = null;
        this.file = null;
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
      
        this.file = getRequest().getLocal();
      
        try {
            transferObserver = storageService.downloadToFile(serviceKey, file);
            transferObserver.setTransferListener(new DownloadTransferListener());
        } catch (Exception exception) {
            onError.accept(new StorageException(
                    "Issue downloading file",
                    exception,
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
                    "Something went wrong while attempting to cancel your AWS S3 Storage download file operation",
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
                    "Something went wrong while attempting to pause your AWS S3 Storage download file operation",
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
                    "Something went wrong while attempting to resume your AWS S3 Storage download file operation",
                    exception,
                    "See attached exception for more information and suggestions"
                ));
            }
        }
    }

    @SuppressLint("SyntheticAccessor")
    private final class DownloadTransferListener implements TransferListener {
        @Override
        public void onStateChanged(int transferId, TransferState state) {
            Amplify.Hub.publish(HubChannel.STORAGE,
                    HubEvent.create(StorageChannelEventName.DOWNLOAD_STATE, state.name()));
            switch (state) {
                case COMPLETED:
                    onSuccess.accept(StorageDownloadFileResult.fromFile(file));
                    return;
                case FAILED:
                    onError.accept(new StorageException(
                            "Storage download operation was interrupted.",
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
                    HubEvent.create(StorageChannelEventName.DOWNLOAD_ERROR, exception));
            onError.accept(new StorageException(
                    "Something went wrong with your AWS S3 Storage download file operation",
                    exception,
                    "See attached exception for more information and suggestions"
            ));
        }
    }
}
