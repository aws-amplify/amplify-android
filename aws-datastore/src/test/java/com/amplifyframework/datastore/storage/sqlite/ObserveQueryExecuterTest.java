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
import com.amplifyframework.core.async.Cancelable;
import com.amplifyframework.core.model.Model;
import com.amplifyframework.core.model.ModelSchema;
import com.amplifyframework.core.model.SerializedModel;
import com.amplifyframework.core.model.query.ObserveQueryOptions;
import com.amplifyframework.core.model.query.QuerySortBy;
import com.amplifyframework.core.model.query.predicate.QueryPredicates;
import com.amplifyframework.datastore.DataStoreConfiguration;
import com.amplifyframework.datastore.DataStoreException;
import com.amplifyframework.datastore.DataStoreQuerySnapshot;
import com.amplifyframework.datastore.storage.StorageItemChange;
import com.amplifyframework.testmodels.commentsblog.BlogOwner;
import com.amplifyframework.testmodels.commentsblog.Post;
import com.amplifyframework.testmodels.commentsblog.PostStatus;

import org.junit.Assert;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
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

public class ObserveQueryExecuterTest {

    /***
     * Tests for ObserveQueryExecuter.
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

        Consumer<Cancelable> observationStarted = value -> { };
        Consumer<DataStoreQuerySnapshot<BlogOwner>> onQuerySnapshot = value -> {
            Assert.assertTrue(value.getItems().contains(blogOwner));
            latch.countDown(); };
        Consumer<DataStoreException> onObservationError = value -> { };
        Action onObservationComplete = () -> { };
        SqlQueryProcessor mockSqlQueryProcessor = mock(SqlQueryProcessor.class);
        when(mockSqlQueryProcessor.queryOfflineData(eq(BlogOwner.class), any(), any())).thenReturn(resultList);
        Subject<StorageItemChange<? extends Model>> subject =
                PublishSubject.<StorageItemChange<? extends Model>>create().toSerialized();
        ExecutorService threadPool = Executors.newFixedThreadPool(
                Runtime.getRuntime().availableProcessors() * 5);
        ObserveQueryExecuter<BlogOwner> observeQueryExecuter =
                new ObserveQueryExecuter<>(subject,
                        mockSqlQueryProcessor,
                        threadPool,
                        mock(SyncStatus.class),
                        new ModelSorter<>(),
                        DataStoreConfiguration.defaults());
        observeQueryExecuter.observeQuery(
                BlogOwner.class,
                new ObserveQueryOptions(null, null),
                observationStarted,
                onQuerySnapshot,
                onObservationError,
                onObservationComplete);
        Assert.assertTrue(latch.await(1, TimeUnit.SECONDS));
    }

    /***
     * observe Query Returns Records Based On Max Records.
     * @throws InterruptedException InterruptedException
     * @throws DataStoreException DataStoreException
     * @throws AmplifyException AmplifyException
     */
    @Test
    public void observeQueryReturnsRecordsBasedOnMaxRecords() throws InterruptedException, AmplifyException {
        CountDownLatch latch = new CountDownLatch(1);
        CountDownLatch changeLatch = new CountDownLatch(2);
        AtomicInteger count = new AtomicInteger();
        BlogOwner blogOwner = BlogOwner.builder()
                .name("Alan Turing")
                .build();
        List<BlogOwner> datastoreResultList = new ArrayList<>();
        int maxRecords = 2;
        datastoreResultList.add(blogOwner);
        Consumer<Cancelable> observationStarted = value -> { };
        SyncStatus mockSyncStatus = mock(SyncStatus.class);
        when(mockSyncStatus.get(any(), any())).thenReturn(false);
        Subject<StorageItemChange<? extends Model>> subject =
                PublishSubject.<StorageItemChange<? extends Model>>create().toSerialized();
        Consumer<DataStoreQuerySnapshot<BlogOwner>> onQuerySnapshot = value -> {
            if (count.get() == 0) {
                Assert.assertTrue(value.getItems().contains(blogOwner));
                latch.countDown();
            } else if (count.get() == 1) {
                Assert.assertEquals(3, value.getItems().size());
                changeLatch.countDown();
            } else {
                Assert.assertEquals(5, value.getItems().size());
                changeLatch.countDown();
            }
            count.getAndIncrement();
        };
        Consumer<DataStoreException> onObservationError = value -> { };
        Action onObservationComplete = () -> { };
        SqlQueryProcessor mockSqlQueryProcessor = mock(SqlQueryProcessor.class);
        when(mockSqlQueryProcessor.queryOfflineData(eq(BlogOwner.class), any(), any()))
                .thenReturn(datastoreResultList);
        when(mockSqlQueryProcessor.modelExists(any(), any())).thenReturn(true);

        ExecutorService threadPool = Executors.newFixedThreadPool(
                Runtime.getRuntime().availableProcessors() * 5);
        ObserveQueryExecuter<BlogOwner> observeQueryExecuter = new ObserveQueryExecuter<>(subject,
                mockSqlQueryProcessor,
                threadPool,
                mockSyncStatus,
                new ModelSorter<>(),
                maxRecords,
                maxRecords);

        observeQueryExecuter.observeQuery(
                BlogOwner.class,
                new ObserveQueryOptions(null, null),
                observationStarted,
                onQuerySnapshot,
                onObservationError,
                onObservationComplete);
        Assert.assertTrue(latch.await(1, TimeUnit.SECONDS));
        for (int i = 0; i < 5; i++) {
            BlogOwner itemChange = BlogOwner.builder()
                    .name("Alan Turing" + i)
                    .build();
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
        }
        Assert.assertTrue(changeLatch.await(7, TimeUnit.SECONDS));
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
        Consumer<Cancelable> observationStarted = value -> { };
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
        Consumer<DataStoreException> onObservationError = value -> { };
        Action onObservationComplete = () -> { };
        SqlQueryProcessor mockSqlQueryProcessor = mock(SqlQueryProcessor.class);
        when(mockSqlQueryProcessor.queryOfflineData(eq(BlogOwner.class), any(), any()))
                .thenReturn(datastoreResultList);
        when(mockSqlQueryProcessor.modelExists(any(), any())).thenReturn(true);
        ExecutorService threadPool = Executors.newFixedThreadPool(
                Runtime.getRuntime().availableProcessors() * 5);
        ObserveQueryExecuter<BlogOwner> observeQueryExecuter = new ObserveQueryExecuter<>(subject,
                                                                            mockSqlQueryProcessor,
                                                                            threadPool,
                                                                            mockSyncStatus,
                                                                            new ModelSorter<>(),
                                                                            maxRecords, 1);

        observeQueryExecuter.observeQuery(
                BlogOwner.class,
                new ObserveQueryOptions(), observationStarted,
                onQuerySnapshot,
                onObservationError,
                onObservationComplete);
        Assert.assertTrue(latch.await(1, TimeUnit.SECONDS));
        for (int i = 0; i < 5; i++) {
            BlogOwner itemChange = BlogOwner.builder()
                    .name("Alan Turing" + i)
                    //.id("" + i + "")
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
        Assert.assertTrue(changeLatch.await(5, TimeUnit.SECONDS));
    }

    /***
     * observe Query Returns Sorted List Of Total Items.
     * @throws InterruptedException InterruptedException
     * @throws DataStoreException DataStoreException
     */
    @Test
    public void observeQueryReturnsSortedListOfTotalItems() throws InterruptedException,
            DataStoreException {
        CountDownLatch latch = new CountDownLatch(1);
        CountDownLatch changeLatch = new CountDownLatch(1);
        AtomicInteger count = new AtomicInteger();
        List<String> names = Arrays.asList("John", "Jacob", "Joe", "Bob", "Bobby", "Bobb", "Dan", "Dany", "Daniel");
        List<String> weas = Arrays.asList("pon", "lth", "ver", "kly", "ken", "sel", "ner", "rer", "ned");
        List<BlogOwner> owners = new ArrayList<>();

        for (int i = 0; i < names.size() / 2; i++) {
            BlogOwner owner = BlogOwner.builder()
                    .name(names.get(i))
                    .wea(weas.get(i))
                    .build();
            owners.add(owner);
        }
        int maxRecords = 50;
        Consumer<Cancelable> observationStarted = value -> { };
        SyncStatus mockSyncStatus = mock(SyncStatus.class);
        when(mockSyncStatus.get(any(), any())).thenReturn(false);
        Subject<StorageItemChange<? extends Model>> subject =
                PublishSubject.<StorageItemChange<? extends Model>>create().toSerialized();
        Consumer<DataStoreQuerySnapshot<BlogOwner>> onQuerySnapshot = value -> {
            if (count.get() == 0) {
                Assert.assertTrue(value.getItems().contains(owners.get(0)));
                latch.countDown();
            } else if (count.get() == 1) {
                List<BlogOwner> sorted = new ArrayList<>(owners);
                Collections.sort(sorted, Comparator
                        .comparing(BlogOwner::getName)
                        .reversed()
                        .thenComparing(BlogOwner::getWea)
                );
                assertEquals(sorted, value.getItems());
                Assert.assertEquals(8, value.getItems().size());
                changeLatch.countDown();
            }
            count.getAndIncrement();
        };
        Consumer<DataStoreException> onObservationError = value -> { };
        Action onObservationComplete = () -> { };
        SqlQueryProcessor mockSqlQueryProcessor = mock(SqlQueryProcessor.class);
        when(mockSqlQueryProcessor.queryOfflineData(eq(BlogOwner.class), any(), any())).thenReturn(owners);
        when(mockSqlQueryProcessor.modelExists(any(), any())).thenReturn(true);

        ExecutorService threadPool = Executors.newFixedThreadPool(
                Runtime.getRuntime().availableProcessors() * 5);
        ObserveQueryExecuter<BlogOwner> observeQueryExecuter = new ObserveQueryExecuter<>(subject,
                                                         mockSqlQueryProcessor,
                                                         threadPool,
                                                         mockSyncStatus,
                                                         new ModelSorter<>(), maxRecords, 1);

        List<QuerySortBy> sortBy = new ArrayList<>();
        sortBy.add(BlogOwner.NAME.descending());
        sortBy.add(BlogOwner.WEA.ascending());
        observeQueryExecuter.observeQuery(
                BlogOwner.class,
                new ObserveQueryOptions(null, sortBy),
                observationStarted,
                onQuerySnapshot,
                onObservationError,
                onObservationComplete);
        Assert.assertTrue(latch.await(1, TimeUnit.SECONDS));
        for (int i = (names.size() / 2) + 1; i < names.size(); i++) {
            BlogOwner itemChange = BlogOwner.builder()
                    .name(names.get(i))
                    .wea(weas.get(i))
                    .build();
            owners.add(itemChange);
            try {
                subject.onNext(StorageItemChange.<BlogOwner>builder()
                        .changeId(UUID.randomUUID().toString())
                        .initiator(StorageItemChange.Initiator.SYNC_ENGINE)
                        .item(itemChange)
                        .patchItem(SerializedModel.create(itemChange,
                                ModelSchema.fromModelClass(BlogOwner.class)))
                        .modelSchema(ModelSchema.fromModelClass(BlogOwner.class))
                        .predicate(QueryPredicates.all())
                        .type(StorageItemChange.Type.CREATE)
                        .build());
            } catch (AmplifyException exception) {
                exception.printStackTrace();
            }
        }
        Assert.assertTrue(changeLatch.await(5, TimeUnit.SECONDS));
    }

    /***
     * ObserveQuery returns sorted list of total items with int.
     * @throws InterruptedException interrupted exception.
     * @throws AmplifyException data store exception.
     */
    @Test
    public void observeQueryReturnsSortedListOfTotalItemsWithInt() throws InterruptedException,
            AmplifyException {
        CountDownLatch latch = new CountDownLatch(1);
        CountDownLatch changeLatch = new CountDownLatch(1);
        AtomicInteger count = new AtomicInteger();
        List<Post> posts = new ArrayList<>();

        for (int counter = 0; counter < 5; counter++) {
            final Post post = Post.builder()
                    .title(counter + "-title")
                    .status(PostStatus.INACTIVE)
                    .rating(counter)
                    .build();
            posts.add(post);
        }
        int maxRecords = 50;
        Consumer<Cancelable> observationStarted = value -> { };
        SyncStatus mockSyncStatus = mock(SyncStatus.class);
        when(mockSyncStatus.get(any(), any())).thenReturn(false);
        Subject<StorageItemChange<? extends Model>> subject =
                PublishSubject.<StorageItemChange<? extends Model>>create().toSerialized();
        Consumer<DataStoreQuerySnapshot<Post>> onQuerySnapshot = value -> {
            if (count.get() == 0) {
                Assert.assertTrue(value.getItems().contains(posts.get(0)));
                latch.countDown();
            } else if (count.get() == 1) {
                List<Post> sorted = new ArrayList<>(posts);
                Collections.sort(sorted, Comparator.comparing(Post::getRating));
                assertEquals(sorted, value.getItems());
                Assert.assertEquals(11, value.getItems().size());
                changeLatch.countDown();
            }
            count.getAndIncrement();
        };
        Consumer<DataStoreException> onObservationError = value -> { };
        Action onObservationComplete = () -> { };
        SqlQueryProcessor mockSqlQueryProcessor = mock(SqlQueryProcessor.class);
        when(mockSqlQueryProcessor.queryOfflineData(eq(Post.class), any(), any())).thenReturn(posts);
        when(mockSqlQueryProcessor.modelExists(any(), any())).thenReturn(true);

        ExecutorService threadPool = Executors.newFixedThreadPool(
                Runtime.getRuntime().availableProcessors() * 5);
        ObserveQueryExecuter<Post> observeQueryExecuter = new ObserveQueryExecuter<>(subject,
            mockSqlQueryProcessor,
            threadPool,
            mockSyncStatus,
            new ModelSorter<>(),
            maxRecords, 2);

        List<QuerySortBy> sortBy = new ArrayList<>();
        sortBy.add(Post.RATING.ascending());
        observeQueryExecuter.observeQuery(
                Post.class,
                new ObserveQueryOptions(null, sortBy),
                observationStarted,
                onQuerySnapshot,
                onObservationError,
                onObservationComplete);
        Assert.assertTrue(latch.await(2, TimeUnit.SECONDS));
        for (int i = 5; i < 11; i++) {
            Post itemChange = Post.builder()
                    .title(i + "-title")
                    .status(PostStatus.INACTIVE)
                    .rating(i)
                    .build();
            posts.add(itemChange);
            subject.onNext(StorageItemChange.<Post>builder()
                .changeId(UUID.randomUUID().toString())
                .initiator(StorageItemChange.Initiator.SYNC_ENGINE)
                .item(itemChange)
                .patchItem(SerializedModel.create(itemChange,
                        ModelSchema.fromModelClass(Post.class)))
                .modelSchema(ModelSchema.fromModelClass(BlogOwner.class))
                .predicate(QueryPredicates.all())
                .type(StorageItemChange.Type.CREATE)
                .build());
        }
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
        Consumer<DataStoreQuerySnapshot<BlogOwner>> onQuerySnapshot = value -> { };
        Consumer<DataStoreException> onObservationError = value -> { };
        Action onObservationComplete = () -> { };
        SqlQueryProcessor mockSqlQueryProcessor = mock(SqlQueryProcessor.class);
        when(mockSqlQueryProcessor.queryOfflineData(eq(BlogOwner.class), any(), any())).thenReturn(resultList);
        Subject<StorageItemChange<? extends Model>> subject =
                PublishSubject.<StorageItemChange<? extends Model>>create().toSerialized();
        ExecutorService threadPool = Executors.newFixedThreadPool(
                Runtime.getRuntime().availableProcessors() * 5);
        ObserveQueryExecuter<BlogOwner> observeQueryExecuter =
                new ObserveQueryExecuter<>(subject,
                        mockSqlQueryProcessor,
                        threadPool,
                        mock(SyncStatus.class),
                        new ModelSorter<>(),
                        DataStoreConfiguration.defaults());
        Consumer<Cancelable> observationStarted = value -> {
            value.cancel();
            Assert.assertTrue(observeQueryExecuter.getIsCancelled());
            assertEquals(0, observeQueryExecuter.getCompleteMap().size());
            assertEquals(0, observeQueryExecuter.getChangeList().size());
            subject.test()
                    .assertNoErrors().
                    isDisposed();
        };
        observeQueryExecuter.observeQuery(
                BlogOwner.class,
                new ObserveQueryOptions(null, null),
                observationStarted,
                onQuerySnapshot,
                onObservationError,
                onObservationComplete);
    }
}
