package com.amplifyframework.datastore.storage.sqlite;

import android.database.sqlite.SQLiteDatabase;

import com.amplifyframework.core.model.ModelSchemaRegistry;

import org.junit.Before;
import org.junit.Test;

import static org.mockito.Mockito.mock;

/**
 * Tests the {@link SQLiteModelTree}.
 */
public final class SQLiteModelTreeTest {
    private SQLiteModelTree tree;

    @Before
    public void setup() {
        ModelSchemaRegistry registry = ModelSchemaRegistry.instance();
        SQLiteCommandFactory factory = new SQLiteCommandFactory();
        SQLiteDatabase database = mock(SQLiteDatabase.class);
        this.tree = new SQLiteModelTree(registry, commandFactory, database);
    }

    @Test
    public void descendantsOfSerializedModel() {
    }

    @Test
    public void descendantsOfJavaModel() {

    }
}
