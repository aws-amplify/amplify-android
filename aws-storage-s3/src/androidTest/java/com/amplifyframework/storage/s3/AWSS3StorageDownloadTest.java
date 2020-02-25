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
import com.amplifyframework.core.async.Cancelable;
import com.amplifyframework.core.async.Resumable;
import com.amplifyframework.hub.HubChannel;
import com.amplifyframework.hub.HubEvent;
import com.amplifyframework.hub.SubscriptionToken;
import com.amplifyframework.storage.StorageAccessLevel;
import com.amplifyframework.storage.StorageException;
import com.amplifyframework.storage.operation.StorageDownloadFileOperation;
import com.amplifyframework.storage.options.StorageDownloadFileOptions;
import com.amplifyframework.testutils.FileAssert;
import com.amplifyframework.testutils.RandomTempFile;
import com.amplifyframework.testutils.Sleep;

import com.amazonaws.mobileconnectors.s3.transferutility.TransferState;
import com.amplifyframework.testutils.SynchronousAWSMobileClient;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.File;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

/**
 * Instrumentation test for operational work on download.
 */
public final class AWSS3StorageDownloadTest extends StorageInstrumentationTestBase {

    // TODO: This is a temporary work-around to resolve a race-condition
    // TransferUtility crashes if a transfer is paused and instantly resumed.
    private static final int SLEEP_DURATION_IN_MILLISECONDS = 300;

    private static final StorageAccessLevel DEFAULT_ACCESS_LEVEL = StorageAccessLevel.PUBLIC;

    private static final long LARGE_FILE_SIZE = 10 * 1024 * 1024L; // 10 MB
    private static final long SMALL_FILE_SIZE = 100L;

    private static final String LARGE_FILE_NAME = "large-test-" + System.currentTimeMillis();
    private static final String SMALL_FILE_NAME = "small-test-" + System.currentTimeMillis();

    private static File largeFile;
    private static File smallFile;

    private final String destination = "download-test-" + System.currentTimeMillis();
    private File downloadFile;
    private StorageDownloadFileOptions options;

    private Set<SubscriptionToken> subscriptions;

    /**
     * Upload required resources ahead of time.
     * @throws Exception if file creation or upload fails
     */
    @BeforeClass
    public static void setUpBeforeClass() throws Exception {
        // Randomly write objects to upload
        largeFile = new RandomTempFile(LARGE_FILE_NAME, LARGE_FILE_SIZE);
        smallFile = new RandomTempFile(SMALL_FILE_NAME, SMALL_FILE_SIZE);

        // Upload to bucket and confirm successful upload
        latchedUploadAndConfirm(largeFile, DEFAULT_ACCESS_LEVEL, getIdentityId());
        latchedUploadAndConfirm(smallFile, DEFAULT_ACCESS_LEVEL, getIdentityId());
    }

    /**
     * Cleans up the S3 bucket for files that were uploaded
     * during testing processes.
     */
    @AfterClass
    public static void cleanUp() throws SynchronousAWSMobileClient.MobileClientException {
        String largeFileKey = getS3Key(DEFAULT_ACCESS_LEVEL, LARGE_FILE_NAME);
        String smallFileKey = getS3Key(DEFAULT_ACCESS_LEVEL, SMALL_FILE_NAME);
        cleanUpS3Object(largeFileKey);
        cleanUpS3Object(smallFileKey);
    }

    /**
     * Sets up the required files for testing transfers.
     * @throws Exception if fails to create temp files
     */
    @Before
    public void setUp() throws Exception {
        // Set up file to download test-object to
        downloadFile = new RandomTempFile(destination);

        // Always interact with PUBLIC access for consistency
        options = (StorageDownloadFileOptions) StorageDownloadFileOptions.builder()
                .accessLevel(DEFAULT_ACCESS_LEVEL)
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
     * Tests that small file (single-part) downloads successfully.
     *
     * @throws StorageException if uploading test-file fails
     */
    @Test
    public void testDownloadSmallFile() throws StorageException {
        synchronousStorage().downloadFile(SMALL_FILE_NAME, downloadFile.getAbsolutePath(), options);
        FileAssert.assertEquals(smallFile, downloadFile);
    }

    /**
     * Tests that large file (multi-part) downloads successfully.
     *
     * @throws StorageException if uploading test-file fails
     */
    @Test
    public void testDownloadLargeFile() throws StorageException {
        synchronousStorage().downloadFile(LARGE_FILE_NAME, downloadFile.getAbsolutePath(), options);
        FileAssert.assertEquals(largeFile, downloadFile);
    }

    /**
     * Tests that file download operation can be canceled while the
     * transfer hasn't completed yet.
     *
     * @throws Exception if uploading test-file fails, or if download
     *         is not canceled successfully before timeout
     */
    @Test
    @SuppressWarnings("unchecked")
    public void testDownloadFileIsCancelable() throws Exception {
        final CountDownLatch canceled = new CountDownLatch(1);
        final AtomicReference<Cancelable> opContainer = new AtomicReference<>();
        final AtomicReference<Throwable> errorContainer = new AtomicReference<>();

        // Listen to Hub events to cancel when progress has been made
        SubscriptionToken progressSubscription = Amplify.Hub.subscribe(HubChannel.STORAGE, hubEvent -> {
            if ("downloadProgress".equals(hubEvent.getName())) {
                HubEvent<Float> progressEvent = (HubEvent<Float>) hubEvent;
                Float progress = progressEvent.getData();
                if (progress != null && progress > 0) {
                    opContainer.get().cancel();
                }
            }
        });
        subscriptions.add(progressSubscription);

        // Listen to Hub events for cancel
        SubscriptionToken cancelSubscription = Amplify.Hub.subscribe(HubChannel.STORAGE, hubEvent -> {
            if ("downloadState".equals(hubEvent.getName())) {
                HubEvent<String> stateEvent = (HubEvent<String>) hubEvent;
                TransferState state = TransferState.getState(stateEvent.getData());
                if (TransferState.CANCELED.equals(state)) {
                    canceled.countDown();
                }
            }
        });
        subscriptions.add(cancelSubscription);

        // Begin downloading large file
        StorageDownloadFileOperation<?> op = Amplify.Storage.downloadFile(
            LARGE_FILE_NAME,
            downloadFile.getAbsolutePath(),
            options,
            onResult -> errorContainer.set(new RuntimeException("Upload completed without canceling.")),
            onError -> errorContainer.set(onError.getCause())
        );
        opContainer.set(op);

        // Assert that the required conditions have been met
        assertTrue(canceled.await(EXTENDED_TIMEOUT_IN_SECONDS, TimeUnit.SECONDS));
        assertNull(errorContainer.get());
    }

    /**
     * Tests that file download operation can be paused and resumed
     * while the transfer hasn't completed yet.
     *
     * @throws Exception if uploading test-file fails, or if download
     *         is not paused, resumed, and completed successfully
     *         before timeout
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
            if ("downloadProgress".equals(hubEvent.getName())) {
                HubEvent<Float> progressEvent = (HubEvent<Float>) hubEvent;
                Float progress = progressEvent.getData();
                // Pause if progress is non-zero amount
                if (progress != null && progress > 0) {
                    opContainer.get().pause();
                }
            }
        });
        subscriptions.add(pauseToken);

        // Listen to Hub events to resume when operation has been paused
        SubscriptionToken resumeToken = Amplify.Hub.subscribe(HubChannel.STORAGE, hubEvent -> {
            if ("downloadState".equals(hubEvent.getName())) {
                HubEvent<String> stateEvent = (HubEvent<String>) hubEvent;
                TransferState state = TransferState.getState(stateEvent.getData());
                // Resume if transfer was paused
                if (TransferState.PAUSED.equals(state)) {
                    Amplify.Hub.unsubscribe(pauseToken); // So it doesn't pause on each progress report
                    // Wait briefly for transfer to pause successfully
                    Sleep.milliseconds(SLEEP_DURATION_IN_MILLISECONDS); // TODO: This is kind of gross
                    resumed.countDown();
                    opContainer.get().resume();
                }
            }
        });
        subscriptions.add(resumeToken);

        // Begin downloading large file
        StorageDownloadFileOperation<?> op = Amplify.Storage.downloadFile(
            LARGE_FILE_NAME,
            downloadFile.getAbsolutePath(),
            options,
            onResult -> completed.countDown(),
            onError -> errorContainer.set(onError.getCause())
        );
        opContainer.set(op);

        // Assert that all the required conditions have been met
        assertTrue(resumed.await(EXTENDED_TIMEOUT_IN_SECONDS, TimeUnit.SECONDS));
        assertTrue(completed.await(EXTENDED_TIMEOUT_IN_SECONDS, TimeUnit.SECONDS));
        FileAssert.assertEquals(largeFile, downloadFile);
        assertNull(errorContainer.get());
    }
}
