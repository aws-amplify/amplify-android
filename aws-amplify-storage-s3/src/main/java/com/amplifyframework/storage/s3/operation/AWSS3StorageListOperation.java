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
import com.amplifyframework.storage.operation.StorageListOperation;
import com.amplifyframework.storage.result.StorageListResult;
import com.amplifyframework.storage.s3.request.AWSS3StorageListRequest;
import com.amplifyframework.storage.s3.service.AWSS3StorageService;
import com.amplifyframework.storage.s3.utils.S3RequestUtils;

import com.amazonaws.mobile.client.AWSMobileClient;

/**
 * An operation to list items from AWS S3.
 */

public final class AWSS3StorageListOperation extends StorageListOperation {
    private final AWSS3StorageService storageService;
    private final AWSS3StorageListRequest request;
    private final Listener<StorageListResult> callback;

    /**
     * Constructs a new AWSS3StorageListOperation.
     * @param storageService S3 client wrapper
     * @param request list request parameters
     * @param callback Listener to invoke when results are available
     */
    public AWSS3StorageListOperation(AWSS3StorageService storageService,
                                     AWSS3StorageListRequest request,
                                     Listener<StorageListResult> callback) {
        this.request = request;
        this.storageService = storageService;
        this.callback = callback;
    }

    // TODO: This is currently a blocking method since listFiles is blocking, consistent with the S3 SDK.
    //          This should be discussed for refactoring as an async method or if not, documented clearly as blocking.
    @Override
    public void start() throws StorageException {
        String identityId;

        try {
            identityId = AWSMobileClient.getInstance().getIdentityId();
        } catch (Exception exception) {
            StorageException storageException = new StorageException(
                    "AWSMobileClient could not get user id." +
                            "Check whether you configured it properly before calling this method.",
                    exception
            );

            if (callback != null) {
                callback.onError(storageException);
            }
            throw storageException;
        }

        try {
            StorageListResult result = storageService.listFiles(
                    S3RequestUtils.getServiceKey(
                            request.getAccessLevel(),
                            identityId,
                            request.getPath(),
                            request.getTargetIdentityId()
                    )
            );

            if (callback != null) {
                callback.onResult(result);
            }
        } catch (Exception error) {
            if (callback != null) {
                callback.onError(error);
            }
            throw error;
        }
    }
}
