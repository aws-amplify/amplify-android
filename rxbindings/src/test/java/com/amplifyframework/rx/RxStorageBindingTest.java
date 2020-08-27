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
import com.amplifyframework.storage.operation.StorageListOperation;
import com.amplifyframework.storage.operation.StorageRemoveOperation;
import com.amplifyframework.storage.operation.StorageUploadFileOperation;
import com.amplifyframework.storage.options.StorageDownloadFileOptions;
import com.amplifyframework.storage.options.StorageUploadFileOptions;
import com.amplifyframework.storage.result.StorageDownloadFileResult;
import com.amplifyframework.storage.result.StorageListResult;
import com.amplifyframework.storage.result.StorageRemoveResult;
import com.amplifyframework.storage.result.StorageTransferProgress;
import com.amplifyframework.storage.result.StorageUploadFileResult;
import com.amplifyframework.testutils.random.RandomString;

import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.concurrent.TimeUnit;

import io.reactivex.rxjava3.core.Completable;
import io.reactivex.rxjava3.core.Observable;
import io.reactivex.rxjava3.core.Single;
import io.reactivex.rxjava3.observers.TestObserver;

import static com.amplifyframework.rx.Matchers.anyConsumer;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Tests the {@link RxStorageBinding}.
 */
public final class RxStorageBindingTest {
    private static final long TIMEOUT_MS = TimeUnit.SECONDS.toMillis(5);
    private RxStorageCategoryBehavior rxStorage;
    private StoragePlugin<?> delegate;
    private File localFile;
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
        remoteKey = RandomString.string();
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

            Observable.interval(100, TimeUnit.MILLISECONDS)
                      .take(5)
                      .flatMapCompletable(aLong -> {
                          progressConsumer.accept(new StorageTransferProgress(aLong, 500));
                          return Completable.complete();
                      })
                      .doOnComplete(() -> {
                          resultConsumer.accept(result);
                      })
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
        testProgressObserver.assertValueCount(5);
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

            Observable.interval(100, TimeUnit.MILLISECONDS)
                      .take(5)
                      .flatMapCompletable(aLong -> {
                          progressConsumer.accept(new StorageTransferProgress(aLong, 500));
                          return Completable.complete();
                      })
                      .doOnComplete(() -> {
                          resultConsumer.accept(result);
                      })
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
        testProgressObserver.assertValueCount(5);

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
     * When {@link StorageCategoryBehavior#list(String, Consumer, Consumer)} emits a result,
     * then the {@link Single} returned by {@link RxStorageCategoryBehavior#list(String)}
     * should emit an {@link StorageListResult}.
     */
    @Test
    public void listReturnsResult() {
        StorageListResult result = StorageListResult.fromItems(Collections.emptyList());
        doAnswer(invocation -> {
            final int indexOfResultConsumer = 1; // 0 localPath, 1 onResult, 2 onError
            Consumer<StorageListResult> resultConsumer = invocation.getArgument(indexOfResultConsumer);
            resultConsumer.accept(result);
            return mock(StorageListOperation.class);
        })
        .when(delegate)
            .list(eq(remoteKey), anyConsumer(), anyConsumer());

        rxStorage
            .list(remoteKey)
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
            final int indexOfErrorConsumer = 2; // 0 localPath, 1 onResult, 2 onError
            Consumer<StorageException> errorConsumer = invocation.getArgument(indexOfErrorConsumer);
            errorConsumer.accept(error);
            return mock(StorageListOperation.class);
        })
        .when(delegate)
            .list(eq(remoteKey), anyConsumer(), anyConsumer());

        rxStorage
            .list(remoteKey)
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
}
