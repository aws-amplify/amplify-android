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
import com.amplifyframework.storage.operation.StorageRemoveOperation;
import com.amplifyframework.storage.result.StorageRemoveResult;
import com.amplifyframework.storage.s3.request.AWSS3StorageRemoveRequest;
import com.amplifyframework.storage.s3.service.AWSS3StorageService;
import com.amplifyframework.storage.s3.utils.S3RequestUtils;

import com.amazonaws.mobile.client.AWSMobileClient;

/**
 * An operation to remove a file from AWS S3.
 */
public final class AWSS3StorageRemoveOperation extends StorageRemoveOperation {
    private final AWSS3StorageService storageService;
    private final AWSS3StorageRemoveRequest request;
    private final Listener<StorageRemoveResult> callback;

    /**
     * Constructs a new AWSS3StorageRemoveOperation.
     * @param storageService S3 client wrapper
     * @param request remove request parameters
     * @param callback Listener to invoke when results are available
     */
    public AWSS3StorageRemoveOperation(AWSS3StorageService storageService,
                                       AWSS3StorageRemoveRequest request,
                                       Listener<StorageRemoveResult> callback) {
        this.request = request;
        this.storageService = storageService;
        this.callback = callback;
    }

    // TODO: This is currently a blocking method since deleteObject is blocking, consistent with the S3 SDK.
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
            storageService.deleteObject(
                    S3RequestUtils.getServiceKey(
                            request.getAccessLevel(),
                            identityId,
                            request.getKey(),
                            request.getTargetIdentityId()
                    )
            );

            if (callback != null) {
                callback.onResult(StorageRemoveResult.fromKey(request.getKey()));
            }
        } catch (Exception error) {
            if (callback != null) {
                callback.onError(error);
            }
            throw error;
        }
    }
}
