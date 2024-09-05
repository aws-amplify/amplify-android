/*
 * Copyright 2023 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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

package com.amplifyframework.storage.s3;

import android.annotation.SuppressLint;
import android.content.Context;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.OptIn;
import androidx.annotation.VisibleForTesting;

import com.amplifyframework.annotations.InternalAmplifyApi;
import com.amplifyframework.annotations.InternalApiWarning;
import com.amplifyframework.auth.AuthCredentialsProvider;
import com.amplifyframework.auth.CognitoCredentialsProvider;
import com.amplifyframework.core.Consumer;
import com.amplifyframework.core.NoOpConsumer;
import com.amplifyframework.core.async.AmplifyOperation;
import com.amplifyframework.core.configuration.AmplifyOutputsData;
import com.amplifyframework.storage.BucketInfo;
import com.amplifyframework.storage.InvalidStorageBucketException;
import com.amplifyframework.storage.OutputsStorageBucket;
import com.amplifyframework.storage.ResolvedStorageBucket;
import com.amplifyframework.storage.StorageAccessLevel;
import com.amplifyframework.storage.StorageBucket;
import com.amplifyframework.storage.StorageException;
import com.amplifyframework.storage.StoragePath;
import com.amplifyframework.storage.StoragePlugin;
import com.amplifyframework.storage.TransferState;
import com.amplifyframework.storage.operation.StorageDownloadFileOperation;
import com.amplifyframework.storage.operation.StorageGetUrlOperation;
import com.amplifyframework.storage.operation.StorageListOperation;
import com.amplifyframework.storage.operation.StorageRemoveOperation;
import com.amplifyframework.storage.operation.StorageTransferOperation;
import com.amplifyframework.storage.operation.StorageUploadFileOperation;
import com.amplifyframework.storage.operation.StorageUploadInputStreamOperation;
import com.amplifyframework.storage.options.StorageDownloadFileOptions;
import com.amplifyframework.storage.options.StorageGetUrlOptions;
import com.amplifyframework.storage.options.StorageListOptions;
import com.amplifyframework.storage.options.StoragePagedListOptions;
import com.amplifyframework.storage.options.StorageRemoveOptions;
import com.amplifyframework.storage.options.StorageUploadFileOptions;
import com.amplifyframework.storage.options.StorageUploadInputStreamOptions;
import com.amplifyframework.storage.result.StorageDownloadFileResult;
import com.amplifyframework.storage.result.StorageGetUrlResult;
import com.amplifyframework.storage.result.StorageListResult;
import com.amplifyframework.storage.result.StorageRemoveResult;
import com.amplifyframework.storage.result.StorageTransferProgress;
import com.amplifyframework.storage.result.StorageTransferResult;
import com.amplifyframework.storage.result.StorageUploadFileResult;
import com.amplifyframework.storage.result.StorageUploadInputStreamResult;
import com.amplifyframework.storage.s3.configuration.AWSS3StoragePluginConfiguration;
import com.amplifyframework.storage.s3.operation.AWSS3StorageDownloadFileOperation;
import com.amplifyframework.storage.s3.operation.AWSS3StorageGetPresignedUrlOperation;
import com.amplifyframework.storage.s3.operation.AWSS3StorageListOperation;
import com.amplifyframework.storage.s3.operation.AWSS3StoragePathDownloadFileOperation;
import com.amplifyframework.storage.s3.operation.AWSS3StoragePathGetPresignedUrlOperation;
import com.amplifyframework.storage.s3.operation.AWSS3StoragePathListOperation;
import com.amplifyframework.storage.s3.operation.AWSS3StoragePathRemoveOperation;
import com.amplifyframework.storage.s3.operation.AWSS3StoragePathUploadFileOperation;
import com.amplifyframework.storage.s3.operation.AWSS3StoragePathUploadInputStreamOperation;
import com.amplifyframework.storage.s3.operation.AWSS3StorageRemoveOperation;
import com.amplifyframework.storage.s3.operation.AWSS3StorageUploadFileOperation;
import com.amplifyframework.storage.s3.operation.AWSS3StorageUploadInputStreamOperation;
import com.amplifyframework.storage.s3.options.AWSS3StorageDownloadFileOptions;
import com.amplifyframework.storage.s3.options.AWSS3StorageGetPresignedUrlOptions;
import com.amplifyframework.storage.s3.options.AWSS3StoragePagedListOptions;
import com.amplifyframework.storage.s3.options.AWSS3StorageUploadFileOptions;
import com.amplifyframework.storage.s3.options.AWSS3StorageUploadInputStreamOptions;
import com.amplifyframework.storage.s3.request.AWSS3StorageDownloadFileRequest;
import com.amplifyframework.storage.s3.request.AWSS3StorageGetPresignedUrlRequest;
import com.amplifyframework.storage.s3.request.AWSS3StorageListRequest;
import com.amplifyframework.storage.s3.request.AWSS3StoragePathDownloadFileRequest;
import com.amplifyframework.storage.s3.request.AWSS3StoragePathGetPresignedUrlRequest;
import com.amplifyframework.storage.s3.request.AWSS3StoragePathListRequest;
import com.amplifyframework.storage.s3.request.AWSS3StoragePathRemoveRequest;
import com.amplifyframework.storage.s3.request.AWSS3StoragePathUploadRequest;
import com.amplifyframework.storage.s3.request.AWSS3StorageRemoveRequest;
import com.amplifyframework.storage.s3.request.AWSS3StorageUploadRequest;
import com.amplifyframework.storage.s3.service.AWSS3StorageService;
import com.amplifyframework.storage.s3.service.AWSS3StorageServiceContainer;
import com.amplifyframework.storage.s3.transfer.S3StorageTransferClientProvider;
import com.amplifyframework.storage.s3.transfer.StorageTransferClientProvider;
import com.amplifyframework.storage.s3.transfer.TransferObserver;
import com.amplifyframework.storage.s3.transfer.TransferRecord;
import com.amplifyframework.storage.s3.transfer.TransferStatusUpdater;
import com.amplifyframework.storage.s3.transfer.TransferType;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.InputStream;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import aws.sdk.kotlin.services.s3.S3Client;

/**
 * A plugin for the storage category which uses S3 as a storage
 * repository.
 */
@SuppressLint("UnsafeOptInUsageWarning") // opting in to Internal Amplify usages
public final class AWSS3StoragePlugin extends StoragePlugin<S3Client> {

    /**
     * TAG for logging in storage category.
     */
    public static final String AWS_S3_STORAGE_LOG_NAMESPACE = "amplify:aws-s3-storage:%s";
    private static final String AWS_S3_STORAGE_PLUGIN_KEY = "awsS3StoragePlugin";

    private static final int DEFAULT_URL_EXPIRATION_DAYS = 7;

    private final AWSS3StorageService.Factory storageServiceFactory;
    private final ExecutorService executorService;
    private AuthCredentialsProvider authCredentialsProvider;
    private final AWSS3StoragePluginConfiguration awsS3StoragePluginConfiguration;
    private AWSS3StorageService defaultStorageService;
    @SuppressWarnings("deprecation")
    private StorageAccessLevel defaultAccessLevel;
    private int defaultUrlExpiration;

    private AWSS3StorageServiceContainer awss3StorageServiceContainer;
    @SuppressLint("UnsafeOptInUsageError")
    private List<AmplifyOutputsData.StorageBucket> configuredBuckets;

    @SuppressLint("UnsafeOptInUsageError")
    private StorageTransferClientProvider clientProvider
            = new S3StorageTransferClientProvider((region, bucketName) -> {
                if (region != null && bucketName != null) {
                    StorageBucket bucket = StorageBucket.fromBucketInfo(new BucketInfo(bucketName, region));
                    return awss3StorageServiceContainer.get((ResolvedStorageBucket) bucket).getClient();
                }

                if (region != null) {
                    return S3StorageTransferClientProvider.getS3Client(region, authCredentialsProvider);
                }
                return defaultStorageService.getClient();
            });

    /**
     * Constructs the AWS S3 Storage Plugin initializing the executor service.
     */
    @SuppressWarnings("unused") // This is a public API.
    @OptIn(markerClass = InternalApiWarning.class)
    public AWSS3StoragePlugin() {
        this(new CognitoCredentialsProvider());
    }

    /**
     * Constructs the AWS S3 Storage Plugin with the plugin configuration.
     *
     * @param awsS3StoragePluginConfiguration storage plugin configuration
     */
    @SuppressWarnings("unused") // This is a public API.
    @OptIn(markerClass = InternalApiWarning.class)
    public AWSS3StoragePlugin(AWSS3StoragePluginConfiguration awsS3StoragePluginConfiguration) {
        this(new CognitoCredentialsProvider(), awsS3StoragePluginConfiguration);
    }

    @VisibleForTesting
    AWSS3StoragePlugin(AuthCredentialsProvider authCredentialsProvider) {
        this((context, region, bucket, clientProvider) ->
                new AWSS3StorageService(
                    context,
                    region,
                    bucket,
                    authCredentialsProvider,
                    AWS_S3_STORAGE_PLUGIN_KEY,
                    clientProvider
                ),
            authCredentialsProvider,
            new AWSS3StoragePluginConfiguration.Builder().build());
    }

    @VisibleForTesting
    AWSS3StoragePlugin(AuthCredentialsProvider authCredentialsProvider,
                       AWSS3StoragePluginConfiguration awss3StoragePluginConfiguration) {

        this((context, region, bucket, clientProvider) ->
                new AWSS3StorageService(
                    context,
                    region,
                    bucket,
                    authCredentialsProvider,
                    AWS_S3_STORAGE_PLUGIN_KEY,
                    clientProvider
                ),
            authCredentialsProvider,
            awss3StoragePluginConfiguration);
    }

    @VisibleForTesting
    AWSS3StoragePlugin(
        AWSS3StorageService.Factory storageServiceFactory,
        AuthCredentialsProvider authCredentialsProvider,
        AWSS3StoragePluginConfiguration awss3StoragePluginConfiguration
    ) {
        super();
        this.storageServiceFactory = storageServiceFactory;
        this.executorService = Executors.newCachedThreadPool();
        this.authCredentialsProvider = authCredentialsProvider;
        this.awsS3StoragePluginConfiguration = awss3StoragePluginConfiguration;
    }

    @NonNull
    @Override
    public String getPluginKey() {
        return AWS_S3_STORAGE_PLUGIN_KEY;
    }

    @SuppressLint("UnsafeOptInUsageError")
    @Override
    @SuppressWarnings("deprecation")
    public void configure(
        JSONObject pluginConfiguration,
        @NonNull Context context
    ) throws StorageException {
        String regionStr;
        String bucket;
        try {
            regionStr = pluginConfiguration.getString(JsonKeys.REGION.getConfigurationKey());
        } catch (JSONException error) {
            throw new StorageException(
                "Missing or malformed value for Region in " + AWS_S3_STORAGE_PLUGIN_KEY + "configuration.",
                error,
                "Check the attached error to see where the parsing issue took place."
            );
        } catch (NullPointerException error) {
            throw new StorageException(
                "Missing configuration for " + AWS_S3_STORAGE_PLUGIN_KEY,
                "Check amplifyconfiguration.json to make sure that there is a section for " +
                    AWS_S3_STORAGE_PLUGIN_KEY + " under the storage category."
            );
        }

        String region = regionStr;

        if (region == null) {
            throw new StorageException(
                "Invalid region provided",
                "Make sure the region you have configured for the AWS S3 Storage plugin is a value we support."
            );
        }

        try {
            bucket = pluginConfiguration.getString(JsonKeys.BUCKET.getConfigurationKey());
        } catch (JSONException error) {
            throw new StorageException(
                "Missing or malformed value for bucket in " + AWS_S3_STORAGE_PLUGIN_KEY + "configuration.",
                error,
                "Check the attached error to see where the parsing issue took place."
            );
        }

        BucketInfo bucketInfo = new BucketInfo(bucket, region);
        configure(context, region, (ResolvedStorageBucket) StorageBucket.fromBucketInfo(bucketInfo));
    }

    @Override
    @InternalAmplifyApi
    public void configure(@NonNull AmplifyOutputsData configuration, @NonNull Context context) throws StorageException {
        AmplifyOutputsData.Storage storage = configuration.getStorage();

        if (storage == null) {
            throw new StorageException(
                "Missing storage configuration",
                "Ensure that storage configuration is present in your Amplify Outputs"
            );
        }

        this.configuredBuckets = storage.getBuckets();
        BucketInfo bucketInfo = new BucketInfo(storage.getBucketName(), storage.getAwsRegion());
        configure(context, storage.getAwsRegion(), (ResolvedStorageBucket) StorageBucket.fromBucketInfo(bucketInfo));
    }

    @SuppressWarnings("deprecation")
    @SuppressLint("UnsafeOptInUsageError")
    private void configure(
            @NonNull Context context,
            @NonNull String region,
            @NonNull ResolvedStorageBucket bucket
    ) throws StorageException {
        try {
            this.defaultStorageService = storageServiceFactory.create(
                    context,
                    region,
                    bucket.getBucketInfo().getBucketName(),
                    clientProvider);
            this.awss3StorageServiceContainer = new AWSS3StorageServiceContainer(
                    context, storageServiceFactory,
                    (S3StorageTransferClientProvider) clientProvider);
            this.awss3StorageServiceContainer.put(bucket.getBucketInfo().getBucketName(), this.defaultStorageService);
        } catch (RuntimeException exception) {
            throw new StorageException(
                "Failed to create storage service.",
                exception,
                "Did you make sure to add AWSCognitoAuthPlugin to Amplify? " +
                    "Check the attached exception for more details."
            );
        }

        // TODO: Integrate into config + options
        this.defaultAccessLevel = StorageAccessLevel.PUBLIC;
        this.defaultUrlExpiration = (int) TimeUnit.DAYS.toSeconds(DEFAULT_URL_EXPIRATION_DAYS);
    }

    @NonNull
    @Override
    public S3Client getEscapeHatch() {
        return defaultStorageService.getClient();
    }

    @NonNull
    @Override
    public String getVersion() {
        return BuildConfig.VERSION_NAME;
    }

    @SuppressWarnings("deprecation")
    @NonNull
    @Override
    public StorageGetUrlOperation<?> getUrl(
        @NonNull String key,
        @NonNull Consumer<StorageGetUrlResult> onSuccess,
        @NonNull Consumer<StorageException> onError) {
        return getUrl(key, StorageGetUrlOptions.defaultInstance(), onSuccess, onError);
    }

    @NonNull
    @Override
    public StorageGetUrlOperation<?> getUrl(
            @NonNull StoragePath path,
            @NonNull Consumer<StorageGetUrlResult> onSuccess,
            @NonNull Consumer<StorageException> onError
    ) {
        return getUrl(path, StorageGetUrlOptions.defaultInstance(), onSuccess, onError);
    }

    @NonNull
    @Override
    @SuppressWarnings("deprecation")
    public StorageGetUrlOperation<?> getUrl(
        @NonNull String key,
        @NonNull StorageGetUrlOptions options,
        @NonNull Consumer<StorageGetUrlResult> onSuccess,
        @NonNull Consumer<StorageException> onError) {
        boolean useAccelerateEndpoint = options instanceof AWSS3StorageGetPresignedUrlOptions &&
            ((AWSS3StorageGetPresignedUrlOptions) options).useAccelerateEndpoint();
        boolean validateObjectExistence = options instanceof AWSS3StorageGetPresignedUrlOptions &&
                ((AWSS3StorageGetPresignedUrlOptions) options).getValidateObjectExistence();
        AWSS3StorageGetPresignedUrlRequest request = new AWSS3StorageGetPresignedUrlRequest(
            key,
            options.getAccessLevel() != null
                ? options.getAccessLevel()
                : defaultAccessLevel,
            options.getTargetIdentityId(),
            options.getExpires() != 0
                ? options.getExpires()
                : defaultUrlExpiration,
            useAccelerateEndpoint,
            validateObjectExistence
        );

        GetStorageServiceResult result = getStorageServiceResult(options.getBucket());

        AWSS3StorageGetPresignedUrlOperation operation =
            new AWSS3StorageGetPresignedUrlOperation(
                result.storageService,
                executorService,
                authCredentialsProvider,
                request,
                awsS3StoragePluginConfiguration,
                onSuccess,
                onError);

        handleGetStorageServiceResult(onError, result, operation);

        return operation;
    }

    @NonNull
    @Override
    public StorageGetUrlOperation<?> getUrl(
            @NonNull StoragePath path,
            @NonNull StorageGetUrlOptions options,
            @NonNull Consumer<StorageGetUrlResult> onSuccess,
            @NonNull Consumer<StorageException> onError
    ) {
        boolean useAccelerateEndpoint = options instanceof AWSS3StorageGetPresignedUrlOptions &&
                ((AWSS3StorageGetPresignedUrlOptions) options).useAccelerateEndpoint();

        boolean validateObjectExistence = options instanceof AWSS3StorageGetPresignedUrlOptions &&
                ((AWSS3StorageGetPresignedUrlOptions) options).getValidateObjectExistence();

        AWSS3StoragePathGetPresignedUrlRequest request = new AWSS3StoragePathGetPresignedUrlRequest(
                path,
                options.getExpires() != 0 ? options.getExpires() : defaultUrlExpiration,
                useAccelerateEndpoint,
                validateObjectExistence
        );

        GetStorageServiceResult result = getStorageServiceResult(options.getBucket());

        AWSS3StoragePathGetPresignedUrlOperation operation =
                new AWSS3StoragePathGetPresignedUrlOperation(
                        result.storageService,
                        executorService,
                        authCredentialsProvider,
                        request,
                        onSuccess,
                        onError);

        handleGetStorageServiceResult(onError, result, operation);

        return operation;
    }

    @SuppressWarnings("deprecation")
    @NonNull
    @Override
    public StorageDownloadFileOperation<?> downloadFile(
        @NonNull String key,
        @NonNull File local,
        @NonNull Consumer<StorageDownloadFileResult> onSuccess,
        @NonNull Consumer<StorageException> onError
    ) {
        StorageDownloadFileOptions options = StorageDownloadFileOptions.defaultInstance();
        return downloadFile(key, local, options, NoOpConsumer.create(), onSuccess, onError);
    }

    @NonNull
    @Override
    public StorageDownloadFileOperation<?> downloadFile(
            @NonNull StoragePath path,
            @NonNull File local,
            @NonNull Consumer<StorageDownloadFileResult> onSuccess,
            @NonNull Consumer<StorageException> onError
    ) {
        StorageDownloadFileOptions options = StorageDownloadFileOptions.defaultInstance();
        return downloadFile(path, local, options, NoOpConsumer.create(), onSuccess, onError);
    }

    @SuppressWarnings("deprecation")
    @NonNull
    @Override
    public StorageDownloadFileOperation<?> downloadFile(
        @NonNull String key,
        @NonNull File local,
        @NonNull StorageDownloadFileOptions options,
        @NonNull Consumer<StorageDownloadFileResult> onSuccess,
        @NonNull Consumer<StorageException> onError
    ) {
        return downloadFile(key, local, options, NoOpConsumer.create(), onSuccess, onError);
    }

    @NonNull
    @Override
    public StorageDownloadFileOperation<?> downloadFile(
            @NonNull StoragePath path,
            @NonNull File local,
            @NonNull StorageDownloadFileOptions options,
            @NonNull Consumer<StorageDownloadFileResult> onSuccess,
            @NonNull Consumer<StorageException> onError
    ) {
        return downloadFile(path, local, options, NoOpConsumer.create(), onSuccess, onError);
    }

    @NonNull
    @Override
    @SuppressWarnings("deprecation")
    public StorageDownloadFileOperation<?> downloadFile(
        @NonNull String key,
        @NonNull File local,
        @NonNull StorageDownloadFileOptions options,
        @NonNull Consumer<StorageTransferProgress> onProgress,
        @NonNull Consumer<StorageDownloadFileResult> onSuccess,
        @NonNull Consumer<StorageException> onError
    ) {
        boolean useAccelerateEndpoint =
            options instanceof AWSS3StorageDownloadFileOptions &&
                ((AWSS3StorageDownloadFileOptions) options).useAccelerateEndpoint();
        AWSS3StorageDownloadFileRequest request = new AWSS3StorageDownloadFileRequest(
            key,
            local,
            options.getAccessLevel() != null
                ? options.getAccessLevel()
                : defaultAccessLevel,
            options.getTargetIdentityId(),
            useAccelerateEndpoint
        );

        GetStorageServiceResult result = getStorageServiceResult(options.getBucket());

        AWSS3StorageDownloadFileOperation operation = new AWSS3StorageDownloadFileOperation(
            result.storageService,
            executorService,
            authCredentialsProvider,
            request,
            awsS3StoragePluginConfiguration,
            onProgress,
            onSuccess,
            onError
        );

        handleGetStorageServiceResult(onError, result, operation);

        return operation;
    }

    @NonNull
    @Override
    public StorageDownloadFileOperation<?> downloadFile(
            @NonNull StoragePath path,
            @NonNull File local,
            @NonNull StorageDownloadFileOptions options,
            @NonNull Consumer<StorageTransferProgress> onProgress,
            @NonNull Consumer<StorageDownloadFileResult> onSuccess,
            @NonNull Consumer<StorageException> onError
    ) {
        boolean useAccelerateEndpoint =
                options instanceof AWSS3StorageDownloadFileOptions &&
                        ((AWSS3StorageDownloadFileOptions) options).useAccelerateEndpoint();

        AWSS3StoragePathDownloadFileRequest request = new AWSS3StoragePathDownloadFileRequest(
                path,
                local,
                useAccelerateEndpoint
        );

        GetStorageServiceResult result = getStorageServiceResult(options.getBucket());

        AWSS3StoragePathDownloadFileOperation operation = new AWSS3StoragePathDownloadFileOperation(
                request,
                result.storageService,
                executorService,
                authCredentialsProvider,
                onProgress,
                onSuccess,
                onError
        );

        handleGetStorageServiceResult(onError, result, operation);

        return operation;
    }

    @SuppressWarnings("deprecation")
    @NonNull
    @Override
    public StorageUploadFileOperation<?> uploadFile(
        @NonNull String key,
        @NonNull File local,
        @NonNull Consumer<StorageUploadFileResult> onSuccess,
        @NonNull Consumer<StorageException> onError
    ) {
        StorageUploadFileOptions options = StorageUploadFileOptions.defaultInstance();
        return uploadFile(key, local, options, NoOpConsumer.create(), onSuccess, onError);
    }

    @NonNull
    @Override
    public StorageUploadFileOperation<?> uploadFile(
            @NonNull StoragePath path,
            @NonNull File local,
            @NonNull Consumer<StorageUploadFileResult> onSuccess,
            @NonNull Consumer<StorageException> onError
    ) {
        StorageUploadFileOptions options = StorageUploadFileOptions.defaultInstance();
        return uploadFile(path, local, options, NoOpConsumer.create(), onSuccess, onError);
    }

    @SuppressWarnings("deprecation")
    @NonNull
    @Override
    public StorageUploadFileOperation<?> uploadFile(
        @NonNull String key,
        @NonNull File local,
        @NonNull StorageUploadFileOptions options,
        @NonNull Consumer<StorageUploadFileResult> onSuccess,
        @NonNull Consumer<StorageException> onError
    ) {
        return uploadFile(key, local, options, NoOpConsumer.create(), onSuccess, onError);
    }

    @NonNull
    @Override
    public StorageUploadFileOperation<?> uploadFile(
            @NonNull StoragePath path,
            @NonNull File local,
            @NonNull StorageUploadFileOptions options,
            @NonNull Consumer<StorageUploadFileResult> onSuccess,
            @NonNull Consumer<StorageException> onError
    ) {
        return uploadFile(path, local, options, NoOpConsumer.create(), onSuccess, onError);
    }

    @NonNull
    @Override
    @SuppressWarnings("deprecation")
    public StorageUploadFileOperation<?> uploadFile(
        @NonNull String key,
        @NonNull File local,
        @NonNull StorageUploadFileOptions options,
        @NonNull Consumer<StorageTransferProgress> onProgress,
        @NonNull Consumer<StorageUploadFileResult> onSuccess,
        @NonNull Consumer<StorageException> onError
    ) {
        boolean useAccelerateEndpoint = options instanceof AWSS3StorageUploadFileOptions &&
            ((AWSS3StorageUploadFileOptions) options).useAccelerateEndpoint();
        AWSS3StorageUploadRequest<File> request = new AWSS3StorageUploadRequest<>(
            key,
            local,
            options.getAccessLevel() != null
                ? options.getAccessLevel()
                : defaultAccessLevel,
            options.getTargetIdentityId(),
            options.getContentType(),
            options instanceof AWSS3StorageUploadFileOptions
                ? ((AWSS3StorageUploadFileOptions) options).getServerSideEncryption()
                : ServerSideEncryption.NONE,
            options.getMetadata(),
            useAccelerateEndpoint
        );

        GetStorageServiceResult result = getStorageServiceResult(options.getBucket());

        AWSS3StorageUploadFileOperation operation = new AWSS3StorageUploadFileOperation(
            result.storageService,
            executorService,
            authCredentialsProvider,
            request,
            awsS3StoragePluginConfiguration,
            onProgress,
            onSuccess,
            onError
        );

        handleGetStorageServiceResult(onError, result, operation);

        return operation;
    }

    @NonNull
    @Override
    public StorageUploadFileOperation<?> uploadFile(
            @NonNull StoragePath path,
            @NonNull File local,
            @NonNull StorageUploadFileOptions options,
            @NonNull Consumer<StorageTransferProgress> onProgress,
            @NonNull Consumer<StorageUploadFileResult> onSuccess,
            @NonNull Consumer<StorageException> onError
    ) {
        boolean useAccelerateEndpoint = options instanceof AWSS3StorageUploadFileOptions &&
                ((AWSS3StorageUploadFileOptions) options).useAccelerateEndpoint();
        AWSS3StoragePathUploadRequest<File> request = new AWSS3StoragePathUploadRequest<>(
                path,
                local,
                options.getContentType(),
                options instanceof AWSS3StorageUploadFileOptions
                        ? ((AWSS3StorageUploadFileOptions) options).getServerSideEncryption()
                        : ServerSideEncryption.NONE,
                options.getMetadata(),
                useAccelerateEndpoint
        );

        GetStorageServiceResult result = getStorageServiceResult(options.getBucket());

        AWSS3StoragePathUploadFileOperation operation = new AWSS3StoragePathUploadFileOperation(
                request,
                result.storageService,
                executorService,
                authCredentialsProvider,
                onProgress,
                onSuccess,
                onError
        );

        handleGetStorageServiceResult(onError, result, operation);

        return operation;
    }

    @SuppressWarnings("deprecation")
    @NonNull
    @Override
    public StorageUploadInputStreamOperation<?> uploadInputStream(
        @NonNull String key,
        @NonNull InputStream local,
        @NonNull Consumer<StorageUploadInputStreamResult> onSuccess,
        @NonNull Consumer<StorageException> onError
    ) {
        StorageUploadInputStreamOptions options = StorageUploadInputStreamOptions.defaultInstance();
        return uploadInputStream(key, local, options, NoOpConsumer.create(), onSuccess, onError);
    }

    @NonNull
    @Override
    public StorageUploadInputStreamOperation<?> uploadInputStream(
            @NonNull StoragePath path,
            @NonNull InputStream local,
            @NonNull Consumer<StorageUploadInputStreamResult> onSuccess,
            @NonNull Consumer<StorageException> onError) {
        StorageUploadInputStreamOptions options = StorageUploadInputStreamOptions.defaultInstance();
        return uploadInputStream(path, local, options, NoOpConsumer.create(), onSuccess, onError);
    }

    @SuppressWarnings("deprecation")
    @NonNull
    @Override
    public StorageUploadInputStreamOperation<?> uploadInputStream(
        @NonNull String key,
        @NonNull InputStream local,
        @NonNull StorageUploadInputStreamOptions options,
        @NonNull Consumer<StorageUploadInputStreamResult> onSuccess,
        @NonNull Consumer<StorageException> onError
    ) {
        return uploadInputStream(key, local, options, NoOpConsumer.create(), onSuccess, onError);
    }

    @Override
    public StorageUploadInputStreamOperation<?> uploadInputStream(
            @NonNull StoragePath path,
            @NonNull InputStream local,
            @NonNull StorageUploadInputStreamOptions options,
            @NonNull Consumer<StorageUploadInputStreamResult> onSuccess,
            @NonNull Consumer<StorageException> onError
    ) {
        return uploadInputStream(path, local, options, NoOpConsumer.create(), onSuccess, onError);
    }

    @NonNull
    @Override
    @SuppressWarnings("deprecation")
    public StorageUploadInputStreamOperation<?> uploadInputStream(
        @NonNull String key,
        @NonNull InputStream local,
        @NonNull StorageUploadInputStreamOptions options,
        @NonNull Consumer<StorageTransferProgress> onProgress,
        @NonNull Consumer<StorageUploadInputStreamResult> onSuccess,
        @NonNull Consumer<StorageException> onError
    ) {
        boolean useAccelerateEndpoint = options instanceof AWSS3StorageUploadInputStreamOptions &&
            ((AWSS3StorageUploadInputStreamOptions) options).useAccelerateEndpoint();
        AWSS3StorageUploadRequest<InputStream> request = new AWSS3StorageUploadRequest<>(
            key,
            local,
            options.getAccessLevel() != null
                ? options.getAccessLevel()
                : defaultAccessLevel,
            options.getTargetIdentityId(),
            options.getContentType(),
            options instanceof AWSS3StorageUploadInputStreamOptions
                ? ((AWSS3StorageUploadInputStreamOptions) options).getServerSideEncryption()
                : ServerSideEncryption.NONE,
            options.getMetadata(),
            useAccelerateEndpoint
        );

        GetStorageServiceResult result = getStorageServiceResult(options.getBucket());

        AWSS3StorageUploadInputStreamOperation operation = new AWSS3StorageUploadInputStreamOperation(
            result.storageService,
            executorService,
            authCredentialsProvider,
            awsS3StoragePluginConfiguration,
            request,
            onProgress,
            onSuccess,
            onError
        );

        handleGetStorageServiceResult(onError, result, operation);

        return operation;
    }

    @NonNull
    @Override
    public StorageUploadInputStreamOperation<?> uploadInputStream(
            @NonNull StoragePath path,
            @NonNull InputStream local,
            @NonNull StorageUploadInputStreamOptions options,
            @NonNull Consumer<StorageTransferProgress> onProgress,
            @NonNull Consumer<StorageUploadInputStreamResult> onSuccess,
            @NonNull Consumer<StorageException> onError
    ) {
        boolean useAccelerateEndpoint = options instanceof AWSS3StorageUploadInputStreamOptions &&
                ((AWSS3StorageUploadInputStreamOptions) options).useAccelerateEndpoint();
        AWSS3StoragePathUploadRequest<InputStream> request = new AWSS3StoragePathUploadRequest<>(
                path,
                local,
                options.getContentType(),
                options instanceof AWSS3StorageUploadInputStreamOptions
                        ? ((AWSS3StorageUploadInputStreamOptions) options).getServerSideEncryption()
                        : ServerSideEncryption.NONE,
                options.getMetadata(),
                useAccelerateEndpoint
        );

        GetStorageServiceResult result = getStorageServiceResult(options.getBucket());

        AWSS3StoragePathUploadInputStreamOperation operation =
                new AWSS3StoragePathUploadInputStreamOperation(
                        request,
                        result.storageService,
                        executorService,
                        authCredentialsProvider,
                        onProgress,
                        onSuccess,
                        onError
                );

        handleGetStorageServiceResult(onError, result, operation);

        return operation;
    }

    @SuppressWarnings("deprecation")
    @NonNull
    @Override
    public StorageRemoveOperation<?> remove(
        @NonNull String key,
        @NonNull Consumer<StorageRemoveResult> onSuccess,
        @NonNull Consumer<StorageException> onError
    ) {
        return remove(key, StorageRemoveOptions.defaultInstance(), onSuccess, onError);
    }

    @NonNull
    @Override
    public StorageRemoveOperation<?> remove(
            @NonNull StoragePath path,
            @NonNull Consumer<StorageRemoveResult> onSuccess,
            @NonNull Consumer<StorageException> onError
    ) {
        return remove(path, StorageRemoveOptions.defaultInstance(), onSuccess, onError);
    }

    @NonNull
    @Override
    @SuppressWarnings("deprecation")
    public StorageRemoveOperation<?> remove(
        @NonNull String key,
        @NonNull StorageRemoveOptions options,
        @NonNull Consumer<StorageRemoveResult> onSuccess,
        @NonNull Consumer<StorageException> onError
    ) {
        AWSS3StorageRemoveRequest request = new AWSS3StorageRemoveRequest(
            key,
            options.getAccessLevel() != null
                ? options.getAccessLevel()
                : defaultAccessLevel,
            options.getTargetIdentityId()
        );

        GetStorageServiceResult result = getStorageServiceResult(options.getBucket());

        AWSS3StorageRemoveOperation operation =
            new AWSS3StorageRemoveOperation(
                result.storageService,
                executorService,
                authCredentialsProvider,
                request,
                awsS3StoragePluginConfiguration,
                onSuccess,
                onError);

        handleGetStorageServiceResult(onError, result, operation);

        return operation;
    }

    @NonNull
    @Override
    public StorageRemoveOperation<?> remove(
            @NonNull StoragePath path,
            @NonNull StorageRemoveOptions options,
            @NonNull Consumer<StorageRemoveResult> onSuccess,
            @NonNull Consumer<StorageException> onError
    ) {
        AWSS3StoragePathRemoveRequest request = new AWSS3StoragePathRemoveRequest(path);

        GetStorageServiceResult result = getStorageServiceResult(options.getBucket());

        AWSS3StoragePathRemoveOperation operation =
                new AWSS3StoragePathRemoveOperation(
                        result.storageService,
                        executorService,
                        authCredentialsProvider,
                        request,
                        onSuccess,
                        onError);

        handleGetStorageServiceResult(onError, result, operation);

        return operation;
    }
    
    @SuppressLint("UnsafeOptInUsageError")
    @Override
    @SuppressWarnings("deprecation")
    public void getTransfer(
        @NonNull String transferId,
        @NonNull Consumer<StorageTransferOperation<?, ? extends StorageTransferResult>> onReceived,
        @NonNull Consumer<StorageException> onError) {
        executorService.submit(() -> {
            try {
                TransferRecord transferRecord = defaultStorageService.getTransfer(transferId);
                if (transferRecord != null) {
                    TransferObserver transferObserver =
                        new TransferObserver(
                            transferRecord.getId(),
                            defaultStorageService.getTransferManager().getTransferStatusUpdater(),
                            transferRecord.getBucketName(),
                            transferRecord.getRegion(),
                            transferRecord.getKey(),
                            transferRecord.getFile(),
                            null,
                            transferRecord.getState() != null ? transferRecord.getState() : TransferState.UNKNOWN);
                    TransferType transferType = transferRecord.getType();

                    AWSS3StorageService storageService
                            = getAwss3StorageServiceFromTransferRecord(onError, transferRecord);

                    switch (Objects.requireNonNull(transferType)) {
                        case UPLOAD:
                            if (transferRecord.getFile().startsWith(TransferStatusUpdater.TEMP_FILE_PREFIX)) {
                                AWSS3StorageUploadInputStreamOperation operation =
                                    new AWSS3StorageUploadInputStreamOperation(
                                        transferId,
                                        storageService,
                                        executorService,
                                        authCredentialsProvider,
                                        awsS3StoragePluginConfiguration,
                                        null,
                                        transferObserver);
                                onReceived.accept(operation);
                            } else {
                                AWSS3StorageUploadFileOperation operation =
                                    new AWSS3StorageUploadFileOperation(
                                        transferId,
                                        storageService,
                                        executorService,
                                        authCredentialsProvider,
                                        awsS3StoragePluginConfiguration,
                                        null,
                                        transferObserver);
                                onReceived.accept(operation);
                            }
                            break;
                        case DOWNLOAD:
                            AWSS3StorageDownloadFileOperation
                                downloadFileOperation = new AWSS3StorageDownloadFileOperation(
                                transferId,
                                new File(transferRecord.getFile()),
                                storageService,
                                executorService,
                                authCredentialsProvider,
                                awsS3StoragePluginConfiguration,
                                null,
                                transferObserver);
                            onReceived.accept(downloadFileOperation);
                            break;
                        default:
                    }
                } else {
                    onError.accept(new StorageException("Get transfer failed",
                        "Please verify that the transfer id is valid and the transfer is not completed"));
                }
            } catch (Exception exception) {
                onError.accept(new StorageException("Get transfer failed",
                    exception,
                    "Please verify that the transfer id is valid and the transfer is not completed"));
            }
        });
    }

    private AWSS3StorageService getAwss3StorageServiceFromTransferRecord(
            @NonNull Consumer<StorageException> onError,
            TransferRecord transferRecord
    ) {
        AWSS3StorageService storageService = defaultStorageService;
        if (transferRecord.getRegion() != null && transferRecord.getBucketName() != null) {
            try {
                BucketInfo bucketInfo = new BucketInfo(
                        transferRecord.getBucketName(),
                        transferRecord.getRegion());
                StorageBucket bucket = StorageBucket.fromBucketInfo(bucketInfo);
                storageService = getStorageService(bucket);
            } catch (StorageException exception) {
                onError.accept(exception);
            }
        }
        return storageService;
    }

    @NonNull
    @SuppressWarnings("deprecation")
    @Override
    public StorageListOperation<?> list(
        @NonNull String path,
        @NonNull Consumer<StorageListResult> onSuccess,
        @NonNull Consumer<StorageException> onError
    ) {
        return list(path, StorageListOptions.defaultInstance(), onSuccess, onError);
    }

    @NonNull
    @Override
    @SuppressWarnings("deprecation")
    public StorageListOperation<?> list(
        @NonNull String path,
        @NonNull StorageListOptions options,
        @NonNull Consumer<StorageListResult> onSuccess,
        @NonNull Consumer<StorageException> onError
    ) {
        StoragePagedListOptions storagePagedListOptions =
            StoragePagedListOptions.builder().
                accessLevel(options.getAccessLevel())
                .targetIdentityId(options.getTargetIdentityId())
                .setPageSize(AWSS3StoragePagedListOptions.ALL_PAGE_SIZE)
                .build();
        return list(path, storagePagedListOptions, onSuccess, onError);
    }

    @Override
    @SuppressWarnings("deprecation")
    public StorageListOperation<?> list(@NonNull String path,
                                        @NonNull StoragePagedListOptions options,
                                        @NonNull Consumer<StorageListResult> onSuccess,
                                        @NonNull Consumer<StorageException> onError) {

        AWSS3StorageListRequest request = new AWSS3StorageListRequest(
            path,
            options.getAccessLevel() != null ? options.getAccessLevel() : defaultAccessLevel,
            options.getTargetIdentityId(),
            options.getPageSize(),
            options.getNextToken(),
            options.getSubpathStrategy());

        GetStorageServiceResult result = getStorageServiceResult(options.getBucket());

        AWSS3StorageListOperation operation =
            new AWSS3StorageListOperation(
                result.storageService,
                executorService,
                authCredentialsProvider,
                request,
                awsS3StoragePluginConfiguration,
                onSuccess,
                onError);

        handleGetStorageServiceResult(onError, result, operation);

        return operation;
    }

    @NonNull
    @Override
    public StorageListOperation<?> list(
            @NonNull StoragePath path,
            @NonNull StoragePagedListOptions options,
            @NonNull Consumer<StorageListResult> onSuccess,
            @NonNull Consumer<StorageException> onError
    ) {
        AWSS3StoragePathListRequest request = new AWSS3StoragePathListRequest(
                path,
                options.getPageSize(),
                options.getNextToken(),
                options.getSubpathStrategy());

        GetStorageServiceResult result = getStorageServiceResult(options.getBucket());

        AWSS3StoragePathListOperation operation =
                new AWSS3StoragePathListOperation(
                        result.storageService,
                        executorService,
                        authCredentialsProvider,
                        request,
                        onSuccess,
                        onError);

        handleGetStorageServiceResult(onError, result, operation);

        return operation;
    }

    private static void handleGetStorageServiceResult(
            @NonNull Consumer<StorageException> onError,
            GetStorageServiceResult result,
            AmplifyOperation<?> operation
    ) {
        if (result.storageException == null) {
            operation.start();
        } else {
            onError.accept(result.storageException);
        }
    }

    @VisibleForTesting
    @NonNull
    GetStorageServiceResult getStorageServiceResult(@Nullable StorageBucket bucket) {
        StorageException storageException = null;
        AWSS3StorageService storageService = defaultStorageService;
        try {
            storageService = getStorageService(bucket);
        } catch (StorageException exception) {
            storageException = exception;
        }
        return new GetStorageServiceResult(storageService, storageException);
    }

    @SuppressLint("UnsafeOptInUsageError")
    @VisibleForTesting
    @NonNull
    AWSS3StorageService getStorageService(@Nullable StorageBucket bucket) throws StorageException {
        if (bucket == null) {
            return defaultStorageService;
        }

        if (bucket instanceof OutputsStorageBucket) {
            if (configuredBuckets != null && !configuredBuckets.isEmpty()) {
                String name = ((OutputsStorageBucket) bucket).getName();
                for (AmplifyOutputsData.StorageBucket configuredBucket : configuredBuckets) {
                    if (configuredBucket.getName().equals(name)) {
                        String bucketName = configuredBucket.getBucketName();
                        String region = configuredBucket.getAwsRegion();
                        return awss3StorageServiceContainer.get(bucketName, region);
                    }
                }
            }
            throw new StorageException(
                    "Unable to find bucket from name in Amplify Outputs.",
                    new InvalidStorageBucketException(),
                    "Ensure the bucket name used is available in Amplify Outputs.");
        }

        if (bucket instanceof ResolvedStorageBucket) {
            return awss3StorageServiceContainer.get((ResolvedStorageBucket) bucket);
        }

        return defaultStorageService;
    }

    /**
     * Holds the keys for the various configuration properties for this plugin.
     */
    public enum JsonKeys {
        /**
         * The S3 bucket this plugin will work with.
         */
        BUCKET("bucket"),

        /**
         * The AWS region this plugin will work with.
         */
        REGION("region");

        /**
         * The key this property is listed under in the config JSON.
         */
        private final String configurationKey;

        /**
         * Construct the enum with the config key.
         *
         * @param configurationKey The key this property is listed under in the config JSON.
         */
        JsonKeys(final String configurationKey) {
            this.configurationKey = configurationKey;
        }

        /**
         * Returns the key this property is listed under in the config JSON.
         *
         * @return The key as a string
         */
        @NonNull
        public String getConfigurationKey() {
            return configurationKey;
        }
    }

    @VisibleForTesting
    @SuppressWarnings("checkstyle:VisibilityModifier")
    static class GetStorageServiceResult {
        final AWSS3StorageService storageService;
        final StorageException storageException;

        GetStorageServiceResult(AWSS3StorageService storageService, StorageException exception) {
            this.storageService = storageService;
            this.storageException = exception;
        }
    }
}
