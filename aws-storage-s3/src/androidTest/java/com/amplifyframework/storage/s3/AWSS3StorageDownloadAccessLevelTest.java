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

import com.amplifyframework.core.Amplify;
import com.amplifyframework.storage.StorageAccessLevel;
import com.amplifyframework.storage.StorageException;
import com.amplifyframework.storage.options.StorageDownloadFileOptions;

import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.File;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Instrumentation test to confirm that Storage Download behaves
 * correctly with regards to the provided storage access level.
 */
public final class AWSS3StorageDownloadAccessLevelTest extends StorageInstrumentationTestBase {

    private static final String USER_NAME_ONE = "test-user-1";
    private static final String USER_NAME_TWO = "test-user-2";
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
        // Randomly write a file to upload
        uploadFile = createTempFile(UPLOAD_NAME, UPLOAD_SIZE);

        // Upload to each access level
        latchedUploadAndConfirm(uploadFile, StorageAccessLevel.PUBLIC, getIdentityId());

        signInAs(USER_NAME_ONE);
        identityIds.put(USER_NAME_ONE, getIdentityId());
        latchedUploadAndConfirm(uploadFile, StorageAccessLevel.PROTECTED, getIdentityId());
        latchedUploadAndConfirm(uploadFile, StorageAccessLevel.PRIVATE, getIdentityId());

        signInAs(USER_NAME_TWO);
        identityIds.put(USER_NAME_TWO, getIdentityId());
        latchedUploadAndConfirm(uploadFile, StorageAccessLevel.PROTECTED, getIdentityId());
        latchedUploadAndConfirm(uploadFile, StorageAccessLevel.PRIVATE, getIdentityId());
    }

    /**
     * Clean up the uploaded resources after the test.
     */
    @AfterClass
    public static void cleanUp() {
        // Clean up each access level
        cleanUpS3Object(getS3Key(StorageAccessLevel.PUBLIC, UPLOAD_NAME));

        signInAs(USER_NAME_ONE);
        cleanUpS3Object(getS3Key(StorageAccessLevel.PROTECTED, UPLOAD_NAME));
        cleanUpS3Object(getS3Key(StorageAccessLevel.PRIVATE, UPLOAD_NAME));

        signInAs(USER_NAME_TWO);
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
        downloadFile = createTempFile(destination);
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
        testDownload(downloadFile, StorageAccessLevel.PROTECTED, identityIds.get(USER_NAME_ONE));
        testDownload(downloadFile, StorageAccessLevel.PROTECTED, identityIds.get(USER_NAME_TWO));
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
        testDownload(downloadFile, StorageAccessLevel.PRIVATE, identityIds.get(USER_NAME_ONE));

        // This one should be unreachable, since first attempt fails
        testDownload(downloadFile, StorageAccessLevel.PRIVATE, identityIds.get(USER_NAME_TWO));
    }

    /**
     * Test downloading with protected access after signing in.
     *
     * @throws Exception if download is unsuccessful
     */
    @Test
    public void testDownloadAuthenticatedProtectedAccess() throws Exception {
        signInAs(USER_NAME_ONE);
        testDownload(downloadFile, StorageAccessLevel.PROTECTED, getIdentityId());

        signInAs(USER_NAME_TWO);
        testDownload(downloadFile, StorageAccessLevel.PROTECTED, getIdentityId());
    }

    /**
     * Test downloading with private access after signing in.
     *
     * @throws Exception if download is unsuccessful
     */
    @Test
    public void testDownloadAuthenticatedPrivateAccess() throws Exception {
        signInAs(USER_NAME_ONE);
        testDownload(downloadFile, StorageAccessLevel.PRIVATE, getIdentityId());

        signInAs(USER_NAME_TWO);
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
        signInAs(USER_NAME_ONE);
        testDownload(downloadFile, StorageAccessLevel.PROTECTED, identityIds.get(USER_NAME_TWO));

        signInAs(USER_NAME_TWO);
        testDownload(downloadFile, StorageAccessLevel.PROTECTED, identityIds.get(USER_NAME_ONE));
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
        signInAs(USER_NAME_ONE);
        testDownload(downloadFile, StorageAccessLevel.PRIVATE, identityIds.get(USER_NAME_TWO));

        // This part of the test should be unreachable
        signInAs(USER_NAME_TWO);
        testDownload(downloadFile, StorageAccessLevel.PRIVATE, identityIds.get(USER_NAME_ONE));
    }

    @SuppressWarnings("Indentation") // Doesn't seem to like lambda indentation
    private void testDownload(
            File downloadTo,
            StorageAccessLevel accessLevel,
            String identityId
    ) throws Exception {
        final CountDownLatch completed = new CountDownLatch(1);
        AtomicReference<StorageException> errorContainer = new AtomicReference<>();

        StorageDownloadFileOptions downloadOptions = StorageDownloadFileOptions.builder()
                .accessLevel(accessLevel)
                .targetIdentityId(identityId)
                .build();
        Amplify.Storage.downloadFile(
                UPLOAD_NAME,
                downloadTo.getAbsolutePath(),
                downloadOptions,
                onResult -> completed.countDown(),
                onError -> {
                    errorContainer.set(onError);
                    completed.countDown();
                }
        );
        completed.await(DEFAULT_TIMEOUT_IN_SECONDS, TimeUnit.SECONDS);

        // Throw if upload was not successful
        StorageException error = errorContainer.get();
        if (error != null) {
            error.printStackTrace();
            throw error;
        }
    }
}
