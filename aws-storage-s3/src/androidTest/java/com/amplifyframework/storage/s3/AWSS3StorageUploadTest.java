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
import com.amplifyframework.storage.StorageChannelEventName;
import com.amplifyframework.storage.operation.StorageUploadFileOperation;
import com.amplifyframework.storage.options.StorageUploadFileOptions;
import com.amplifyframework.testutils.RandomTempFile;
import com.amplifyframework.testutils.Sleep;
import com.amplifyframework.testutils.SynchronousAWSMobileClient;

import com.amazonaws.mobileconnectors.s3.transferutility.TransferState;
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
 * Instrumentation test for operational work on upload.
 */
public final class AWSS3StorageUploadTest extends StorageInstrumentationTestBase {

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

    private StorageUploadFileOptions options;

    private Set<SubscriptionToken> subscriptions;

    /**
     * Create temp files to upload ahead of time.
     * @throws Exception if file creation or upload fails
     */
    @BeforeClass
    public static void setUpBeforeClass() throws Exception {
        // Randomly write objects to upload
        largeFile = new RandomTempFile(LARGE_FILE_NAME, LARGE_FILE_SIZE);
        smallFile = new RandomTempFile(SMALL_FILE_NAME, SMALL_FILE_SIZE);
    }

    /**
     * Cleans up the S3 bucket for files that were uploaded
     * during testing processes.
     * @throws SynchronousAWSMobileClient.MobileClientException from failure to obtain
     *         a valid identity ID from mobile client
     */
    @AfterClass
    public static void cleanUp() throws SynchronousAWSMobileClient.MobileClientException {
        String largeFileKey = getS3Key(DEFAULT_ACCESS_LEVEL, LARGE_FILE_NAME);
        String smallFileKey = getS3Key(DEFAULT_ACCESS_LEVEL, SMALL_FILE_NAME);
        cleanUpS3Object(largeFileKey);
        cleanUpS3Object(smallFileKey);
    }

    /**
     * Sets up the options to use for transfer.
     */
    @Before
    public void setUp() {
        // Always interact with PUBLIC access for consistency
        options = (StorageUploadFileOptions) StorageUploadFileOptions.builder()
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
     * Tests that small file (single-part) uploads successfully.
     *
     * @throws Exception if upload fails
     */
    @Test
    public void testUploadSmallFile() throws Exception {
        latchedUploadAndConfirm(smallFile, DEFAULT_ACCESS_LEVEL, getIdentityId());
    }

    /**
     * Tests that large file (multi-part) uploads successfully.
     *
     * @throws Exception if upload fails
     */
    @Test
    public void testUploadLargeFile() throws Exception {
        latchedUploadAndConfirm(largeFile, DEFAULT_ACCESS_LEVEL, getIdentityId());
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

        // Begin uploading large file
        StorageUploadFileOperation<?> op = Amplify.Storage.uploadFile(
            largeFile.getName(),
            largeFile.getAbsolutePath(),
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
                    Amplify.Hub.unsubscribe(pauseToken); // So it doesn't pause on each progress report
                    // Wait briefly for transfer to pause successfully
                    Sleep.milliseconds(SLEEP_DURATION_IN_MILLISECONDS); // TODO: This is kind of gross
                    opContainer.get().resume();
                    resumed.countDown();
                }
            }
        });
        subscriptions.add(resumeToken);

        // Begin uploading large file
        StorageUploadFileOperation<?> op = Amplify.Storage.uploadFile(
            largeFile.getName(),
            largeFile.getAbsolutePath(),
            options,
            onResult -> completed.countDown(),
            onError -> errorContainer.set(onError.getCause())
        );
        opContainer.set(op);

        // Assert that all the required conditions have been met
        assertTrue(resumed.await(EXTENDED_TIMEOUT_IN_SECONDS, TimeUnit.SECONDS));
        assertTrue(completed.await(EXTENDED_TIMEOUT_IN_SECONDS, TimeUnit.SECONDS));
        assertS3ObjectExists(getS3Key(DEFAULT_ACCESS_LEVEL, largeFile.getName()));
        assertNull(errorContainer.get());
    }
}
