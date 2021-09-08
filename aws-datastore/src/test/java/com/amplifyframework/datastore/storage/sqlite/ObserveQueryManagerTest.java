package com.amplifyframework.datastore.storage.sqlite;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;


import com.amplifyframework.AmplifyException;
import com.amplifyframework.core.Action;
import com.amplifyframework.core.Consumer;
import com.amplifyframework.core.async.Cancelable;
import com.amplifyframework.core.model.Model;
import com.amplifyframework.core.model.ModelSchema;
import com.amplifyframework.core.model.SerializedModel;
import com.amplifyframework.core.model.query.Where;
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

public class ObserveQueryManagerTest {


    @Test
    public void observeQueryReturnsOfflineData() throws InterruptedException, DataStoreException {
        CountDownLatch latch = new CountDownLatch(1);
        final BlogOwner blogOwner = BlogOwner.builder()
                .name("Alan Turing")
                .build();
        List<BlogOwner> resultList = new ArrayList<>();
        resultList.add(blogOwner);

        Consumer<Cancelable> observationStarted = value -> {};
        Consumer<DataStoreQuerySnapshot<BlogOwner>> onQuerySnapshot = value->{
            Assert.assertTrue(value.getItems().contains(blogOwner));
            latch.countDown();
        };
        Consumer<DataStoreException> onObservationError = value-> {};
        Action onObservationComplete= () -> {};
        SqlQueryProcessor mockSqlQueryProcessor = mock(SqlQueryProcessor.class);
        when(mockSqlQueryProcessor.queryOfflineData(eq(BlogOwner.class), any(), any())).thenReturn(resultList);
        Subject<StorageItemChange<? extends Model>> subject =  PublishSubject.<StorageItemChange<? extends Model>>create().toSerialized();
        ExecutorService threadPool = Executors.newFixedThreadPool(
                Runtime.getRuntime().availableProcessors() * 5);
                ObserveQueryManager<BlogOwner> observeQueryManager =
                        new ObserveQueryManager<BlogOwner>(subject,
                                mockSqlQueryProcessor,
                                threadPool,
                                mock(SyncStatus.class),
                                DataStoreConfiguration.defaults());


        observeQueryManager.observeQuery(
                BlogOwner.class,
                Where.matchesAll(), observationStarted, onQuerySnapshot, onObservationError,
                onObservationComplete);
        Assert.assertTrue(latch.await(1, TimeUnit.SECONDS));

    }


    @Test
    public void observeQueryReturnsRecordsBasedOnMaxRecords() throws InterruptedException, DataStoreException {
        CountDownLatch latch = new CountDownLatch(1);
        CountDownLatch changeLatch = new CountDownLatch(1);
        AtomicInteger count = new AtomicInteger();
        BlogOwner blogOwner = BlogOwner.builder()
                .name("Alan Turing")
                .build();
        List<BlogOwner> datastoreResultList = new ArrayList<>();
        int maxRecords = 2;
        datastoreResultList.add(blogOwner);
        Consumer<Cancelable> observationStarted = value -> {};
        SyncStatus mockSyncStatus = mock(SyncStatus.class);
        when(mockSyncStatus.get(any(), any())).thenReturn(false);
        Subject<StorageItemChange<? extends Model>> subject =  PublishSubject.<StorageItemChange<? extends Model>>create().toSerialized();
        Consumer<DataStoreQuerySnapshot<BlogOwner>> onQuerySnapshot = value->{
            if ( count.get() == 0){
                Assert.assertTrue(value.getItems().contains(blogOwner));
                latch.countDown();
            } else if ( count.get() == 1){
                Assert.assertEquals(value.getItemChanges().size(), 2);
            }
            else {
                Assert.assertEquals(value.getItemChanges().size(), 2);
                changeLatch.countDown();
            }
            count.getAndIncrement();
        };
        Consumer<DataStoreException> onObservationError = value-> {};
        Action onObservationComplete= () -> {};
        SqlQueryProcessor mockSqlQueryProcessor = mock(SqlQueryProcessor.class);
        when(mockSqlQueryProcessor.queryOfflineData(eq(BlogOwner.class), any(), any())).thenReturn(datastoreResultList);
        when(mockSqlQueryProcessor.modelExists(any(),any())).thenReturn(true);

        ExecutorService threadPool = Executors.newFixedThreadPool(
                Runtime.getRuntime().availableProcessors() * 5);
        ObserveQueryManager<BlogOwner> observeQueryManager = new ObserveQueryManager<BlogOwner>(subject, mockSqlQueryProcessor,threadPool, mockSyncStatus, maxRecords, maxRecords);


        observeQueryManager.observeQuery(
                BlogOwner.class,
                Where.matchesAll(), observationStarted, onQuerySnapshot, onObservationError, onObservationComplete);
        Assert.assertTrue(latch.await(1, TimeUnit.SECONDS));
        for ( int i = 0; i < 5; i++){
            BlogOwner itemChange = BlogOwner.builder()
                    .name("Alan Turing" + i)
                    //.id("" + i + "")
                    .build();
            try {
                subject.onNext(StorageItemChange.<BlogOwner>builder()
                        .changeId(UUID.randomUUID().toString())
                        .initiator(StorageItemChange.Initiator.DATA_STORE_API)
                        .item(itemChange)
                        .patchItem(SerializedModel.create(itemChange, ModelSchema.fromModelClass(BlogOwner.class)))
                        .modelSchema(ModelSchema.fromModelClass(BlogOwner.class))
                        .predicate(QueryPredicates.all())
                        .type(StorageItemChange.Type.UPDATE)
                        .build());
            } catch (AmplifyException e) {
                e.printStackTrace();
            }
        }
        Assert.assertTrue(changeLatch.await(1, TimeUnit.SECONDS));
    }

}