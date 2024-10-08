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
import androidx.annotation.OptIn;

import com.amplifyframework.annotations.InternalAmplifyApi;
import com.amplifyframework.annotations.InternalApiWarning;
import com.amplifyframework.auth.AuthCredentialsProvider;
import com.amplifyframework.core.Consumer;
import com.amplifyframework.storage.StorageException;
import com.amplifyframework.storage.operation.StorageRemoveOperation;
import com.amplifyframework.storage.result.StorageRemoveResult;
import com.amplifyframework.storage.s3.configuration.AWSS3StoragePluginConfiguration;
import com.amplifyframework.storage.s3.request.AWSS3StorageRemoveRequest;
import com.amplifyframework.storage.s3.service.StorageService;

import java.util.Objects;
import java.util.concurrent.ExecutorService;

/**
 * An operation to remove a file from AWS S3.
 * @deprecated Class should not be public and explicitly cast to. Cast to StorageRemoveOperation.
 * Internal usages are moving to AWSS3StoragePathRemoveOperation
 */
@Deprecated
@OptIn(markerClass = InternalApiWarning.class)
public final class AWSS3StorageRemoveOperation extends StorageRemoveOperation<AWSS3StorageRemoveRequest> {
    private final StorageService storageService;
    private final ExecutorService executorService;
    private final AuthCredentialsProvider authCredentialsProvider;
    private final Consumer<StorageRemoveResult> onSuccess;
    private final Consumer<StorageException> onError;
    private final AWSS3StoragePluginConfiguration awsS3StoragePluginConfiguration;

    /**
     * Constructs a new AWSS3StorageRemoveOperation.
     *
     * @param storageService      S3 client wrapper
     * @param executorService     Executor service used for running blocking operations on a
     *                            separate thread
     * @param authCredentialsProvider Interface to retrieve AWS specific auth information
     * @param request             remove request parameters
     * @param awsS3StoragePluginConfiguration s3Plugin configuration
     * @param onSuccess           notified when remove operation results available
     * @param onError             notified when remove operation does not complete due to error
     */
    public AWSS3StorageRemoveOperation(
            @NonNull StorageService storageService,
            @NonNull ExecutorService executorService,
            @NonNull AuthCredentialsProvider authCredentialsProvider,
            @NonNull AWSS3StorageRemoveRequest request,
            AWSS3StoragePluginConfiguration awsS3StoragePluginConfiguration,
            @NonNull Consumer<StorageRemoveResult> onSuccess,
            @NonNull Consumer<StorageException> onError
    ) {
        super(Objects.requireNonNull(request));
        this.storageService = Objects.requireNonNull(storageService);
        this.executorService = Objects.requireNonNull(executorService);
        this.authCredentialsProvider = authCredentialsProvider;
        this.onSuccess = Objects.requireNonNull(onSuccess);
        this.onError = Objects.requireNonNull(onError);
        this.awsS3StoragePluginConfiguration = awsS3StoragePluginConfiguration;
    }

    @OptIn(markerClass = InternalAmplifyApi.class)
    @SuppressWarnings("SyntheticAccessor, deprecation")
    @Override
    public void start() {
        executorService.submit(() -> {
            awsS3StoragePluginConfiguration.getAWSS3PluginPrefixResolver(authCredentialsProvider).
                resolvePrefix(getRequest().getAccessLevel(),
                    getRequest().getTargetIdentityId(),
                    prefix -> {
                        try {
                            String serviceKey = prefix.concat(getRequest().getKey());
                            storageService.deleteObject(serviceKey);
                            onSuccess.accept(new StorageRemoveResult(serviceKey, getRequest().getKey()));
                        } catch (Exception exception) {
                            onError.accept(new StorageException(
                                    "Something went wrong with your AWS S3 Storage remove operation",
                                    exception,
                                    "See attached exception for more information and suggestions"
                            ));
                        }

                    },
                    onError);
        });
    }
}
