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

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import com.amplifyframework.AmplifyException;
import com.amplifyframework.core.model.ModelProvider;
import com.amplifyframework.core.model.ModelSchema;
import com.amplifyframework.core.model.SchemaRegistry;
import com.amplifyframework.core.model.query.Where;
import com.amplifyframework.core.model.query.predicate.QueryPredicate;
import com.amplifyframework.testmodels.commentsblog.AmplifyModelProvider;
import com.amplifyframework.testmodels.commentsblog.BlogOwner;
import com.amplifyframework.testmodels.customprimarykey.Comment;
import com.amplifyframework.util.GsonFactory;

import com.google.gson.Gson;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * Asserts that the SQLCommandProcessor executes SqlCommand objects as expected.
 */
@RunWith(RobolectricTestRunner.class)
public class SQLCommandProcessorTest {
    private SQLCommandFactory sqlCommandFactory;
    private SQLCommandProcessor sqlCommandProcessor;
    private SQLiteDatabase sqliteDatabase;
    private SchemaRegistry schemaRegistry;
    private Gson gson;

    /**
     * Sets up model registry and in-memory database.
     * @throws AmplifyException if model fails to register.
     */
    @Before
    public void setup() throws AmplifyException {
        ModelProvider modelProvider = AmplifyModelProvider.getInstance();
        schemaRegistry = SchemaRegistry.instance();
        schemaRegistry.register(modelProvider.models());
        sqlCommandFactory = new SQLiteCommandFactory(schemaRegistry, GsonFactory.instance());
        sqliteDatabase = createDatabase(modelProvider, schemaRegistry);
        sqlCommandProcessor = new SQLCommandProcessor(sqliteDatabase);
        gson = GsonFactory.instance();
    }

    private SQLiteDatabase createDatabase(ModelProvider modelProvider, SchemaRegistry registry) {
        SQLiteDatabase.OpenParams openParams = new SQLiteDatabase.OpenParams.Builder().build();
        SQLiteDatabase db = SQLiteDatabase.createInMemory(openParams);
        db.beginTransaction();
        try {
            for (String modelName : modelProvider.modelNames()) {
                final ModelSchema modelSchema = registry.getModelSchemaForModelClass(modelName);
                db.execSQL(sqlCommandFactory.createTableFor(modelSchema).sqlStatement());
                for (SqlCommand command : sqlCommandFactory.createIndexesFor(modelSchema)) {
                    db.execSQL(command.sqlStatement());
                }
            }
            db.setTransactionSuccessful();
        } finally {
            db.endTransaction();
        }
        return db;
    }

    /**
     * Closes in-memory database.
     */
    @After
    public void clear() {
        schemaRegistry.clear();
        sqliteDatabase.close();
    }

    /**
     * Create and insert a BlogOwner, and then verify that a rawQuery returns a Cursor with one result, containing the
     * previously inserted BlogOwner.
     * @throws AmplifyException on failure to create ModelSchema from class
     */
    @Test
    public void rawQueryReturnsResults() throws AmplifyException {
        // Insert a BlogOwner
        ModelSchema blogOwnerSchema = ModelSchema.fromModelClass(BlogOwner.class);
        BlogOwner abigailMcGregor = BlogOwner.builder()
                .name("Abigail McGregor")
                .build();
        sqlCommandProcessor.execute(sqlCommandFactory.insertFor(blogOwnerSchema, abigailMcGregor));

        // Query for all BlogOwners, and verify that there is one result.
        SqlCommand queryCommand = sqlCommandFactory.queryFor(blogOwnerSchema, Where.matchesAll());
        Cursor cursor = sqlCommandProcessor.rawQuery(queryCommand);
        List<BlogOwner> results = new ArrayList<>();

        SQLiteModelFieldTypeConverter converter = new SQLiteModelFieldTypeConverter(blogOwnerSchema,
                schemaRegistry,
                GsonFactory.instance());

        if (cursor.moveToFirst()) {
            do {
                Map<String, Object> map = converter.buildMapForModel(cursor);
                String jsonString = gson.toJson(map);
                results.add(gson.fromJson(jsonString, BlogOwner.class));
            } while (cursor.moveToNext());
        }
        assertEquals(Arrays.asList(abigailMcGregor), results);
    }

    /**
     * Create and insert a BlogOwner, and then verify that executeExists return true.
     * @throws AmplifyException on failure to create ModelSchema from class.
     */
    @Test
    public void executeExistsReturnsTrueWhenItemExists() throws AmplifyException {
        // Insert a BlogOwner
        ModelSchema blogOwnerSchema = ModelSchema.fromModelClass(BlogOwner.class);
        BlogOwner abigailMcGregor = BlogOwner.builder()
                .name("Abigail McGregor")
                .build();
        sqlCommandProcessor.execute(sqlCommandFactory.insertFor(blogOwnerSchema, abigailMcGregor));

        // Check that the BlogOwner exists
        QueryPredicate predicate = BlogOwner.ID.eq(abigailMcGregor.getId());
        SqlCommand existsCommand = sqlCommandFactory.existsFor(blogOwnerSchema, predicate);
        assertTrue(sqlCommandProcessor.executeExists(existsCommand));
    }

    /**
     * Create a BlogOwner, but don't insert it.  Then verify that executeExists returns false.
     * @throws AmplifyException on failure to create ModelSchema from class.
     */
    @Test
    public void executeExistsReturnsFalseWhenItemDoesntExist() throws AmplifyException {
        // Create a BlogOwner, but don't insert it
        ModelSchema blogOwnerSchema = ModelSchema.fromModelClass(BlogOwner.class);
        BlogOwner abigailMcGregor = BlogOwner.builder()
                .name("Abigail McGregor")
                .build();

        QueryPredicate predicate = BlogOwner.ID.eq(abigailMcGregor.getId());
        SqlCommand existsCommand = sqlCommandFactory.existsFor(blogOwnerSchema, predicate);
        assertFalse(sqlCommandProcessor.executeExists(existsCommand));
    }

    /**
     * Verify that index for fields included in belongs to is not created for Comments.
     * @throws AmplifyException on failure to create ModelSchema from class.
     */
    @Test
    public void testIndexNotCreatedWhenFieldsInBelongsTo() throws AmplifyException {
        ModelSchema commentSchema = ModelSchema.fromModelClass(Comment.class);
        sqlCommandFactory.createIndexesFor(commentSchema);
        Set<SqlCommand> sqlCommands = sqlCommandFactory.createIndexesFor(commentSchema);
        assertEquals(1, sqlCommands.size());
        String sqlCommand = sqlCommands.iterator().next().sqlStatement();
        assertTrue(sqlCommand.contains("CREATE INDEX IF NOT EXISTS" +
                " `undefined_title_content_likes` ON `Comment` (`title`, `content`, `likes`);"));
        assertFalse(sqlCommand.contains("`postCommentsId`, `content`"));
    }


    /**
     * Verify that index for foreign key fields is included in the commands.
     * @throws AmplifyException on failure to create ModelSchema from class.
     */
    @Test
    public void testForeignKeyIndexCreated() throws AmplifyException {
        ModelSchema commentSchema = ModelSchema.fromModelClass(Comment.class);
        sqlCommandFactory.createIndexesFor(commentSchema);
        Set<SqlCommand> sqlCommands = sqlCommandFactory.createIndexesForForeignKeys(commentSchema);
        assertEquals(1, sqlCommands.size());
        String sqlCommand = sqlCommands.iterator().next().sqlStatement();
        assertTrue(sqlCommand.contains("CREATE INDEX IF NOT EXISTS `Comment@@postForeignKey` " +
                "ON `Comment` (`@@postForeignKey`);"));

    }
}
