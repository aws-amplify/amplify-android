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

import com.amplifyframework.core.Amplify;
import com.amplifyframework.core.async.Cancelable;
import com.amplifyframework.core.async.Resumable;
import com.amplifyframework.hub.HubChannel;
import com.amplifyframework.hub.HubEvent;
import com.amplifyframework.hub.SubscriptionToken;
import com.amplifyframework.storage.StorageAccessLevel;
import com.amplifyframework.storage.StorageCategory;
import com.amplifyframework.storage.StorageChannelEventName;
import com.amplifyframework.storage.operation.StorageUploadFileOperation;
import com.amplifyframework.storage.options.StorageUploadFileOptions;
import com.amplifyframework.storage.s3.test.R;
import com.amplifyframework.testutils.random.RandomTempFile;
import com.amplifyframework.testutils.sync.SynchronousMobileClient;
import com.amplifyframework.testutils.sync.SynchronousStorage;

import com.amazonaws.mobileconnectors.s3.transferutility.TransferState;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;

import java.io.File;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static androidx.test.core.app.ApplicationProvider.getApplicationContext;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

/**
 * Instrumentation test for operational work on upload.
 */
@Ignore("Contains test which either hang themselves, or hang the suite overall.")
public final class AWSS3StorageUploadTest {
    private static final long EXTENDED_TIMEOUT_MS = TimeUnit.SECONDS.toMillis(20);

    private static final StorageAccessLevel TESTING_ACCESS_LEVEL = StorageAccessLevel.PUBLIC;
    private static final long LARGE_FILE_SIZE = 10 * 1024 * 1024L; // 10 MB
    private static final long SMALL_FILE_SIZE = 100L;

    private static StorageCategory storageCategory;
    private static SynchronousStorage synchronousStorage;

    private StorageUploadFileOptions options;
    private Set<SubscriptionToken> subscriptions;

    /**
     * Initialize mobile client and configure the storage.
     * @throws Exception if mobile client initialization fails
     */
    @BeforeClass
    public static void setUpOnce() throws Exception {
        Context context = getApplicationContext();

        // Init auth stuff
        SynchronousMobileClient.instance().initialize();

        // Get a handle to storage
        storageCategory = TestStorageCategory.create(context, R.raw.amplifyconfiguration);
        synchronousStorage = SynchronousStorage.delegatingTo(storageCategory);
    }

    /**
     * Sets up the options to use for transfer.
     */
    @Before
    public void setUp() {
        // Always interact with PUBLIC access for consistency
        options = StorageUploadFileOptions.builder()
                .accessLevel(TESTING_ACCESS_LEVEL)
                .build();

        // Create a set to remember all the subscriptions
        subscriptions = new HashSet<>();
    }

    /**
     * Unsubscribe from everything after each test.
     */
    @After
    public void unsubscribe() {
        // Unsubscribe from everything
        for (SubscriptionToken token : subscriptions) {
            Amplify.Hub.unsubscribe(token);
        }
    }

    /**
     * Tests that small file (single-part) uploads successfully.
     *
     * @throws Exception if upload fails
     */
    @Test
    public void testUploadSmallFile() throws Exception {
        File uploadFile = new RandomTempFile(SMALL_FILE_SIZE);
        String fileName = uploadFile.getName();
        synchronousStorage.uploadFile(fileName, uploadFile, options);
    }

    /**
     * Tests that large file (multi-part) uploads successfully.
     *
     * @throws Exception if upload fails
     */
    @Test
    public void testUploadLargeFile() throws Exception {
        File uploadFile = new RandomTempFile(LARGE_FILE_SIZE);
        String fileName = uploadFile.getName();
        synchronousStorage.uploadFile(fileName, uploadFile, options, EXTENDED_TIMEOUT_MS);
    }

    /**
     * Tests that file upload operation can be canceled while the
     * transfer hasn't completed yet.
     *
     * @throws Exception if upload is not canceled successfully
     *         before timeout
     */
    @Test
    @SuppressWarnings("unchecked")
    public void testUploadFileIsCancelable() throws Exception {
        final CountDownLatch canceled = new CountDownLatch(1);
        final AtomicReference<Cancelable> opContainer = new AtomicReference<>();
        final AtomicReference<Throwable> errorContainer = new AtomicReference<>();

        // Create a file large enough that transfer won't finish before being canceled
        File uploadFile = new RandomTempFile(LARGE_FILE_SIZE);

        // Listen to Hub events to cancel when progress has been made
        SubscriptionToken progressToken = Amplify.Hub.subscribe(HubChannel.STORAGE, hubEvent -> {
            if (StorageChannelEventName.UPLOAD_PROGRESS.toString().equals(hubEvent.getName())) {
                HubEvent<Float> progressEvent = (HubEvent<Float>) hubEvent;
                Float progress = progressEvent.getData();
                if (progress != null && progress > 0) {
                    opContainer.get().cancel();
                }
            }
        });
        subscriptions.add(progressToken);

        // Listen to Hub events for cancel
        SubscriptionToken cancelToken = Amplify.Hub.subscribe(HubChannel.STORAGE, hubEvent -> {
            if (StorageChannelEventName.UPLOAD_STATE.toString().equals(hubEvent.getName())) {
                HubEvent<String> stateEvent = (HubEvent<String>) hubEvent;
                TransferState state = TransferState.getState(stateEvent.getData());
                if (TransferState.CANCELED.equals(state)) {
                    canceled.countDown();
                }
            }
        });
        subscriptions.add(cancelToken);

        // Begin uploading a large file
        StorageUploadFileOperation<?> op = storageCategory.uploadFile(
            uploadFile.getName(),
            uploadFile,
            options,
            onResult -> errorContainer.set(new RuntimeException("Upload completed without canceling.")),
            errorContainer::set
        );
        opContainer.set(op);

        // Assert that the required conditions have been met
        assertTrue(canceled.await(EXTENDED_TIMEOUT_MS, TimeUnit.MILLISECONDS));
        assertNull(errorContainer.get());
    }

    /**
     * Tests that file upload operation can be paused and resumed
     * while the transfer hasn't completed yet.
     *
     * @throws Exception if upload is not paused, resumed, and
     *         completed successfully before timeout
     */
    @Test
    @SuppressWarnings("unchecked")
    public void testUploadFileIsResumable() throws Exception {
        final CountDownLatch completed = new CountDownLatch(1);
        final CountDownLatch resumed = new CountDownLatch(1);
        final AtomicReference<Resumable> opContainer = new AtomicReference<>();
        final AtomicReference<Throwable> errorContainer = new AtomicReference<>();

        // Create a file large enough that transfer won't finish before being paused
        File uploadFile = new RandomTempFile(LARGE_FILE_SIZE);

        // Listen to Hub events to pause when progress has been made
        SubscriptionToken pauseToken = Amplify.Hub.subscribe(HubChannel.STORAGE, hubEvent -> {
            if (StorageChannelEventName.UPLOAD_PROGRESS.toString().equals(hubEvent.getName())) {
                HubEvent<Float> progressEvent = (HubEvent<Float>) hubEvent;
                Float progress = progressEvent.getData();
                if (progress != null && progress > 0) {
                    opContainer.get().pause();
                }
            }
        });
        subscriptions.add(pauseToken);

        // Listen to Hub events to resume when operation has been paused
        SubscriptionToken resumeToken = Amplify.Hub.subscribe(HubChannel.STORAGE, hubEvent -> {
            if (StorageChannelEventName.UPLOAD_STATE.toString().equals(hubEvent.getName())) {
                HubEvent<String> stateEvent = (HubEvent<String>) hubEvent;
                TransferState state = TransferState.getState(stateEvent.getData());
                if (TransferState.PAUSED.equals(state)) {
                    // So it doesn't pause on each progress report
                    Amplify.Hub.unsubscribe(pauseToken);
                    opContainer.get().resume();
                    resumed.countDown();
                }
            }
        });
        subscriptions.add(resumeToken);

        // Begin uploading a large file
        StorageUploadFileOperation<?> op = storageCategory.uploadFile(
            uploadFile.getName(),
            uploadFile,
            options,
            onResult -> completed.countDown(),
            errorContainer::set
        );
        opContainer.set(op);

        // Assert that all the required conditions have been met
        assertTrue(resumed.await(EXTENDED_TIMEOUT_MS, TimeUnit.MILLISECONDS));
        assertTrue(completed.await(EXTENDED_TIMEOUT_MS, TimeUnit.MILLISECONDS));
        assertNull(errorContainer.get());
    }
}
