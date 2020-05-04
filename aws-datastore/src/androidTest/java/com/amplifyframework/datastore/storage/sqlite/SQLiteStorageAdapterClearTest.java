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

package com.amplifyframework.datastore.storage.sqlite;

import android.content.Context;
import androidx.test.core.app.ApplicationProvider;

import com.amplifyframework.datastore.DataStoreException;
import com.amplifyframework.datastore.StrictMode;
import com.amplifyframework.datastore.storage.StorageItemChange;
import com.amplifyframework.datastore.storage.SynchronousStorageAdapter;
import com.amplifyframework.testmodels.commentsblog.AmplifyModelProvider;
import com.amplifyframework.testmodels.commentsblog.BlogOwner;

import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.attribute.FileTime;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import io.reactivex.disposables.Disposable;
import io.reactivex.observers.TestObserver;

import static com.amplifyframework.datastore.storage.sqlite.SQLiteStorageAdapter.DATABASE_NAME;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public final class SQLiteStorageAdapterClearTest {
    private SynchronousStorageAdapter adapter;
    private Context context;
    private TestObserver<StorageItemChange.Record> observer;
    private AtomicReference<Disposable> subscriberDisposableRef = new AtomicReference<>();

    @BeforeClass
    public static void enableStrictMode() {
        StrictMode.enable();
    }

    @Before
    public void setup() {
        TestStorageAdapter.cleanup();
        context = ApplicationProvider.getApplicationContext();
        adapter = TestStorageAdapter.create(AmplifyModelProvider.getInstance());
        observer = TestObserver.create();
        //Set subscriberDisposableRef = <value received from RxJava>.
        //Needed so we can make assertions on the state of the subscriber later.
        adapter.observe()
            .doOnSubscribe(subscriberDisposableRef::set)
            .subscribe(observer);

    }

    @After
    public void teardown() {
        TestStorageAdapter.cleanup(adapter);
    }

    /**
     * Save a record to the database and verify it was saved.
     * Then call clear and verify that the database file is re-created
     * and is writable.
     * @throws DataStoreException bubbles up exceptions thrown from the adapter
     * @throws IOException if it can't ready file creation time
     * @throws InterruptedException if something happens when sleeping for 1 second
     */
    @Test
    public void clearDeletesAndRecreatesDatabase() throws DataStoreException, IOException, InterruptedException {
        BlogOwner blogger1 = createBlogger("Dummy Blogger Sr.");
        BlogOwner blogger2 = createBlogger("Dummy Blogger Jr.");
        //Save a record and check if it's there
        adapter.save(blogger1);
        assertRecordIsInDb(blogger1.getName());
        FileTime dbFileCreationTime1 = getDbFileCreationTime();
        //Verify observer is still alive
        assertFalse(subscriberDisposableRef.get().isDisposed());
        assertObserverReceivedRecord(blogger1);
        //The ensures files are created at least 1 second apart
        Thread.sleep(1000);

        adapter.clear();
        //Make sure file was re-created
        assertDbFileExists();
        //Verify observer is still alive
        assertFalse(subscriberDisposableRef.get().isDisposed());
        FileTime dbFileCreationTime2 = getDbFileCreationTime();
        //Make sure a new file was actually created by comparing
        //the creation timestamps
        assertNotEquals(dbFileCreationTime1, dbFileCreationTime2);

        //Make sure the new file is writable
        adapter.save(blogger2);
        //Check the new record is in the database
        //and the old record is not.
        assertRecordIsInDb(blogger2.getName());
        assertRecordIsNotInDb(blogger1.getName());
        assertObserverReceivedRecord(blogger2);
        //Terminate the adapter
        adapter.terminate();
        //Verify observer was disposed.
        assertTrue(subscriberDisposableRef.get().isDisposed());

    }

    private BlogOwner createBlogger(String name) throws DataStoreException {
        return BlogOwner.builder()
            .name(name)
            .build();
    }

    private void assertObserverReceivedRecord(BlogOwner blogger) {
        long count = observer
            .values()
            .stream()
            .filter(record -> {
                return record.getEntry().contains(blogger.getName());
            })
            .count();
        assertEquals(1, count);
    }

    private void assertRecordIsInDb(String name) throws DataStoreException {
        List<BlogOwner> blogOwners = adapter.query(BlogOwner.class, BlogOwner.NAME.eq(name));
        assertNotNull(blogOwners);
        assertEquals(1, blogOwners.size());
    }

    private void assertRecordIsNotInDb(String name) throws DataStoreException {
        List<BlogOwner> blogOwners = adapter.query(BlogOwner.class, BlogOwner.NAME.eq(name));
        assertNotNull(blogOwners);
        assertEquals(0, blogOwners.size());
    }

    private FileTime getDbFileCreationTime() throws IOException {
        return (FileTime) Files.getAttribute(
            context.getDatabasePath(DATABASE_NAME).toPath(), "creationTime");
    }

    private void assertDbFileExists() {
        assertTrue(context.getDatabasePath(DATABASE_NAME).exists());
    }
}
