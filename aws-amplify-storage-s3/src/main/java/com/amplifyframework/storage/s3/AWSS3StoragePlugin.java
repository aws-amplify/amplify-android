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
import com.amplifyframework.storage.s3.Operation.AWSS3StorageDownloadFileOperation;
import com.amplifyframework.storage.s3.Operation.AWSS3StorageRemoveOperation;
import com.amplifyframework.storage.s3.Operation.AWSS3StorageUploadFileOperation;
import com.amplifyframework.storage.s3.Request.AWSS3StorageDownloadFileRequest;
import com.amplifyframework.storage.s3.Request.AWSS3StorageRemoveRequest;
import com.amplifyframework.storage.s3.Request.AWSS3StorageUploadFileRequest;
import com.amplifyframework.storage.s3.Service.AWSS3StorageService;

import com.amazonaws.mobileconnectors.s3.transferutility.TransferUtility;
import com.amazonaws.regions.Region;
import com.amazonaws.regions.Regions;

/**
 * A plugin for the storage category which uses S3 as a storage
 * repository.
 */
public final class AWSS3StoragePlugin extends StoragePlugin<TransferUtility> {
    private static final String AWS_S3_STORAGE_PLUGIN_KEY = "AWSS3StoragePlugin";
    private static final Regions DEFAULT_REGION = Regions.US_EAST_1;
    private AWSS3StorageService storageService;
    private StorageAccessLevel defaultAccessLevel;

    @Override
    public String getPluginKey() {
        return AWS_S3_STORAGE_PLUGIN_KEY;
    }

    @Override
    public void configure(@NonNull Object pluginConfiguration, Context context) throws PluginException {
        AWSS3StoragePluginConfiguration config;

        try {
            config = (AWSS3StoragePluginConfiguration) pluginConfiguration;
        } catch (Exception exception) {
            throw new PluginException(
                    "AWSS3StoragePlugin must be given an AWSS3StoragePluginConfiguration" +
                    "type object for configuration",
                    exception
            );
        }

        Region region = Region.getRegion(DEFAULT_REGION);

        if (config.getRegion() != null && !config.getRegion().isEmpty()) {
            region = Region.getRegion(config.getRegion());

            if (region == null) {
                throw new PluginException("Invalid region provided");
            }
        }

        try {
            this.storageService = new AWSS3StorageService(
                    region,
                    context,
                    config.getBucket(),
                    config.isTransferAcceleration()
            );
        } catch (Exception exception) {
            throw new PluginException(
                    "Failed to create storage service. Have you initialized AWSMobileClient?" +
                    "See included exception for more details.",
                    exception
            );
        }

        this.defaultAccessLevel = config.getDefaultAccessLevel() != null ?
                config.getDefaultAccessLevel() :
                StorageAccessLevel.PUBLIC;
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
    public StorageListOperation list() throws StorageException {
        return null;
    }

    @Override
    public StorageListOperation list(StorageListOptions options) throws StorageException {
        return null;
    }

    @Override
    public StorageListOperation list(
            StorageListOptions options,
            Listener<StorageListResult> callback
    ) throws StorageException {
        return null;
    }
}
