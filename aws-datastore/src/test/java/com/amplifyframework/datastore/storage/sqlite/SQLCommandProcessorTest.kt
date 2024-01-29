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
package com.amplifyframework.datastore.storage.sqlite

import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteDatabase.OpenParams
import com.amplifyframework.AmplifyException
import com.amplifyframework.core.model.ModelProvider
import com.amplifyframework.core.model.ModelSchema
import com.amplifyframework.core.model.SchemaRegistry
import com.amplifyframework.core.model.query.Where
import com.amplifyframework.core.model.query.predicate.QueryPredicate
import com.amplifyframework.datastore.DataStoreException
import com.amplifyframework.testmodels.commentsblog.AmplifyModelProvider
import com.amplifyframework.testmodels.commentsblog.BlogOwner
import com.amplifyframework.testmodels.customprimarykey.Comment
import com.amplifyframework.util.GsonFactory
import com.google.gson.Gson
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/**
 * Asserts that the SQLCommandProcessor executes SqlCommand objects as expected.
 */
@RunWith(RobolectricTestRunner::class)
class SQLCommandProcessorTest {
    private lateinit var sqlCommandFactory: SQLCommandFactory
    private lateinit var sqlCommandProcessor: SQLCommandProcessor
    private lateinit var sqliteDatabase: SQLiteDatabase
    private lateinit var schemaRegistry: SchemaRegistry
    private lateinit var gson: Gson

    /**
     * Sets up model registry and in-memory database.
     * @throws AmplifyException if model fails to register.
     */
    @Before
    @Throws(AmplifyException::class)
    fun setup() {
        val modelProvider: ModelProvider = AmplifyModelProvider.getInstance()
        schemaRegistry = SchemaRegistry.instance()
        schemaRegistry.register(modelProvider.models())
        sqlCommandFactory = SQLiteCommandFactory(schemaRegistry, GsonFactory.instance())
        sqliteDatabase = createDatabase(modelProvider, schemaRegistry)
        sqlCommandProcessor = SQLCommandProcessor(sqliteDatabase)
        gson = GsonFactory.instance()
    }

    private fun createDatabase(
        modelProvider: ModelProvider,
        registry: SchemaRegistry
    ): SQLiteDatabase {
        val openParams = OpenParams.Builder().build()
        val db = SQLiteDatabase.createInMemory(openParams)
        db.beginTransaction()
        try {
            for (modelName in modelProvider.modelNames()) {
                val modelSchema = registry.getModelSchemaForModelClass(modelName)
                db.execSQL(sqlCommandFactory.createTableFor(modelSchema).sqlStatement())
                for (command in sqlCommandFactory.createIndexesFor(modelSchema)) {
                    db.execSQL(command.sqlStatement())
                }
            }
            db.setTransactionSuccessful()
        } finally {
            db.endTransaction()
        }
        return db
    }

    @After
    fun clear() {
        schemaRegistry.clear()
        sqliteDatabase.close()
    }

    /**
     * Create and insert a BlogOwner, and then verify that a rawQuery returns a Cursor with one result, containing the
     * previously inserted BlogOwner.
     */
    @Test
    fun rawQueryReturnsResults() {
        // Insert a BlogOwner
        val blogOwnerSchema = ModelSchema.fromModelClass(
            BlogOwner::class.java
        )
        val abigailMcGregor = BlogOwner.builder()
            .name("Abigail McGregor")
            .build()
        sqlCommandProcessor.execute(
            sqlCommandFactory.insertFor(
                blogOwnerSchema,
                abigailMcGregor
            )
        )

        // Query for all BlogOwners, and verify that there is one result.
        val queryCommand = sqlCommandFactory.queryFor(blogOwnerSchema, Where.matchesAll())
        val cursor = sqlCommandProcessor.rawQuery(queryCommand)
        val results: MutableList<BlogOwner> = ArrayList()
        val converter = SQLiteModelFieldTypeConverter(
            blogOwnerSchema,
            schemaRegistry,
            GsonFactory.instance()
        )
        if (cursor.moveToFirst()) {
            do {
                val map = converter.buildMapForModel(cursor)
                val jsonString = gson.toJson(map)
                results.add(gson.fromJson(jsonString, BlogOwner::class.java))
            } while (cursor.moveToNext())
        }
        assertEquals(listOf(abigailMcGregor), results)
    }

    /**
     * Create and insert a BlogOwner, and then verify that executeExists return true.
     */
    @Test
    fun executeExistsReturnsTrueWhenItemExists() {
        // Insert a BlogOwner
        val blogOwnerSchema = ModelSchema.fromModelClass(
            BlogOwner::class.java
        )
        val abigailMcGregor = BlogOwner.builder()
            .name("Abigail McGregor")
            .build()
        sqlCommandProcessor.execute(
            sqlCommandFactory.insertFor(
                blogOwnerSchema,
                abigailMcGregor
            )
        )

        // Check that the BlogOwner exists
        val predicate: QueryPredicate = BlogOwner.ID.eq(abigailMcGregor.id)
        val existsCommand = sqlCommandFactory.existsFor(blogOwnerSchema, predicate)
        assertTrue(sqlCommandProcessor.executeExists(existsCommand))
    }

    /**
     * Create a BlogOwner, but don't insert it.  Then verify that executeExists returns false.
     */
    @Test
    fun executeExistsReturnsFalseWhenItemDoesntExist() {
        // Create a BlogOwner, but don't insert it
        val blogOwnerSchema = ModelSchema.fromModelClass(
            BlogOwner::class.java
        )
        val abigailMcGregor = BlogOwner.builder()
            .name("Abigail McGregor")
            .build()
        val predicate: QueryPredicate = BlogOwner.ID.eq(abigailMcGregor.id)
        val existsCommand = sqlCommandFactory.existsFor(blogOwnerSchema, predicate)
        assertFalse(sqlCommandProcessor.executeExists(existsCommand))
    }

    /**
     * Verify that index for fields included in belongs to is not created for Comments.
     * @throws AmplifyException on failure to create ModelSchema from class.
     */
    @Test
    fun testIndexNotCreatedWhenFieldsInBelongsTo() {
        val commentSchema = ModelSchema.fromModelClass(
            Comment::class.java
        )
        sqlCommandFactory.createIndexesFor(commentSchema)
        val sqlCommands = sqlCommandFactory.createIndexesFor(commentSchema)
        assertEquals(1, sqlCommands.size.toLong())
        val sqlCommand = sqlCommands.iterator().next().sqlStatement()
        assertTrue(
            sqlCommand.contains(
                "CREATE INDEX IF NOT EXISTS" +
                    " `undefined_title_content_likes` ON `Comment` (`title`, `content`, `likes`);"
            )
        )
        assertFalse(sqlCommand.contains("`postCommentsId`, `content`"))
    }

    /**
     * Verify that index for foreign key fields is included in the commands.
     */
    @Test
    fun testForeignKeyIndexCreated() {
        val commentSchema = ModelSchema.fromModelClass(
            Comment::class.java
        )
        sqlCommandFactory.createIndexesFor(commentSchema)
        val sqlCommands = sqlCommandFactory.createIndexesForForeignKeys(commentSchema)
        assertEquals(1, sqlCommands.size.toLong())
        val sqlCommand = sqlCommands.iterator().next().sqlStatement()
        assertTrue(
            sqlCommand.contains(
                "CREATE INDEX IF NOT EXISTS `Comment@@postForeignKey` " +
                    "ON `Comment` (`@@postForeignKey`);"
            )
        )
    }

    @Test
    fun testRunInTransactionWritesToDatabase() {
        // Insert a BlogOwner
        val blogOwnerSchema = ModelSchema.fromModelClass(
            BlogOwner::class.java
        )
        val owner = BlogOwner.builder()
            .name("My Name")
            .build()

        sqlCommandProcessor.runInTransaction {
            sqlCommandProcessor.execute(sqlCommandFactory.insertFor(blogOwnerSchema, owner))
        }

        // Check that the BlogOwner exists
        val predicate: QueryPredicate = BlogOwner.ID.eq(owner.id)
        val existsCommand = sqlCommandFactory.existsFor(blogOwnerSchema, predicate)
        assertTrue(sqlCommandProcessor.executeExists(existsCommand))
    }

    @Test
    fun testRunInTransactionErrorDoesNotCommit() {
        // Insert a BlogOwner
        val blogOwnerSchema = ModelSchema.fromModelClass(
            BlogOwner::class.java
        )
        val owner = BlogOwner.builder()
            .name("My Name")
            .build()

        val expectedException = DataStoreException("expected", "expected")
        var capturedException: DataStoreException? = null
        try {
            sqlCommandProcessor.runInTransaction {
                sqlCommandProcessor.execute(sqlCommandFactory.insertFor(blogOwnerSchema, owner))
                throw expectedException
            }
        } catch (e: DataStoreException) {
            capturedException = e
        }

        assertEquals(expectedException, capturedException)

        // Check that the BlogOwner does not exist
        val predicate: QueryPredicate = BlogOwner.ID.eq(owner.id)
        val existsCommand = sqlCommandFactory.existsFor(blogOwnerSchema, predicate)
        assertFalse(sqlCommandProcessor.executeExists(existsCommand))
    }

    @Test
    fun testRunInTransactionAndSucceedOnDatastoreException() {
        // Insert a BlogOwner
        val blogOwnerSchema = ModelSchema.fromModelClass(
            BlogOwner::class.java
        )
        val owner = BlogOwner.builder()
            .name("My Name")
            .build()

        val expectedException = DataStoreException("expected", "expected")
        var capturedException: DataStoreException? = null
        try {
            sqlCommandProcessor.runInTransactionAndSucceedOnDatastoreException {
                sqlCommandProcessor.execute(sqlCommandFactory.insertFor(blogOwnerSchema, owner))
                throw expectedException
            }
        } catch (e: DataStoreException) {
            capturedException = e
        }

        assertEquals(expectedException, capturedException)

        // Check that the BlogOwner does not exist
        val predicate: QueryPredicate = BlogOwner.ID.eq(owner.id)
        val existsCommand = sqlCommandFactory.existsFor(blogOwnerSchema, predicate)
        assertTrue(sqlCommandProcessor.executeExists(existsCommand))
    }
}
