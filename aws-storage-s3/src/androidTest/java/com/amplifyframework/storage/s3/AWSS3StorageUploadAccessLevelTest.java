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

import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertTrue;

/**
 * Instrumentation test to confirm that Storage Upload behaves
 * correctly with regards to the provided storage access level.
 */
public final class AWSS3StorageUploadAccessLevelTest extends StorageInstrumentationTestBase {

    private static String userNameOne = "test-user-1";
    private static String userNameTwo = "test-user-2";
    private static Map<String, String> identityIds = new HashMap<>();

    private final String filename = "test-" + System.currentTimeMillis();
    private File fileToUpload;

    /**
     * Obtain the user IDs prior to running the tests.
     */
    @BeforeClass
    public static void setUpBeforeClass() {
        // Get registered user names from test resources
        List<String> users = getUsers();
        assertTrue(users.size() >= 2); // This test suite requires at least two verified users
        userNameOne = users.get(0);
        userNameTwo = users.get(1);

        // Obtain the user identity id values of each user ahead of time
        signInAs(userNameOne);
        identityIds.put(userNameOne, getIdentityId());

        signInAs(userNameTwo);
        identityIds.put(userNameTwo, getIdentityId());
    }

    /**
     * Signs out by default and sets up the file to test uploading.
     *
     * @throws Exception if an error is encountered while creating file
     */
    @Before
    public void setUp() throws Exception {
        signOut();
        fileToUpload = createTempFile(filename);
    }

    /**
     * Test uploading with public access without signing in.
     *
     * @throws Exception if upload is unsuccessful
     */
    @Test
    public void testUploadUnauthenticatedPublicAccess() throws Exception {
        testUploadAndCleanUp(fileToUpload, StorageAccessLevel.PUBLIC, getIdentityId());
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
        testUploadAndCleanUp(fileToUpload, StorageAccessLevel.PROTECTED, identityIds.get(userNameOne));

        // This part of the test is unreachable, since above should fail
        testUploadAndCleanUp(fileToUpload, StorageAccessLevel.PROTECTED, identityIds.get(userNameTwo));
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
        testUploadAndCleanUp(fileToUpload, StorageAccessLevel.PRIVATE, identityIds.get(userNameOne));

        // This part of the test is unreachable, since above should fail
        testUploadAndCleanUp(fileToUpload, StorageAccessLevel.PRIVATE, identityIds.get(userNameTwo));
    }

    /**
     * Test uploading with protected access after signing in.
     *
     * @throws Exception if upload is unsuccessful
     */
    @Test
    public void testUploadAuthenticatedProtectedAccess() throws Exception {
        signInAs(userNameOne);
        testUploadAndCleanUp(fileToUpload, StorageAccessLevel.PROTECTED, getIdentityId());

        signInAs(userNameTwo);
        testUploadAndCleanUp(fileToUpload, StorageAccessLevel.PROTECTED, getIdentityId());
    }

    /**
     * Test uploading with private access after signing in.
     *
     * @throws Exception if upload is unsuccessful
     */
    @Test
    public void testUploadAuthenticatedPrivateAccess() throws Exception {
        signInAs(userNameOne);
        testUploadAndCleanUp(fileToUpload, StorageAccessLevel.PRIVATE, getIdentityId());

        signInAs(userNameTwo);
        testUploadAndCleanUp(fileToUpload, StorageAccessLevel.PRIVATE, getIdentityId());
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
        signInAs(userNameOne);
        testUploadAndCleanUp(fileToUpload, StorageAccessLevel.PROTECTED, identityIds.get(userNameTwo));

        // This part of the test is unreachable
        signInAs(userNameTwo);
        testUploadAndCleanUp(fileToUpload, StorageAccessLevel.PROTECTED, identityIds.get(userNameOne));
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
        signInAs(userNameOne);
        testUploadAndCleanUp(fileToUpload, StorageAccessLevel.PRIVATE, identityIds.get(userNameTwo));

        // This part of the test is unreachable
        signInAs(userNameTwo);
        testUploadAndCleanUp(fileToUpload, StorageAccessLevel.PRIVATE, identityIds.get(userNameOne));
    }

    private void testUploadAndCleanUp(
            File file,
            StorageAccessLevel accessLevel,
            String identityId
    ) throws Exception {
        StorageUploadFileOptions options = StorageUploadFileOptions.builder()
                .accessLevel(accessLevel)
                .targetIdentityId(identityId)
                .build();
        synchronousStorage().uploadFile(file.getName(),
                file.getAbsolutePath(),
                options);

        // Confirm and clean up
        String s3key = getS3Key(accessLevel, file.getName());
        assertS3ObjectExists(s3key);
        cleanUpS3Object(s3key);
    }
}
