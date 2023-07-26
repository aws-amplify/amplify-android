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

import com.amplifyframework.AmplifyException;
import com.amplifyframework.core.Action;
import com.amplifyframework.core.Consumer;
import com.amplifyframework.core.NoOpAction;
import com.amplifyframework.core.NoOpConsumer;
import com.amplifyframework.core.async.Cancelable;
import com.amplifyframework.core.model.Model;
import com.amplifyframework.core.model.ModelSchema;
import com.amplifyframework.core.model.SchemaRegistry;
import com.amplifyframework.core.model.SerializedModel;
import com.amplifyframework.core.model.query.ObserveQueryOptions;
import com.amplifyframework.core.model.query.predicate.QueryPredicates;
import com.amplifyframework.datastore.DataStoreConfiguration;
import com.amplifyframework.datastore.DataStoreException;
import com.amplifyframework.datastore.DataStoreQuerySnapshot;
import com.amplifyframework.datastore.storage.StorageItemChange;
import com.amplifyframework.testmodels.commentsblog.BlogOwner;

import org.junit.Assert;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import io.reactivex.rxjava3.subjects.PublishSubject;
import io.reactivex.rxjava3.subjects.Subject;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class ObserveQueryExecutorTest {

    /***
     * Tests for ObserveQueryExecutor.
     * @throws InterruptedException InterruptedException
     * @throws DataStoreException DataStoreException
     */
    @Test
    public void observeQueryReturnsOfflineData() throws InterruptedException, DataStoreException {
        CountDownLatch latch = new CountDownLatch(1);
        final BlogOwner blogOwner = BlogOwner.builder()
                .name("Alan Turing")
                .build();
        List<BlogOwner> resultList = new ArrayList<>();
        resultList.add(blogOwner);

        Consumer<Cancelable> observationStarted = NoOpConsumer.create();
        Consumer<DataStoreQuerySnapshot<BlogOwner>> onQuerySnapshot = value -> {
            Assert.assertTrue(value.getItems().contains(blogOwner));
            latch.countDown(); };
        Consumer<DataStoreException> onObservationError = NoOpConsumer.create();
        Action onObservationComplete = NoOpAction.create();
        SqlQueryProcessor mockSqlQueryProcessor = mock(SqlQueryProcessor.class);
        when(mockSqlQueryProcessor.queryOfflineData(eq(BlogOwner.class), any(), any())).thenReturn(resultList);
        Subject<StorageItemChange<? extends Model>> subject =
                PublishSubject.<StorageItemChange<? extends Model>>create().toSerialized();
        ExecutorService threadPool = Executors.newFixedThreadPool(
                Runtime.getRuntime().availableProcessors() * 5);
        ObserveQueryExecutor<BlogOwner> observeQueryExecutor =
                new ObserveQueryExecutor<>(subject,
                        mockSqlQueryProcessor,
                        threadPool,
                        mock(SyncStatus.class),
                        new ModelSorter<>(),
                        DataStoreConfiguration.defaults());
        observeQueryExecutor.observeQuery(
                BlogOwner.class,
                new ObserveQueryOptions(null, null),
                observationStarted,
                onQuerySnapshot,
                onObservationError,
                onObservationComplete);
        Assert.assertTrue(latch.await(1, TimeUnit.SECONDS));
    }

    /***
     * observe Query Returns batched Records Based On MaxTime.
     * @throws InterruptedException InterruptedException
     * @throws DataStoreException DataStoreException
     */
    @Test
    public void observeQueryReturnsRecordsBasedOnMaxTime() throws InterruptedException,
            DataStoreException {
        CountDownLatch latch = new CountDownLatch(1);
        CountDownLatch changeLatch = new CountDownLatch(1);
        AtomicInteger count = new AtomicInteger();
        BlogOwner blogOwner = BlogOwner.builder()
                .name("Alan Turing")
                .build();
        List<BlogOwner> datastoreResultList = new ArrayList<>();
        int maxRecords = 50;
        datastoreResultList.add(blogOwner);
        SyncStatus mockSyncStatus = mock(SyncStatus.class);
        when(mockSyncStatus.get(any(), any())).thenReturn(false);
        Subject<StorageItemChange<? extends Model>> subject =
                PublishSubject.<StorageItemChange<? extends Model>>create().toSerialized();
        Consumer<DataStoreQuerySnapshot<BlogOwner>> onQuerySnapshot = value -> {
            if (count.get() == 0) {
                Assert.assertTrue(value.getItems().contains(blogOwner));
                latch.countDown();
            } else if (count.get() == 1) {
                Assert.assertEquals(6, value.getItems().size());
                changeLatch.countDown();
            }
            count.getAndIncrement();
        };
        Consumer<DataStoreException> onObservationError = NoOpConsumer.create();
        Consumer<Cancelable> observationStarted = value -> {
            for (int i = 0; i < 5; i++) {
                BlogOwner itemChange = BlogOwner.builder()
                        .name("Alan Turing" + i)
                        .build();
                try {
                    subject.onNext(StorageItemChange.<BlogOwner>builder()
                            .changeId(UUID.randomUUID().toString())
                            .initiator(StorageItemChange.Initiator.SYNC_ENGINE)
                            .item(itemChange)
                            .patchItem(SerializedModel.create(itemChange,
                                    ModelSchema.fromModelClass(BlogOwner.class)))
                            .modelSchema(ModelSchema.fromModelClass(BlogOwner.class))
                            .predicate(QueryPredicates.all())
                            .type(StorageItemChange.Type.UPDATE)
                            .build());
                } catch (AmplifyException exception) {
                    exception.printStackTrace();
                }
            }
        };
        Action onObservationComplete = () -> { };
        SqlQueryProcessor mockSqlQueryProcessor = mock(SqlQueryProcessor.class);
        when(mockSqlQueryProcessor.queryOfflineData(eq(BlogOwner.class), any(), any()))
                .thenReturn(datastoreResultList);
        when(mockSqlQueryProcessor.modelExists(any(), any())).thenReturn(true);
        ExecutorService threadPool = Executors.newFixedThreadPool(
                Runtime.getRuntime().availableProcessors() * 5);
        ObserveQueryExecutor<BlogOwner> observeQueryExecutor = new ObserveQueryExecutor<>(subject,
                                                                            mockSqlQueryProcessor,
                                                                            threadPool,
                                                                            mockSyncStatus,
                                                                            new ModelSorter<>(),
                                                                            maxRecords, 1);

        observeQueryExecutor.observeQuery(
                BlogOwner.class,
                new ObserveQueryOptions(), observationStarted,
                onQuerySnapshot,
                onObservationError,
                onObservationComplete);
        Assert.assertTrue(latch.await(1, TimeUnit.SECONDS));
        Assert.assertTrue(changeLatch.await(5, TimeUnit.SECONDS));
    }

    /***
     * testing cancel on observe query.
     * @throws DataStoreException DataStoreException
     */
    @Test
    public void observeQueryCancelsTheOperationOnCancel() throws DataStoreException {
        final BlogOwner blogOwner = BlogOwner.builder()
                .name("Alan Turing")
                .build();
        List<BlogOwner> resultList = new ArrayList<>();
        resultList.add(blogOwner);
        Consumer<DataStoreQuerySnapshot<BlogOwner>> onQuerySnapshot = NoOpConsumer.create();
        Consumer<DataStoreException> onObservationError = NoOpConsumer.create();
        Action onObservationComplete = () -> { };
        SqlQueryProcessor mockSqlQueryProcessor = mock(SqlQueryProcessor.class);
        when(mockSqlQueryProcessor.queryOfflineData(eq(BlogOwner.class), any(), any())).thenReturn(resultList);
        Subject<StorageItemChange<? extends Model>> subject =
                PublishSubject.<StorageItemChange<? extends Model>>create().toSerialized();
        ExecutorService threadPool = Executors.newFixedThreadPool(
                Runtime.getRuntime().availableProcessors() * 5);
        ObserveQueryExecutor<BlogOwner> observeQueryExecutor =
                new ObserveQueryExecutor<>(subject,
                        mockSqlQueryProcessor,
                        threadPool,
                        mock(SyncStatus.class),
                        new ModelSorter<>(),
                        DataStoreConfiguration.defaults());
        Consumer<Cancelable> observationStarted = value -> {
            value.cancel();
            Assert.assertTrue(observeQueryExecutor.getIsCancelled());
            assertEquals(0, observeQueryExecutor.getCompleteMap().size());
            assertEquals(0, observeQueryExecutor.getChangeList().size());
            subject.test()
                    .assertNoErrors().
                    isDisposed();
        };
        observeQueryExecutor.observeQuery(
                BlogOwner.class,
                new ObserveQueryOptions(null, null),
                observationStarted,
                onQuerySnapshot,
                onObservationError,
                onObservationComplete);
    }

    /***
     * testing cancel on observe query.
     * @throws DataStoreException DataStoreException
     */
    @Test
    public void observeQueryCancelsTheOperationOnQueryError() throws DataStoreException {
        Consumer<DataStoreQuerySnapshot<BlogOwner>> onQuerySnapshot = NoOpConsumer.create();
        Consumer<DataStoreException> onObservationError = NoOpConsumer.create();
        Action onObservationComplete = NoOpAction.create();
        SQLCommandProcessor sqlCommandProcessor = mock(SQLCommandProcessor.class);
        when(sqlCommandProcessor.rawQuery(any())).thenThrow(new DataStoreException("test", "test"));

        SqlQueryProcessor sqlQueryProcessor = new SqlQueryProcessor(sqlCommandProcessor,
                mock(SQLiteCommandFactory.class),
                mock(SchemaRegistry.class));
        Subject<StorageItemChange<? extends Model>> subject =
                PublishSubject.<StorageItemChange<? extends Model>>create().toSerialized();
        ExecutorService threadPool = Executors.newFixedThreadPool(
                Runtime.getRuntime().availableProcessors() * 5);
        ObserveQueryExecutor<BlogOwner> observeQueryExecutor =
                new ObserveQueryExecutor<>(subject,
                        sqlQueryProcessor,
                        threadPool,
                        mock(SyncStatus.class),
                        new ModelSorter<>(),
                        DataStoreConfiguration.defaults());
        Consumer<Cancelable> observationStarted = value -> {
            value.cancel();
            Assert.assertTrue(observeQueryExecutor.getIsCancelled());
            assertEquals(0, observeQueryExecutor.getCompleteMap().size());
            assertEquals(0, observeQueryExecutor.getChangeList().size());
            subject.test()
                    .assertNoErrors().
                    isDisposed();
        };
        observeQueryExecutor.observeQuery(
                BlogOwner.class,
                new ObserveQueryOptions(null, null),
                observationStarted,
                onQuerySnapshot,
                onObservationError,
                onObservationComplete);
    }

    /***
     * Observe Query Should not return deleted record.
     * @throws InterruptedException InterruptedException
     * @throws DataStoreException DataStoreException
     */
    @Test
    public void observeQueryShouldNotReturnDeletedRecord() throws InterruptedException, DataStoreException {
        CountDownLatch latch = new CountDownLatch(1);
        CountDownLatch changeLatch = new CountDownLatch(1);
        BlogOwner blogOwner = BlogOwner.builder()
                .name("Alan Turing")
                .build();
        List<BlogOwner> datastoreResultList = new ArrayList<>();
        int maxRecords = 50;
        datastoreResultList.add(blogOwner);
        SyncStatus mockSyncStatus = mock(SyncStatus.class);
        when(mockSyncStatus.get(any(), any())).thenReturn(true);
        Subject<StorageItemChange<? extends Model>> subject =
                PublishSubject.<StorageItemChange<? extends Model>>create().toSerialized();
        Consumer<DataStoreQuerySnapshot<BlogOwner>> onQuerySnapshot = value -> {
            if (latch.getCount() > 0) {
                Assert.assertTrue(value.getItems().contains(blogOwner));
                latch.countDown();
            } else if (latch.getCount() == 0) {
                Assert.assertFalse(value.getItems().contains(blogOwner));
                changeLatch.countDown();
            }
        };
        Consumer<Cancelable> observationStarted = value -> {
            try {
                subject.onNext(StorageItemChange.<BlogOwner>builder()
                        .changeId(UUID.randomUUID().toString())
                        .initiator(StorageItemChange.Initiator.SYNC_ENGINE)
                        .item(blogOwner)
                        .patchItem(SerializedModel.create(blogOwner,
                                ModelSchema.fromModelClass(BlogOwner.class)))
                        .modelSchema(ModelSchema.fromModelClass(BlogOwner.class))
                        .predicate(QueryPredicates.all())
                        .type(StorageItemChange.Type.DELETE)
                        .build());
            } catch (AmplifyException exception) {
                exception.printStackTrace();
            }
        };
        Consumer<DataStoreException> onObservationError = NoOpConsumer.create();
        Action onObservationComplete = () -> { };
        SqlQueryProcessor mockSqlQueryProcessor = mock(SqlQueryProcessor.class);
        when(mockSqlQueryProcessor.queryOfflineData(eq(BlogOwner.class), any(), any()))
                .thenReturn(datastoreResultList);
        when(mockSqlQueryProcessor.modelExists(any(), any())).thenReturn(false);
        ExecutorService threadPool = Executors.newFixedThreadPool(
                Runtime.getRuntime().availableProcessors() * 5);
        ObserveQueryExecutor<BlogOwner> observeQueryExecutor = new ObserveQueryExecutor<>(subject,
                mockSqlQueryProcessor,
                threadPool,
                mockSyncStatus,
                new ModelSorter<>(),
                maxRecords, 2);
        observeQueryExecutor.observeQuery(
                BlogOwner.class,
                new ObserveQueryOptions(), observationStarted,
                onQuerySnapshot,
                onObservationError,
                onObservationComplete);
        Assert.assertTrue(latch.await(1, TimeUnit.SECONDS));
        Assert.assertTrue(changeLatch.await(10, TimeUnit.SECONDS));
    }
}
