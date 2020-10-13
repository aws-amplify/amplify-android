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

package com.amplifyframework.storage.s3;

import android.content.Context;
import androidx.annotation.NonNull;
import androidx.annotation.VisibleForTesting;

import com.amplifyframework.core.BuildConfig;
import com.amplifyframework.core.Consumer;
import com.amplifyframework.core.NoOpConsumer;
import com.amplifyframework.storage.StorageAccessLevel;
import com.amplifyframework.storage.StorageException;
import com.amplifyframework.storage.StoragePlugin;
import com.amplifyframework.storage.operation.StorageDownloadFileOperation;
import com.amplifyframework.storage.operation.StorageGetUrlOperation;
import com.amplifyframework.storage.operation.StorageListOperation;
import com.amplifyframework.storage.operation.StorageRemoveOperation;
import com.amplifyframework.storage.operation.StorageUploadFileOperation;
import com.amplifyframework.storage.options.StorageDownloadFileOptions;
import com.amplifyframework.storage.options.StorageGetUrlOptions;
import com.amplifyframework.storage.options.StorageListOptions;
import com.amplifyframework.storage.options.StorageRemoveOptions;
import com.amplifyframework.storage.options.StorageUploadFileOptions;
import com.amplifyframework.storage.result.StorageDownloadFileResult;
import com.amplifyframework.storage.result.StorageGetUrlResult;
import com.amplifyframework.storage.result.StorageListResult;
import com.amplifyframework.storage.result.StorageRemoveResult;
import com.amplifyframework.storage.result.StorageTransferProgress;
import com.amplifyframework.storage.result.StorageUploadFileResult;
import com.amplifyframework.storage.s3.operation.AWSS3StorageDownloadFileOperation;
import com.amplifyframework.storage.s3.operation.AWSS3StorageGetPresignedUrlOperation;
import com.amplifyframework.storage.s3.operation.AWSS3StorageListOperation;
import com.amplifyframework.storage.s3.operation.AWSS3StorageRemoveOperation;
import com.amplifyframework.storage.s3.operation.AWSS3StorageUploadFileOperation;
import com.amplifyframework.storage.s3.options.AWSS3StorageUploadFileOptions;
import com.amplifyframework.storage.s3.request.AWSS3StorageDownloadFileRequest;
import com.amplifyframework.storage.s3.request.AWSS3StorageGetPresignedUrlRequest;
import com.amplifyframework.storage.s3.request.AWSS3StorageListRequest;
import com.amplifyframework.storage.s3.request.AWSS3StorageRemoveRequest;
import com.amplifyframework.storage.s3.request.AWSS3StorageUploadFileRequest;
import com.amplifyframework.storage.s3.service.AWSS3StorageService;
import com.amplifyframework.storage.s3.service.StorageService;

import com.amazonaws.regions.Region;
import com.amazonaws.services.s3.AmazonS3Client;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * A plugin for the storage category which uses S3 as a storage
 * repository.
 */
public final class AWSS3StoragePlugin extends StoragePlugin<AmazonS3Client> {
    private static final String AWS_S3_STORAGE_PLUGIN_KEY = "awsS3StoragePlugin";

    private final StorageService.Factory storageServiceFactory;
    private final ExecutorService executorService;
    private final CognitoAuthProvider cognitoAuthProvider;
    private StorageService storageService;
    private StorageAccessLevel defaultAccessLevel;
    private int defaultUrlExpiration;

    /**
     * Constructs the AWS S3 Storage Plugin initializing the executor service.
     */
    @SuppressWarnings("unused") // This is a public API.
    public AWSS3StoragePlugin() {
        this(new AWSMobileClientAuthProvider());
    }

    @VisibleForTesting
    AWSS3StoragePlugin(CognitoAuthProvider cognitoAuthProvider) {
        this((context, region, bucket) ->
                new AWSS3StorageService(context, region, bucket, cognitoAuthProvider, false),
                cognitoAuthProvider);
    }

    @VisibleForTesting
    AWSS3StoragePlugin(
            StorageService.Factory storageServiceFactory,
            CognitoAuthProvider cognitoAuthProvider
    ) {
        super();
        this.storageServiceFactory = storageServiceFactory;
        this.executorService = Executors.newCachedThreadPool();
        this.cognitoAuthProvider = cognitoAuthProvider;
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

        Region region = Region.getRegion(regionStr);

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
            this.storageService = storageServiceFactory.create(context, region, bucket);
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
    public AmazonS3Client getEscapeHatch() {
        return ((AWSS3StorageService) storageService).getClient();
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
        AWSS3StorageGetPresignedUrlRequest request = new AWSS3StorageGetPresignedUrlRequest(
                key,
                options.getAccessLevel() != null
                        ? options.getAccessLevel()
                        : defaultAccessLevel,
                options.getTargetIdentityId(),
                options.getExpires() != 0
                        ? options.getExpires()
                        : defaultUrlExpiration
        );

        AWSS3StorageGetPresignedUrlOperation operation =
                new AWSS3StorageGetPresignedUrlOperation(
                        storageService,
                        executorService,
                        cognitoAuthProvider,
                        request,
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
        AWSS3StorageDownloadFileRequest request = new AWSS3StorageDownloadFileRequest(
                key,
                local,
                options.getAccessLevel() != null
                        ? options.getAccessLevel()
                        : defaultAccessLevel,
                options.getTargetIdentityId()
        );

        AWSS3StorageDownloadFileOperation operation = new AWSS3StorageDownloadFileOperation(
            storageService, cognitoAuthProvider, request, onProgress, onSuccess, onError
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
        AWSS3StorageUploadFileRequest request = new AWSS3StorageUploadFileRequest(
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
                options.getMetadata()
        );

        AWSS3StorageUploadFileOperation operation = new AWSS3StorageUploadFileOperation(
            storageService, cognitoAuthProvider, request, onProgress, onSuccess, onError
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
                        cognitoAuthProvider,
                        request,
                        onSuccess,
                        onError);

        operation.start();

        return operation;
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
                        cognitoAuthProvider,
                        request,
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
         * @param configurationKey The key this property is listed under in the config JSON.
         */
        JsonKeys(final String configurationKey) {
            this.configurationKey = configurationKey;
        }

        /**
         * Returns the key this property is listed under in the config JSON.
         * @return The key as a string
         */
        @NonNull
        public String getConfigurationKey() {
            return configurationKey;
        }
    }
}
