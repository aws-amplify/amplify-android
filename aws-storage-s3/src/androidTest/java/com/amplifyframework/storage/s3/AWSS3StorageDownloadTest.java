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
import com.amplifyframework.storage.options.StorageUploadFileOptions;
import com.amplifyframework.storage.result.StorageDownloadFileResult;
import com.amplifyframework.testutils.Await;

import com.amazonaws.mobileconnectors.s3.transferutility.TransferState;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static junit.framework.TestCase.assertEquals;
import static org.junit.Assert.fail;

/**
 * Instrumentation test for operational work on download.
 */
@SuppressWarnings("Indentation") // Doesn't seem to like lambda indentation
public final class AWSS3StorageDownloadTest extends StorageInstrumentationTestBase {

    private static final StorageAccessLevel DEFAULT_ACCESS_LEVEL = StorageAccessLevel.PUBLIC;
    private static final long DEFAULT_TIMEOUT_IN_SECONDS = 10;

    private static final long LARGE_FILE_SIZE = 10 * 1024 * 1024L; // 10 MB
    private static final long SMALL_FILE_SIZE = 100L;

    private final String largeFileName = "large-test-" + System.currentTimeMillis();
    private final String smallFileName = "small-test-" + System.currentTimeMillis();

    private File largeFile;
    private File smallFile;

    private final String destination = "download-test-" + System.currentTimeMillis();
    private File downloadFile;
    private StorageDownloadFileOptions options;

    /**
     * Sets up the required files for testing transfers.
     * @throws Exception if fails to create temp files
     */
    @Before
    public void setUp() throws Exception {
        // Set up file to download test-object to
        downloadFile = createTempFile(destination);

        // Randomly write objects to upload
        largeFile = createTempFile(largeFileName, LARGE_FILE_SIZE);
        smallFile = createTempFile(smallFileName, SMALL_FILE_SIZE);

        // Always interact with PUBLIC access for consistency
        options = StorageDownloadFileOptions.builder()
                .accessLevel(DEFAULT_ACCESS_LEVEL)
                .build();
    }

    /**
     * Cleans up the S3 bucket for files that were uploaded
     * during testing processes.
     */
    @After
    public void cleanUp() {
        String largeFileKey = getS3Key(DEFAULT_ACCESS_LEVEL, largeFileName);
        String smallFileKey = getS3Key(DEFAULT_ACCESS_LEVEL, smallFileName);
        cleanUpS3Object(largeFileKey);
        cleanUpS3Object(smallFileKey);
    }

    /**
     * Tests that small file (single-part) downloads successfully.
     *
     * @throws Exception if uploading test-file fails
     */
    @Test
    public void testDownloadSmallFile() throws Exception {
        latchedUpload(smallFile);

        StorageDownloadFileResult result = Await.<StorageDownloadFileResult, StorageException>result(
                (onResult, onError) ->
                        Amplify.Storage.downloadFile(
                                smallFileName,
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
        latchedUpload(largeFile);

        StorageDownloadFileResult result = Await.<StorageDownloadFileResult, StorageException>result(
                (onResult, onError) ->
                        Amplify.Storage.downloadFile(
                                largeFileName,
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
        latchedUpload(largeFile);

        // Begin downloading large file
        StorageDownloadFileOperation<?> op = Amplify.Storage.downloadFile(
                largeFileName,
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
        latchedUpload(largeFile);

        // Begin downloading large file
        StorageDownloadFileOperation<?> op = Amplify.Storage.downloadFile(
                largeFileName,
                downloadFile.getAbsolutePath(),
                options,
                onResult -> completed.countDown(),
                onError -> fail("Encountered an error during download.")
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
                    resumed.countDown();
                    op.resume();
                }
            }
        });
        resumed.await(DEFAULT_TIMEOUT_IN_SECONDS, TimeUnit.SECONDS);
        completed.await(DEFAULT_TIMEOUT_IN_SECONDS, TimeUnit.SECONDS);
    }

    private void latchedUpload(File file) throws Exception {
        final CountDownLatch completed = new CountDownLatch(1);
        StorageUploadFileOptions options = StorageUploadFileOptions.builder()
                .accessLevel(DEFAULT_ACCESS_LEVEL)
                .build();
        Amplify.Storage.uploadFile(
                file.getName(),
                file.getAbsolutePath(),
                options,
                onResult -> completed.countDown(),
                onError -> fail("Upload was not successful.")
        );
        completed.await(DEFAULT_TIMEOUT_IN_SECONDS, TimeUnit.SECONDS);
        String s3key = getS3Key(DEFAULT_ACCESS_LEVEL, file.getName());
        assertS3ObjectExists(s3key);
    }
}
