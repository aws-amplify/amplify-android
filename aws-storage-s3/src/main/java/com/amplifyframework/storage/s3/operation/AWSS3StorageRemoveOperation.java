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

import com.amplifyframework.core.ResultListener;
import com.amplifyframework.storage.StorageException;
import com.amplifyframework.storage.operation.StorageRemoveOperation;
import com.amplifyframework.storage.result.StorageRemoveResult;
import com.amplifyframework.storage.s3.request.AWSS3StorageRemoveRequest;
import com.amplifyframework.storage.s3.service.AWSS3StorageService;
import com.amplifyframework.storage.s3.utils.S3RequestUtils;

import com.amazonaws.mobile.client.AWSMobileClient;

import java.util.concurrent.ExecutorService;

/**
 * An operation to remove a file from AWS S3.
 */
public final class AWSS3StorageRemoveOperation extends StorageRemoveOperation<AWSS3StorageRemoveRequest> {
    private final AWSS3StorageService storageService;
    private final ResultListener<StorageRemoveResult> resultListener;
    private final ExecutorService executorService;

    /**
     * Constructs a new AWSS3StorageRemoveOperation.
     * @param storageService S3 client wrapper
     * @param executorService Executor service used for running blocking operations on a separate thread
     * @param request remove request parameters
     * @param resultListener notified when remove operation results available
     */
    public AWSS3StorageRemoveOperation(@NonNull AWSS3StorageService storageService,
                                       @NonNull ExecutorService executorService,
                                       @NonNull AWSS3StorageRemoveRequest request,
                                       @NonNull ResultListener<StorageRemoveResult> resultListener) {
        super(request);
        this.storageService = storageService;
        this.executorService = executorService;
        this.resultListener = resultListener;
    }

    @Override
    public void start() {
        executorService.submit(() -> {
            String identityId;

            try {
                identityId = AWSMobileClient.getInstance().getIdentityId();

                try {
                    storageService.deleteObject(
                        S3RequestUtils.getServiceKey(
                            getRequest().getAccessLevel(),
                            identityId,
                            getRequest().getKey(),
                            getRequest().getTargetIdentityId()
                        )
                    );

                    resultListener.onResult(StorageRemoveResult.fromKey(getRequest().getKey()));
                } catch (Exception exception) {
                    resultListener.onError(new StorageException(
                        "Something went wrong with your AWS S3 Storage remove operation",
                        exception,
                        "See attached exception for more information and suggestions"
                    ));
                }
            } catch (Exception exception) {
                resultListener.onError(new StorageException(
                    "Something went wrong with your AWS S3 Storage remove operation",
                    exception,
                    "See attached exception for more information and suggestions"
                ));
            }
        });
    }
}
