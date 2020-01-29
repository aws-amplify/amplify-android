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

import androidx.annotation.NonNull;

import com.amplifyframework.core.Consumer;
import com.amplifyframework.storage.StorageException;
import com.amplifyframework.storage.operation.StorageRemoveOperation;
import com.amplifyframework.storage.result.StorageRemoveResult;
import com.amplifyframework.storage.s3.IdentityIdProvider;
import com.amplifyframework.storage.s3.request.AWSS3StorageRemoveRequest;
import com.amplifyframework.storage.s3.service.StorageService;
import com.amplifyframework.storage.s3.utils.S3RequestUtils;

import java.util.Objects;
import java.util.concurrent.ExecutorService;

/**
 * An operation to remove a file from AWS S3.
 */
public final class AWSS3StorageRemoveOperation extends StorageRemoveOperation<AWSS3StorageRemoveRequest> {
    private final IdentityIdProvider identityIdProvider;
    private final StorageService storageService;
    private final ExecutorService executorService;
    private final Consumer<StorageRemoveResult> onSuccess;
    private final Consumer<StorageException> onError;

    /**
     * Constructs a new AWSS3StorageRemoveOperation.
     * @param storageService S3 client wrapper
     * @param executorService Executor service used for running blocking operations on a separate thread
     * @param request remove request parameters
     * @param onSuccess notified when remove operation results available
     * @param onError notified when remove operation does not complete due to error
     */
    public AWSS3StorageRemoveOperation(
            @NonNull IdentityIdProvider identityIdProvider,
            @NonNull StorageService storageService,
            @NonNull ExecutorService executorService,
            @NonNull AWSS3StorageRemoveRequest request,
            @NonNull Consumer<StorageRemoveResult> onSuccess,
            @NonNull Consumer<StorageException> onError) {
        super(Objects.requireNonNull(request));
        this.identityIdProvider = Objects.requireNonNull(identityIdProvider);
        this.storageService = Objects.requireNonNull(storageService);
        this.executorService = Objects.requireNonNull(executorService);
        this.onSuccess = Objects.requireNonNull(onSuccess);
        this.onError = Objects.requireNonNull(onError);
    }

    @SuppressWarnings("SyntheticAccessor")
    @Override
    public void start() {
        executorService.submit(() -> {
            String identityId;

            try {
                identityId = identityIdProvider.getIdentityId();

                try {
                    storageService.deleteObject(
                        S3RequestUtils.getServiceKey(
                            getRequest().getAccessLevel(),
                            identityId,
                            getRequest().getKey(),
                            getRequest().getTargetIdentityId()
                        )
                    );

                    onSuccess.accept(StorageRemoveResult.fromKey(getRequest().getKey()));
                } catch (Exception exception) {
                    onError.accept(new StorageException(
                        "Something went wrong with your AWS S3 Storage remove operation",
                        exception,
                        "See attached exception for more information and suggestions"
                    ));
                }
            } catch (Exception exception) {
                onError.accept(new StorageException(
                    "Something went wrong with your AWS S3 Storage remove operation",
                    exception,
                    "See attached exception for more information and suggestions"
                ));
            }
        });
    }
}
