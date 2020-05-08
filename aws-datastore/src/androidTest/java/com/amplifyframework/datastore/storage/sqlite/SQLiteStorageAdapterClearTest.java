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
import android.os.Handler;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.test.core.app.ApplicationProvider;

import com.amplifyframework.core.model.Model;
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
import java.util.List;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicReference;

import io.reactivex.disposables.Disposable;
import io.reactivex.observers.TestObserver;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public final class SQLiteStorageAdapterClearTest {
    private static final String DATABASE_NAME = "AmplifyDatastore.db";

    private SynchronousStorageAdapter adapter;
    private Context context;
    private TestObserver<StorageItemChange<? extends Model>> observer;
    private AtomicReference<Disposable> subscriberDisposableRef = new AtomicReference<>();
    private TestFileObserver fileObserver;

    @BeforeClass
    public static void enableStrictMode() {
        StrictMode.enable();
    }

    @Before
    public void setup() {
        TestStorageAdapter.cleanup();
        context = ApplicationProvider.getApplicationContext();
        adapter = TestStorageAdapter.create(AmplifyModelProvider.getInstance());
        //Set subscriberDisposableRef = <value received from RxJava>.
        //Needed so we can make assertions on the state of the subscriber later.
        observer = adapter
            .observe()
            .doOnSubscribe(subscriberDisposableRef::set)
            .test();

        fileObserver = new TestFileObserver(Objects.requireNonNull(context.getDatabasePath(DATABASE_NAME).getParent()));
        fileObserver.startWatching();
    }

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
     * @throws IOException if it can't ready file creation time
     * @throws InterruptedException if something happens when sleeping for 1 second
     */
    @Test
    public void clearDeletesAndRecreatesDatabase() throws DataStoreException, IOException, InterruptedException {
        assertDbFileExists();
        assertEquals(0, fileObserver.createFileEventCount);
        assertEquals(0, fileObserver.deleteFileEventCount);
        BlogOwner blogger1 = createBlogger("Dummy Blogger Sr.");
        BlogOwner blogger2 = createBlogger("Dummy Blogger Jr.");
        //Save a record and check if it's there
        adapter.save(blogger1);
        assertRecordIsInDb(blogger1);
        //Verify observer is still alive
        assertFalse(subscriberDisposableRef.get().isDisposed());
        assertObserverReceivedRecord(blogger1);

        adapter.clear();
        //Make sure file was deleted and re-created
        assertEquals(1, fileObserver.createFileEventCount);
        assertEquals(1, fileObserver.deleteFileEventCount);
        assertDbFileExists();
        //Verify observer is still alive
        assertFalse(subscriberDisposableRef.get().isDisposed());

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
        assertTrue(subscriberDisposableRef.get().isDisposed());
    }

    private BlogOwner createBlogger(String name) throws DataStoreException {
        return BlogOwner.builder()
            .name(name)
            .build();
    }

    private void assertObserverReceivedRecord(BlogOwner blogger) {
        for (StorageItemChange<? extends Model> owner : observer.values()) {
            if (BlogOwner.class.isAssignableFrom(owner.itemClass()) &&
                blogger.getName().equals(((BlogOwner) owner.item()).getName())) {
                return;
            }
        }
        fail("Could not find " + blogger + " in event observer.");
    }

    private <T extends Model> void assertRecordIsInDb(T item) throws DataStoreException {
        List<? extends Model> results = adapter.query(item.getClass(), BlogOwner.ID.eq(item.getId()));
        assertEquals(1, results.size());
        assertEquals(item, results.get(0));
    }

    private <T extends Model> void assertRecordIsNotInDb(T item) throws DataStoreException {
        List<? extends Model> results = adapter.query(item.getClass(), BlogOwner.ID.eq(item.getId()));
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

        @SuppressWarnings("deprecation")
        TestFileObserver(@NonNull String path) {
            super(path, FileObserver.CREATE | FileObserver.DELETE | FileObserver.DELETE_SELF);

            this.deleteFileEventCount = 0;
            this.createFileEventCount = 0;
        }

        /**
         * The event handler, which must be implemented by subclasses.
         *
         * <p class="note">This method is invoked on a special FileObserver thread.
         * It runs independently of any threads, so take care to use appropriate
         * synchronization!  Consider using {@link Handler#post(Runnable)} to shift
         * event handling work to the main thread to avoid concurrency problems.</p>
         *
         * <p>Event handlers must not throw exceptions.</p>
         *
         * @param event The type of event which happened
         * @param path  The path, relative to the main monitored file or directory,
         *              of the file or directory which triggered the event.  This value can
         *              be {@code null} for certain events, such as {@link #MOVE_SELF}.
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
