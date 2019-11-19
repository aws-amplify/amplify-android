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

import com.amplifyframework.core.ResultListener;
import com.amplifyframework.storage.exception.StorageException;
import com.amplifyframework.storage.operation.StorageListOperation;
import com.amplifyframework.storage.result.StorageListResult;
import com.amplifyframework.storage.s3.request.AWSS3StorageListRequest;
import com.amplifyframework.storage.s3.service.AWSS3StorageService;
import com.amplifyframework.storage.s3.utils.S3RequestUtils;

import com.amazonaws.mobile.client.AWSMobileClient;

import java.util.concurrent.ExecutorService;

/**
 * An operation to set items from AWS S3.
 */

public final class AWSS3StorageListOperation extends StorageListOperation<AWSS3StorageListRequest> {
    private final AWSS3StorageService storageService;
    private final ExecutorService executorService;
    private final ResultListener<StorageListResult> resultListener;

    /**
     * Constructs a new AWSS3StorageListOperation.
     * @param storageService S3 client wrapper
     * @param executorService Executor service used for running blocking operations on a separate thread
     * @param request set request parameters
     * @param resultListener notified when set operation results are available
     */
    public AWSS3StorageListOperation(AWSS3StorageService storageService,
                                     ExecutorService executorService,
                                     AWSS3StorageListRequest request,
                                     ResultListener<StorageListResult> resultListener) {
        super(request);
        this.storageService = storageService;
        this.executorService = executorService;
        this.resultListener = resultListener;
    }

    @Override
    public void start() throws StorageException {
        executorService.submit(() -> {
            String identityId;

            try {
                identityId = AWSMobileClient.getInstance().getIdentityId();
            } catch (Exception exception) {
                StorageException storageException = new StorageException(
                        "AWSMobileClient could not get user id." +
                                "Check whether you configured it properly before calling this method.",
                        exception
                );

                if (resultListener != null) {
                    resultListener.onError(storageException);
                }
                throw storageException;
            }

            try {
                StorageListResult result = storageService.listFiles(
                        S3RequestUtils.getServiceKey(
                                getRequest().getAccessLevel(),
                                identityId,
                                getRequest().getPath(),
                                getRequest().getTargetIdentityId()
                        )
                );

                if (resultListener != null) {
                    resultListener.onResult(result);
                }
            } catch (Exception error) {
                if (resultListener != null) {
                    resultListener.onError(error);
                }
                throw error;
            }
        });
    }
}
