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

import java.io.File;
import java.util.List;

import io.reactivex.observers.TestObserver;

import static com.amplifyframework.datastore.storage.sqlite.SQLiteStorageAdapter.DATABASE_NAME;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class SQLiteStorageAdapterClearTest {
    private SynchronousStorageAdapter adapter;
    private Context context;
    private TestObserver<StorageItemChange.Record> observer;

    @BeforeClass
    public static void enableStrictMode() {
        StrictMode.enable();
    }

    @Before
    public void setup() {
        TestStorageAdapter.cleanup();
        context = (ApplicationProvider.getApplicationContext());
        adapter = TestStorageAdapter.create(AmplifyModelProvider.getInstance());
        observer = TestObserver.create();
        adapter.observe().subscribe(observer);
    }

    @After
    public void teardown() {
        TestStorageAdapter.cleanup(adapter);
    }

    /**
     * Happy path test: saves a record to the database
     * then calls clear and verifies that the database
     * file has been deleted.
     * @throws DataStoreException
     */
    @Test
    public void clearDeletesDatabaseFromDisk() throws DataStoreException {
        final BlogOwner dummy = BlogOwner.builder()
            .name("Dummy Blogger")
            .build();

        adapter.save(dummy);
        List<BlogOwner> query = adapter.query(BlogOwner.class);
        assertEquals(1, query.size());
        //Check that exactly one event was received by the observer.
        observer.assertValueCount(1);

        adapter.clear();
        assertFalse(context.getDatabasePath(DATABASE_NAME).exists());
        //check that it's not subscribed anymore.
        observer.assertNotSubscribed();
        //Assert the observer was terminated during the file removal.
        observer.assertTerminated();
    }

    /**
     * Assert that the database was created. Then call clear and
     * assert the file was deleted; attempt to call clear again
     * and ensure there are no exceptions thrown.
     *
     */
    @Test
    public void clearHandlesMissingFile() throws DataStoreException {
        assertTrue(context.getDatabasePath(DATABASE_NAME).exists());
        //check that it's subscribed
        observer.assertSubscribed();

        adapter.clear();
        //check that it's not subscribed anymore.
        observer.assertNotSubscribed();
        assertFalse(context.getDatabasePath(DATABASE_NAME).exists());

        adapter.clear();
    }
}
