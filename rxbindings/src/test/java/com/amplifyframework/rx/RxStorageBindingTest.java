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

package com.amplifyframework.rx;

import android.content.Context;

import com.amplifyframework.AmplifyException;
import com.amplifyframework.core.Consumer;
import com.amplifyframework.storage.StorageCategory;
import com.amplifyframework.storage.StorageCategoryBehavior;
import com.amplifyframework.storage.StorageCategoryConfiguration;
import com.amplifyframework.storage.StorageException;
import com.amplifyframework.storage.StoragePlugin;
import com.amplifyframework.storage.operation.StorageDownloadFileOperation;
import com.amplifyframework.storage.operation.StorageGetUrlOperation;
import com.amplifyframework.storage.operation.StorageListOperation;
import com.amplifyframework.storage.operation.StorageRemoveOperation;
import com.amplifyframework.storage.operation.StorageTransferOperation;
import com.amplifyframework.storage.operation.StorageUploadFileOperation;
import com.amplifyframework.storage.operation.StorageUploadInputStreamOperation;
import com.amplifyframework.storage.options.StorageDownloadFileOptions;
import com.amplifyframework.storage.options.StorageGetUrlOptions;
import com.amplifyframework.storage.options.StoragePagedListOptions;
import com.amplifyframework.storage.options.StorageUploadFileOptions;
import com.amplifyframework.storage.options.StorageUploadInputStreamOptions;
import com.amplifyframework.storage.result.StorageDownloadFileResult;
import com.amplifyframework.storage.result.StorageGetUrlResult;
import com.amplifyframework.storage.result.StorageListResult;
import com.amplifyframework.storage.result.StorageRemoveResult;
import com.amplifyframework.storage.result.StorageTransferProgress;
import com.amplifyframework.storage.result.StorageTransferResult;
import com.amplifyframework.storage.result.StorageUploadFileResult;
import com.amplifyframework.storage.result.StorageUploadInputStreamResult;
import com.amplifyframework.testutils.random.RandomBytes;
import com.amplifyframework.testutils.random.RandomString;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InOrder;
import org.robolectric.RobolectricTestRunner;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Collections;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import io.reactivex.rxjava3.core.Observable;
import io.reactivex.rxjava3.core.Single;
import io.reactivex.rxjava3.observers.TestObserver;

import static com.amplifyframework.rx.Matchers.anyConsumer;
import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Tests the {@link RxStorageBinding}.
 */
@RunWith(RobolectricTestRunner.class)
public final class RxStorageBindingTest {
    private static final long TIMEOUT_MS = TimeUnit.SECONDS.toMillis(10);
    private RxStorageCategoryBehavior rxStorage;
    private StoragePlugin<?> delegate;
    private File localFile;
    private InputStream localInputStream;
    private String remoteKey;

    /**
     * Creates a StorageCategory backed by a mock plugin. Uses this category
     * as a backing for an Rx Binding, under test.
     * @throws AmplifyException On failure to add plugin or config/init the storage category
     */
    @Before
    public void createBindingInFrontOfMockPlugin() throws AmplifyException {
        delegate = mock(StoragePlugin.class);
        when(delegate.getPluginKey()).thenReturn(RandomString.string());

        final StorageCategory storageCategory = new StorageCategory();
        storageCategory.addPlugin(delegate);
        storageCategory.configure(new StorageCategoryConfiguration(), mock(Context.class));
        storageCategory.initialize(mock(Context.class));

        rxStorage = new RxStorageBinding(storageCategory);
    }

    /**
     * Creates stable/expected values for a local path and remote key, to be matched
     * against in behavior mocks/verifications.
     * @throws IOException when a temporary file cannot be created
     */
    @Before
    public void createRandomRequestParams() throws IOException {
        localFile = File.createTempFile("random", "data");
        localInputStream = new ByteArrayInputStream(RandomBytes.bytes());
        remoteKey = RandomString.string();
    }

    /**
     * When the delegate returns a result from the
     * {@link StorageCategoryBehavior#getUrl(String, StorageGetUrlOptions, Consumer, Consumer)},
     * the binding should emit the result via the single.
     * @throws MalformedURLException Not expected; it's part of the URL constructor signature, though
     */
    @Test
    public void getUrlReturnsResult() throws MalformedURLException {
        URL someRandomUrl = new URL("https://bogus.tld/foo");
        StorageGetUrlResult expectedResult = StorageGetUrlResult.fromUrl(someRandomUrl);

        doAnswer(invocation -> {
            int indexOfResultConsumer = 2;
            Consumer<StorageGetUrlResult> onResult = invocation.getArgument(indexOfResultConsumer);
            onResult.accept(expectedResult);
            return mock(StorageGetUrlOperation.class);
        }).when(delegate)
            .getUrl(eq(remoteKey), any(StorageGetUrlOptions.class), anyConsumer(), anyConsumer());

        rxStorage.getUrl(remoteKey, StorageGetUrlOptions.defaultInstance())
            .test()
            .awaitCount(1)
            .assertNoErrors()
            .assertValue(expectedResult);
    }

    /**
     * When the delegate emits a failure from the
     * {@link StorageCategoryBehavior#getUrl(String, StorageGetUrlOptions, Consumer, Consumer)},
     * the binding should emit a failure to its single observer.
     */
    @Test
    public void getUrlEmitsFailure() {
        StorageException expectedException = new StorageException("oh", "boy!");

        doAnswer(invocation -> {
            int indexOfErrorConsumer = 3;
            Consumer<StorageException> onError = invocation.getArgument(indexOfErrorConsumer);
            onError.accept(expectedException);
            return mock(StorageGetUrlOperation.class);
        }).when(delegate)
            .getUrl(eq(remoteKey), any(StorageGetUrlOptions.class), anyConsumer(), anyConsumer());

        rxStorage.getUrl(remoteKey, StorageGetUrlOptions.defaultInstance())
            .test()
            .awaitDone(TIMEOUT_MS, TimeUnit.MILLISECONDS)
            .assertError(expectedException);
    }

    /**
     * When {@link StorageCategoryBehavior#downloadFile(String, File, StorageDownloadFileOptions,
     * Consumer, Consumer, Consumer)} invokes its success callback, the {@link StorageDownloadFileResult}
     * should propagate via the {@link Single} returned by
     * {@link RxStorageBinding.RxProgressAwareSingleOperation#observeResult()}.
     * @throws InterruptedException not expected.
     */
    @Test
    public void downloadFileReturnsResult() throws InterruptedException {
        StorageDownloadFileResult result = StorageDownloadFileResult.fromFile(mock(File.class));
        doAnswer(invocation -> {
            // 0 key, 1 local, 2 options, 3 onProgress 4 onResult, 5 onError
            final int indexOfProgressConsumer = 3;
            final int indexOfResultConsumer = 4;
            Consumer<StorageTransferProgress> progressConsumer = invocation.getArgument(indexOfProgressConsumer);
            Consumer<StorageDownloadFileResult> resultConsumer = invocation.getArgument(indexOfResultConsumer);

            Observable.interval(100, 100, TimeUnit.MILLISECONDS)
                      .take(5)
                      .doOnNext(aLong -> progressConsumer.accept(new StorageTransferProgress(aLong, 500)))
                      .doOnComplete(() -> resultConsumer.accept(result))
                      .subscribe();
            return mock(StorageDownloadFileOperation.class);
        }).when(delegate).downloadFile(eq(remoteKey),
                                       eq(localFile),
                                       any(StorageDownloadFileOptions.class),
                                       anyConsumer(),
                                       anyConsumer(),
                                       anyConsumer());

        RxStorageBinding.RxProgressAwareSingleOperation<StorageDownloadFileResult> rxOperation =
            rxStorage.downloadFile(remoteKey, localFile, StorageDownloadFileOptions.defaultInstance());
        TestObserver<StorageDownloadFileResult> testObserver = rxOperation.observeResult().test();
        TestObserver<StorageTransferProgress> testProgressObserver = rxOperation.observeProgress().test();
        testObserver.await(TIMEOUT_MS, TimeUnit.MILLISECONDS);
        testObserver.assertValues(result);
        testProgressObserver.awaitCount(5);
        testProgressObserver.assertValueCount(5);
    }

    /**
     * When {@link StorageCategoryBehavior#downloadFile(String, File, StorageDownloadFileOptions,
     * Consumer, Consumer, Consumer)} invokes its pause, resume and cancel operation, the {@link
     * StorageDownloadFileOperation}
     * should invoke corresponding api.
     *
     * @throws InterruptedException not expected.
     */
    @Test
    public void performActionOnDownloadFile() throws InterruptedException {
        StorageDownloadFileResult result = StorageDownloadFileResult.fromFile(mock(File.class));
        String transferId = UUID.randomUUID().toString();
        StorageDownloadFileOperation<?> storageDownloadFileOperationMock = mock(StorageDownloadFileOperation.class);
        when(storageDownloadFileOperationMock.getTransferId()).thenReturn(transferId);
        doAnswer(invocation -> {
            return storageDownloadFileOperationMock;
        }).when(delegate).downloadFile(eq(remoteKey),
            eq(localFile),
            any(StorageDownloadFileOptions.class),
            anyConsumer(),
            anyConsumer(),
            anyConsumer());

        RxStorageBinding.RxProgressAwareSingleOperation<StorageDownloadFileResult> rxOperation =
            rxStorage.downloadFile(remoteKey, localFile, StorageDownloadFileOptions.defaultInstance());
        assertEquals(transferId, rxOperation.getTransferId());
        InOrder inOrder = inOrder(storageDownloadFileOperationMock);

        rxOperation.pause();
        rxOperation.resume();
        rxOperation.cancel();
        inOrder.verify(storageDownloadFileOperationMock).pause();
        inOrder.verify(storageDownloadFileOperationMock).resume();
        inOrder.verify(storageDownloadFileOperationMock).cancel();
    }

    /**
     * When {@link StorageCategoryBehavior#downloadFile(String, File, Consumer, Consumer)} invokes
     * its error callback, the {@link StorageException} is communicated via the {@link Single}
     * returned by {@link RxStorageCategoryBehavior#downloadFile(String, File)}.
     */
    @Test
    public void downloadFileReturnsError() {
        StorageException downloadError = new StorageException("Test exception.", "It is expected.");
        doAnswer(invocation -> {
            // 0 key, 1 local, 2 options, 3 onProgress 4 onResult, 5 onError
            final int indexOfErrorConsumer = 5;
            Consumer<StorageException> resultConsumer = invocation.getArgument(indexOfErrorConsumer);
            resultConsumer.accept(downloadError);
            return mock(StorageDownloadFileOperation.class);
        }).when(delegate).downloadFile(eq(remoteKey),
                                       eq(localFile),
                                       any(StorageDownloadFileOptions.class),
                                       anyConsumer(),
                                       anyConsumer(),
                                       anyConsumer());

        rxStorage.downloadFile(remoteKey, localFile)
                 .observeResult()
                 .test()
                 .assertError(downloadError);
    }

    /**
     * When {@link StorageCategoryBehavior#uploadFile(String, File, Consumer, Consumer)} returns
     * a {@link StorageUploadFileResult}, then the {@link Single} returned by
     * {@link RxStorageCategoryBehavior#uploadFile(String, File)} should emit that result.
     * @throws InterruptedException Not expected.
     */
    @Test
    public void uploadFileReturnsResult() throws InterruptedException {
        StorageUploadFileResult result = StorageUploadFileResult.fromKey(remoteKey);
        doAnswer(invocation -> {
            // 0 key, 1 local, 2 options, 3 onProgress, 4 onResult, 5 onError
            final int indexOfResultConsumer = 4;
            final int indexOfProgressConsumer = 3;
            Consumer<StorageUploadFileResult> resultConsumer = invocation.getArgument(indexOfResultConsumer);
            Consumer<StorageTransferProgress> progressConsumer = invocation.getArgument(indexOfProgressConsumer);

            Observable.interval(100, 100, TimeUnit.MILLISECONDS)
                      .take(5)
                      .doOnNext(aLong -> progressConsumer.accept(new StorageTransferProgress(aLong, 500)))
                      .doOnComplete(() -> resultConsumer.accept(result))
                      .subscribe();
            return mock(StorageUploadFileOperation.class);
        }).when(delegate).uploadFile(eq(remoteKey),
                                     eq(localFile),
                                     any(StorageUploadFileOptions.class),
                                     anyConsumer(),
                                     anyConsumer(),
                                     anyConsumer());

        RxStorageBinding.RxProgressAwareSingleOperation<StorageUploadFileResult> rxOperation =
            rxStorage.uploadFile(remoteKey, localFile);
        TestObserver<StorageUploadFileResult> testObserver = rxOperation.observeResult().test();
        TestObserver<StorageTransferProgress> testProgressObserver = rxOperation.observeProgress().test();
        testObserver.await(TIMEOUT_MS, TimeUnit.MILLISECONDS);
        testObserver.assertValues(result);
        testProgressObserver.awaitCount(5);
        testProgressObserver.assertValueCount(5);
    }

    /**
     * When {@link StorageCategoryBehavior#uploadFile(String, File, Consumer, Consumer)} returns
     * a {@link RxStorageBinding.RxProgressAwareSingleOperation}, then the pause, resume and cancel action
     * performed on the rxOperation should invoke corresponding api in {@link StorageUploadFileOperation}.
     *
     * @throws InterruptedException Not expected.
     */
    @Test
    public void performActionOnUploadFile() throws InterruptedException {
        StorageUploadFileResult result = StorageUploadFileResult.fromKey(remoteKey);
        String transferId = UUID.randomUUID().toString();
        StorageUploadFileOperation<?> storageUploadFileOperationMock = mock(StorageUploadFileOperation.class);
        when(storageUploadFileOperationMock.getTransferId()).thenReturn(transferId);
        doAnswer(invocation -> {
            return storageUploadFileOperationMock;
        }).when(delegate).uploadFile(eq(remoteKey),
            eq(localFile),
            any(StorageUploadFileOptions.class),
            anyConsumer(),
            anyConsumer(),
            anyConsumer());

        RxStorageBinding.RxProgressAwareSingleOperation<StorageUploadFileResult> rxOperation =
            rxStorage.uploadFile(remoteKey, localFile);
        assertEquals(transferId, rxOperation.getTransferId());
        InOrder inOrder = inOrder(storageUploadFileOperationMock);
        rxOperation.pause();
        rxOperation.resume();
        rxOperation.cancel();
        inOrder.verify(storageUploadFileOperationMock).pause();
        inOrder.verify(storageUploadFileOperationMock).resume();
        inOrder.verify(storageUploadFileOperationMock).cancel();
    }

    /**
     * When {@link StorageCategoryBehavior#uploadInputStream(String, InputStream, Consumer, Consumer)} returns
     * a {@link StorageUploadInputStreamResult}, then the {@link Single} returned by
     * {@link RxStorageCategoryBehavior#uploadInputStream(String, InputStream)} should emit that result.
     * @throws InterruptedException Not expected.
     */
    @Test
    public void uploadInputStreamReturnsResult() throws InterruptedException {
        StorageUploadInputStreamResult result = StorageUploadInputStreamResult.fromKey(remoteKey);
        doAnswer(invocation -> {
            // 0 key, 1 local, 2 options, 3 onProgress, 4 onResult, 5 onError
            final int indexOfResultConsumer = 4;
            final int indexOfProgressConsumer = 3;
            Consumer<StorageUploadInputStreamResult> resultConsumer = invocation.getArgument(indexOfResultConsumer);
            Consumer<StorageTransferProgress> progressConsumer = invocation.getArgument(indexOfProgressConsumer);

            Observable.interval(100, 100, TimeUnit.MILLISECONDS)
                    .take(5)
                    .doOnNext(aLong -> progressConsumer.accept(new StorageTransferProgress(aLong, 500)))
                    .doOnComplete(() -> resultConsumer.accept(result))
                    .subscribe();
            return mock(StorageUploadInputStreamOperation.class);
        }).when(delegate).uploadInputStream(eq(remoteKey),
                eq(localInputStream),
                any(StorageUploadInputStreamOptions.class),
                anyConsumer(),
                anyConsumer(),
                anyConsumer());

        RxStorageBinding.RxProgressAwareSingleOperation<StorageUploadInputStreamResult> rxOperation =
                rxStorage.uploadInputStream(remoteKey, localInputStream);
        TestObserver<StorageUploadInputStreamResult> testObserver = rxOperation.observeResult().test();
        TestObserver<StorageTransferProgress> testProgressObserver = rxOperation.observeProgress().test();
        testObserver.await(TIMEOUT_MS, TimeUnit.MILLISECONDS);
        testObserver.assertValues(result);
        testProgressObserver.awaitCount(5);
        testProgressObserver.assertValueCount(5);
    }

    /**
     * When {@link StorageCategoryBehavior#uploadInputStream(String, InputStream, Consumer, Consumer)} returns
     * a {@link StorageUploadInputStreamResult}, then the {@link Single} returned by
     * {@link RxStorageCategoryBehavior#uploadInputStream(String, InputStream)} should invoke corresponding API in
     * {@link StorageUploadInputStreamOperation}.
     *
     * @throws InterruptedException Not expected.
     */
    @Test
    public void performActionOnUploadInputStream() throws InterruptedException {
        StorageUploadInputStreamResult result = StorageUploadInputStreamResult.fromKey(remoteKey);
        String transferId = UUID.randomUUID().toString();
        StorageUploadInputStreamOperation<?> storageUploadInputStreamOperationMock =
            mock(StorageUploadInputStreamOperation.class);
        when(storageUploadInputStreamOperationMock.getTransferId()).thenReturn(transferId);
        doAnswer(invocation -> {
            return storageUploadInputStreamOperationMock;
        }).when(delegate).uploadInputStream(eq(remoteKey),
            eq(localInputStream),
            any(StorageUploadInputStreamOptions.class),
            anyConsumer(),
            anyConsumer(),
            anyConsumer());

        RxStorageBinding.RxProgressAwareSingleOperation<StorageUploadInputStreamResult> rxOperation =
            rxStorage.uploadInputStream(remoteKey, localInputStream);
        assertEquals(transferId, rxOperation.getTransferId());
        InOrder inOrder = inOrder(storageUploadInputStreamOperationMock);
        rxOperation.pause();
        rxOperation.resume();
        rxOperation.cancel();
        inOrder.verify(storageUploadInputStreamOperationMock).pause();
        inOrder.verify(storageUploadInputStreamOperationMock).resume();
        inOrder.verify(storageUploadInputStreamOperationMock).cancel();
    }

    /**
     * When {@link StorageCategoryBehavior#uploadFile(String, File, Consumer, Consumer)} returns
     * an {@link StorageException}, then the {@link Single} returned by
     * {@link RxStorageCategoryBehavior#uploadFile(String, File)} should emit a {@link StorageException}.
     */
    @Test
    public void uploadFileReturnsError() {
        StorageException error = new StorageException("Error uploading.", "Expected.");
        doAnswer(invocation -> {
            // 0 key, 1 local, 2 options, 3 onProgress 4 onResult, 5 onError
            final int indexOfResultConsumer = 5;
            Consumer<StorageException> errorConsumer = invocation.getArgument(indexOfResultConsumer);
            errorConsumer.accept(error);
            return mock(StorageUploadFileOperation.class);
        }).when(delegate).uploadFile(eq(remoteKey),
                                     eq(localFile),
                                     any(StorageUploadFileOptions.class),
                                     anyConsumer(),
                                     anyConsumer(),
                                     anyConsumer());

        rxStorage
                .uploadFile(remoteKey, localFile)
                .observeResult()
                .test()
                .assertError(error);
    }

    /**
     * When {@link StorageCategoryBehavior#uploadInputStream(String, InputStream, Consumer, Consumer)} returns
     * an {@link StorageException}, then the {@link Single} returned by
     * {@link RxStorageCategoryBehavior#uploadInputStream(String, InputStream)} should emit a {@link StorageException}.
     */
    @Test
    public void uploadInputStreamReturnsError() {
        StorageException error = new StorageException("Error uploading.", "Expected.");
        doAnswer(invocation -> {
            // 0 key, 1 local, 2 options, 3 onProgress 4 onResult, 5 onError
            final int indexOfResultConsumer = 5;
            Consumer<StorageException> errorConsumer = invocation.getArgument(indexOfResultConsumer);
            errorConsumer.accept(error);
            return mock(StorageUploadInputStreamOperation.class);
        }).when(delegate).uploadInputStream(eq(remoteKey),
                                            eq(localInputStream),
                                            any(StorageUploadInputStreamOptions.class),
                                            anyConsumer(),
                                            anyConsumer(),
                                            anyConsumer());

        rxStorage
                .uploadInputStream(remoteKey, localInputStream)
                .observeResult()
                .test()
                .assertError(error);
    }

    /**
     * When {@link StorageCategoryBehavior#list(String, Consumer, Consumer)} emits a result,
     * then the {@link Single} returned by {@link RxStorageCategoryBehavior#list(String)}
     * should emit an {@link StorageListResult}.
     */
    @Test
    public void listReturnsResult() {
        StorageListResult result = StorageListResult.fromItems(Collections.emptyList(), null);
        doAnswer(invocation -> {
            final int indexOfResultConsumer = 2; // 0 localPath, 1 options, 2 onResult, 3 onError
            Consumer<StorageListResult> resultConsumer = invocation.getArgument(indexOfResultConsumer);
            resultConsumer.accept(result);
            return mock(StorageListOperation.class);
        })
        .when(delegate)
            .list(eq(remoteKey), any(StoragePagedListOptions.class), anyConsumer(), anyConsumer());

        rxStorage
            .list(remoteKey, StoragePagedListOptions.builder().build())
            .test()
            .assertValues(result);
    }

    /**
     * When the {@link StorageCategoryBehavior#list(String, Consumer, Consumer)} emits an error,
     * the {@link Single} returned by {@link RxStorageCategoryBehavior#list(String)} should emit an
     * {@link StorageException}.
     */
    @Test
    public void listReturnsError() {
        StorageException error = new StorageException("Error removing item.", "Expected.");
        doAnswer(invocation -> {
            final int indexOfErrorConsumer = 3; // 0 localPath, 1 options, 2 onResult, 3 onError
            Consumer<StorageException> errorConsumer = invocation.getArgument(indexOfErrorConsumer);
            errorConsumer.accept(error);
            return mock(StorageListOperation.class);
        })
        .when(delegate)
            .list(eq(remoteKey), any(StoragePagedListOptions.class), anyConsumer(), anyConsumer());

        rxStorage
            .list(remoteKey, StoragePagedListOptions.builder().build())
            .test()
            .assertError(error);
    }

    /**
     * When the {@link StorageCategoryBehavior#remove(String, Consumer, Consumer)} emits
     * a result, the {@link Single} returned by {@link RxStorageCategoryBehavior#remove(String)} should
     * emit a {@link StorageRemoveResult}.
     */
    @Test
    public void removeReturnsResult() {
        StorageRemoveResult result = StorageRemoveResult.fromKey(remoteKey);
        doAnswer(invocation -> {
            final int indexOfResultConsumer = 1; // 0 remoteKey, 1 onResult, 2 onError
            Consumer<StorageRemoveResult> resultConsumer = invocation.getArgument(indexOfResultConsumer);
            resultConsumer.accept(result);
            return mock(StorageRemoveOperation.class);
        })
        .when(delegate)
            .remove(eq(remoteKey), anyConsumer(), anyConsumer());

        rxStorage
            .remove(remoteKey)
            .test()
            .assertValues(result);
    }

    /**
     * When {@link StorageCategoryBehavior#remove(String, Consumer, Consumer)} calls its
     * error consumer, then the {@link Single} returned by {@link RxStorageCategoryBehavior#remove(String)}
     * should emit an error.
     */
    @Test
    public void removeReturnsError() {
        StorageException error = new StorageException("Error removing item.", "Expected.");
        doAnswer(invocation -> {
            final int indexOfErrorConsumer = 2; // 0 remoteKey, 1 onResult, 2 onError
            Consumer<StorageException> errorConsumer = invocation.getArgument(indexOfErrorConsumer);
            errorConsumer.accept(error);
            return mock(StorageRemoveOperation.class);
        })
        .when(delegate)
            .remove(eq(remoteKey), anyConsumer(), anyConsumer());

        rxStorage
            .remove(remoteKey)
            .test()
            .assertError(error);
    }

    /**
     * When {@link StorageCategoryBehavior#getTransfer(String, Consumer, Consumer)} emits a result,
     * then the {@link Single} returned by {@link RxStorageCategoryBehavior#getTransfer(String)}
     * should emit an {@link com.amplifyframework.storage.operation.StorageTransferOperation}.
     */
    @Test
    public void getTransferReturnsResult() {
        StorageTransferOperation<?, ? extends StorageTransferResult> result =
            (StorageTransferOperation<?, ? extends StorageTransferResult>) mock(StorageTransferOperation.class);
        doAnswer(invocation -> {
            final int indexOfResultConsumer = 1; // 0 transferId, 1 onResult, 2 onError
            Consumer<StorageTransferOperation<?, ? extends StorageTransferResult>> resultConsumer =
                invocation.getArgument(indexOfResultConsumer);
            resultConsumer.accept(result);
            return result;
        })
            .when(delegate)
            .getTransfer(eq(remoteKey), anyConsumer(), anyConsumer());

        rxStorage
            .getTransfer(remoteKey)
            .test()
            .assertResult(result);
    }

    /**
     * When {@link StorageCategoryBehavior#getTransfer(String, Consumer, Consumer)}calls its
     * error consumer, then the {@link Single} returned by {@link RxStorageCategoryBehavior#getTransfer(String)}
     * should emit an error.
     */
    @Test
    public void getTransferReturnsError() {
        StorageException error = new StorageException("Error removing item.", "Expected.");
        doAnswer(invocation -> {
            final int indexOfErrorConsumer = 2; // 0 remoteKey, 1 onResult, 2 onError
            Consumer<StorageException> errorConsumer = invocation.getArgument(indexOfErrorConsumer);
            errorConsumer.accept(error);
            return error;
        })
            .when(delegate)
            .getTransfer(eq(remoteKey), anyConsumer(), anyConsumer());

        rxStorage
            .getTransfer(remoteKey)
            .test()
            .assertError(error);
    }
}
