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
import com.amplifyframework.hub.HubChannel;
import com.amplifyframework.hub.HubEvent;
import com.amplifyframework.storage.StorageAccessLevel;
import com.amplifyframework.storage.StorageException;
import com.amplifyframework.storage.operation.StorageUploadFileOperation;
import com.amplifyframework.storage.options.StorageUploadFileOptions;
import com.amplifyframework.storage.result.StorageUploadFileResult;
import com.amplifyframework.testutils.Await;

import com.amazonaws.mobileconnectors.s3.transferutility.TransferState;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

/**
 * Instrumentation test for operational work on upload.
 */
@SuppressWarnings("Indentation") // Doesn't seem to like lambda indentation
public final class AWSS3StorageUploadTest extends StorageInstrumentationTestBase {

    private static final StorageAccessLevel DEFAULT_ACCESS_LEVEL = StorageAccessLevel.PUBLIC;
    private static final long DEFAULT_TIMEOUT_IN_SECONDS = 10;

    private static final long LARGE_FILE_SIZE = 10 * 1024 * 1024L; // 10 MB
    private static final long SMALL_FILE_SIZE = 100L;

    private final String largeFileName = "large-test-" + System.currentTimeMillis();
    private final String smallFileName = "small-test-" + System.currentTimeMillis();

    private File largeFile;
    private File smallFile;

    private StorageUploadFileOptions options;
    private String largeFileKey;
    private String smallFileKey;

    /**
     * Sets up the required files for testing transfers.
     * @throws Exception if fails to create temp files
     */
    @Before
    public void setUp() throws Exception {
        // Randomly write objects to upload
        largeFile = createTempFile(largeFileName, LARGE_FILE_SIZE);
        smallFile = createTempFile(smallFileName, SMALL_FILE_SIZE);

        // Always interact with PUBLIC access for consistency
        options = StorageUploadFileOptions.builder()
                .accessLevel(DEFAULT_ACCESS_LEVEL)
                .build();

        largeFileKey = getS3Key(DEFAULT_ACCESS_LEVEL, largeFileName);
        smallFileKey = getS3Key(DEFAULT_ACCESS_LEVEL, smallFileName);
    }

    /**
     * Cleans up the S3 bucket for files that were uploaded
     * during testing processes.
     */
    @After
    public void cleanUp() {
        cleanUpS3Object(largeFileKey);
        cleanUpS3Object(smallFileKey);
    }

    /**
     * Tests that small file (single-part) uploads successfully.
     *
     * @throws StorageException if upload fails
     */
    @Test
    public void testUploadSmallFile() throws StorageException {
        StorageUploadFileResult result = Await.<StorageUploadFileResult, StorageException>result(
                (onResult, onError) ->
                        Amplify.Storage.uploadFile(
                                smallFileName,
                                smallFile.getAbsolutePath(),
                                onResult,
                                onError
                        )
        );
        assertEquals(smallFileName, result.getKey());
        assertS3ObjectExists(smallFileKey);
    }

    /**
     * Tests that large file (multi-part) uploads successfully.
     *
     * @throws Exception if upload fails
     */
    @Test
    public void testUploadLargeFile() throws Exception {
        final CountDownLatch completed = new CountDownLatch(1);
        Amplify.Storage.uploadFile(
                largeFileName,
                largeFile.getAbsolutePath(),
                onResult -> {
                    assertEquals(largeFileName, onResult.getKey());
                    assertS3ObjectExists(largeFileKey);
                    completed.countDown();
                },
                onError -> fail("Upload was not successful.")
        );
        completed.await(DEFAULT_TIMEOUT_IN_SECONDS, TimeUnit.SECONDS);
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

        // Begin uploading large file
        StorageUploadFileOperation<?> op = Amplify.Storage.uploadFile(
                largeFileName,
                largeFile.getAbsolutePath(),
                options,
                onResult -> fail("Upload finished before being successfully cancelled."),
                onError -> fail("Upload failed for a different reason.")
        );

        // Listen to Hub events to cancel when progress has been made
        Amplify.Hub.subscribe(HubChannel.STORAGE, hubEvent -> {
            if ("uploadProgress".equals(hubEvent.getName())) {
                HubEvent<Float> progressEvent = (HubEvent<Float>) hubEvent;
                Float progress = progressEvent.getData();
                if (progress != null && progress > 0) {
                    op.cancel();
                }
            }
        });

        // Listen to Hub events for cancel
        Amplify.Hub.subscribe(HubChannel.STORAGE, hubEvent -> {
            if ("uploadState".equals(hubEvent.getName())) {
                HubEvent<String> stateEvent = (HubEvent<String>) hubEvent;
                TransferState state = TransferState.getState(stateEvent.getData());
                if (TransferState.CANCELED.equals(state)) {
                    canceled.countDown();
                }
            }
        });
        canceled.await(DEFAULT_TIMEOUT_IN_SECONDS, TimeUnit.SECONDS);
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

        // Begin uploading large file
        StorageUploadFileOperation<?> op = Amplify.Storage.uploadFile(
                largeFileName,
                largeFile.getAbsolutePath(),
                options,
                onResult -> completed.countDown(),
                onError -> fail("Encountered an error during upload.")
        );

        // Listen to Hub events to pause when progress has been made
        Amplify.Hub.subscribe(HubChannel.STORAGE, hubEvent -> {
            if ("uploadProgress".equals(hubEvent.getName())) {
                HubEvent<Float> progressEvent = (HubEvent<Float>) hubEvent;
                Float progress = progressEvent.getData();
                if (progress != null && progress > 0) {
                    op.pause();
                }
            }
        });

        // Listen to Hub events to resume when operation has been paused
        Amplify.Hub.subscribe(HubChannel.STORAGE, hubEvent -> {
            if ("uploadState".equals(hubEvent.getName())) {
                HubEvent<String> stateEvent = (HubEvent<String>) hubEvent;
                TransferState state = TransferState.getState(stateEvent.getData());
                if (TransferState.PAUSED.equals(state)) {
                    resumed.countDown();
                    op.resume();
                }
            }
        });
        resumed.await(DEFAULT_TIMEOUT_IN_SECONDS, TimeUnit.SECONDS);
        completed.await(DEFAULT_TIMEOUT_IN_SECONDS, TimeUnit.SECONDS);
    }
}
