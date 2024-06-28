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

import com.amplifyframework.auth.AuthCredentialsProvider;
import com.amplifyframework.core.Consumer;
import com.amplifyframework.storage.StorageException;
import com.amplifyframework.storage.operation.StorageGetUrlOperation;
import com.amplifyframework.storage.result.StorageGetUrlResult;
import com.amplifyframework.storage.s3.configuration.AWSS3StoragePluginConfiguration;
import com.amplifyframework.storage.s3.request.AWSS3StorageGetPresignedUrlRequest;
import com.amplifyframework.storage.s3.service.StorageService;

import java.net.URL;
import java.util.concurrent.ExecutorService;

import aws.sdk.kotlin.services.s3.model.NotFound;

/**
 * An operation to retrieve pre-signed object URL from AWS S3.
 *
 * @deprecated Class should not be public and explicitly cast to. Cast to StorageGetUrlOperation.
 * Internal usages are moving to AWSS3StoragePathGetPresignedUrlOperation
 */
@Deprecated
public final class AWSS3StorageGetPresignedUrlOperation
        extends StorageGetUrlOperation<AWSS3StorageGetPresignedUrlRequest> {
    private final StorageService storageService;
    private final ExecutorService executorService;
    private final AuthCredentialsProvider authCredentialsProvider;
    private final Consumer<StorageGetUrlResult> onSuccess;
    private final Consumer<StorageException> onError;
    private final AWSS3StoragePluginConfiguration awsS3StoragePluginConfiguration;

    /**
     * Constructs a new AWSS3StorageGetUrlOperation.
     *
     * @param storageService                  S3 client wrapper
     * @param executorService                 Executor service used for running
     *                                        blocking operations on a separate thread
     * @param authCredentialsProvider         Interface to retrieve AWS specific auth information
     * @param request                         getUrl request parameters
     * @param awss3StoragePluginConfiguration s3Plugin configuration
     * @param onSuccess                       Notified when URL is generated.
     * @param onError                         Notified upon URL generation error
     */
    public AWSS3StorageGetPresignedUrlOperation(
            @NonNull StorageService storageService,
            @NonNull ExecutorService executorService,
            @NonNull AuthCredentialsProvider authCredentialsProvider,
            @NonNull AWSS3StorageGetPresignedUrlRequest request,
            @NonNull AWSS3StoragePluginConfiguration awss3StoragePluginConfiguration,
            @NonNull Consumer<StorageGetUrlResult> onSuccess,
            @NonNull Consumer<StorageException> onError
    ) {
        super(request);
        this.storageService = storageService;
        this.executorService = executorService;
        this.authCredentialsProvider = authCredentialsProvider;
        this.awsS3StoragePluginConfiguration = awss3StoragePluginConfiguration;
        this.onSuccess = onSuccess;
        this.onError = onError;
    }

    @SuppressLint("SyntheticAccessor")
    @Override
    public void start() {
        executorService.submit(() -> {
            awsS3StoragePluginConfiguration.getAWSS3PluginPrefixResolver(authCredentialsProvider).
                    resolvePrefix(getRequest().getAccessLevel(),
                        getRequest().getTargetIdentityId(),
                        prefix -> {
                            try {
                                String serviceKey = prefix.concat(getRequest().getKey());

                                if (getRequest().validateObjectExistence()) {
                                    try {
                                        storageService.validateObjectExists(serviceKey);
                                    } catch (NotFound nfe) {
                                        onError.accept(new StorageException(
                                                "Unable to generate URL for non-existent path: $serviceKey",
                                                nfe,
                                                "Please ensure the path is valid or the object has been uploaded."
                                        ));
                                        return;
                                    } catch (Exception exception) {
                                        onError.accept(new StorageException(
                                                "Encountered an issue while validating the existence of object",
                                                exception,
                                                "See included exception for more details and suggestions to fix."
                                        ));
                                        return;
                                    }
                                }

                                URL url = storageService.getPresignedUrl(
                                        serviceKey,
                                        getRequest().getExpires(),
                                        getRequest().useAccelerateEndpoint());
                                onSuccess.accept(StorageGetUrlResult.fromUrl(url));
                            } catch (Exception exception) {
                                onError.accept(new StorageException(
                                        "Encountered an issue while generating pre-signed URL",
                                        exception,
                                        "See included exception for more details and suggestions to fix."
                                ));
                            }

                        }, onError);
            }
        );
    }
}
