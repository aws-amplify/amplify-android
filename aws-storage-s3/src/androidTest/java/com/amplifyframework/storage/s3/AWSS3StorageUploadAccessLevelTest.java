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
import com.amplifyframework.storage.options.StorageUploadFileOptions;
import com.amplifyframework.storage.result.StorageUploadFileResult;
import com.amplifyframework.testutils.Await;

import org.junit.Before;
import org.junit.Test;

import java.io.File;

import static org.junit.Assert.assertEquals;

/**
 * Instrumentation test to confirm that Storage Upload behaves
 * correctly with regards to the provided storage access level.
 */
public final class AWSS3StorageUploadAccessLevelTest extends StorageInstrumentationTestBase {

    private final String filename = "test-" + System.currentTimeMillis();

    private File fileToUpload;

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
        testUploadAndCleanUp(fileToUpload, StorageAccessLevel.PUBLIC);
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
        testUploadAndCleanUp(fileToUpload, StorageAccessLevel.PROTECTED);
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
        testUploadAndCleanUp(fileToUpload, StorageAccessLevel.PRIVATE);
    }

    /**
     * Test uploading with protected access after signing in.
     *
     * @throws Exception if upload is unsuccessful
     */
    @Test
    public void testUploadAuthenticatedProtectedAccess() throws Exception {
        signInAs("test-user-1");
        testUploadAndCleanUp(fileToUpload, StorageAccessLevel.PROTECTED);
    }

    /**
     * Test uploading with private access after signing in.
     *
     * @throws Exception if upload is unsuccessful
     */
    @Test
    public void testUploadAuthenticatedPrivateAccess() throws Exception {
        signInAs("test-user-2");
        testUploadAndCleanUp(fileToUpload, StorageAccessLevel.PRIVATE);
    }

    private void testUploadAndCleanUp(File file,
                                      StorageAccessLevel accessLevel) throws StorageException {
        StorageUploadFileOptions options = StorageUploadFileOptions.builder()
                .accessLevel(accessLevel)
                .build();
        StorageUploadFileResult result =
                Await.<StorageUploadFileResult, StorageException>result((onResult, onError) ->
                Amplify.Storage.uploadFile(
                        file.getName(),
                        file.getAbsolutePath(),
                        options,
                        onResult,
                        onError
                )
        );

        // Will only make it this far if transfer was successful
        assertEquals(file.getName(), result.getKey());

        // Clean up
        String s3key = getS3Key(accessLevel, file.getName());
        assertS3ObjectExists(s3key);
        cleanUpS3Object(s3key);
    }
}
