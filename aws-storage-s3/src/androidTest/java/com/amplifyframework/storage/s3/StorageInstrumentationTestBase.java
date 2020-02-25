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

package com.amplifyframework.storage.s3;

import androidx.annotation.NonNull;

import com.amplifyframework.AmplifyException;
import com.amplifyframework.storage.StorageAccessLevel;
import com.amplifyframework.storage.StorageException;
import com.amplifyframework.storage.options.StorageUploadFileOptions;
import com.amplifyframework.storage.s3.utils.S3RequestUtils;
import com.amplifyframework.testutils.SynchronousAWSMobileClient;
import com.amplifyframework.testutils.SynchronousStorage;

import com.amazonaws.services.s3.AmazonS3Client;
import org.junit.BeforeClass;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.assertTrue;

/**
 * Abstract test base for Storage instrumented tests. This test contains
 * basic static methods that involve direct interactions with low-level
 * {@link AmazonS3Client} and AWS Mobile Client.
 */
public abstract class StorageInstrumentationTestBase {

    // Transferring large files sometimes takes more than 10 seconds unfortunately
    static final long EXTENDED_TIMEOUT_MS = TimeUnit.SECONDS.toMillis(20);

    private static AmazonS3Client s3;
    private static String bucketName;
    private static Map<String, String> credentials;

    private static SynchronousStorage synchronousStorage;

    /**
     * Setup the Android application context.
     *
     * @throws AmplifyException from Amplify configuration
     * @throws SynchronousAWSMobileClient.MobileClientException from failure to initialize
     *         AWS Mobile Client
     */
    @BeforeClass
    public static void setUpOnce() throws AmplifyException, SynchronousAWSMobileClient.MobileClientException {
        TestConfiguration config = TestConfiguration.configureIfNotConfigured();

        s3 = config.plugin().getEscapeHatch();
        bucketName = config.getBucketName();
        credentials = config.getUserCredentials();

        synchronousStorage = SynchronousStorage.singleton();
    }

    static SynchronousStorage synchronousStorage() {
        return synchronousStorage;
    }

    static synchronized String getS3Key(StorageAccessLevel accessLevel, String key)
            throws SynchronousAWSMobileClient.MobileClientException {
        return S3RequestUtils.getServiceKey(accessLevel,
                getIdentityId(),
                key);
    }

    static String getIdentityId() throws SynchronousAWSMobileClient.MobileClientException {
        return SynchronousAWSMobileClient.instance().getIdentityId();
    }

    static void assertS3ObjectExists(String key) {
        assertTrue(s3.doesBucketExist(bucketName));
        assertTrue(s3.doesObjectExist(bucketName, key));
    }

    static void cleanUpS3Object(String key) {
        if (s3.doesObjectExist(bucketName, key)) {
            s3.deleteObject(bucketName, key);
        }
    }

    static List<String> getUsers() {
        return new ArrayList<>(credentials.keySet());
    }

    static void signInAs(@NonNull String username) throws SynchronousAWSMobileClient.MobileClientException {
        SynchronousAWSMobileClient.instance().signOut();
        try {
            String password = Objects.requireNonNull(credentials.get(username));
            SynchronousAWSMobileClient.instance().signIn(username, password);
        } catch (Exception exception) {
            throw new RuntimeException("Failed to sign in as " + username, exception);
        }
    }

    static void signOut() throws SynchronousAWSMobileClient.MobileClientException {
        SynchronousAWSMobileClient.instance().signOut();
    }

    static void latchedUploadAndConfirm(
            File file,
            StorageAccessLevel accessLevel,
            String identityId
    ) throws StorageException {
        StorageUploadFileOptions options = StorageUploadFileOptions.builder()
                .accessLevel(accessLevel)
                .targetIdentityId(identityId)
                .build();
        synchronousStorage().uploadFile(
                file.getName(),
                file.getAbsolutePath(),
                options,
                EXTENDED_TIMEOUT_MS);

        // Confirm successful upload
        String s3key = S3RequestUtils.getServiceKey(accessLevel, identityId, file.getName());
        assertS3ObjectExists(s3key);
    }
}
