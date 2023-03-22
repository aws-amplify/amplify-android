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

import com.amplifyframework.AmplifyException;
import com.amplifyframework.auth.AuthCredentialsProvider;
import com.amplifyframework.storage.ObjectMetadata;
import com.amplifyframework.storage.StorageCategory;
import com.amplifyframework.storage.StorageCategoryConfiguration;
import com.amplifyframework.storage.StorageException;
import com.amplifyframework.storage.StorageItem;
import com.amplifyframework.storage.TransferState;
import com.amplifyframework.storage.result.StorageDownloadFileResult;
import com.amplifyframework.storage.result.StorageGetUrlResult;
import com.amplifyframework.storage.result.StorageListResult;
import com.amplifyframework.storage.result.StorageRemoveResult;
import com.amplifyframework.storage.result.StorageUploadFileResult;
import com.amplifyframework.storage.result.StorageUploadInputStreamResult;
import com.amplifyframework.storage.s3.configuration.AWSS3StoragePluginConfiguration;
import com.amplifyframework.storage.s3.service.AWSS3StorageService;
import com.amplifyframework.storage.s3.service.StorageService;
import com.amplifyframework.storage.s3.transfer.TransferListener;
import com.amplifyframework.storage.s3.transfer.TransferObserver;
import com.amplifyframework.testutils.Await;
import com.amplifyframework.testutils.random.RandomBytes;
import com.amplifyframework.testutils.random.RandomString;
import com.amplifyframework.testutils.random.RandomTempFile;

import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Collections;
import java.util.Date;

import static androidx.test.core.app.ApplicationProvider.getApplicationContext;
import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.anyInt;
import static org.mockito.Mockito.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Test that storage category correctly invokes the methods via
 * AWSS3StoragePlugin.
 */
@RunWith(RobolectricTestRunner.class)
public final class StorageComponentTest {
    private static final long FILE_SIZE = 100L;

    private StorageCategory storage;
    private StorageService storageService;

    /**
     * Sets up Storage category by registering a mock AWSS3StoragePlugin
     * instance to Amplify and configuring.
     *
     * @throws AmplifyException if Amplify fails to configure with mock
     *                          Storage category configuration.
     */
    @Before
    public void setup() throws AmplifyException {
        this.storage = new StorageCategory();
        this.storageService = mock(AWSS3StorageService.class);
        StorageService.Factory storageServiceFactory = (context, region, bucket) -> storageService;
        AuthCredentialsProvider cognitoAuthProvider = mock(AuthCredentialsProvider.class);
        doReturn(RandomString.string()).when(cognitoAuthProvider).getIdentityId(null);
        this.storage.addPlugin(new AWSS3StoragePlugin(storageServiceFactory,
                cognitoAuthProvider, new AWSS3StoragePluginConfiguration.Builder().build())
        );
        this.storage.configure(buildConfiguration(), getApplicationContext());
        this.storage.initialize(getApplicationContext());
    }

    private static StorageCategoryConfiguration buildConfiguration() {
        StorageCategoryConfiguration configuration = new StorageCategoryConfiguration();
        try {
            configuration.populateFromJSON(
                    new JSONObject().put("plugins", new JSONObject()
                            .put("awsS3StoragePlugin", new JSONObject()
                                    .put("region", "us-east-1")
                                    .put("bucket", "hamburger-bucket")))
            );
        } catch (JSONException jsonException) {
            throw new RuntimeException(jsonException);
        }
        return configuration;
    }

    /**
     * Test that calling get URL method from Storage category correctly invokes
     * the registered AWSS3StoragePlugin instance and returns a {@link URL}
     * instance for that download.
     *
     * @throws StorageException when an error is encountered while generating
     *                          URL from storage service
     */
    @Test
    public void testGenerateUrlGetsPresignedUrl() throws StorageException {
        final String fromRemoteKey = RandomString.string();
        final URL urlFromRemoteKey;
        try {
            // URL instance cannot be mocked so just make one
            // https://{random-host}:0/{fromRemoteKey}
            urlFromRemoteKey = new URL(
                    "https",
                    RandomString.string(),
                    0,
                    fromRemoteKey,
                    null
            );
        } catch (MalformedURLException exception) {
            throw new RuntimeException(exception);
        }

        // Allow mock StorageService instance to return a non-null
        // URL instance.
        when(storageService.getPresignedUrl(anyString(), anyInt(), anyBoolean()))
                .thenReturn(urlFromRemoteKey);

        // Let Storage category invoke getUrl on mock Storage Service.
        StorageGetUrlResult result = Await.<StorageGetUrlResult, StorageException>result(
            (onResult, onError) -> storage.getUrl(
                 fromRemoteKey,
                 onResult,
                 onError
             )
        );

        assertEquals(urlFromRemoteKey, result.getUrl());
    }

    /**
     * Test that calling download file method from Storage category correctly
     * invokes the registered AWSS3StoragePlugin instance and returns a
     * {@link StorageDownloadFileResult} with correct file path.
     *
     * @throws Exception when an error is encountered while downloading
     */
    @Test
    public void testDownloadToFileGetsFile() throws Exception {
        final String fromRemoteKey = RandomString.string();
        final File toLocalFile = new RandomTempFile();

        // Since we use a mock StorageService, it will return a null
        // result by default. We need a non-null transfer observer.
        // One option is to mock that, too.
        TransferObserver observer = mock(TransferObserver.class);
        when(storageService.downloadToFile(anyString(), anyString(), any(File.class), anyBoolean()))
                .thenReturn(observer);

        // Since we use a mock TransferObserver, it has no internal logic
        // to know to call back the listener! So, we simulate the success
        // callback, as part of our "happy path" test.
        doAnswer(invocation -> {
            TransferListener listener = invocation.getArgument(0);
            listener.onStateChanged(0, TransferState.COMPLETED, fromRemoteKey);
            return null;
        }).when(observer)
                .setTransferListener(any(TransferListener.class));

        StorageDownloadFileResult result =
                Await.<StorageDownloadFileResult, StorageException>result((onResult, onError) ->
                        storage.downloadFile(
                                fromRemoteKey,
                                toLocalFile,
                                onResult,
                                onError
                        )
                );

        assertEquals(toLocalFile.getAbsolutePath(), result.getFile().toString());
    }

    /**
     * Test that calling download file method from Storage category fails
     * successfully when {@link TransferListener} emits an error.
     *
     * @throws IOException when the temporary file cannot be created
     */
    @Test
    public void testDownloadError() throws IOException {
        final StorageException testError = new StorageException(
                "Test error message",
                "Test recovery message"
        );

        final String fromRemoteKey = RandomString.string();
        final File toLocalFile = new RandomTempFile();

        TransferObserver observer = mock(TransferObserver.class);
        when(storageService.downloadToFile(anyString(), anyString(), any(File.class), anyBoolean()))
                .thenReturn(observer);

        doAnswer(invocation -> {
            TransferListener listener = invocation.getArgument(0);
            listener.onError(0, testError);
            return null;
        }).when(observer)
                .setTransferListener(any(TransferListener.class));

        StorageException error =
                Await.<StorageDownloadFileResult, StorageException>error((onResult, onError) ->
                        storage.downloadFile(
                                fromRemoteKey,
                                toLocalFile,
                                onResult,
                                onError
                        )
                );

        assertEquals(testError, error.getCause());
    }

    /**
     * Test that calling upload file method from Storage category correctly
     * invokes the registered AWSS3StoragePlugin instance and returns a
     * {@link StorageUploadFileResult} with correct remote key.
     *
     * @throws Exception when an error is encountered while uploading
     */
    @Test
    public void testUploadFileGetsKey() throws Exception {
        final String toRemoteKey = RandomString.string();
        final File fromLocalFile = new RandomTempFile(FILE_SIZE);

        TransferObserver observer =
                mock(com.amplifyframework.storage.s3.transfer.TransferObserver.class);
        when(
            storageService.uploadFile(anyString(),
                anyString(),
                any(File.class),
                any(ObjectMetadata.class),
                anyBoolean())
        )
                .thenReturn(observer);

        doAnswer(invocation -> {
            TransferListener listener = invocation.getArgument(0);
            listener.onStateChanged(0, TransferState.COMPLETED, toRemoteKey);
            return null;
        }).when(observer)
                .setTransferListener(any(com.amplifyframework.storage.s3.transfer.TransferListener.class));

        StorageUploadFileResult result =
                Await.<StorageUploadFileResult, StorageException>result((onResult, onError) ->
                        storage.uploadFile(
                                toRemoteKey,
                                fromLocalFile,
                                onResult,
                                onError
                        )
                );

        assertEquals(toRemoteKey, result.getKey());
    }

    /**
     * Test that calling upload inputStream method from Storage category correctly
     * invokes the registered AWSS3StoragePlugin instance and returns a
     * {@link StorageUploadFileResult} with correct remote key.
     *
     * @throws Exception when an error is encountered while uploading
     */
    @Test
    public void testUploadInputStreamGetsKey() throws Exception {
        final String toRemoteKey = RandomString.string();
        final InputStream inputStream = new ByteArrayInputStream(RandomBytes.bytes());

        TransferObserver observer = mock(TransferObserver.class);
        when(storageService.uploadInputStream(
            anyString(),
            anyString(),
            any(InputStream.class),
            any(ObjectMetadata.class),
            anyBoolean())
        )
                .thenReturn(observer);

        doAnswer(invocation -> {
            TransferListener listener = invocation.getArgument(0);
            listener.onStateChanged(0, TransferState.COMPLETED, toRemoteKey);
            return null;
        }).when(observer)
                .setTransferListener(any(TransferListener.class));

        StorageUploadInputStreamResult result =
                Await.<StorageUploadInputStreamResult, StorageException>result((onResult, onError) ->
                        storage.uploadInputStream(
                                toRemoteKey,
                                inputStream,
                                onResult,
                                onError
                        )
                );

        assertEquals(toRemoteKey, result.getKey());
    }

    /**
     * Test that calling upload file method from Storage category fails
     * successfully when {@link TransferListener} emits an error.
     *
     * @throws IOException when the upload file cannot be created
     */
    @Test
    public void testUploadFileError() throws IOException {
        final StorageException testError = new StorageException(
                "Test error message",
                "Test recovery message"
        );

        final String toRemoteKey = RandomString.string();
        final File fromLocalFile = new RandomTempFile(FILE_SIZE);

        com.amplifyframework.storage.s3.transfer.TransferObserver observer =
                mock(com.amplifyframework.storage.s3.transfer.TransferObserver.class);
        when(
            storageService.uploadFile(
                anyString(),
                anyString(),
                any(File.class),
                any(ObjectMetadata.class),
                anyBoolean())
        ).thenReturn(observer);

        doAnswer(invocation -> {
            com.amplifyframework.storage.s3.transfer.TransferListener listener = invocation.getArgument(0);
            listener.onError(0, testError);
            return null;
        }).when(observer)
                .setTransferListener(any(com.amplifyframework.storage.s3.transfer.TransferListener.class));

        StorageException error =
                Await.<StorageUploadFileResult, StorageException>error((onResult, onError) ->
                        storage.uploadFile(
                                toRemoteKey,
                                fromLocalFile,
                                onResult,
                                onError
                        )
                );

        assertEquals(testError, error.getCause());
    }

    /**
     * Test that calling upload inputStream method from Storage category fails
     * successfully when {@link TransferListener} emits an error.
     *
     * @throws IOException when the upload file cannot be created
     */
    @Test
    public void testInputStreamError() throws IOException {
        final StorageException testError = new StorageException(
                "Test error message",
                "Test recovery message"
        );

        final String toRemoteKey = RandomString.string();
        final InputStream inputStream = new ByteArrayInputStream(RandomBytes.bytes());

        TransferObserver observer = mock(TransferObserver.class);
        when(storageService.uploadInputStream(
            anyString(),
            anyString(),
            any(InputStream.class),
            any(ObjectMetadata.class),
            anyBoolean())
        )
                .thenReturn(observer);

        doAnswer(invocation -> {
            TransferListener listener = invocation.getArgument(0);
            listener.onError(0, testError);
            return null;
        }).when(observer)
                .setTransferListener(any(TransferListener.class));
        StorageException error =
                Await.<StorageUploadInputStreamResult, StorageException>error((onResult, onError) ->
                        storage.uploadInputStream(
                            toRemoteKey,
                            inputStream,
                            onResult,
                            onError
                        )
                );
        assertEquals(testError, error.getCause());
    }

    /**
     * Test that calling list method from Storage category correctly
     * invokes the registered AWSS3StoragePlugin instance and returns a
     * {@link StorageListResult} with list of stored items.
     *
     * @throws StorageException when an error is encountered while listing
     *                          files inside storage
     */
    @Test
    public void testListObject() throws StorageException {
        final String path = RandomString.string();
        final StorageItem item = new StorageItem(
                RandomString.string(),
                0L,
                new Date(),
                RandomString.string(),
                null
        );

        when(storageService.listFiles(anyString(), anyString()))
                .thenReturn(Collections.singletonList(item));

        StorageListResult result =
                Await.<StorageListResult, StorageException>result((onResult, onError) ->
                        storage.list(
                                path,
                                onResult,
                                onError
                        )
                );

        assertEquals(item, result.getItems().get(0));
    }

    /**
     * Test that calling remove method from Storage category correctly
     * invokes the registered AWSS3StoragePlugin instance and returns a
     * {@link StorageRemoveResult} with key of removed item.
     *
     * @throws StorageException when an error is encountered while deleting
     *                          file from storage
     */
    @Test
    public void testRemoveObjectGetsKey() throws StorageException {
        final String remoteKey = RandomString.string();

        StorageRemoveResult result =
                Await.<StorageRemoveResult, StorageException>result((onResult, onError) ->
                        storage.remove(
                                remoteKey,
                                onResult,
                                onError
                        )
                );

        assertEquals(remoteKey, result.getKey());
    }
}
