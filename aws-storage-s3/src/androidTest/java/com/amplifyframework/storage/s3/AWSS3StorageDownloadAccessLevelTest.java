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

import android.content.Context;

import com.amplifyframework.storage.StorageAccessLevel;
import com.amplifyframework.storage.StorageCategory;
import com.amplifyframework.storage.StorageException;
import com.amplifyframework.storage.options.StorageDownloadFileOptions;
import com.amplifyframework.storage.options.StorageUploadFileOptions;
import com.amplifyframework.storage.s3.UserCredentials.Credential;
import com.amplifyframework.storage.s3.UserCredentials.IdentityIdSource;
import com.amplifyframework.storage.s3.test.R;
import com.amplifyframework.testutils.FileAssert;
import com.amplifyframework.testutils.random.RandomTempFile;
import com.amplifyframework.testutils.sync.SynchronousMobileClient;
import com.amplifyframework.testutils.sync.SynchronousStorage;

import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.File;
import java.util.Iterator;

import static androidx.test.core.app.ApplicationProvider.getApplicationContext;

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

    private static Credential userOne;
    private static Credential userTwo;

    private File downloadFile;
    private StorageDownloadFileOptions downloadOptions;

    /**
     * Upload the required resources in cloud prior to running the tests.
     * @throws Exception from failure to sign in with Cognito User Pools
     */
    @BeforeClass
    public static void setUpOnce() throws Exception {
        Context context = getApplicationContext();

        mobileClient = SynchronousMobileClient.instance();
        mobileClient.initialize();
        IdentityIdSource identityIdSource = MobileClientIdentityIdSource.create(mobileClient);
        UserCredentials userCredentials = UserCredentials.create(context, identityIdSource);
        Iterator<Credential> users = userCredentials.iterator();
        userOne = users.next();
        userTwo = users.next();

        StorageCategory asyncDelegate = TestStorageCategory.create(context, R.raw.amplifyconfiguration);
        storage = SynchronousStorage.delegatingTo(asyncDelegate);

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
        storage.downloadFile(UPLOAD_NAME, downloadFile, downloadOptions);
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
                .targetIdentityId(userOne.getIdentityId())
                .build();
        storage.downloadFile(UPLOAD_NAME, downloadFile, downloadOptions);
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
                .targetIdentityId(userOne.getIdentityId())
                .build();
        storage.downloadFile(UPLOAD_NAME, downloadFile, downloadOptions);
        FileAssert.assertEquals(uploadFile, downloadFile);
    }

    /**
     * Test downloading with protected access after signing in.
     *
     * @throws Exception if download is unsuccessful
     */
    @Test
    public void testDownloadAuthenticatedProtectedAccess() throws Exception {
        mobileClient.signIn(userOne.getUsername(), userOne.getPassword());
        downloadOptions = StorageDownloadFileOptions.builder()
                .accessLevel(StorageAccessLevel.PROTECTED)
                .targetIdentityId(userOne.getIdentityId())
                .build();
        storage.downloadFile(UPLOAD_NAME, downloadFile, downloadOptions);
        FileAssert.assertEquals(uploadFile, downloadFile);
    }

    /**
     * Test downloading with private access after signing in.
     *
     * @throws Exception if download is unsuccessful
     */
    @Test
    public void testDownloadAuthenticatedPrivateAccess() throws Exception {
        mobileClient.signIn(userOne.getUsername(), userOne.getPassword());
        downloadOptions = StorageDownloadFileOptions.builder()
                .accessLevel(StorageAccessLevel.PRIVATE)
                .targetIdentityId(userOne.getIdentityId())
                .build();
        storage.downloadFile(UPLOAD_NAME, downloadFile, downloadOptions);
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
        mobileClient.signIn(userOne.getUsername(), userOne.getPassword());
        downloadOptions = StorageDownloadFileOptions.builder()
                .accessLevel(StorageAccessLevel.PROTECTED)
                .targetIdentityId(userTwo.getIdentityId())
                .build();
        storage.downloadFile(UPLOAD_NAME, downloadFile, downloadOptions);
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
        mobileClient.signIn(userOne.getUsername(), userOne.getPassword());
        downloadOptions = StorageDownloadFileOptions.builder()
                .accessLevel(StorageAccessLevel.PRIVATE)
                .targetIdentityId(userTwo.getIdentityId())
                .build();
        storage.downloadFile(UPLOAD_NAME, downloadFile, downloadOptions);
        FileAssert.assertEquals(uploadFile, downloadFile);
    }

    private static void uploadTestFile() throws Exception {
        // Create a temporary file to upload
        uploadFile = new RandomTempFile(UPLOAD_NAME, UPLOAD_SIZE);
        final String key = UPLOAD_NAME;

        StorageUploadFileOptions options;

        // Upload as GUEST
        mobileClient.signOut();
        options = StorageUploadFileOptions.builder()
                .accessLevel(StorageAccessLevel.PUBLIC)
                .build();
        storage.uploadFile(key, uploadFile, options);

        // Upload as user one
        mobileClient.signOut();
        mobileClient.signIn(userOne.getUsername(), userOne.getPassword());
        options = StorageUploadFileOptions.builder()
                .accessLevel(StorageAccessLevel.PROTECTED)
                .build();
        storage.uploadFile(key, uploadFile, options);
        options = StorageUploadFileOptions.builder()
                .accessLevel(StorageAccessLevel.PRIVATE)
                .build();
        storage.uploadFile(key, uploadFile, options);

        // Upload as user two
        mobileClient.signOut();
        mobileClient.signIn(userTwo.getUsername(), userTwo.getPassword());
        options = StorageUploadFileOptions.builder()
                .accessLevel(StorageAccessLevel.PROTECTED)
                .build();
        storage.uploadFile(key, uploadFile, options);
        options = StorageUploadFileOptions.builder()
                .accessLevel(StorageAccessLevel.PRIVATE)
                .build();
        storage.uploadFile(key, uploadFile, options);
    }
}
