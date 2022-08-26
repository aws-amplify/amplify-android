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
import android.os.FileObserver;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.test.core.app.ApplicationProvider;

import com.amplifyframework.AmplifyException;
import com.amplifyframework.core.model.Model;
import com.amplifyframework.core.model.query.Where;
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

import java.util.List;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

import io.reactivex.rxjava3.observers.TestObserver;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public final class SQLiteStorageAdapterClearTest {
    private static final String DATABASE_NAME = "AmplifyDatastore.db";
    private static final long TIMEOUT_MS = TimeUnit.SECONDS.toMillis(5);

    private SynchronousStorageAdapter adapter;
    private Context context;
    private TestObserver<StorageItemChange<? extends Model>> observer;
    private TestFileObserver fileObserver;

    /**
     * Enable strict mode.
     */
    @BeforeClass
    public static void enableStrictMode() {
        StrictMode.enable();
    }

    /**
     * Setup actions for every test in this class.
     */
    @Before
    public void setup() {
        TestStorageAdapter.cleanup();
        context = ApplicationProvider.getApplicationContext();
        adapter = TestStorageAdapter.create(AmplifyModelProvider.getInstance());
        observer = adapter
            .observe()
            .test();
        fileObserver = new TestFileObserver(Objects.requireNonNull(context.getDatabasePath(DATABASE_NAME).getParent()));
        fileObserver.startWatching();
    }

    /**
     * Tearing down components created for each test.
     */
    @After
    public void teardown() {
        fileObserver.stopWatching();
        TestStorageAdapter.cleanup(adapter);
    }

    /**
     * Save a record to the database and verify it was saved.
     * Then call clear and verify that the database file is re-created
     * and is writable.
     * @throws DataStoreException bubbles up exceptions thrown from the adapter
     * @throws InterruptedException If interrupted while test observer awaits terminal result.
     * @throws AmplifyException If schema cannot be found in the registry.
     */
    @Test
    public void clearDeletesAndRecreatesDatabase() throws AmplifyException, InterruptedException {
        assertDbFileExists();
        assertEquals(0, fileObserver.createFileEventCount);
        assertEquals(0, fileObserver.deleteFileEventCount);
        BlogOwner blogger1 = createBlogger("Dummy Blogger Sr.");
        BlogOwner blogger2 = createBlogger("Dummy Blogger Jr.");
        //Save a record and check if it's there
        adapter.save(blogger1);
        assertRecordIsInDb(blogger1);
        //Verify observer is still alive
        assertFalse(observer.isDisposed());
        assertObserverReceivedRecord(blogger1);

        adapter.clear();
        //Make sure file was deleted and re-created
        assertEquals(1, fileObserver.createFileEventCount);
        assertEquals(1, fileObserver.deleteFileEventCount);
        assertDbFileExists();
        //Verify observer is still alive
        assertFalse(observer.isDisposed());

        //Make sure the new file is writable
        adapter.save(blogger2);
        //Check the new record is in the database
        //and the old record is not.
        assertRecordIsInDb(blogger2);
        assertRecordIsNotInDb(blogger1);
        assertObserverReceivedRecord(blogger2);
        //Terminate the adapter
        adapter.terminate();
        //Verify observer was disposed.
        observer.assertComplete();
        observer.await(TIMEOUT_MS, TimeUnit.MILLISECONDS);
    }

    private BlogOwner createBlogger(String name) {
        return BlogOwner.builder()
            .name(name)
            .build();
    }

    private void assertObserverReceivedRecord(BlogOwner blogger) {
        for (StorageItemChange<? extends Model> owner : observer.values()) {
            if (BlogOwner.class.isAssignableFrom(owner.modelSchema().getModelClass()) &&
                    blogger.equals((owner.item()))) {
                return;
            }
        }
        fail("Could not find " + blogger + " in event observer.");
    }

    private <T extends Model> void assertRecordIsInDb(T item) throws AmplifyException {
        List<? extends Model> results = adapter.query(item.getClass(),
                Where.identifier(item.getClass(), item.getPrimaryKeyString()));
        assertEquals(1, results.size());
        assertEquals(item, results.get(0));
    }

    private <T extends Model> void assertRecordIsNotInDb(T item) throws AmplifyException {
        List<? extends Model> results = adapter.query(item.getClass(),
                Where.identifier(item.getClass(), item.getPrimaryKeyString()));
        assertNotNull(results);
        assertEquals(0, results.size());
    }

    private void assertDbFileExists() {
        assertTrue(context.getDatabasePath(DATABASE_NAME).exists());
    }

    private static final class TestFileObserver extends FileObserver {
        private int createFileEventCount;
        private int deleteFileEventCount;

        /**
         * Equivalent to FileObserver(file, FileObserver.ALL_EVENTS).
         *
         * @param path Directory to watch
         */
        @SuppressWarnings("deprecation") // super(...)
        TestFileObserver(@NonNull String path) {
            super(path, FileObserver.CREATE | FileObserver.DELETE | FileObserver.DELETE_SELF);
            this.deleteFileEventCount = 0;
            this.createFileEventCount = 0;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public void onEvent(int event, @Nullable String path) {
            if (!DATABASE_NAME.equals(path)) {
                return;
            }
            if ((event & FileObserver.CREATE) == FileObserver.CREATE) {
                createFileEventCount++;
            } else {
                deleteFileEventCount++;
            }
        }
    }
}
