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

import com.amplifyframework.core.Consumer;
import com.amplifyframework.storage.StorageCategoryBehavior;
import com.amplifyframework.storage.StorageException;
import com.amplifyframework.storage.operation.StorageDownloadFileOperation;
import com.amplifyframework.storage.operation.StorageListOperation;
import com.amplifyframework.storage.operation.StorageRemoveOperation;
import com.amplifyframework.storage.operation.StorageUploadFileOperation;
import com.amplifyframework.storage.result.StorageDownloadFileResult;
import com.amplifyframework.storage.result.StorageListResult;
import com.amplifyframework.storage.result.StorageRemoveResult;
import com.amplifyframework.storage.result.StorageUploadFileResult;
import com.amplifyframework.testutils.RandomString;

import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.util.Collections;

import io.reactivex.Single;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;

/**
 * Tests the {@link RxStorageBinding}.
 */
@SuppressWarnings("unchecked")
public final class RxStorageBindingTest {
    private RxStorage rxStorage;
    private StorageCategoryBehavior delegate;
    private String localPath;
    private String remoteKey;

    @Before
    public void setup() {
        delegate = mock(StorageCategoryBehavior.class);
        rxStorage = new RxStorageBinding(delegate);
        localPath = RandomString.string();
        remoteKey = RandomString.string();
    }

    /**
     * When {@link StorageCategoryBehavior#downloadFile(String, String, Consumer, Consumer)}
     * invokes its success callback, the {@link StorageDownloadFileResult} should propagate
     * via the {@link Single} returned by {@link RxStorage#downloadFile(String, String)}.
     */
    @Test
    public void downloadFileReturnsResult() {
        StorageDownloadFileResult result = StorageDownloadFileResult.fromFile(mock(File.class));
        doAnswer(invocation -> {
            final int indexOfResultConsumer = 2; // 0 key, 1 local, 2 onResult, 3 onError
            Consumer<StorageDownloadFileResult> resultConsumer = invocation.getArgument(indexOfResultConsumer);
            resultConsumer.accept(result);
            return mock(StorageDownloadFileOperation.class);
        })
        .when(delegate)
            .downloadFile(eq(remoteKey), eq(localPath), any(Consumer.class), any(Consumer.class));

        rxStorage.downloadFile(remoteKey, localPath)
            .test()
            .assertValues(result);
    }

    /**
     * When {@link StorageCategoryBehavior#downloadFile(String, String, Consumer, Consumer)} invokes
     * its error callback, the {@link StorageException} is communicated via the {@link Single}
     * returned by {@link RxStorage#downloadFile(String, String)}.
     */
    @Test
    public void downloadFileReturnsError() {
        StorageException downloadError = new StorageException("Test exception.", "It is expected.");
        doAnswer(invocation -> {
            final int indexOfErrorConsumer = 3; // 0 key, 1 local, 2 onResult, 3 onError
            Consumer<StorageException> resultConsumer = invocation.getArgument(indexOfErrorConsumer);
            resultConsumer.accept(downloadError);
            return mock(StorageDownloadFileOperation.class);
        })
        .when(delegate)
            .downloadFile(eq(remoteKey), eq(localPath), any(Consumer.class), any(Consumer.class));

        rxStorage.downloadFile(remoteKey, localPath)
            .test()
            .assertError(downloadError);
    }

    /**
     * When {@link StorageCategoryBehavior#uploadFile(String, String, Consumer, Consumer)} returns
     * a {@link StorageUploadFileResult}, then the {@link Single} returned by
     * {@link RxStorage#uploadFile(String, String)} should emit that result.
     */
    @Test
    public void uploadFileReturnsResult() {
        StorageUploadFileResult result = StorageUploadFileResult.fromKey(remoteKey);
        doAnswer(invocation -> {
            final int indexOfResultConsumer = 2; // 0 key, 1 local, 2 onResult, 3 onError
            Consumer<StorageUploadFileResult> resultConsumer = invocation.getArgument(indexOfResultConsumer);
            resultConsumer.accept(result);
            return mock(StorageUploadFileOperation.class);
        })
        .when(delegate)
            .uploadFile(eq(remoteKey), eq(localPath), any(Consumer.class), any(Consumer.class));

        rxStorage
            .uploadFile(remoteKey, localPath)
            .test()
            .assertValues(result);
    }

    /**
     * When {@link StorageCategoryBehavior#uploadFile(String, String, Consumer, Consumer)} returns
     * an {@link StorageException}, then the {@link Single} returned by
     * {@link RxStorage#uploadFile(String, String)} should emit a {@link StorageException}.
     */
    @Test
    public void uploadFileReturnsError() {
        StorageException error = new StorageException("Error uploading.", "Expected.");
        doAnswer(invocation -> {
            final int indexOfResultConsumer = 3; // 0 key, 1 local, 2 onResult, 3 onError
            Consumer<StorageException> errorConsumer = invocation.getArgument(indexOfResultConsumer);
            errorConsumer.accept(error);
            return mock(StorageUploadFileOperation.class);
        })
        .when(delegate)
            .uploadFile(eq(remoteKey), eq(localPath), any(Consumer.class), any(Consumer.class));

        rxStorage
            .uploadFile(remoteKey, localPath)
            .test()
            .assertError(error);
    }

    /**
     * When {@link StorageCategoryBehavior#list(String, Consumer, Consumer)} emits a result,
     * then the {@link Single} returned by {@link RxStorage#list(String)}
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
            .list(eq(localPath), any(Consumer.class), any(Consumer.class));

        rxStorage
            .list(localPath)
            .test()
            .assertValues(result);
    }

    /**
     * When the {@link StorageCategoryBehavior#list(String, Consumer, Consumer)} emits an error,
     * the {@link Single} returned by {@link RxStorage#list(String)} should emit an
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
            .list(eq(localPath), any(Consumer.class), any(Consumer.class));

        rxStorage
            .list(localPath)
            .test()
            .assertError(error);
    }

    /**
     * When the {@link StorageCategoryBehavior#remove(String, Consumer, Consumer)} emits
     * a result, the {@link Single} returned by {@link RxStorage#remove(String)} should
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
            .remove(eq(remoteKey), any(Consumer.class), any(Consumer.class));

        rxStorage
            .remove(remoteKey)
            .test()
            .assertValues(result);
    }

    /**
     * When {@link StorageCategoryBehavior#remove(String, Consumer, Consumer)} calls its
     * error consumer, then the {@link Single} returned by {@link RxStorage#remove(String)}
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
            .remove(eq(remoteKey), any(Consumer.class), any(Consumer.class));

        rxStorage
            .remove(remoteKey)
            .test()
            .assertError(error);
    }
}
