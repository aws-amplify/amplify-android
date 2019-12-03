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

import com.amplifyframework.core.ResultListener;
import com.amplifyframework.storage.StorageAccessLevel;
import com.amplifyframework.storage.StorageException;
import com.amplifyframework.storage.StoragePlugin;
import com.amplifyframework.storage.operation.StorageDownloadFileOperation;
import com.amplifyframework.storage.operation.StorageListOperation;
import com.amplifyframework.storage.operation.StorageRemoveOperation;
import com.amplifyframework.storage.operation.StorageUploadFileOperation;
import com.amplifyframework.storage.options.StorageDownloadFileOptions;
import com.amplifyframework.storage.options.StorageListOptions;
import com.amplifyframework.storage.options.StorageRemoveOptions;
import com.amplifyframework.storage.options.StorageUploadFileOptions;
import com.amplifyframework.storage.result.StorageDownloadFileResult;
import com.amplifyframework.storage.result.StorageListResult;
import com.amplifyframework.storage.result.StorageRemoveResult;
import com.amplifyframework.storage.result.StorageUploadFileResult;
import com.amplifyframework.storage.s3.operation.AWSS3StorageDownloadFileOperation;
import com.amplifyframework.storage.s3.operation.AWSS3StorageListOperation;
import com.amplifyframework.storage.s3.operation.AWSS3StorageRemoveOperation;
import com.amplifyframework.storage.s3.operation.AWSS3StorageUploadFileOperation;
import com.amplifyframework.storage.s3.request.AWSS3StorageDownloadFileRequest;
import com.amplifyframework.storage.s3.request.AWSS3StorageListRequest;
import com.amplifyframework.storage.s3.request.AWSS3StorageRemoveRequest;
import com.amplifyframework.storage.s3.request.AWSS3StorageUploadFileRequest;
import com.amplifyframework.storage.s3.service.AWSS3StorageService;

import com.amazonaws.regions.Region;
import com.amazonaws.services.s3.AmazonS3Client;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * A plugin for the storage category which uses S3 as a storage
 * repository.
 */
public final class AWSS3StoragePlugin extends StoragePlugin<AmazonS3Client> {
    private static final String AWS_S3_STORAGE_PLUGIN_KEY = "AWSS3StoragePlugin";
    private AWSS3StorageService storageService;
    private final ExecutorService executorService;
    private StorageAccessLevel defaultAccessLevel;

    /**
     * Constructs the AWS S3 Storage Plugin initializing the executor service.
     */
    public AWSS3StoragePlugin() {
        super();
        this.executorService = Executors.newCachedThreadPool();
    }

    @Override
    public String getPluginKey() {
        return AWS_S3_STORAGE_PLUGIN_KEY;
    }

    @Override
    public void configure(@NonNull JSONObject pluginConfiguration, Context context) throws StorageException {
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
                    "Missing or malformed value for Region in " + AWS_S3_STORAGE_PLUGIN_KEY + "configuration.",
                    error,
                    "Check the attached error to see where the parsing issue took place."
            );
        }

        try {
            this.storageService = new AWSS3StorageService(
                    region,
                    context,
                    bucket,
                    /* transferAcceleration = */false // This will come from the config in the future
            );
        } catch (Exception exception) {
            throw new StorageException(
                    "Failed to create storage service.",
                    exception,
                    "Have you initialized AWSMobileClient? See included exception for more details."
            );
        }

        this.defaultAccessLevel = StorageAccessLevel.PUBLIC; // This will be passed in the config in the future
    }

    @Override
    public AmazonS3Client getEscapeHatch() {
        return storageService.getClient();
    }

    @Override
    public StorageDownloadFileOperation<?> downloadFile(
            @NonNull String key,
            @NonNull String local,
            @NonNull ResultListener<StorageDownloadFileResult> resultListener
    ) {
        return downloadFile(key, local, StorageDownloadFileOptions.defaultInstance(), resultListener);
    }

    @Override
    public StorageDownloadFileOperation<?> downloadFile(
            @NonNull String key,
            @NonNull String local,
            @NonNull StorageDownloadFileOptions options,
            @NonNull ResultListener<StorageDownloadFileResult> resultListener
    ) {
        AWSS3StorageDownloadFileRequest request = new AWSS3StorageDownloadFileRequest(
                key,
                local,
                options.getAccessLevel() != null ? options.getAccessLevel() : defaultAccessLevel,
                options.getTargetIdentityId()
        );

        AWSS3StorageDownloadFileOperation operation =
                new AWSS3StorageDownloadFileOperation(storageService, request, resultListener);
        operation.start();

        return operation;
    }

    @Override
    public StorageUploadFileOperation<?> uploadFile(
            @NonNull String key,
            @NonNull String local,
            @NonNull ResultListener<StorageUploadFileResult> resultListener
    ) {
        return uploadFile(key, local, StorageUploadFileOptions.defaultInstance(), resultListener);
    }

    @Override
    public StorageUploadFileOperation<?> uploadFile(
            @NonNull String key,
            @NonNull String local,
            @NonNull StorageUploadFileOptions options,
            @NonNull ResultListener<StorageUploadFileResult> resultListener
    ) {
        AWSS3StorageUploadFileRequest request = new AWSS3StorageUploadFileRequest(
                key,
                local,
                options.getAccessLevel() != null ? options.getAccessLevel() : defaultAccessLevel,
                options.getTargetIdentityId(),
                options.getContentType(),
                options.getMetadata()
        );

        AWSS3StorageUploadFileOperation operation =
                new AWSS3StorageUploadFileOperation(storageService, request, resultListener);

        operation.start();

        return operation;
    }

    @Override
    public StorageRemoveOperation<?> remove(
            @NonNull String key,
            @NonNull ResultListener<StorageRemoveResult> resultListener
    ) {
        return remove(key, StorageRemoveOptions.defaultInstance(), resultListener);
    }

    @Override
    public StorageRemoveOperation<?> remove(
            @NonNull String key,
            @NonNull StorageRemoveOptions options,
            @NonNull ResultListener<StorageRemoveResult> resultListener
    ) {
        AWSS3StorageRemoveRequest request = new AWSS3StorageRemoveRequest(
                key,
                options.getAccessLevel() != null ? options.getAccessLevel() : defaultAccessLevel,
                options.getTargetIdentityId()
        );

        AWSS3StorageRemoveOperation operation =
                new AWSS3StorageRemoveOperation(storageService, executorService, request, resultListener);

        operation.start();

        return operation;
    }

    @Override
    public StorageListOperation<?> list(
            @NonNull String path,
            @NonNull ResultListener<StorageListResult> resultListener
    ) {
        return list(path, StorageListOptions.defaultInstance(), resultListener);
    }

    @Override
    public StorageListOperation<?> list(
            @NonNull String path,
            @NonNull StorageListOptions options,
            @NonNull ResultListener<StorageListResult> resultListener
    ) {
        AWSS3StorageListRequest request = new AWSS3StorageListRequest(
                path,
                options.getAccessLevel() != null ? options.getAccessLevel() : defaultAccessLevel,
                options.getTargetIdentityId()
        );

        AWSS3StorageListOperation operation =
                new AWSS3StorageListOperation(storageService, executorService, request, resultListener);

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
        BUCKET("Bucket"),

        /**
         * The AWS region this plugin will work with.
         */
        REGION("Region");

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
        public String getConfigurationKey() {
            return configurationKey;
        }
    }
}
