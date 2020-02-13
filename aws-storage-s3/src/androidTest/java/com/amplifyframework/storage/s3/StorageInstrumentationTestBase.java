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
import com.amplifyframework.core.Amplify;
import com.amplifyframework.storage.StorageAccessLevel;
import com.amplifyframework.storage.StorageException;
import com.amplifyframework.storage.options.StorageUploadFileOptions;
import com.amplifyframework.storage.result.StorageUploadFileResult;
import com.amplifyframework.storage.s3.utils.S3RequestUtils;
import com.amplifyframework.testutils.AmplifyTestBase;
import com.amplifyframework.testutils.Await;

import com.amazonaws.mobile.client.AWSMobileClient;
import com.amazonaws.services.s3.AmazonS3Client;
import org.json.JSONObject;
import org.junit.BeforeClass;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.assertTrue;

/**
 * Abstract test base for Storage instrumented tests. This test contains
 * basic static methods that involve direct interactions with low-level
 * {@link AmazonS3Client} and {@link AWSMobileClient}.
 */
public abstract class StorageInstrumentationTestBase extends AmplifyTestBase {

    static final long EXTENDED_TIMEOUT_IN_SECONDS = 20; // 5 seconds is too short for file transfers

    private static final String TEMP_DIR = System.getProperty("java.io.tmpdir");

    private static AmazonS3Client s3;
    private static String bucketName;
    private static JSONObject credentials;
    private static AWSMobileClient mClient;

    /**
     * Setup the Android application context.
     *
     * @throws AmplifyException from Amplify configuration
     */
    @BeforeClass
    public static void setUpOnce() throws AmplifyException {
        s3 = TestConfiguration.configureIfNotConfigured()
                .plugin()
                .getEscapeHatch();
        bucketName = TestConfiguration.configureIfNotConfigured()
                .getBucketName();
        credentials = getPackageConfigure("Storage");
        mClient = AWSMobileClient.getInstance();
    }

    static synchronized File createTempFile(String filename) throws IOException {
        File file = new File(TEMP_DIR, filename);
        if (file.createNewFile()) {
            file.deleteOnExit();
        }
        return file;
    }

    static synchronized File createTempFile(String filename, long contentLength) throws IOException {
        File file = createTempFile(filename);
        RandomAccessFile raf = new RandomAccessFile(file, "rw");
        raf.setLength(contentLength);
        return file;
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

    static void signInAs(@NonNull String username) {
        signOut();
        try {
            String password = credentials.getString("password");
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
        Await.<StorageUploadFileResult, StorageException>result(
            TimeUnit.SECONDS.toMillis(EXTENDED_TIMEOUT_IN_SECONDS),
            (onResult, onError) -> Amplify.Storage.uploadFile(
                    file.getName(),
                    file.getAbsolutePath(),
                    options,
                    onResult,
                    onError
            )
        );

        // Confirm that the uploaded file is in S3 bucket
        String s3Key = S3RequestUtils.getServiceKey(
                accessLevel,
                identityId,
                file.getName()
        );
        assertS3ObjectExists(s3Key);
    }
}
