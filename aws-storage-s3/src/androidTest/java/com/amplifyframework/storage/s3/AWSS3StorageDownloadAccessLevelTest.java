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
import com.amplifyframework.testutils.FileAssert;
import com.amplifyframework.testutils.SynchronousAWSMobileClient;
import com.amplifyframework.testutils.RandomTempFile;

import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertTrue;

/**
 * Instrumentation test to confirm that Storage Download behaves
 * correctly with regards to the provided storage access level.
 */
public final class AWSS3StorageDownloadAccessLevelTest extends StorageInstrumentationTestBase {

    private static String userNameOne;
    private static String userNameTwo;
    private static Map<String, String> identityIds = new HashMap<>();

    private static final String UPLOAD_NAME = "upload-test-" + System.currentTimeMillis();
    private static final long UPLOAD_SIZE = 100L;
    private static File uploadFile;

    private final String destination = "download-test-" + System.currentTimeMillis();
    private File downloadFile;

    /**
     * Upload the required resources in cloud prior to running the tests.
     * @throws Exception if there is a problem while creating or uploading
     *         the files.
     */
    @BeforeClass
    public static void setUpBeforeClass() throws Exception {
        // Get registered user names from test resources
        List<String> users = getUsers();
        assertTrue(users.size() >= 2); // This test suite requires at least two verified users
        userNameOne = users.get(0);
        userNameTwo = users.get(1);

        // Randomly write a file to upload
        uploadFile = new RandomTempFile(UPLOAD_NAME, UPLOAD_SIZE);

        // Upload to each access level
        latchedUploadAndConfirm(uploadFile, StorageAccessLevel.PUBLIC, getIdentityId());

        signInAs(userNameOne);
        identityIds.put(userNameOne, getIdentityId());
        latchedUploadAndConfirm(uploadFile, StorageAccessLevel.PROTECTED, getIdentityId());
        latchedUploadAndConfirm(uploadFile, StorageAccessLevel.PRIVATE, getIdentityId());

        signInAs(userNameTwo);
        identityIds.put(userNameTwo, getIdentityId());
        latchedUploadAndConfirm(uploadFile, StorageAccessLevel.PROTECTED, getIdentityId());
        latchedUploadAndConfirm(uploadFile, StorageAccessLevel.PRIVATE, getIdentityId());
    }

    /**
     * Clean up the uploaded resources after the test.
     */
    @AfterClass
    public static void cleanUp() throws SynchronousAWSMobileClient.MobileClientException {
        // Clean up each access level
        cleanUpS3Object(getS3Key(StorageAccessLevel.PUBLIC, UPLOAD_NAME));

        signInAs(userNameOne);
        cleanUpS3Object(getS3Key(StorageAccessLevel.PROTECTED, UPLOAD_NAME));
        cleanUpS3Object(getS3Key(StorageAccessLevel.PRIVATE, UPLOAD_NAME));

        signInAs(userNameTwo);
        cleanUpS3Object(getS3Key(StorageAccessLevel.PROTECTED, UPLOAD_NAME));
        cleanUpS3Object(getS3Key(StorageAccessLevel.PRIVATE, UPLOAD_NAME));

        identityIds.clear();
    }

    /**
     * Signs out by default and sets up the file to download object to.
     * @throws Exception if an error is encountered while creating file
     */
    @Before
    public void setUp() throws Exception {
        signOut();
        downloadFile = new RandomTempFile(destination);
    }

    /**
     * Test downloading with public access without signing in.
     *
     * @throws Exception if download is unsuccessful
     */
    @Test
    public void testDownloadUnauthenticatedPublicAccess() throws Exception {
        testDownload(downloadFile, StorageAccessLevel.PUBLIC, getIdentityId());
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
        testDownload(downloadFile, StorageAccessLevel.PROTECTED, identityIds.get(userNameOne));
        testDownload(downloadFile, StorageAccessLevel.PROTECTED, identityIds.get(userNameTwo));
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
        testDownload(downloadFile, StorageAccessLevel.PRIVATE, identityIds.get(userNameOne));

        // This one should be unreachable, since first attempt fails
        testDownload(downloadFile, StorageAccessLevel.PRIVATE, identityIds.get(userNameTwo));
    }

    /**
     * Test downloading with protected access after signing in.
     *
     * @throws Exception if download is unsuccessful
     */
    @Test
    public void testDownloadAuthenticatedProtectedAccess() throws Exception {
        signInAs(userNameOne);
        testDownload(downloadFile, StorageAccessLevel.PROTECTED, getIdentityId());

        signInAs(userNameTwo);
        testDownload(downloadFile, StorageAccessLevel.PROTECTED, getIdentityId());
    }

    /**
     * Test downloading with private access after signing in.
     *
     * @throws Exception if download is unsuccessful
     */
    @Test
    public void testDownloadAuthenticatedPrivateAccess() throws Exception {
        signInAs(userNameOne);
        testDownload(downloadFile, StorageAccessLevel.PRIVATE, getIdentityId());

        signInAs(userNameTwo);
        testDownload(downloadFile, StorageAccessLevel.PRIVATE, getIdentityId());
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
        signInAs(userNameOne);
        testDownload(downloadFile, StorageAccessLevel.PROTECTED, identityIds.get(userNameTwo));

        signInAs(userNameTwo);
        testDownload(downloadFile, StorageAccessLevel.PROTECTED, identityIds.get(userNameOne));
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
        signInAs(userNameOne);
        testDownload(downloadFile, StorageAccessLevel.PRIVATE, identityIds.get(userNameTwo));

        // This part of the test should be unreachable
        signInAs(userNameTwo);
        testDownload(downloadFile, StorageAccessLevel.PRIVATE, identityIds.get(userNameOne));
    }

    private void testDownload(
            File downloadTo,
            StorageAccessLevel accessLevel,
            String identityId
    ) throws Exception {
        StorageDownloadFileOptions options = (StorageDownloadFileOptions) StorageDownloadFileOptions.builder()
                .accessLevel(accessLevel)
                .targetIdentityId(identityId)
                .build();
        synchronousStorage().downloadFile(UPLOAD_NAME, downloadTo.getAbsolutePath(), options);
        FileAssert.assertEquals(uploadFile, downloadTo);
    }
}
