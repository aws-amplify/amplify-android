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

package com.amplifyframework.storage.s3.operation;

import android.annotation.SuppressLint;
import androidx.annotation.NonNull;

import com.amplifyframework.core.Consumer;
import com.amplifyframework.storage.StorageException;
import com.amplifyframework.storage.operation.StorageGetUrlOperation;
import com.amplifyframework.storage.result.StorageGetUrlResult;
import com.amplifyframework.storage.s3.AWSAuthProvider;
import com.amplifyframework.storage.s3.request.AWSS3StorageGetPresignedUrlRequest;
import com.amplifyframework.storage.s3.service.StorageService;
import com.amplifyframework.storage.s3.utils.S3Keys;

import java.net.URL;
import java.util.concurrent.ExecutorService;

/**
 * An operation to retrieve pre-signed object URL from AWS S3.
 */
public final class AWSS3StorageGetPresignedUrlOperation
        extends StorageGetUrlOperation<AWSS3StorageGetPresignedUrlRequest> {
    private final StorageService storageService;
    private final ExecutorService executorService;
    private final AWSAuthProvider awsAuthProvider;
    private final Consumer<StorageGetUrlResult> onSuccess;
    private final Consumer<StorageException> onError;

    /**
     * Constructs a new AWSS3StorageGetUrlOperation.
     * @param storageService S3 client wrapper
     * @param executorService Executor service used for running
     *                        blocking operations on a separate thread
     * @param awsAuthProvider Interface to retrieve AWS specific auth information
     * @param request getUrl request parameters
     * @param onSuccess Notified when URL is generated.
     * @param onError Notified upon URL generation error
     */
    public AWSS3StorageGetPresignedUrlOperation(
            @NonNull StorageService storageService,
            @NonNull ExecutorService executorService,
            @NonNull AWSAuthProvider awsAuthProvider,
            @NonNull AWSS3StorageGetPresignedUrlRequest request,
            @NonNull Consumer<StorageGetUrlResult> onSuccess,
            @NonNull Consumer<StorageException> onError
    ) {
        super(request);
        this.storageService = storageService;
        this.executorService = executorService;
        this.awsAuthProvider = awsAuthProvider;
        this.onSuccess = onSuccess;
        this.onError = onError;
    }

    @SuppressLint("SyntheticAccessor")
    @Override
    public void start() {
        executorService.submit(() -> {
            // Obtain S3 service key for storage service
            String serviceKey;

            try {
                serviceKey = S3Keys.createServiceKey(
                        getRequest().getAccessLevel(),
                        getRequest().getTargetIdentityId() != null
                                ? getRequest().getTargetIdentityId()
                                : awsAuthProvider.getIdentityId(),
                        getRequest().getKey()
                );
            } catch (StorageException exception) {
                onError.accept(exception);
                return;
            }

            try {
                URL url = storageService.getPresignedUrl(serviceKey, getRequest().getExpires());
                onSuccess.accept(StorageGetUrlResult.fromUrl(url));
            } catch (Exception exception) {
                onError.accept(new StorageException(
                        "Encountered an issue while generating pre-signed URL",
                        exception,
                        "See included exception for more details and suggestions to fix."
                ));
            }
        });
    }
}
