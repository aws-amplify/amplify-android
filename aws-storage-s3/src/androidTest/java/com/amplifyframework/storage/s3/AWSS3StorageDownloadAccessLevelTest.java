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
import com.amplifyframework.storage.options.StorageUploadFileOptions;
import com.amplifyframework.storage.result.StorageDownloadFileResult;
import com.amplifyframework.storage.result.StorageUploadFileResult;
import com.amplifyframework.testutils.Await;

import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.io.IOException;

/**
 * Instrumentation test to confirm that Storage Download behaves
 * correctly with regards to the provided storage access level.
 */
public final class AWSS3StorageDownloadAccessLevelTest extends StorageInstrumentationTestBase {

    private static final String UPLOAD_NAME = "upload-test-" + System.currentTimeMillis();
    private static final long UPLOAD_SIZE = 100L;
    private static final File UPLOAD_FILE;

    static {
        try {
            UPLOAD_FILE = createTempFile(UPLOAD_NAME, UPLOAD_SIZE);
        } catch (IOException error) {
            throw new RuntimeException("Failed to set up downloadable file.");
        }
    }

    private final String downloadedName = "download-test-" + System.currentTimeMillis();
    private File downloadFile;

    /**
     * Signs out by default and sets up the file to download object to.
     * @throws Exception if an error is encountered while creating file
     */
    @Before
    public void setUp() throws Exception {
        signOut();
        downloadFile = createTempFile(downloadedName);
    }

    /**
     * Test downloading with public access without signing in.
     *
     * @throws Exception if download is unsuccessful
     */
    @Test
    public void testDownloadUnauthenticatedPublicAccess() throws Exception {
        final StorageAccessLevel accessLevel = StorageAccessLevel.PUBLIC;

        upload(accessLevel);

        testDownload(downloadFile, accessLevel, getIdentityId());

        // Clean-up uploaded item
        cleanUp(accessLevel);
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
        final StorageAccessLevel accessLevel = StorageAccessLevel.PROTECTED;
        final String identityId;

        // Sign in as "test-user-1"
        signInAs("test-user-1");
        upload(accessLevel);

        // Remember user's identity ID before signing out
        identityId = getIdentityId();
        signOut();

        testDownload(downloadFile, accessLevel, identityId);
        cleanUp(accessLevel);
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
        final StorageAccessLevel accessLevel = StorageAccessLevel.PRIVATE;
        final String identityId;

        // Sign in as "test-user-1"
        signInAs("test-user-1");
        upload(accessLevel);

        // Remember user's identity ID before signing out
        identityId = getIdentityId();
        signOut();

        testDownload(downloadFile, accessLevel, identityId);
        cleanUp(accessLevel);
    }

    /**
     * Test downloading with protected access after signing in.
     *
     * @throws Exception if download is unsuccessful
     */
    @Test
    public void testDownloadAuthenticatedProtectedAccess() throws Exception {
        final StorageAccessLevel accessLevel = StorageAccessLevel.PROTECTED;

        // Sign in as "test-user-1"
        signInAs("test-user-1");
        upload(accessLevel);

        testDownload(downloadFile, accessLevel, getIdentityId());
        cleanUp(accessLevel);
    }

    /**
     * Test downloading with private access after signing in.
     *
     * @throws Exception if download is unsuccessful
     */
    @Test
    public void testDownloadAuthenticatedPrivateAccess() throws Exception {
        final StorageAccessLevel accessLevel = StorageAccessLevel.PRIVATE;

        // Sign in as "test-user-1"
        signInAs("test-user-1");
        upload(accessLevel);

        testDownload(downloadFile, accessLevel, getIdentityId());
        cleanUp(accessLevel);
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
        final StorageAccessLevel accessLevel = StorageAccessLevel.PROTECTED;
        final String identityId;

        // Sign in as "test-user-1"
        signInAs("test-user-1");
        upload(accessLevel);

        // Remember user's identity ID before signing out
        identityId = getIdentityId();

        // Re-sign in as "test-user-2"
        signOut();
        signInAs("test-user-2");

        testDownload(downloadFile, accessLevel, identityId);
        cleanUp(accessLevel);
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
        final StorageAccessLevel accessLevel = StorageAccessLevel.PRIVATE;
        final String identityId;

        // Sign in as "test-user-1"
        signInAs("test-user-1");
        upload(accessLevel);

        // Remember user's identity ID before signing out
        identityId = getIdentityId();

        // Re-sign in as "test-user-2"
        signOut();
        signInAs("test-user-2");

        testDownload(downloadFile, accessLevel, identityId);
        cleanUp(accessLevel);
    }

    private void upload(StorageAccessLevel accessLevel) throws StorageException {
        StorageUploadFileOptions uploadOptions = StorageUploadFileOptions.builder()
                .accessLevel(accessLevel)
                .build();
        Await.<StorageUploadFileResult, StorageException>result((onResult, onError) ->
                Amplify.Storage.uploadFile(
                        UPLOAD_FILE.getName(),
                        UPLOAD_FILE.getAbsolutePath(),
                        uploadOptions,
                        onResult,
                        onError
                )
        );
    }

    private void testDownload(
            File downloadTo,
            StorageAccessLevel accessLevel,
            String identityId
    ) throws StorageException {
        StorageDownloadFileOptions downloadOptions = StorageDownloadFileOptions.builder()
                .accessLevel(accessLevel)
                .targetIdentityId(identityId)
                .build();
        StorageDownloadFileResult result =
                Await.<StorageDownloadFileResult, StorageException>result((onResult, onError) ->
                Amplify.Storage.downloadFile(
                        UPLOAD_NAME,
                        downloadTo.getAbsolutePath(),
                        downloadOptions,
                        onResult,
                        onError
                )
        );
        TestUtils.assertFileEqualsFile(UPLOAD_FILE, result.getFile());
    }

    private void cleanUp(StorageAccessLevel accessLevel) {
        String s3key = getS3Key(accessLevel, UPLOAD_NAME);
        cleanUpS3Object(s3key);
    }
}
