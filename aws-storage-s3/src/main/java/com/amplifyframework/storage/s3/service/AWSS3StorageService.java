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

package com.amplifyframework.storage.s3.service;

import android.content.Context;
import androidx.annotation.NonNull;

import com.amplifyframework.storage.StorageException;
import com.amplifyframework.storage.StorageItem;
import com.amplifyframework.storage.s3.CognitoAuthProvider;
import com.amplifyframework.storage.s3.utils.S3Keys;
import com.amplifyframework.util.UserAgent;

import com.amazonaws.ClientConfiguration;
import com.amazonaws.mobileconnectors.s3.transferutility.TransferObserver;
import com.amazonaws.mobileconnectors.s3.transferutility.TransferUtility;
import com.amazonaws.mobileconnectors.s3.transferutility.UploadOptions;
import com.amazonaws.regions.Region;
import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.services.s3.S3ClientOptions;
import com.amazonaws.services.s3.model.ListObjectsV2Request;
import com.amazonaws.services.s3.model.ListObjectsV2Result;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.amazonaws.services.s3.model.S3ObjectSummary;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * A representation of an S3 backend service endpoint.
 */
public final class AWSS3StorageService implements StorageService {

    private final Context context;
    private final String bucket;
    private final TransferUtility transferUtility;
    private final AmazonS3Client client;
    private final CognitoAuthProvider cognitoAuthProvider;

    /**
     * Constructs a new AWSS3StorageService.
     * @param region Region in which the S3 endpoint resides
     * @param context An Android Context
     * @param bucket An S3 bucket name
     * @param cognitoAuthProvider Provides AWS specific Auth information
     * @param transferAcceleration Whether or not transfer acceleration
     *                             should be enabled
     * @throws IllegalStateException Storage service requires the correct Auth plugin to have been added to Amplify
     */
    public AWSS3StorageService(
            @NonNull Context context,
            @NonNull Region region,
            @NonNull String bucket,
            @NonNull CognitoAuthProvider cognitoAuthProvider,
            boolean transferAcceleration
    ) {
        try {
            this.context = context;
            this.bucket = bucket;
            this.cognitoAuthProvider = cognitoAuthProvider;
            this.client = createS3Client(region);

            if (transferAcceleration) {
                client.setS3ClientOptions(S3ClientOptions.builder()
                        .setAccelerateModeEnabled(true)
                        .build()
                );
            }

            this.transferUtility = TransferUtility.builder()
                    .context(this.context)
                    .s3Client(client)
                    .build();
        } catch (StorageException exception) {
            throw new IllegalStateException(
                "AWSS3StoragePlugin depends on AWSCognitoAuthPlugin but it is currently missing.");
        }
    }

    private AmazonS3Client createS3Client(@NonNull Region region) throws StorageException {
        ClientConfiguration configuration = new ClientConfiguration();
        configuration.setUserAgent(UserAgent.string());
        return new AmazonS3Client(cognitoAuthProvider.getCredentialsProvider(), region, configuration);
    }

    /**
     * Generate pre-signed URL for an object.
     * @param serviceKey S3 service key
     * @param expires Number of seconds before URL expires
     * @return A pre-signed URL
     */
    @NonNull
    public URL getPresignedUrl(@NonNull String serviceKey, int expires) {
        Date expiration = new Date(System.currentTimeMillis() + TimeUnit.SECONDS.toMillis(expires));
        return client.generatePresignedUrl(bucket, serviceKey, expiration);
    }

    /**
     * Begin downloading a file.
     * @param serviceKey S3 service key
     * @param file Target file
     * @return A transfer observer
     */
    @NonNull
    public TransferObserver downloadToFile(
            @NonNull String serviceKey,
            @NonNull File file
    ) {
        startTransferService();
        return transferUtility.download(bucket, serviceKey, file);
    }

    /**
     * Begin uploading a file.
     * @param serviceKey S3 service key
     * @param file Target file
     * @param metadata Object metadata to associate with upload
     * @return A transfer observer
     */
    @NonNull
    public TransferObserver uploadFile(
            @NonNull String serviceKey,
            @NonNull File file,
            @NonNull ObjectMetadata metadata
    ) {
        startTransferService();
        return transferUtility.upload(bucket, serviceKey, file, metadata);
    }

    /**
     * Begin uploading an inputStream.
     * @param serviceKey S3 service key
     * @param inputStream Target InputStream
     * @param metadata Object metadata to associate with upload
     * @return A transfer observer
     * @throws IOException An IOException thrown during the process writing an InputStream into a file
     */
    @NonNull
    public TransferObserver uploadInputStream(
            @NonNull String serviceKey,
            @NonNull InputStream inputStream,
            @NonNull ObjectMetadata metadata
    ) throws IOException {
        startTransferService();
        UploadOptions uploadOptions = UploadOptions.builder()
                                                    .bucket(bucket)
                                                    .objectMetadata(metadata)
                                                    .build();
        return transferUtility.upload(serviceKey, inputStream, uploadOptions);
    }

    /**
     * List items inside an S3 path.
     * @param path The path to list items from
     * @param prefix path appended to S3 keys
     * @return A list of parsed items
     */
    @NonNull
    public List<StorageItem> listFiles(@NonNull String path, @NonNull String prefix) {
        ArrayList<StorageItem> itemList = new ArrayList<>();
        ListObjectsV2Request request =
                new ListObjectsV2Request().withBucketName(this.bucket).withPrefix(path);
        ListObjectsV2Result result;

        do {
            result = client.listObjectsV2(request);

            for (S3ObjectSummary objectSummary : result.getObjectSummaries()) {
                // Remove the access level prefix from service key
                String serviceKey = objectSummary.getKey();
                String amplifyKey = S3Keys.extractAmplifyKey(serviceKey, prefix);

                itemList.add(new StorageItem(
                        amplifyKey,
                        objectSummary.getSize(),
                        objectSummary.getLastModified(),
                        objectSummary.getETag(),
                        null
                ));
            }
            // If there are more than maxKeys keys in the bucket, get a continuation token
            // and fetch the next batch of objects.
            String token = result.getNextContinuationToken();
            request.setContinuationToken(token);
        } while (result.isTruncated());

        return itemList;
    }

    /**
     * Synchronous operation to delete a file in s3.
     * @param serviceKey Fully specified path to file to delete (including public/private/protected folder)
     */
    public void deleteObject(@NonNull String serviceKey) {
        this.client.deleteObject(this.bucket, serviceKey);
    }

    /**
     * Pause a file transfer operation.
     * @param transfer an in-progress transfer
     */
    public void pauseTransfer(@NonNull TransferObserver transfer) {
        transferUtility.pause(transfer.getId());
    }

    /**
     * Resume a file transfer.
     * @param transfer A transfer to be resumed
     */
    public void resumeTransfer(@NonNull TransferObserver transfer) {
        startTransferService();
        transferUtility.resume(transfer.getId());
    }

    /**
     * Cancel a file transfer.
     * @param transfer A file transfer to cancel
     */
    public void cancelTransfer(@NonNull TransferObserver transfer) {
        transferUtility.cancel(transfer.getId());
    }

    private void startTransferService() {
        AmplifyTransferService.Companion.bind(context);
    }

    /**
     * Gets a handle the S3 client underlying this service.
     * @return S3 client instance
     */
    @NonNull
    public AmazonS3Client getClient() {
        return client;
    }
}
