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
//    public void SyncStatusGetReturnOnError() throws DataStoreException, InterruptedException {
//        CountDownLatch latch = new CountDownLatch(1);
//        final LastSyncMetadata lastSyncMetadata = LastSyncMetadata.baseSyncedAt(BlogOwner.class.getName(), Time.now());
//        List<LastSyncMetadata> resultList = new ArrayList<>();
//        resultList.add(lastSyncMetadata);
//        resultList.add(lastSyncMetadata);
//
//        Consumer<DataStoreException> onObservationError = value-> {
//            latch.countDown();
//        };
//
//        SqlQueryProcessor mockSqlQueryProcessor = mock(SqlQueryProcessor.class);
//        when(mockSqlQueryProcessor.queryOfflineData(eq(LastSyncMetadata.class), any(), any())).thenReturn(resultList);
//        DataStoreConfiguration mockDataStoreConfig = mock(DataStoreConfiguration.class);
//        when(mockDataStoreConfig.getSyncIntervalInMinutes()).thenReturn(0L);
//        SyncStatus subject = new SyncStatus(mockSqlQueryProcessor, mockDataStoreConfig);
//        boolean result = subject.get(LastSyncMetadata.class.getName(), onObservationError);
//
//        Assert.assertTrue(latch.await(1, TimeUnit.SECONDS));
//    }

}