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

package com.amplifyframework.storage.s3.Service;

import android.content.Context;
import android.content.Intent;

import com.amazonaws.mobile.client.AWSMobileClient;
import com.amazonaws.mobileconnectors.s3.transferutility.TransferObserver;
import com.amazonaws.mobileconnectors.s3.transferutility.TransferService;
import com.amazonaws.mobileconnectors.s3.transferutility.TransferUtility;
import com.amazonaws.regions.Region;
import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.services.s3.S3ClientOptions;

import java.io.File;

/**
 * A representation of an S3 backend service endpoint.
 */
public final class AWSS3StorageService {

    private final Region region;
    private final Context context;
    private final String bucket;
    private final TransferUtility transferUtility;
    private final AmazonS3Client client;

    private boolean transferUtilityServiceStarted = false;

    /**
     * Constructs a new AWSS3StorageService.
     * @param region Region in which the S3 endpoint resides
     * @param context An Android Context
     * @param bucket An S3 bucket name
     * @param transferAcceleration Whether or not transfer acceleration
     *                             should be enabled
     */
    public AWSS3StorageService(Region region, Context context, String bucket, boolean transferAcceleration) {
        this.context = context;
        this.region = region;
        this.bucket = bucket;
        this.client = new AmazonS3Client(AWSMobileClient.getInstance(), this.region);

        if (transferAcceleration) {
            client.setS3ClientOptions(S3ClientOptions.builder().setAccelerateModeEnabled(true).build());
        }

        this.transferUtility = TransferUtility.builder()
                                .context(this.context)
                                .s3Client(client)
                                .build();
    }

    /**
     * Begin downloading a file.
     * @param serviceKey S3 service key
     * @param file Target file
     * @return A transfer observer
     */
    public TransferObserver downloadToFile(String serviceKey, File file) {
        startServiceIfNotAlreadyStarted();
        return transferUtility.download(bucket, serviceKey, file);
    }

    /**
     * Begin uploading a file.
     * @param serviceKey S3 service key
     * @param file Target file
     * @return A transfer observer
     */
    public TransferObserver uploadFile(String serviceKey, File file) {
        startServiceIfNotAlreadyStarted();
        return transferUtility.upload(bucket, serviceKey, file);
    }

    /**
     * Pause a file transfer operation.
     * @param transfer an in-progress transfer
     */
    public void pauseTransfer(TransferObserver transfer) {
        startServiceIfNotAlreadyStarted();
        transferUtility.pause(transfer.getId());
    }

    /**
     * Resume a file transfer.
     * @param transfer A transfer to be resumed
     */
    public void resumeTransfer(TransferObserver transfer) {
        startServiceIfNotAlreadyStarted();
        transferUtility.resume(transfer.getId());
    }

    /**
     * Cancel a file transfer.
     * @param transfer A file transfer to cancel
     */
    public void cancelTransfer(TransferObserver transfer) {
        startServiceIfNotAlreadyStarted();
        transferUtility.cancel(transfer.getId());
    }

    private void startServiceIfNotAlreadyStarted() {
        if (!transferUtilityServiceStarted) {
            // TODO: When a reset method is defined, stop service.
            context.startService(new Intent(context, TransferService.class));
            transferUtilityServiceStarted = true;
        }
    }

    /**
     * Gets a handle the S3 client underlying this service.
     * @return S3 client instance
     */
    public AmazonS3Client getClient() {
        return client;
    }
}

