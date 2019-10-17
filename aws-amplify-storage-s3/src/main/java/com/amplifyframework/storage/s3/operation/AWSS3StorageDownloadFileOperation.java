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

import com.amplifyframework.core.async.Listener;
import com.amplifyframework.storage.exception.StorageException;
import com.amplifyframework.storage.operation.StorageDownloadFileOperation;
import com.amplifyframework.storage.result.StorageDownloadFileResult;
import com.amplifyframework.storage.s3.request.AWSS3StorageDownloadFileRequest;
import com.amplifyframework.storage.s3.service.AWSS3StorageService;
import com.amplifyframework.storage.s3.utils.S3RequestUtils;

import com.amazonaws.mobile.client.AWSMobileClient;
import com.amazonaws.mobileconnectors.s3.transferutility.TransferListener;
import com.amazonaws.mobileconnectors.s3.transferutility.TransferObserver;
import com.amazonaws.mobileconnectors.s3.transferutility.TransferState;

import java.io.File;

/**
 * An operation to download a file from AWS S3.
 */
public final class AWSS3StorageDownloadFileOperation extends StorageDownloadFileOperation {
    private final AWSS3StorageService storageService;
    private final AWSS3StorageDownloadFileRequest request;
    private final Listener<StorageDownloadFileResult> callback;
    private TransferObserver transferObserver;
    private File file;

    /**
     * Constructs a new AWSS3StorageDownloadFileOperation.
     * @param storageService S3 client wrapper
     * @param request download request parameters
     * @param callback Listener to invoke when results are available
     */
    public AWSS3StorageDownloadFileOperation(AWSS3StorageService storageService,
                                             AWSS3StorageDownloadFileRequest request,
                                             Listener<StorageDownloadFileResult> callback) {
        this.request = request;
        this.storageService = storageService;
        this.callback = callback;
        this.transferObserver = null;
        this.file = null;
    }

    @Override
    public void start() throws StorageException {
        // Only start if it hasn't already been started
        if (transferObserver == null) {
            String identityId;

            try {
                identityId = AWSMobileClient.getInstance().getIdentityId();
            } catch (Exception exception) {
                throw new StorageException(
                        "AWSMobileClient could not get user id." +
                        "Check whether you configured it properly before calling this method.",
                        exception
                );
            }

            String serviceKey = S3RequestUtils.getServiceKey(
                    request.getAccessLevel(),
                    identityId,
                    request.getKey(),
                    request.getTargetIdentityId()
            );
            this.file = new File(request.getLocal()); //TODO: Add error handling if path is invalid

            try {
                transferObserver = storageService.downloadToFile(serviceKey, file);
            } catch (Exception exception) {
                throw new StorageException("Issue downloading file - see included exception", exception);
            }

            transferObserver.setTransferListener(new TransferListener() {
                @Override
                public void onStateChanged(int transferId, TransferState state) {
                    // TODO: dispatch event to hub
                    if (TransferState.COMPLETED == state) {
                        if (callback != null) {
                            callback.onResult(StorageDownloadFileResult.fromFile(file));
                        }
                    }
                }

                @SuppressWarnings("MagicNumber")
                @Override
                public void onProgressChanged(int transferId, long bytesCurrent, long bytesTotal) {
                    int percentage = (int) (bytesCurrent / bytesTotal * 100);
                    // TODO: dispatch event to hub
                }

                @Override
                public void onError(int transferId, Exception exception) {
                    // TODO: dispatch event to hub
                    if (callback != null) {
                        callback.onError(exception);
                    }
                }
            });
        }
    }

    @Override
    public void cancel() throws StorageException {
        if (transferObserver != null) {
            try {
                storageService.cancelTransfer(transferObserver);
            } catch (Exception exception) {
                throw new StorageException("Issue cancelling file download - see included exception", exception);
            }
        }
    }

    @Override
    public void pause() throws StorageException {
        if (transferObserver != null) {
            try {
                storageService.pauseTransfer(transferObserver);
            } catch (Exception exception) {
                throw new StorageException("Issue pausing file download - see included exception", exception);
            }
        }
    }

    @Override
    public void resume() throws StorageException {
        if (transferObserver != null) {
            try {
                storageService.resumeTransfer(transferObserver);
            } catch (Exception exception) {
                throw new StorageException("Issue resuming file download - see included exception", exception);
            }
        }
    }
}
