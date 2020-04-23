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
import com.amplifyframework.storage.operation.StorageDownloadFileOperation;
import com.amplifyframework.storage.options.StorageDownloadFileOptions;
import com.amplifyframework.storage.options.StorageUploadFileOptions;
import com.amplifyframework.storage.s3.test.R;
import com.amplifyframework.testutils.FileAssert;
import com.amplifyframework.testutils.random.RandomTempFile;
import com.amplifyframework.testutils.sync.SynchronousMobileClient;
import com.amplifyframework.testutils.sync.SynchronousStorage;

import com.amazonaws.mobileconnectors.s3.transferutility.TransferState;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
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
 * Instrumentation test for operational work on download.
 */
public final class AWSS3StorageDownloadTest {
    private static final long EXTENDED_TIMEOUT_MS = TimeUnit.SECONDS.toMillis(20);

    private static final StorageAccessLevel TESTING_ACCESS_LEVEL = StorageAccessLevel.PUBLIC;
    private static final long LARGE_FILE_SIZE = 10 * 1024 * 1024L; // 10 MB
    private static final long SMALL_FILE_SIZE = 100L;
    private static final String LARGE_FILE_NAME = "large-" + System.currentTimeMillis();
    private static final String SMALL_FILE_NAME = "small-" + System.currentTimeMillis();

    private static File largeFile;
    private static File smallFile;

    private static StorageCategory storageCategory;
    private static SynchronousStorage synchronousStorage;

    private File downloadFile;
    private StorageDownloadFileOptions options;
    private Set<SubscriptionToken> subscriptions;

    /**
     * Initialize mobile client and configure the storage.
     * Upload the test files ahead of time.
     *
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

        // Upload to PUBLIC for consistency
        String key;
        StorageUploadFileOptions uploadOptions = StorageUploadFileOptions.builder()
                .accessLevel(TESTING_ACCESS_LEVEL)
                .build();

        // Upload large test file
        largeFile = new RandomTempFile(LARGE_FILE_NAME, LARGE_FILE_SIZE);
        key = LARGE_FILE_NAME;
        synchronousStorage.uploadFile(key, largeFile, uploadOptions, EXTENDED_TIMEOUT_MS);

        // Upload small test file
        smallFile = new RandomTempFile(SMALL_FILE_NAME, SMALL_FILE_SIZE);
        key = SMALL_FILE_NAME;
        synchronousStorage.uploadFile(key, smallFile, uploadOptions);
    }

    /**
     * Sets up the options to use for transfer.
     *
     * @throws Exception if fails to create random temp file
     */
    @Before
    public void setUp() throws Exception {
        // Always interact with PUBLIC access for consistency
        options = StorageDownloadFileOptions.builder()
                .accessLevel(TESTING_ACCESS_LEVEL)
                .build();

        // Create a set to remember all the subscriptions
        subscriptions = new HashSet<>();

        // Create a file to download to
        downloadFile = new RandomTempFile();
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
     * Tests that small file (single-part) downloads successfully.
     *
     * @throws Exception if download fails
     */
    @Test
    public void testDownloadSmallFile() throws Exception {
        synchronousStorage.downloadFile(SMALL_FILE_NAME, downloadFile, options);
        FileAssert.assertEquals(smallFile, downloadFile);
    }

    /**
     * Tests that large file (multi-part) downloads successfully.
     *
     * @throws Exception if download fails
     */
    @Test
    public void testDownloadLargeFile() throws Exception {
        synchronousStorage.downloadFile(LARGE_FILE_NAME, downloadFile, options, EXTENDED_TIMEOUT_MS);
        FileAssert.assertEquals(largeFile, downloadFile);
    }

    /**
     * Tests that file download operation can be canceled while the
     * transfer hasn't completed yet.
     *
     * @throws Exception if download is not canceled successfully
     *                   before timeout
     */
    @Test
    @SuppressWarnings("unchecked")
    public void testDownloadFileIsCancelable() throws Exception {
        final CountDownLatch canceled = new CountDownLatch(1);
        final AtomicReference<Cancelable> opContainer = new AtomicReference<>();
        final AtomicReference<Throwable> errorContainer = new AtomicReference<>();

        // Listen to Hub events to cancel when progress has been made
        SubscriptionToken progressToken = Amplify.Hub.subscribe(HubChannel.STORAGE, hubEvent -> {
            if (StorageChannelEventName.DOWNLOAD_PROGRESS.toString().equals(hubEvent.getName())) {
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
            if (StorageChannelEventName.DOWNLOAD_STATE.toString().equals(hubEvent.getName())) {
                HubEvent<String> stateEvent = (HubEvent<String>) hubEvent;
                TransferState state = TransferState.getState(stateEvent.getData());
                if (TransferState.CANCELED.equals(state)) {
                    canceled.countDown();
                }
            }
        });
        subscriptions.add(cancelToken);

        // Begin downloading a large file
        StorageDownloadFileOperation<?> op = storageCategory.downloadFile(
            LARGE_FILE_NAME,
            downloadFile,
            options,
            onResult -> errorContainer.set(new RuntimeException("Download completed without canceling.")),
            errorContainer::set
        );
        opContainer.set(op);

        // Assert that the required conditions have been met
        assertTrue(canceled.await(EXTENDED_TIMEOUT_MS, TimeUnit.MILLISECONDS));
        assertNull(errorContainer.get());
    }

    /**
     * Tests that file download operation can be paused and resumed
     * while the transfer hasn't completed yet.
     *
     * @throws Exception if download is not paused, resumed, and
     *                   completed successfully before timeout
     */
    @Test
    @SuppressWarnings("unchecked")
    public void testDownloadFileIsResumable() throws Exception {
        final CountDownLatch completed = new CountDownLatch(1);
        final CountDownLatch resumed = new CountDownLatch(1);
        final AtomicReference<Resumable> opContainer = new AtomicReference<>();
        final AtomicReference<Throwable> errorContainer = new AtomicReference<>();

        // Listen to Hub events to pause when progress has been made
        SubscriptionToken pauseToken = Amplify.Hub.subscribe(HubChannel.STORAGE, hubEvent -> {
            if (StorageChannelEventName.DOWNLOAD_PROGRESS.toString().equals(hubEvent.getName())) {
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
            if (StorageChannelEventName.DOWNLOAD_STATE.toString().equals(hubEvent.getName())) {
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

        // Begin downloading a large file
        StorageDownloadFileOperation<?> op = storageCategory.downloadFile(
            LARGE_FILE_NAME,
            downloadFile,
            options,
            onResult -> completed.countDown(),
            errorContainer::set
        );
        opContainer.set(op);

        // Assert that all the required conditions have been met
        assertTrue(resumed.await(EXTENDED_TIMEOUT_MS, TimeUnit.MILLISECONDS));
        assertTrue(completed.await(EXTENDED_TIMEOUT_MS, TimeUnit.MILLISECONDS));
        assertNull(errorContainer.get());
        FileAssert.assertEquals(largeFile, downloadFile);
    }
}
