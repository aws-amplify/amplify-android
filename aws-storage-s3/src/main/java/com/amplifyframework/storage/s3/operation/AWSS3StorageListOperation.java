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

import com.amplifyframework.auth.AuthCredentialsProvider;
import com.amplifyframework.core.Consumer;
import com.amplifyframework.storage.StorageException;
import com.amplifyframework.storage.StorageItem;
import com.amplifyframework.storage.operation.StorageListOperation;
import com.amplifyframework.storage.result.StorageListResult;
import com.amplifyframework.storage.s3.configuration.AWSS3StoragePluginConfiguration;
import com.amplifyframework.storage.s3.options.AWSS3StoragePagedListOptions;
import com.amplifyframework.storage.s3.request.AWSS3StorageListRequest;
import com.amplifyframework.storage.s3.service.StorageService;

import java.util.List;
import java.util.concurrent.ExecutorService;

/**
 * An operation to list items from AWS S3.
 */

public final class AWSS3StorageListOperation extends StorageListOperation<AWSS3StorageListRequest> {
    private final StorageService storageService;
    private final ExecutorService executorService;
    private final AuthCredentialsProvider authCredentialsProvider;
    private final Consumer<StorageListResult> onSuccess;
    private final Consumer<StorageException> onError;
    private final AWSS3StoragePluginConfiguration awsS3StoragePluginConfiguration;

    /**
     * Constructs a new AWSS3StorageListOperation.
     *
     * @param storageService      S3 client wrapper
     * @param executorService     Executor service used for running blocking operations on a
     *                            separate thread
     * @param authCredentialsProvider Interface to retrieve AWS specific auth information
     * @param request             list request parameters
     * @param awss3StoragePluginConfiguration s3Plugin configuration
     * @param onSuccess           notified when list operation results are available
     * @param onError             notified when list results cannot be obtained due to error
     */
    public AWSS3StorageListOperation(
            @NonNull StorageService storageService,
            @NonNull ExecutorService executorService,
            @NonNull AuthCredentialsProvider authCredentialsProvider,
            @NonNull AWSS3StorageListRequest request,
            @NonNull AWSS3StoragePluginConfiguration awss3StoragePluginConfiguration,
            @NonNull Consumer<StorageListResult> onSuccess,
            @NonNull Consumer<StorageException> onError
    ) {
        super(request);
        this.storageService = storageService;
        this.executorService = executorService;
        this.authCredentialsProvider = authCredentialsProvider;
        this.onSuccess = onSuccess;
        this.onError = onError;
        this.awsS3StoragePluginConfiguration = awss3StoragePluginConfiguration;
    }

    @SuppressWarnings({"SyntheticAccessor", "deprecation"})
    @Override
    public void start() {
        executorService.submit(() -> {
                awsS3StoragePluginConfiguration.
                    getAWSS3PluginPrefixResolver(authCredentialsProvider).
                    resolvePrefix(getRequest().getAccessLevel(),
                        getRequest().getTargetIdentityId(),
                        prefix -> {
                            try {
                                String serviceKey = prefix.concat(getRequest().getPath());
                                if (getRequest().getPageSize() == AWSS3StoragePagedListOptions.ALL_PAGE_SIZE) {
                                    // fetch all the keys
                                    List<StorageItem> listedItems = storageService.listFiles(serviceKey, prefix);
                                    onSuccess.accept(StorageListResult.fromItems(listedItems, null));
                                } else {
                                    onSuccess.accept(
                                        storageService.listFiles(serviceKey, prefix, getRequest().getPageSize(),
                                            getRequest().getNextToken()));
                                }
                            } catch (Exception exception) {
                                onError.accept(new StorageException(
                                    "Something went wrong with your AWS S3 Storage list operation",
                                    exception,
                                    "See attached exception for more information and suggestions"
                                ));
                            }
                        },
                        onError);
            }
        );
    }
}
