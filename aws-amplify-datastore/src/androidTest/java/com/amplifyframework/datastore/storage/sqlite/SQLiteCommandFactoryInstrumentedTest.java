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

import com.amplifyframework.core.model.Model;
import com.amplifyframework.core.model.ModelProvider;
import com.amplifyframework.core.model.ModelSchema;
import com.amplifyframework.core.model.ModelSchemaRegistry;
import com.amplifyframework.testmodels.AmplifyCliGeneratedModelProvider;
import com.amplifyframework.testutils.LatchedResultListener;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class SQLiteCommandFactoryInstrumentedTest {

    private static final String TAG = SQLiteCommandFactoryInstrumentedTest.class.getSimpleName();
    private static final long SQLITE_OPERATION_TIMEOUT_MS = 1000;

    private static Context context;
    private static SQLiteStorageAdapter sqLiteStorageAdapter;
    private static SQLCommandFactory sqLiteCommandFactory;
    private static SQLiteDatabase sqLiteDatabase;
    private static ModelSchemaRegistry modelSchemaRegistry;

    @BeforeClass
    public static void setUp() {
        context = ApplicationProvider.getApplicationContext();
        deleteDatabase();

        final LatchedResultListener<List<ModelSchema>> schemaListener =
                new LatchedResultListener<>(SQLITE_OPERATION_TIMEOUT_IN_MILLISECONDS);
        ModelProvider modelProvider = AmplifyCliGeneratedModelProvider.singletonInstance();

        sqLiteStorageAdapter = SQLiteStorageAdapter.defaultInstance();
        sqLiteStorageAdapter.initialize(
                context,
                modelProvider,
                schemaListener
        );

        // Await result, and obtain the received list of schema
        final List<ModelSchema> schemaList =
                schemaListener.awaitTerminalEvent().assertNoError().getResult();

        // Prepare a set of the actual model schema names, as string
        Set<String> expectedModelClassNames = new HashSet<>();
        for (ModelSchema actualSchema : schemaList) {
            expectedModelClassNames.add(actualSchema.getName());
        }

        // Ensure that we got a schema for each of the models that we requested.
        for (Class<? extends Model> requestedModel : modelProvider.models()) {
            assertTrue(expectedModelClassNames.contains(requestedModel.getSimpleName()));
        }

        sqLiteCommandFactory = sqLiteStorageAdapter.getSqlCommandFactory();
        sqLiteDatabase = sqLiteStorageAdapter.getDatabaseConnectionHandle();
        modelSchemaRegistry = ModelSchemaRegistry.singleton();
    }

    /**
     * Drop all tables and database, close and delete the database.
     */
    @AfterClass
    public static void tearDown() {
        sqLiteStorageAdapter.terminate();
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
    }

    @Test
    public void queryForReturnsExpectedQueryStatement() {
        assertEquals(
                new SqlCommand("Person", "SELECT * FROM Person WHERE id = 'dummy-id'"),
                sqLiteCommandFactory.queryFor("Person", "id", "dummy-id"));
    }

    private static void deleteDatabase() {
        context.deleteDatabase(SQLiteStorageAdapter.DATABASE_NAME);
    }
}
