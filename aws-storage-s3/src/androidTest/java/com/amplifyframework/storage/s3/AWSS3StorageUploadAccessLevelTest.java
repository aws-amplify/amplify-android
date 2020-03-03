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

import com.amplifyframework.storage.StorageAccessLevel;
import com.amplifyframework.storage.StorageException;
import com.amplifyframework.storage.options.StorageUploadFileOptions;
import com.amplifyframework.testutils.random.RandomTempFile;
import com.amplifyframework.testutils.sync.SynchronousMobileClient;
import com.amplifyframework.testutils.sync.SynchronousStorage;

import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.File;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import static org.junit.Assert.assertTrue;

/**
 * Instrumentation test to confirm that Storage Upload behaves
 * correctly with regards to the provided storage access level.
 */
public final class AWSS3StorageUploadAccessLevelTest {

    private static final long UPLOAD_SIZE = 100L;

    private static SynchronousStorage storage;
    private static SynchronousMobileClient mobileClient;

    private static String userOne;
    private static String userTwo;
    private static Map<String, String> userIdentityIds;

    private String fileName;
    private String filePath;
    private StorageUploadFileOptions uploadOptions;

    /**
     * Obtain the user IDs prior to running the tests.
     * @throws Exception from failure to sign in with Cognito User Pools
     */
    @BeforeClass
    public static void setUpOnce() throws Exception {
        // Configure Amplify if not already configured
        TestConfiguration configuration = TestConfiguration.configureIfNotConfigured();
        Map<String, String> userCredentials = configuration.getUserCredentials();

        // This test suite requires at least two verified users in User Pools
        assertTrue(userCredentials.size() >= 2);

        // Get registered user names from test resources
        Iterator<String> users = userCredentials.keySet().iterator();
        userOne = users.next();
        userTwo = users.next();

        // Obtain synchronous storage and mobile client singletons
        storage = SynchronousStorage.singleton();
        mobileClient = SynchronousMobileClient.instance(userCredentials);

        // Obtain the user identity ID values of each user ahead of time
        userIdentityIds = new HashMap<>();
        mobileClient.signOut();
        mobileClient.signIn(userOne);
        userIdentityIds.put(userOne, mobileClient.getIdentityId());

        mobileClient.signOut();
        mobileClient.signIn(userTwo);
        userIdentityIds.put(userTwo, mobileClient.getIdentityId());
    }

    /**
     * Signs out by default and sets up the file to test uploading.
     *
     * @throws Exception if an error is encountered while creating file
     */
    @Before
    public void setUp() throws Exception {
        // Start as a GUEST user
        mobileClient.signOut();

        // Create a new file to upload
        File fileToUpload = new RandomTempFile(UPLOAD_SIZE);
        fileName = fileToUpload.getName();
        filePath = fileToUpload.getPath();

        // Override this per test-case
        uploadOptions = StorageUploadFileOptions.defaultInstance();
    }

    /**
     * Test uploading with public access without signing in.
     *
     * @throws Exception if upload is unsuccessful
     */
    @Test
    public void testUploadUnauthenticatedPublicAccess() throws Exception {
        uploadOptions = StorageUploadFileOptions.builder()
                .accessLevel(StorageAccessLevel.PUBLIC)
                .build();
        storage.uploadFile(fileName, filePath, uploadOptions);
    }

    /**
     * Test uploading with protected access without signing in.
     *
     * A user cannot upload any file to protected access without proper
     * authentication. Protected resources are READ-ONLY to guest users.
     * This test will throw an exception.
     *
     * @throws Exception if upload is unsuccessful
     */
    @Test(expected = StorageException.class)
    public void testUploadUnauthenticatedProtectedAccess() throws Exception {
        uploadOptions = StorageUploadFileOptions.builder()
                .accessLevel(StorageAccessLevel.PROTECTED)
                .targetIdentityId(userIdentityIds.get(userOne))
                .build();
        storage.uploadFile(fileName, filePath, uploadOptions);
    }

    /**
     * Test uploading with private access without signing in.
     *
     * A user cannot upload any file to private access without proper
     * authentication. Private resources are only accessible to owners.
     * This test will throw an exception.
     *
     * @throws Exception if upload is unsuccessful
     */
    @Test(expected = StorageException.class)
    public void testUploadUnauthenticatedPrivateAccess() throws Exception {
        uploadOptions = StorageUploadFileOptions.builder()
                .accessLevel(StorageAccessLevel.PRIVATE)
                .targetIdentityId(userIdentityIds.get(userOne))
                .build();
        storage.uploadFile(fileName, filePath, uploadOptions);
    }

    /**
     * Test uploading with protected access after signing in.
     *
     * @throws Exception if upload is unsuccessful
     */
    @Test
    public void testUploadAuthenticatedProtectedAccess() throws Exception {
        mobileClient.signIn(userOne);
        uploadOptions = StorageUploadFileOptions.builder()
                .accessLevel(StorageAccessLevel.PROTECTED)
                .targetIdentityId(userIdentityIds.get(userOne))
                .build();
        storage.uploadFile(fileName, filePath, uploadOptions);
    }

    /**
     * Test uploading with private access after signing in.
     *
     * @throws Exception if upload is unsuccessful
     */
    @Test
    public void testUploadAuthenticatedPrivateAccess() throws Exception {
        mobileClient.signIn(userOne);
        uploadOptions = StorageUploadFileOptions.builder()
                .accessLevel(StorageAccessLevel.PRIVATE)
                .targetIdentityId(userIdentityIds.get(userOne))
                .build();
        storage.uploadFile(fileName, filePath, uploadOptions);
    }

    /**
     * Test uploading with protected access after signing in
     * as another user.
     *
     * A protected resource is READ-ONLY to all users. Upload is
     * not allowed and this test will throw an exception.
     *
     * @throws Exception if download is unsuccessful
     */
    @Test(expected = StorageException.class)
    public void testUploadDifferentUserProtectedAccess() throws Exception {
        mobileClient.signIn(userOne);
        uploadOptions = StorageUploadFileOptions.builder()
                .accessLevel(StorageAccessLevel.PROTECTED)
                .targetIdentityId(userIdentityIds.get(userTwo))
                .build();
        storage.uploadFile(fileName, filePath, uploadOptions);
    }

    /**
     * Test uploading with private access after signing in
     * as another user.
     *
     * Private resources are only accessible to owners. This test
     * will throw an exception.
     *
     * @throws Exception if download is unsuccessful
     */
    @Test(expected = StorageException.class)
    public void testUploadDifferentUserPrivateAccess() throws Exception {
        mobileClient.signIn(userOne);
        uploadOptions = StorageUploadFileOptions.builder()
                .accessLevel(StorageAccessLevel.PRIVATE)
                .targetIdentityId(userIdentityIds.get(userTwo))
                .build();
        storage.uploadFile(fileName, filePath, uploadOptions);
    }
}
