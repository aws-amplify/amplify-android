/*
 * Copyright (c) 2019. Lorem ipsum dolor sit amet, consectetur adipiscing elit.
 * Morbi non lorem porttitor neque feugiat blandit. Ut vitae ipsum eget quam lacinia accumsan.
 * Etiam sed turpis ac ipsum condimentum fringilla. Maecenas magna.
 * Proin dapibus sapien vel ante. Aliquam erat volutpat. Pellentesque sagittis ligula eget metus.
 * Vestibulum commodo. Ut rhoncus gravida arcu.
 */

package com.amplifyframework.datastore.storage.sqlite;

import android.content.Context;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;

import androidx.test.core.app.ApplicationProvider;

import com.amplifyframework.core.ResultListener;
import com.amplifyframework.core.model.ModelSchema;
import com.amplifyframework.core.model.ModelSchemaRegistry;
import com.amplifyframework.testutils.model.AmplifyCliGeneratedModelProvider;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.Assert.*;

public class SQLiteCommandFactoryInstrumentedTest {

    private static final String TAG = "sqlite-cmd-factory-test";
    private static final long SQLITE_OPERATION_TIMEOUT_IN_MILLISECONDS = 1000;

    private Context context;
    private SQLiteStorageAdapter sqLiteStorageAdapter;
    private SQLCommandFactory sqLiteCommandFactory;
    private SQLiteDatabase sqLiteDatabase;
    private ModelSchemaRegistry modelSchemaRegistry;

    @Before
    public void setUp() throws InterruptedException {
        context = ApplicationProvider.getApplicationContext();
        deleteDatabase();

        sqLiteStorageAdapter = SQLiteStorageAdapter.defaultInstance();
        setUpSQLiteStorageAdapter();

        sqLiteCommandFactory = sqLiteStorageAdapter.getSqlCommandFactory();
        sqLiteDatabase = sqLiteStorageAdapter.getDatabaseConnectionHandle();
        modelSchemaRegistry = ModelSchemaRegistry.getInstance();
    }

    /**
     * Drop all tables and database, close and delete the database.
     */
    @After
    public void tearDown() {
        if (sqLiteStorageAdapter != null) {
            sqLiteStorageAdapter.getDatabaseConnectionHandle().close();
            sqLiteStorageAdapter.getSqLiteOpenHelper().close();
        }
        deleteDatabase();
    }

    @Test
    public void updateForPreparedCompiledStatement() throws SQLException {
        SqlCommand sqlCommand = sqLiteCommandFactory.updateFor(
                "Person",
                modelSchemaRegistry.getModelSchemaForModelClass("Person"),
                sqLiteDatabase);
        assertEquals("Person", sqlCommand.tableName());
        assertTrue(sqlCommand.hasCompiledSqlStatement());
        Log.d(TAG, sqlCommand.toString());
    }

    @Test
    public void queryForReturnsExpectedQueryStatement() {
        SqlCommand sqlCommand = sqLiteCommandFactory.queryFor(
                "Person", "id", "dummy-id");
        assertEquals("SELECT * FROM Person WHERE id = 'dummy-id'",
                sqlCommand.sqlStatement());
    }

    @Test
    public void queryForReturnsValidQueryStatement() {
        SqlCommand sqlCommand = sqLiteCommandFactory.queryFor(
                "Person", "id", "dummy-id");

    }

    private void setUpSQLiteStorageAdapter() throws InterruptedException {
        AtomicReference<List<ModelSchema>> responseSuccess = new AtomicReference<>();
        AtomicReference<Throwable> responseError = new AtomicReference<>();
        final CountDownLatch waitForSetUp = new CountDownLatch(1);
        sqLiteStorageAdapter.setUp(context,
                AmplifyCliGeneratedModelProvider.getInstance(),
                new ResultListener<List<ModelSchema>>() {
                    @Override
                    public void onResult(List<ModelSchema> result) {
                        responseSuccess.set(result);
                        waitForSetUp.countDown();
                    }

                    @Override
                    public void onError(Throwable error) {
                        responseError.set(error);
                        waitForSetUp.countDown();
                    }
                });
        assertTrue(waitForSetUp.await(SQLITE_OPERATION_TIMEOUT_IN_MILLISECONDS,
                TimeUnit.MILLISECONDS));
        assertNotNull(responseSuccess.get());
        assertFalse(responseSuccess.get().isEmpty());
        assertNull(responseError.get());
    }

    private void deleteDatabase() {
        context.deleteDatabase(SQLiteStorageAdapter.DATABASE_NAME);
    }
}
