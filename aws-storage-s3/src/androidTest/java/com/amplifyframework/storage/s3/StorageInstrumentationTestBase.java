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
import com.amplifyframework.testutils.SynchronousStorage;

import com.amazonaws.mobile.client.AWSMobileClient;
import com.amazonaws.services.s3.AmazonS3Client;
import org.junit.BeforeClass;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertTrue;

/**
 * Abstract test base for Storage instrumented tests. This test contains
 * basic static methods that involve direct interactions with low-level
 * {@link AmazonS3Client} and {@link AWSMobileClient}.
 */
public abstract class StorageInstrumentationTestBase {

    static final long EXTENDED_TIMEOUT_IN_SECONDS = 20; // 5 seconds is too short for file transfers

    private static AmazonS3Client s3;
    private static String bucketName;
    private static Map<String, String> credentials;

    private static AWSMobileClient mClient;
    private static SynchronousStorage synchronousStorage;

    /**
     * Setup the Android application context.
     *
     * @throws AmplifyException from Amplify configuration
     */
    @BeforeClass
    public static void setUpOnce() throws AmplifyException {
        TestConfiguration config = TestConfiguration.configureIfNotConfigured();

        s3 = config.plugin().getEscapeHatch();
        bucketName = config.getBucketName();
        credentials = config.getUserCredentials();

        mClient = AWSMobileClient.getInstance();
        synchronousStorage = SynchronousStorage.singleton();
    }

    static SynchronousStorage synchronousStorage() {
        return synchronousStorage;
    }

    static synchronized String getIdentityId() {
        return mClient.getIdentityId();
    }

    static synchronized String getS3Key(StorageAccessLevel accessLevel, String key) {
        return S3RequestUtils.getServiceKey(accessLevel,
                getIdentityId(),
                key);
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

    static void signInAs(@NonNull String username) {
        signOut();
        try {
            String password = credentials.get(username);
            mClient.signIn(username, password, null);
        } catch (Exception exception) {
            throw new RuntimeException("Failed to sign in as " + username, exception);
        }
    }

    static void signOut() {
        mClient.signOut();
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
        synchronousStorage().uploadFile(file.getName(),
                file.getAbsolutePath(),
                options);

        // Confirm successful upload
        String s3key = S3RequestUtils.getServiceKey(accessLevel, identityId, file.getName());
        assertS3ObjectExists(s3key);
    }
}
