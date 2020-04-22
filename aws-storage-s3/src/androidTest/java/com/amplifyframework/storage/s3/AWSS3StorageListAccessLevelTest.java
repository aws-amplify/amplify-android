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
import com.amplifyframework.storage.StorageItem;
import com.amplifyframework.storage.options.StorageListOptions;
import com.amplifyframework.storage.options.StorageUploadFileOptions;
import com.amplifyframework.storage.result.StorageListResult;
import com.amplifyframework.storage.s3.UserCredentials.IdentityIdSource;
import com.amplifyframework.storage.s3.test.R;
import com.amplifyframework.testutils.random.RandomTempFile;
import com.amplifyframework.testutils.sync.SynchronousMobileClient;
import com.amplifyframework.testutils.sync.SynchronousStorage;

import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.File;
import java.util.Iterator;
import java.util.List;

import static androidx.test.core.app.ApplicationProvider.getApplicationContext;
import static com.amplifyframework.storage.s3.UserCredentials.Credential;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

/**
 * Instrumentation test to confirm that Storage List behaves
 * correctly with regards to the provided storage access level.
 */
public final class AWSS3StorageListAccessLevelTest {
    private static final String TEST_DIR_NAME = Long.toString(System.currentTimeMillis());
    private static final long UPLOAD_SIZE = 100L;

    private static String uploadKey;

    private static SynchronousStorage storage;
    private static SynchronousMobileClient mobileClient;
    private static Credential userOne;
    private static Credential userTwo;

    private StorageListOptions listOptions;

    /**
     * Upload the required resources in cloud prior to running the tests.
     * @throws Exception from failure to sign in with Cognito User Pools
     */
    @BeforeClass
    public static void setUpOnce() throws Exception {
        Context context = getApplicationContext();

        // Init auth stuff
        mobileClient = SynchronousMobileClient.instance();
        mobileClient.initialize();
        IdentityIdSource identityIdSource = MobileClientIdentityIdSource.create(mobileClient);
        UserCredentials credentials = UserCredentials.create(context, identityIdSource);
        Iterator<Credential> users = credentials.iterator();
        userOne = users.next();
        userTwo = users.next();

        // Get a handle to storage
        StorageCategory asyncDelegate = TestStorageCategory.create(context, R.raw.amplifyconfiguration);
        storage = SynchronousStorage.delegatingTo(asyncDelegate);

        // Upload test file in S3 ahead of time
        uploadTestFiles();
    }

    /**
     * Signs out by default.
     * @throws Exception on failure to sign out
     */
    @Before
    public void setUp() throws Exception {
        // Start as a GUEST user
        mobileClient.signOut();
    }

    private void assertListContainsUploadedFile(StorageListResult result) {
        List<StorageItem> items = result.getItems();
        assertNotNull(items);
        assertEquals(1, items.size());

        // Get the first item (there should only be one)
        StorageItem storedItem = items.get(0);
        assertEquals(UPLOAD_SIZE, storedItem.getSize());
        assertEquals(uploadKey, storedItem.getKey());
    }

    /**
     * Test listing files with public access without signing in.
     *
     * @throws Exception if list is unsuccessful
     */
    @Test
    public void testListUnauthenticatedPublicAccess() throws Exception {
        listOptions = StorageListOptions.builder()
                .accessLevel(StorageAccessLevel.PUBLIC)
                .build();
        StorageListResult result = storage.list(TEST_DIR_NAME, listOptions);
        assertListContainsUploadedFile(result);
    }

    /**
     * Test listing files with protected access without signing in.
     *
     * A protected resource is READ-ONLY to guest users. List is
     * allowed even without authentication.
     *
     * @throws Exception if list is unsuccessful
     */
    @Test
    public void testListUnauthenticatedProtectedAccess() throws Exception {
        listOptions = StorageListOptions.builder()
                .accessLevel(StorageAccessLevel.PROTECTED)
                .targetIdentityId(userOne.getIdentityId())
                .build();
        StorageListResult result = storage.list(TEST_DIR_NAME, listOptions);
        assertListContainsUploadedFile(result);
    }

    /**
     * Test listing files with private access without signing in.
     *
     * A user cannot list any private access file without proper
     * authentication. Private resources are only accessible to owners.
     * This test will throw an exception.
     *
     * @throws Exception if list is unsuccessful
     */
    @Test(expected = StorageException.class)
    public void testListUnauthenticatedPrivateAccess() throws Exception {
        listOptions = StorageListOptions.builder()
                .accessLevel(StorageAccessLevel.PRIVATE)
                .targetIdentityId(userOne.getIdentityId())
                .build();
        StorageListResult result = storage.list(TEST_DIR_NAME, listOptions);
        assertListContainsUploadedFile(result);
    }

    /**
     * Test listing files with protected access after signing in.
     *
     * @throws Exception if list is unsuccessful
     */
    @Test
    public void testListAuthenticatedProtectedAccess() throws Exception {
        mobileClient.signIn(userOne.getUsername(), userOne.getPassword());
        listOptions = StorageListOptions.builder()
                .accessLevel(StorageAccessLevel.PROTECTED)
                .targetIdentityId(userOne.getIdentityId())
                .build();
        StorageListResult result = storage.list(TEST_DIR_NAME, listOptions);
        assertListContainsUploadedFile(result);
    }

    /**
     * Test listing files with private access after signing in.
     *
     * @throws Exception if list is unsuccessful
     */
    @Test
    public void testListAuthenticatedPrivateAccess() throws Exception {
        mobileClient.signIn(userOne.getUsername(), userOne.getPassword());
        listOptions = StorageListOptions.builder()
                .accessLevel(StorageAccessLevel.PRIVATE)
                .targetIdentityId(userOne.getIdentityId())
                .build();
        StorageListResult result = storage.list(TEST_DIR_NAME, listOptions);
        assertListContainsUploadedFile(result);
    }

    /**
     * Test listing files with protected access after signing in
     * as another user.
     *
     * A protected resource is READ-ONLY to all users. List is
     * allowed even if signed-in user does not own the resource.
     *
     * @throws Exception if list is unsuccessful
     */
    @Test
    public void testListDifferentUsersProtectedAccess() throws Exception {
        mobileClient.signIn(userOne.getUsername(), userOne.getPassword());
        listOptions = StorageListOptions.builder()
                .accessLevel(StorageAccessLevel.PROTECTED)
                .targetIdentityId(userTwo.getIdentityId())
                .build();
        StorageListResult result = storage.list(TEST_DIR_NAME, listOptions);
        assertListContainsUploadedFile(result);
    }

    /**
     * Test listing files with private access after signing in
     * as another user.
     *
     * Private resources are only accessible to owners. This test
     * will throw an exception.
     *
     * @throws Exception if list is unsuccessful
     */
    @Test(expected = StorageException.class)
    public void testListDifferentUsersPrivateAccess() throws Exception {
        mobileClient.signIn(userOne.getUsername(), userTwo.getPassword());
        listOptions = StorageListOptions.builder()
                .accessLevel(StorageAccessLevel.PRIVATE)
                .targetIdentityId(userTwo.getIdentityId())
                .build();
        StorageListResult result = storage.list(TEST_DIR_NAME, listOptions);
        assertListContainsUploadedFile(result);
    }

    private static void uploadTestFiles() throws Exception {
        // Create a temporary file to upload
        File uploadFile = new RandomTempFile(UPLOAD_SIZE);
        String uploadName = uploadFile.getName();
        String uploadPath = uploadFile.getAbsolutePath();
        uploadKey = TEST_DIR_NAME + "/" + uploadName;
        StorageUploadFileOptions options;

        // Upload as GUEST
        mobileClient.signOut();
        options = StorageUploadFileOptions.builder()
                .accessLevel(StorageAccessLevel.PUBLIC)
                .build();
        storage.uploadFile(uploadKey, uploadFile, options);

        // Upload as user one
        mobileClient.signOut();
        mobileClient.signIn(userOne.getUsername(), userOne.getPassword());
        options = StorageUploadFileOptions.builder()
                .accessLevel(StorageAccessLevel.PROTECTED)
                .build();
        storage.uploadFile(uploadKey, uploadFile, options);
        options = StorageUploadFileOptions.builder()
                .accessLevel(StorageAccessLevel.PRIVATE)
                .build();
        storage.uploadFile(uploadKey, uploadFile, options);

        // Upload as user two
        mobileClient.signOut();
        mobileClient.signIn(userTwo.getUsername(), userTwo.getPassword());
        options = StorageUploadFileOptions.builder()
                .accessLevel(StorageAccessLevel.PROTECTED)
                .build();
        storage.uploadFile(uploadKey, uploadFile, options);
        options = StorageUploadFileOptions.builder()
                .accessLevel(StorageAccessLevel.PRIVATE)
                .build();
        storage.uploadFile(uploadKey, uploadFile, options);
    }
}
