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
import com.amplifyframework.storage.operation.StorageDownloadFileOperation;
import com.amplifyframework.storage.options.StorageDownloadFileOptions;
import com.amplifyframework.storage.result.StorageDownloadFileResult;
import com.amplifyframework.testutils.Await;

import com.amazonaws.mobileconnectors.s3.transferutility.TransferState;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.File;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

/**
 * Instrumentation test for operational work on download.
 */
@SuppressWarnings("Indentation") // Doesn't seem to like lambda indentation
public final class AWSS3StorageDownloadTest extends StorageInstrumentationTestBase {

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

    /**
     * Upload required resources ahead of time.
     * @throws Exception if file creation or upload fails
     */
    @BeforeClass
    public static void setUpBeforeClass() throws Exception {
        // Randomly write objects to upload
        largeFile = createTempFile(LARGE_FILE_NAME, LARGE_FILE_SIZE);
        smallFile = createTempFile(SMALL_FILE_NAME, SMALL_FILE_SIZE);

        // Upload to bucket and confirm successful upload
        latchedUploadAndConfirm(largeFile, DEFAULT_ACCESS_LEVEL, getIdentityId());
        latchedUploadAndConfirm(smallFile, DEFAULT_ACCESS_LEVEL, getIdentityId());
    }

    /**
     * Cleans up the S3 bucket for files that were uploaded
     * during testing processes.
     */
    @AfterClass
    public static void cleanUp() {
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
        downloadFile = createTempFile(destination);

        // Always interact with PUBLIC access for consistency
        options = StorageDownloadFileOptions.builder()
                .accessLevel(DEFAULT_ACCESS_LEVEL)
                .build();
    }

    /**
     * Tests that small file (single-part) downloads successfully.
     *
     * @throws Exception if uploading test-file fails
     */
    @Test
    public void testDownloadSmallFile() throws Exception {
        StorageDownloadFileResult result = Await.<StorageDownloadFileResult, StorageException>result(
                (onResult, onError) ->
                        Amplify.Storage.downloadFile(
                                SMALL_FILE_NAME,
                                downloadFile.getAbsolutePath(),
                                options,
                                onResult,
                                onError
                        )
        );
        assertEquals(downloadFile, result.getFile());
        TestUtils.assertFileEqualsFile(smallFile, downloadFile);
    }

    /**
     * Tests that large file (multi-part) downloads successfully.
     *
     * @throws Exception if uploading test-file fails
     */
    @Test
    public void testDownloadLargeFile() throws Exception {
        StorageDownloadFileResult result = Await.<StorageDownloadFileResult, StorageException>result(
                (onResult, onError) ->
                        Amplify.Storage.downloadFile(
                                LARGE_FILE_NAME,
                                downloadFile.getAbsolutePath(),
                                options,
                                onResult,
                                onError
                        )
        );
        assertEquals(downloadFile, result.getFile());
        TestUtils.assertFileEqualsFile(largeFile, downloadFile);
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
        final CountDownLatch latch = new CountDownLatch(1);
        // Begin downloading large file
        StorageDownloadFileOperation<?> op = Amplify.Storage.downloadFile(
                LARGE_FILE_NAME,
                downloadFile.getAbsolutePath(),
                options,
                onResult -> fail("Download finished before being successfully cancelled."),
                onError -> fail("Download failed for a different reason.")
        );

        // Listen to Hub events to cancel when progress has been made
        Amplify.Hub.subscribe(HubChannel.STORAGE, hubEvent -> {
            if ("downloadProgress".equals(hubEvent.getName())) {
                HubEvent<Float> progressEvent = (HubEvent<Float>) hubEvent;
                Float progress = progressEvent.getData();
                if (progress != null && progress > 0) {
                    op.cancel();
                }
            }
        });

        // Listen to Hub events for cancel
        Amplify.Hub.subscribe(HubChannel.STORAGE, hubEvent -> {
            if ("downloadState".equals(hubEvent.getName())) {
                HubEvent<String> stateEvent = (HubEvent<String>) hubEvent;
                TransferState state = TransferState.getState(stateEvent.getData());
                if (TransferState.CANCELED.equals(state)) {
                    latch.countDown();
                }
            }
        });
        latch.await(DEFAULT_TIMEOUT_IN_SECONDS, TimeUnit.SECONDS);
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

        // Begin downloading large file
        StorageDownloadFileOperation<?> op = Amplify.Storage.downloadFile(
                LARGE_FILE_NAME,
                downloadFile.getAbsolutePath(),
                options,
                onResult -> completed.countDown(),
                onError -> fail("Download is not successful.")
        );

        // Listen to Hub events to pause when progress has been made
        Amplify.Hub.subscribe(HubChannel.STORAGE, hubEvent -> {
            if ("downloadProgress".equals(hubEvent.getName())) {
                HubEvent<Float> progressEvent = (HubEvent<Float>) hubEvent;
                Float progress = progressEvent.getData();
                if (progress != null && progress > 0) {
                    op.pause();
                }
            }
        });

        // Listen to Hub events to resume when operation has been paused
        Amplify.Hub.subscribe(HubChannel.STORAGE, hubEvent -> {
            if ("downloadState".equals(hubEvent.getName())) {
                HubEvent<String> stateEvent = (HubEvent<String>) hubEvent;
                TransferState state = TransferState.getState(stateEvent.getData());
                if (TransferState.PAUSED.equals(state)) {
                    op.resume();
                    resumed.countDown();
                }
            }
        });
        resumed.await(DEFAULT_TIMEOUT_IN_SECONDS, TimeUnit.SECONDS);
        completed.await(DEFAULT_TIMEOUT_IN_SECONDS, TimeUnit.SECONDS);
    }
}
