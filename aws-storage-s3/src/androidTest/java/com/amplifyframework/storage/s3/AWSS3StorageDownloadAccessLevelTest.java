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
import com.amplifyframework.storage.options.StorageDownloadFileOptions;
import com.amplifyframework.storage.options.StorageUploadFileOptions;
import com.amplifyframework.testutils.FileAssert;
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
 * Instrumentation test to confirm that Storage Download behaves
 * correctly with regards to the provided storage access level.
 */
public final class AWSS3StorageDownloadAccessLevelTest {

    private static final long UPLOAD_SIZE = 100L;
    private static final String UPLOAD_NAME = "test-" + System.currentTimeMillis();

    private static File uploadFile;

    private static SynchronousStorage storage;
    private static SynchronousMobileClient mobileClient;

    private static String userOne;
    private static String userTwo;
    private static Map<String, String> userIdentityIds;

    private File downloadFile;
    private String downloadPath;
    private StorageDownloadFileOptions downloadOptions;

    /**
     * Upload the required resources in cloud prior to running the tests.
     * @throws Exception from failure to sign in with Cognito User Pools
     */
    @BeforeClass
    public static void setUpBeforeClass() throws Exception {
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

        // Upload test file in S3 ahead of time
        uploadTestFile();
    }

    /**
     * Signs out by default and sets up download file destination.
     *
     * @throws Exception if an error is encountered while creating file
     */
    @Before
    public void setUp() throws Exception {
        // Start as a GUEST user
        mobileClient.signOut();

        // Create a new download destination
        downloadFile = new RandomTempFile();
        downloadPath = downloadFile.getPath();

        // Override this per test-case
        downloadOptions = StorageDownloadFileOptions.defaultInstance();
    }

    /**
     * Test downloading with public access without signing in.
     *
     * @throws Exception if download is unsuccessful
     */
    @Test
    public void testDownloadUnauthenticatedPublicAccess() throws Exception {
        downloadOptions = StorageDownloadFileOptions.builder()
                .accessLevel(StorageAccessLevel.PUBLIC)
                .build();
        storage.downloadFile(UPLOAD_NAME, downloadPath, downloadOptions);
        FileAssert.assertEquals(uploadFile, downloadFile);
    }

    /**
     * Test downloading with protected access without signing in.
     *
     * A protected resource is READ-ONLY to guest users. Download is
     * allowed even without authentication.
     *
     * @throws Exception if download is unsuccessful
     */
    @Test
    public void testDownloadUnauthenticatedProtectedAccess() throws Exception {
        downloadOptions = StorageDownloadFileOptions.builder()
                .accessLevel(StorageAccessLevel.PROTECTED)
                .targetIdentityId(userIdentityIds.get(userOne))
                .build();
        storage.downloadFile(UPLOAD_NAME, downloadPath, downloadOptions);
        FileAssert.assertEquals(uploadFile, downloadFile);
    }

    /**
     * Test downloading with private access without signing in.
     *
     * A user cannot download any private access file without proper
     * authentication. Private resources are only accessible to owners.
     * This test will throw an exception.
     *
     * @throws Exception if download is unsuccessful
     */
    @Test(expected = StorageException.class)
    public void testDownloadUnauthenticatedPrivateAccess() throws Exception {
        downloadOptions = StorageDownloadFileOptions.builder()
                .accessLevel(StorageAccessLevel.PRIVATE)
                .targetIdentityId(userIdentityIds.get(userOne))
                .build();
        storage.downloadFile(UPLOAD_NAME, downloadPath, downloadOptions);
        FileAssert.assertEquals(uploadFile, downloadFile);
    }

    /**
     * Test downloading with protected access after signing in.
     *
     * @throws Exception if download is unsuccessful
     */
    @Test
    public void testDownloadAuthenticatedProtectedAccess() throws Exception {
        mobileClient.signIn(userOne);
        downloadOptions = StorageDownloadFileOptions.builder()
                .accessLevel(StorageAccessLevel.PROTECTED)
                .targetIdentityId(userIdentityIds.get(userOne))
                .build();
        storage.downloadFile(UPLOAD_NAME, downloadPath, downloadOptions);
        FileAssert.assertEquals(uploadFile, downloadFile);
    }

    /**
     * Test downloading with private access after signing in.
     *
     * @throws Exception if download is unsuccessful
     */
    @Test
    public void testDownloadAuthenticatedPrivateAccess() throws Exception {
        mobileClient.signIn(userOne);
        downloadOptions = StorageDownloadFileOptions.builder()
                .accessLevel(StorageAccessLevel.PRIVATE)
                .targetIdentityId(userIdentityIds.get(userOne))
                .build();
        storage.downloadFile(UPLOAD_NAME, downloadPath, downloadOptions);
        FileAssert.assertEquals(uploadFile, downloadFile);
    }

    /**
     * Test downloading with protected access after signing in
     * as another user.
     *
     * A protected resource is READ-ONLY to all users. Download is
     * allowed even if signed-in user does not own the resource.
     *
     * @throws Exception if download is unsuccessful
     */
    @Test
    public void testDownloadDifferentUsersProtectedAccess() throws Exception {
        mobileClient.signIn(userOne);
        downloadOptions = StorageDownloadFileOptions.builder()
                .accessLevel(StorageAccessLevel.PROTECTED)
                .targetIdentityId(userIdentityIds.get(userTwo))
                .build();
        storage.downloadFile(UPLOAD_NAME, downloadPath, downloadOptions);
        FileAssert.assertEquals(uploadFile, downloadFile);
    }

    /**
     * Test downloading with private access after signing in
     * as another user.
     *
     * Private resources are only accessible to owners. This test
     * will throw an exception.
     *
     * @throws Exception if download is unsuccessful
     */
    @Test(expected = StorageException.class)
    public void testDownloadDifferentUsersPrivateAccess() throws Exception {
        mobileClient.signIn(userOne);
        downloadOptions = StorageDownloadFileOptions.builder()
                .accessLevel(StorageAccessLevel.PRIVATE)
                .targetIdentityId(userIdentityIds.get(userTwo))
                .build();
        storage.downloadFile(UPLOAD_NAME, downloadPath, downloadOptions);
        FileAssert.assertEquals(uploadFile, downloadFile);
    }

    private static void uploadTestFile() throws Exception {
        // Create a temporary file to upload
        uploadFile = new RandomTempFile(UPLOAD_NAME, UPLOAD_SIZE);
        final String key = UPLOAD_NAME;
        final String local = uploadFile.getAbsolutePath();

        StorageUploadFileOptions options;

        // Upload as GUEST
        mobileClient.signOut();
        options = StorageUploadFileOptions.builder()
                .accessLevel(StorageAccessLevel.PUBLIC)
                .build();
        storage.uploadFile(key, local, options);

        // Upload as user one
        mobileClient.signOut();
        mobileClient.signIn(userOne);
        options = StorageUploadFileOptions.builder()
                .accessLevel(StorageAccessLevel.PROTECTED)
                .build();
        storage.uploadFile(key, local, options);
        options = StorageUploadFileOptions.builder()
                .accessLevel(StorageAccessLevel.PRIVATE)
                .build();
        storage.uploadFile(key, local, options);

        // Upload as user two
        mobileClient.signOut();
        mobileClient.signIn(userTwo);
        options = StorageUploadFileOptions.builder()
                .accessLevel(StorageAccessLevel.PROTECTED)
                .build();
        storage.uploadFile(key, local, options);
        options = StorageUploadFileOptions.builder()
                .accessLevel(StorageAccessLevel.PRIVATE)
                .build();
        storage.uploadFile(key, local, options);
    }

}
