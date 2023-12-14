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
import com.amplifyframework.testmodels.commentsblog.AmplifyModelProvider
import com.amplifyframework.testmodels.commentsblog.BlogOwner
import com.amplifyframework.testmodels.customprimarykey.Comment
import com.amplifyframework.util.GsonFactory
import com.google.gson.Gson
import org.junit.After
import org.junit.Assert
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.util.Arrays

/**
 * Asserts that the SQLCommandProcessor executes SqlCommand objects as expected.
 */
@RunWith(RobolectricTestRunner::class)
class SQLCommandProcessorTest {
    private val modelProvider: ModelProvider = AmplifyModelProvider.getInstance()
    private val schemaRegistry: SchemaRegistry = SchemaRegistry.instance().apply {
        register(modelProvider.models())
    }
    private val sqlCommandFactory: SQLCommandFactory = SQLiteCommandFactory(schemaRegistry, GsonFactory.instance())
    private val gson: Gson = GsonFactory.instance()
    private val sqliteDatabase = createDatabase(AmplifyModelProvider.getInstance(), schemaRegistry)
    private val sqlCommandProcessor = SQLCommandProcessor(sqliteDatabase)

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
        Assert.assertEquals(Arrays.asList(abigailMcGregor), results)
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
        Assert.assertTrue(sqlCommandProcessor.executeExists(existsCommand))
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
        Assert.assertFalse(sqlCommandProcessor.executeExists(existsCommand))
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
        Assert.assertEquals(1, sqlCommands.size.toLong())
        val sqlCommand = sqlCommands.iterator().next().sqlStatement()
        Assert.assertTrue(
            sqlCommand.contains(
                "CREATE INDEX IF NOT EXISTS" +
                        " `undefined_title_content_likes` ON `Comment` (`title`, `content`, `likes`);"
            )
        )
        Assert.assertFalse(sqlCommand.contains("`postCommentsId`, `content`"))
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
        Assert.assertEquals(1, sqlCommands.size.toLong())
        val sqlCommand = sqlCommands.iterator().next().sqlStatement()
        Assert.assertTrue(
            sqlCommand.contains(
                "CREATE INDEX IF NOT EXISTS `Comment@@postForeignKey` " +
                        "ON `Comment` (`@@postForeignKey`);"
            )
        )
    }

    @Test
    fun testBeginTransactionToDatabase() {
        Assert.assertFalse(sqliteDatabase.inTransaction())
        sqlCommandProcessor.beginTransaction()
        Assert.assertTrue(sqliteDatabase.inTransaction())
    }

    @Test
    fun testEndTransactionToDatabase() {
        sqlCommandProcessor.beginTransaction()
        Assert.assertTrue(sqliteDatabase.inTransaction())
        sqlCommandProcessor.endTransaction()
        Assert.assertFalse(sqliteDatabase.inTransaction())
    }

    @Test
    /**
     * Asserts that write in transaction is successful if setTransactionSuccessful if called.
     */
    fun testMarkSuccessfulToDatabase() {
        // Insert a BlogOwner
        val blogOwnerSchema = ModelSchema.fromModelClass(
            BlogOwner::class.java
        )
        val owner = BlogOwner.builder()
            .name("My Name")
            .build()
        sqlCommandProcessor.beginTransaction()
        sqlCommandProcessor.execute(sqlCommandFactory.insertFor(blogOwnerSchema, owner))
        sqlCommandProcessor.setTransactionSuccessful()
        sqlCommandProcessor.endTransaction()

        // Check that the BlogOwner exists
        val predicate: QueryPredicate = BlogOwner.ID.eq(owner.id)
        val existsCommand = sqlCommandFactory.existsFor(blogOwnerSchema, predicate)
        Assert.assertTrue(sqlCommandProcessor.executeExists(existsCommand))
    }
}