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

import android.content.Context;
import androidx.annotation.NonNull;
import androidx.annotation.OptIn;
import androidx.annotation.VisibleForTesting;

import com.amplifyframework.annotations.InternalApiWarning;
import com.amplifyframework.auth.AuthCredentialsProvider;
import com.amplifyframework.auth.CognitoCredentialsProvider;
import com.amplifyframework.core.Consumer;
import com.amplifyframework.core.NoOpConsumer;
import com.amplifyframework.storage.StorageAccessLevel;
import com.amplifyframework.storage.StorageException;
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
import com.amplifyframework.storage.s3.operation.AWSS3StorageRemoveOperation;
import com.amplifyframework.storage.s3.operation.AWSS3StorageUploadFileOperation;
import com.amplifyframework.storage.s3.operation.AWSS3StorageUploadInputStreamOperation;
import com.amplifyframework.storage.s3.options.AWSS3StorageDownloadFileOptions;
import com.amplifyframework.storage.s3.options.AWSS3StorageGetPresignedUrlOptions;
import com.amplifyframework.storage.s3.options.AWSS3StorageUploadFileOptions;
import com.amplifyframework.storage.s3.options.AWSS3StorageUploadInputStreamOptions;
import com.amplifyframework.storage.s3.request.AWSS3StorageDownloadFileRequest;
import com.amplifyframework.storage.s3.request.AWSS3StorageGetPresignedUrlRequest;
import com.amplifyframework.storage.s3.request.AWSS3StorageListRequest;
import com.amplifyframework.storage.s3.request.AWSS3StorageRemoveRequest;
import com.amplifyframework.storage.s3.request.AWSS3StorageUploadRequest;
import com.amplifyframework.storage.s3.service.AWSS3StorageService;
import com.amplifyframework.storage.s3.service.StorageService;
import com.amplifyframework.storage.s3.transfer.TransferObserver;
import com.amplifyframework.storage.s3.transfer.TransferRecord;
import com.amplifyframework.storage.s3.transfer.TransferStatusUpdater;
import com.amplifyframework.storage.s3.transfer.TransferType;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.InputStream;
import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import aws.sdk.kotlin.services.s3.S3Client;

/**
 * A plugin for the storage category which uses S3 as a storage
 * repository.
 */
public final class AWSS3StoragePlugin extends StoragePlugin<S3Client> {

    /**
     * TAG for logging in storage category.
     */
    public static final String AWS_S3_STORAGE_LOG_NAMESPACE = "amplify:aws-s3-storage:%s";
    private static final String AWS_S3_STORAGE_PLUGIN_KEY = "awsS3StoragePlugin";

    private final StorageService.Factory storageServiceFactory;
    private final ExecutorService executorService;
    private final AuthCredentialsProvider authCredentialsProvider;
    private final AWSS3StoragePluginConfiguration awsS3StoragePluginConfiguration;
    private AWSS3StorageService storageService;
    private StorageAccessLevel defaultAccessLevel;
    private int defaultUrlExpiration;

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
        this((context, region, bucket) ->
                new AWSS3StorageService(
                    context,
                    region,
                    bucket,
                    authCredentialsProvider,
                    AWS_S3_STORAGE_PLUGIN_KEY
                ),
            authCredentialsProvider,
            new AWSS3StoragePluginConfiguration.Builder().build());
    }

    @VisibleForTesting
    AWSS3StoragePlugin(AuthCredentialsProvider authCredentialsProvider,
                       AWSS3StoragePluginConfiguration awss3StoragePluginConfiguration) {
        this((context, region, bucket) ->
                new AWSS3StorageService(
                    context,
                    region,
                    bucket,
                    authCredentialsProvider,
                    AWS_S3_STORAGE_PLUGIN_KEY
                ),
            authCredentialsProvider,
            awss3StoragePluginConfiguration);
    }

    @VisibleForTesting
    AWSS3StoragePlugin(
        StorageService.Factory storageServiceFactory,
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

    @Override
    @SuppressWarnings("MagicNumber") // TODO: Remove once default values are moved to configuration
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

        try {
            this.storageService = (AWSS3StorageService) storageServiceFactory.create(context, region, bucket);
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
        this.defaultUrlExpiration = (int) TimeUnit.DAYS.toSeconds(7);
    }

    @NonNull
    @Override
    public S3Client getEscapeHatch() {
        return storageService.getClient();
    }

    @NonNull
    @Override
    public String getVersion() {
        return BuildConfig.VERSION_NAME;
    }

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
        @NonNull String key,
        @NonNull StorageGetUrlOptions options,
        @NonNull Consumer<StorageGetUrlResult> onSuccess,
        @NonNull Consumer<StorageException> onError) {
        boolean useAccelerateEndpoint = options instanceof AWSS3StorageGetPresignedUrlOptions &&
            ((AWSS3StorageGetPresignedUrlOptions) options).useAccelerateEndpoint();
        AWSS3StorageGetPresignedUrlRequest request = new AWSS3StorageGetPresignedUrlRequest(
            key,
            options.getAccessLevel() != null
                ? options.getAccessLevel()
                : defaultAccessLevel,
            options.getTargetIdentityId(),
            options.getExpires() != 0
                ? options.getExpires()
                : defaultUrlExpiration,
            useAccelerateEndpoint
        );

        AWSS3StorageGetPresignedUrlOperation operation =
            new AWSS3StorageGetPresignedUrlOperation(
                storageService,
                executorService,
                authCredentialsProvider,
                request,
                awsS3StoragePluginConfiguration,
                onSuccess,
                onError);
        operation.start();

        return operation;
    }

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

        AWSS3StorageDownloadFileOperation operation = new AWSS3StorageDownloadFileOperation(
            storageService,
            executorService,
            authCredentialsProvider,
            request,
            awsS3StoragePluginConfiguration,
            onProgress,
            onSuccess,
            onError
        );
        operation.start();

        return operation;
    }

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

        AWSS3StorageUploadFileOperation operation = new AWSS3StorageUploadFileOperation(
            storageService,
            executorService,
            authCredentialsProvider,
            request,
            awsS3StoragePluginConfiguration,
            onProgress,
            onSuccess,
            onError
        );
        operation.start();

        return operation;
    }

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
        @NonNull String key,
        @NonNull InputStream local,
        @NonNull StorageUploadInputStreamOptions options,
        @NonNull Consumer<StorageUploadInputStreamResult> onSuccess,
        @NonNull Consumer<StorageException> onError
    ) {
        return uploadInputStream(key, local, options, NoOpConsumer.create(), onSuccess, onError);
    }

    @NonNull
    @Override
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

        AWSS3StorageUploadInputStreamOperation operation = new AWSS3StorageUploadInputStreamOperation(
            storageService,
            executorService,
            authCredentialsProvider,
            awsS3StoragePluginConfiguration,
            request,
            onProgress,
            onSuccess,
            onError
        );
        operation.start();

        return operation;
    }

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

        AWSS3StorageRemoveOperation operation =
            new AWSS3StorageRemoveOperation(
                storageService,
                executorService,
                authCredentialsProvider,
                request,
                awsS3StoragePluginConfiguration,
                onSuccess,
                onError);

        operation.start();

        return operation;
    }

    @Override
    public void getTransfer(
        @NonNull String transferId,
        @NonNull Consumer<StorageTransferOperation<?, ? extends StorageTransferResult>> onReceived,
        @NonNull Consumer<StorageException> onError) {
        executorService.submit(() -> {
            try {
                TransferRecord transferRecord = storageService.getTransfer(transferId);
                if (transferRecord != null) {
                    TransferObserver transferObserver =
                        new TransferObserver(
                            transferRecord.getId(),
                            storageService.getTransferManager().getTransferStatusUpdater(),
                            transferRecord.getBucketName(),
                            transferRecord.getKey(),
                            transferRecord.getFile(),
                            null,
                            transferRecord.getState() != null ? transferRecord.getState() : TransferState.UNKNOWN);
                    TransferType transferType = transferRecord.getType();
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

    @NonNull
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
    public StorageListOperation<?> list(
        @NonNull String path,
        @NonNull StorageListOptions options,
        @NonNull Consumer<StorageListResult> onSuccess,
        @NonNull Consumer<StorageException> onError
    ) {
        AWSS3StorageListRequest request = new AWSS3StorageListRequest(
            path,
            options.getAccessLevel() != null
                ? options.getAccessLevel()
                : defaultAccessLevel,
            options.getTargetIdentityId()
        );

        AWSS3StorageListOperation operation =
            new AWSS3StorageListOperation(
                storageService,
                executorService,
                authCredentialsProvider,
                request,
                awsS3StoragePluginConfiguration,
                onSuccess,
                onError);

        operation.start();

        return operation;
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
}
