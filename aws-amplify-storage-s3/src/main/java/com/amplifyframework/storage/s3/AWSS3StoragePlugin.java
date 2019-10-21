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

import com.amplifyframework.core.async.Listener;
import com.amplifyframework.core.plugin.PluginException;
import com.amplifyframework.storage.StorageAccessLevel;
import com.amplifyframework.storage.StoragePlugin;
import com.amplifyframework.storage.exception.StorageException;
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

import com.amazonaws.mobileconnectors.s3.transferutility.TransferUtility;
import com.amazonaws.regions.Region;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * A plugin for the storage category which uses S3 as a storage
 * repository.
 */
public final class AWSS3StoragePlugin extends StoragePlugin<TransferUtility> {
    private static final String AWS_S3_STORAGE_PLUGIN_KEY = "AWSS3StoragePlugin";
    private AWSS3StorageService storageService;
    private StorageAccessLevel defaultAccessLevel;

    @Override
    public String getPluginKey() {
        return AWS_S3_STORAGE_PLUGIN_KEY;
    }

    @Override
    public void configure(@NonNull JSONObject pluginConfiguration, Context context) throws PluginException {
        String regionStr;
        String bucket;

        try {
            regionStr = pluginConfiguration.getString(JsonKeys.REGION.getConfigurationKey());
        } catch (JSONException error) {
            throw new PluginException.PluginConfigurationException(
                    "Missing or malformed value for Region in " + AWS_S3_STORAGE_PLUGIN_KEY + "configuration.",
                    error
            );
        }

        Region region = Region.getRegion(regionStr);

        if (region == null) {
            throw new PluginException.PluginConfigurationException("Invalid region provided");
        }

        try {
            bucket = pluginConfiguration.getString(JsonKeys.BUCKET.getConfigurationKey());
        } catch (JSONException error) {
            throw new PluginException.PluginConfigurationException(
                    "Missing or malformed value for Bucket in " + AWS_S3_STORAGE_PLUGIN_KEY + "configuration.",
                    error
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
            throw new PluginException(
                    "Failed to create storage service. Have you initialized AWSMobileClient?" +
                    "See included exception for more details.",
                    exception
            );
        }

        this.defaultAccessLevel = StorageAccessLevel.PUBLIC; // This will be passed in the config in the future
    }

    @Override
    public TransferUtility getEscapeHatch() {
        return null;
    }

    @Override
    public StorageDownloadFileOperation downloadFile(
            @NonNull String key,
            @NonNull String local
    ) throws StorageException {
        return downloadFile(key, local, StorageDownloadFileOptions.defaultInstance());
    }

    @Override
    public StorageDownloadFileOperation downloadFile(
            @NonNull String key,
            @NonNull String local,
            StorageDownloadFileOptions options
    ) throws StorageException {
        return downloadFile(key, local, options, null);
    }

    @Override
    public StorageDownloadFileOperation downloadFile(
            @NonNull String key,
            @NonNull String local,
            Listener<StorageDownloadFileResult> callback
    ) throws StorageException {
        return downloadFile(key, local, StorageDownloadFileOptions.defaultInstance(), callback);
    }

    @Override
    public StorageDownloadFileOperation downloadFile(
            @NonNull String key,
            @NonNull String local,
            StorageDownloadFileOptions options,
            Listener<StorageDownloadFileResult> callback
    ) throws StorageException {
        AWSS3StorageDownloadFileRequest request = new AWSS3StorageDownloadFileRequest(
                key,
                local,
                options.getAccessLevel() != null ? options.getAccessLevel() : defaultAccessLevel,
                options.getTargetIdentityId()
        );

        AWSS3StorageDownloadFileOperation operation =
                new AWSS3StorageDownloadFileOperation(storageService, request, callback);
        operation.start();

        return operation;
    }

    @Override
    public StorageUploadFileOperation uploadFile(
            @NonNull String key,
            @NonNull String local
    ) throws StorageException {
        return uploadFile(key, local, StorageUploadFileOptions.defaultInstance());
    }

    @Override
    public StorageUploadFileOperation uploadFile(
            @NonNull String key,
            @NonNull String local,
            StorageUploadFileOptions options
    ) throws StorageException {
        return uploadFile(key, local, options, null);
    }

    @Override
    public StorageUploadFileOperation uploadFile(
            @NonNull String key,
            @NonNull String local,
            Listener<StorageUploadFileResult> callback
    ) throws StorageException {
        return uploadFile(key, local, StorageUploadFileOptions.defaultInstance(), callback);
    }

    @Override
    public StorageUploadFileOperation uploadFile(
            @NonNull String key,
            @NonNull String local,
            StorageUploadFileOptions options,
            Listener<StorageUploadFileResult> callback
    ) throws StorageException {
        AWSS3StorageUploadFileRequest request = new AWSS3StorageUploadFileRequest(
                key,
                local,
                options.getAccessLevel() != null ? options.getAccessLevel() : defaultAccessLevel,
                options.getTargetIdentityId(),
                options.getContentType(),
                options.getMetadata()
        );

        AWSS3StorageUploadFileOperation operation =
                new AWSS3StorageUploadFileOperation(storageService, request, callback);

        operation.start();

        return operation;
    }

    @Override
    public StorageRemoveOperation remove(
            @NonNull String key
    ) throws StorageException {
        return remove(key, StorageRemoveOptions.defaultInstance());
    }

    @Override
    public StorageRemoveOperation remove(
            @NonNull String key,
            StorageRemoveOptions options
    ) throws StorageException {
        return remove(key, options, null);
    }

    @Override
    public StorageRemoveOperation remove(
            @NonNull String key,
            Listener<StorageRemoveResult> callback
    ) throws StorageException {
        return remove(key, StorageRemoveOptions.defaultInstance(), callback);
    }

    @Override
    public StorageRemoveOperation remove(
            @NonNull String key,
            StorageRemoveOptions options,
            Listener<StorageRemoveResult> callback
    ) throws StorageException {
        AWSS3StorageRemoveRequest request = new AWSS3StorageRemoveRequest(
                key,
                options.getAccessLevel() != null ? options.getAccessLevel() : defaultAccessLevel,
                options.getTargetIdentityId()
        );

        AWSS3StorageRemoveOperation operation =
                new AWSS3StorageRemoveOperation(storageService, request, callback);

        operation.start();

        return operation;
    }

    @Override
    public StorageListOperation list(@NonNull String path) throws StorageException {
        return list(path, StorageListOptions.defaultInstance());
    }

    @Override
    public StorageListOperation list(@NonNull String path, StorageListOptions options) throws StorageException {
        return list(path, options, null);
    }

    @Override
    public StorageListOperation list(@NonNull String path, Listener<StorageListResult> callback)
            throws StorageException {
        return list(path, StorageListOptions.defaultInstance(), callback);
    }

    @Override
    public StorageListOperation list(
            @NonNull String path,
            StorageListOptions options,
            Listener<StorageListResult> callback
    ) throws StorageException {
        AWSS3StorageListRequest request = new AWSS3StorageListRequest(
                path,
                options.getAccessLevel() != null ? options.getAccessLevel() : defaultAccessLevel,
                options.getTargetIdentityId()
        );

        AWSS3StorageListOperation operation =
                new AWSS3StorageListOperation(storageService, request, callback);

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
