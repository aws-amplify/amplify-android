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

package com.amplifyframework.storage.s3.Operation;

import com.amplifyframework.core.async.Listener;
import com.amplifyframework.storage.exception.StorageException;
import com.amplifyframework.storage.operation.StorageOperation;
import com.amplifyframework.storage.result.StorageRemoveResult;
import com.amplifyframework.storage.s3.Request.AWSS3StorageRemoveRequest;
import com.amplifyframework.storage.s3.Service.AWSS3StorageService;
import com.amplifyframework.storage.s3.Utils.S3RequestUtils;

import com.amazonaws.mobile.client.AWSMobileClient;

/**
 * An operation to remove a file from AWS S3.
 */
public final class AWSS3StorageRemoveOperation extends StorageOperation {
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

            callback.onError(storageException);
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

            callback.onResult(StorageRemoveResult.fromKey(request.getKey()));
        } catch (Exception error) {
            callback.onError(error);
            throw error;
        }
    }

    @Override
    public void cancel() throws StorageException {
        // TODO: This is a NO-OP for remove - discuss what to do for this case
    }

    @Override
    public void pause() throws StorageException {
        // TODO: This is a NO-OP for remove - discuss what to do for this case
    }

    @Override
    public void resume() throws StorageException {
        // TODO: This is a NO-OP for remove - discuss what to do for this case
    }
}
