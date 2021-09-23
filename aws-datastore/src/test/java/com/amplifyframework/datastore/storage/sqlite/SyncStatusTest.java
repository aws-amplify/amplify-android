/*
 * Copyright 2021 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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

package com.amplifyframework.datastore.storage.sqlite;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.amplifyframework.core.Consumer;
import com.amplifyframework.datastore.DataStoreConfiguration;
import com.amplifyframework.datastore.DataStoreException;

import com.amplifyframework.datastore.syncengine.LastSyncMetadata;
import com.amplifyframework.testmodels.commentsblog.BlogOwner;
import com.amplifyframework.util.Time;

import org.junit.Assert;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

public class SyncStatusTest {

    @Test
    public void SyncStatusGetReturnSyncedStatus() throws DataStoreException {
        final LastSyncMetadata lastSyncMetadata = LastSyncMetadata.baseSyncedAt(BlogOwner.class.getName(), Time.now());
        List<LastSyncMetadata> resultList = new ArrayList<>();
        resultList.add(lastSyncMetadata);

        Consumer<DataStoreException> onObservationError = value-> {};

        SqlQueryProcessor mockSqlQueryProcessor = mock(SqlQueryProcessor.class);
        when(mockSqlQueryProcessor.queryOfflineData(eq(LastSyncMetadata.class), any(), any())).thenReturn(resultList);
        DataStoreConfiguration mockDataStoreConfig = mock(DataStoreConfiguration.class);
        when(mockDataStoreConfig.getSyncIntervalInMinutes()).thenReturn(5L);
        SyncStatus subject = new SyncStatus(mockSqlQueryProcessor, mockDataStoreConfig);
        boolean result = subject.get(LastSyncMetadata.class.getName(), onObservationError);

        Assert.assertTrue(result);
    }

    @Test
    public void SyncStatusGetReturnNotSyncedStatus() throws DataStoreException {
        final LastSyncMetadata lastSyncMetadata = LastSyncMetadata.baseSyncedAt(BlogOwner.class.getName(), Time.now());
        List<LastSyncMetadata> resultList = new ArrayList<>();
        resultList.add(lastSyncMetadata);

        Consumer<DataStoreException> onObservationError = value-> {};

        SqlQueryProcessor mockSqlQueryProcessor = mock(SqlQueryProcessor.class);
        when(mockSqlQueryProcessor.queryOfflineData(eq(LastSyncMetadata.class), any(), any())).thenReturn(resultList);
        DataStoreConfiguration mockDataStoreConfig = mock(DataStoreConfiguration.class);
        when(mockDataStoreConfig.getSyncIntervalInMinutes()).thenReturn(0L);
        SyncStatus subject = new SyncStatus(mockSqlQueryProcessor, mockDataStoreConfig);
        boolean result = subject.get(LastSyncMetadata.class.getName(), onObservationError);

        Assert.assertFalse(result);
    }

//    @Test
//    public void SyncStatusGetReturnOnError() throws InterruptedException {
//        CountDownLatch latch = new CountDownLatch(1);
//        Consumer<DataStoreException> onObservationError = value-> {
//            latch.countDown();
//        };
//
//        SqlQueryProcessor mockSqlQueryProcessor = mock(SqlQueryProcessor.class);
//        when(mockSqlQueryProcessor.queryOfflineData(eq(LastSyncMetadata.class), any(), any()))
//                .thenAnswer(invocation -> new DataStoreException("test","test"));
//        DataStoreConfiguration mockDataStoreConfig = mock(DataStoreConfiguration.class);
//        when(mockDataStoreConfig.getSyncIntervalInMinutes()).thenReturn(0L);
//        SyncStatus subject = new SyncStatus(mockSqlQueryProcessor, mockDataStoreConfig);
//        subject.get(LastSyncMetadata.class.getName(), onObservationError);
//        Assert.assertTrue(latch.await(1, TimeUnit.SECONDS));
//    }

}