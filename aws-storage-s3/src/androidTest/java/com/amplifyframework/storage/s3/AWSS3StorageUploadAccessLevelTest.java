/*
 * Copyright 2021 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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

import com.amplifyframework.AmplifyException;
import com.amplifyframework.auth.cognito.AWSCognitoAuthPlugin;
import com.amplifyframework.storage.StorageAccessLevel;
import com.amplifyframework.storage.StorageCategory;
import com.amplifyframework.storage.StorageException;
import com.amplifyframework.storage.options.StorageRemoveOptions;
import com.amplifyframework.storage.options.StorageUploadFileOptions;
import com.amplifyframework.storage.s3.UserCredentials.Credential;
import com.amplifyframework.storage.s3.test.R;
import com.amplifyframework.storage.s3.util.WorkmanagerTestUtils;
import com.amplifyframework.testutils.random.RandomTempFile;
import com.amplifyframework.testutils.sync.SynchronousAuth;
import com.amplifyframework.testutils.sync.SynchronousStorage;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.File;
import java.util.Iterator;

import static androidx.test.core.app.ApplicationProvider.getApplicationContext;
import static com.amplifyframework.storage.s3.UserCredentials.IdentityIdSource;

/**
 * Instrumentation test to confirm that Storage Upload behaves
 * correctly with regards to the provided storage access level.
 */
@SuppressWarnings("deprecation")
public final class AWSS3StorageUploadAccessLevelTest {
    private static final long UPLOAD_SIZE = 100L;

    private static SynchronousStorage storage;

    private static SynchronousAuth synchronousAuth;
    private static Credential userOne;
    private static Credential userTwo;

    private File uploadFile;
    private String remoteKey;
    private StorageUploadFileOptions uploadOptions;

    /**
     * Obtain the user IDs prior to running the tests.
     * @throws AmplifyException On failure to initialize Amplify
     * @throws InterruptedException On thread interruption
     */
    @BeforeClass
    public static void setUpOnce() throws AmplifyException, InterruptedException {
        Context context = getApplicationContext();

        WorkmanagerTestUtils.INSTANCE.initializeWorkmanagerTestUtil(context);
        synchronousAuth = SynchronousAuth.delegatingToCognito(context, new AWSCognitoAuthPlugin());
        IdentityIdSource identityIdSource = MobileClientIdentityIdSource.create(synchronousAuth);
        UserCredentials userCredentials = UserCredentials.create(context, identityIdSource);
        Iterator<Credential> iterator = userCredentials.iterator();
        userOne = iterator.next();
        userTwo = iterator.next();

        // Setup storage.
        StorageCategory asyncDelegate = TestStorageCategory.create(context, R.raw.amplifyconfiguration);
        storage = SynchronousStorage.delegatingTo(asyncDelegate);
    }

    /**
     * Reset all the assigned static fields.
     */
    @AfterClass
    public static void tearDownSuite() {
        synchronousAuth = null;
        userOne = null;
        userTwo = null;
        storage = null;
    }

    /**
     * Signs out by default and sets up the file to test uploading.
     *
     * @throws Exception if an error is encountered while creating file
     */
    @Before
    public void setUp() throws Exception {
        // Create a new file to upload
        uploadFile = new RandomTempFile(UPLOAD_SIZE);
        remoteKey = uploadFile.getName();

        // Override this per test-case
        uploadOptions = StorageUploadFileOptions.defaultInstance();
    }

    /**
     * Remove test file and sign out after each test.
     * @throws Exception if error encountered while signing out
     */
    @After
    public void tearDown() throws Exception {
        synchronousAuth.signOut();
        synchronousAuth.signIn(userOne.getUsername(), userOne.getPassword());
        StorageAccessLevel accessLevel = uploadOptions.getAccessLevel();
        StorageRemoveOptions options = StorageRemoveOptions.builder().accessLevel(accessLevel).build();
        storage.remove(remoteKey, options);
        synchronousAuth.signOut();
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
        storage.uploadFile(remoteKey, uploadFile, uploadOptions);
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
                .targetIdentityId(userOne.getIdentityId())
                .build();
        storage.uploadFile(remoteKey, uploadFile, uploadOptions);
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
                .targetIdentityId(userOne.getIdentityId())
                .build();
        storage.uploadFile(remoteKey, uploadFile, uploadOptions);
    }

    /**
     * Test uploading with protected access after signing in.
     *
     * @throws Exception if upload is unsuccessful
     */
    @Test
    public void testUploadAuthenticatedProtectedAccess() throws Exception {
        synchronousAuth.signIn(userOne.getUsername(), userOne.getPassword());
        uploadOptions = StorageUploadFileOptions.builder()
                .accessLevel(StorageAccessLevel.PROTECTED)
                .targetIdentityId(userOne.getIdentityId())
                .build();
        storage.uploadFile(remoteKey, uploadFile, uploadOptions);
    }

    /**
     * Test uploading with private access after signing in.
     *
     * @throws Exception if upload is unsuccessful
     */
    @Test
    public void testUploadAuthenticatedPrivateAccess() throws Exception {
        synchronousAuth.signIn(userOne.getUsername(), userOne.getPassword());
        uploadOptions = StorageUploadFileOptions.builder()
                .accessLevel(StorageAccessLevel.PRIVATE)
                .targetIdentityId(userOne.getIdentityId())
                .build();
        storage.uploadFile(remoteKey, uploadFile, uploadOptions);
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
        synchronousAuth.signIn(userOne.getUsername(), userOne.getPassword());
        uploadOptions = StorageUploadFileOptions.builder()
                .accessLevel(StorageAccessLevel.PROTECTED)
                .targetIdentityId(userTwo.getIdentityId())
                .build();
        storage.uploadFile(remoteKey, uploadFile, uploadOptions);
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
        synchronousAuth.signIn(userOne.getUsername(), userOne.getPassword());
        uploadOptions = StorageUploadFileOptions.builder()
                .accessLevel(StorageAccessLevel.PRIVATE)
                .targetIdentityId(userTwo.getIdentityId())
                .build();
        storage.uploadFile(remoteKey, uploadFile, uploadOptions);
    }
}
