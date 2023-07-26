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

package com.amplifyframework.datastore.storage.sqlite.adapter;

import com.amplifyframework.AmplifyException;
import com.amplifyframework.core.model.ModelSchema;
import com.amplifyframework.datastore.storage.sqlite.SQLiteDataType;
import com.amplifyframework.testmodels.commentsblog.Post;
import com.amplifyframework.testmodels.customprimarykey.Comment;
import com.amplifyframework.testmodels.customprimarykey.Item;
import com.amplifyframework.testmodels.customprimarykey.ModelCompositeMultiplePk;

import org.junit.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.assertEquals;

public class SQLiteTableTest {

    /**
     * Test if a {@link ModelSchema} for {@link Post} returns an expected {@link SQLiteTable}.  This tests the general
     * use case, for an object with most data types (String, Integer, enum, BelongsTo, and HasMany relationships).
     * @throws AmplifyException on error deriving ModelSchema.
     */
    @Test
    public void createSQLiteTableForPost() throws AmplifyException {
        ModelSchema schema = ModelSchema.fromModelClass(Post.class);
        Map<String, SQLiteColumn> columns = new HashMap<>();
        columns.put("id", SQLiteColumn.builder()
                .name("id")
                .fieldName("id")
                .dataType(SQLiteDataType.TEXT)
                .isNonNull(true)
                .tableName("Post")
                .build());
        columns.put("blog", SQLiteColumn.builder()
                .name("blogPostsId")
                .fieldName("blog")
                .dataType(SQLiteDataType.TEXT)
                .isNonNull(false)
                .tableName("Post")
                .ownerOf("Blog")
                .build());
        columns.put("title", SQLiteColumn.builder()
                .name("title")
                .fieldName("title")
                .dataType(SQLiteDataType.TEXT)
                .isNonNull(true)
                .tableName("Post")
                .build());
        columns.put("status", SQLiteColumn.builder()
                .name("status")
                .fieldName("status")
                .dataType(SQLiteDataType.TEXT)
                .isNonNull(true)
                .tableName("Post")
                .build());
        columns.put("updatedAt", SQLiteColumn.builder()
                .name("updatedAt")
                .fieldName("updatedAt")
                .dataType(SQLiteDataType.TEXT)
                .isNonNull(false)
                .tableName("Post")
                .build());
        columns.put("status", SQLiteColumn.builder()
                .name("status")
                .fieldName("status")
                .dataType(SQLiteDataType.TEXT)
                .isNonNull(true)
                .tableName("Post")
                .build());
        columns.put("rating", SQLiteColumn.builder()
                .name("rating")
                .fieldName("rating")
                .dataType(SQLiteDataType.INTEGER)
                .isNonNull(true)
                .tableName("Post")
                .build());
        columns.put("author", SQLiteColumn.builder()
                .name("authorPostsId")
                .fieldName("author")
                .dataType(SQLiteDataType.TEXT)
                .isNonNull(false)
                .tableName("Post")
                .ownerOf("Author")
                .build());
        columns.put("createdAt", SQLiteColumn.builder()
                .name("createdAt")
                .fieldName("createdAt")
                .dataType(SQLiteDataType.TEXT)
                .isNonNull(false)
                .tableName("Post")
                .build());

        SQLiteTable expected = SQLiteTable.builder()
                .columns(columns)
                .name("Post")
                .build();

        SQLiteTable actual = SQLiteTable.fromSchema(schema);
        assertEquals(expected, actual);
        assertEquals("id", actual.getPrimaryKey().getFieldName());
    }

    /**
     * Test if a {@link ModelSchema} for {@link com.amplifyframework.testmodels.customprimarykey.Comment}
     * returns an expected {@link SQLiteTable}.  This tests the general
     * use case, for an object with most data types (String, Integer, enum, BelongsTo, and HasMany relationships).
     * @throws AmplifyException on error deriving ModelSchema.
     */
    @Test
    public void createSQLiteTableForaModelWithParentHavingCPK() throws AmplifyException {
        ModelSchema schema = ModelSchema.fromModelClass(Comment.class);
        Map<String, SQLiteColumn> columns = new HashMap<>();
        columns.put("title", SQLiteColumn.builder()
                .name("title")
                .fieldName("title")
                .dataType(SQLiteDataType.TEXT)
                .isNonNull(true)
                .tableName("Comment")
                .build());
        columns.put("post", SQLiteColumn.builder()
                .name("@@postForeignKey")
                .fieldName("post")
                .dataType(SQLiteDataType.TEXT)
                .isNonNull(false)
                .tableName("Comment")
                .ownerOf("Post")
                .build());
        columns.put("@@primaryKey", SQLiteColumn.builder()
                .name("@@primaryKey")
                .fieldName("@@primaryKey")
                .dataType(SQLiteDataType.TEXT)
                .isNonNull(true)
                .tableName("Comment")
                .build());
        columns.put("description", SQLiteColumn.builder()
                .name("description")
                .fieldName("description")
                .dataType(SQLiteDataType.TEXT)
                .isNonNull(false)
                .tableName("Comment")
                .build());
        columns.put("content", SQLiteColumn.builder()
                .name("content")
                .fieldName("content")
                .dataType(SQLiteDataType.TEXT)
                .isNonNull(true)
                .tableName("Comment")
                .build());
        columns.put("likes", SQLiteColumn.builder()
                .name("likes")
                .fieldName("likes")
                .dataType(SQLiteDataType.INTEGER)
                .isNonNull(true)
                .tableName("Comment")
                .build());
        columns.put("updatedAt", SQLiteColumn.builder()
                .name("updatedAt")
                .fieldName("updatedAt")
                .dataType(SQLiteDataType.TEXT)
                .isNonNull(false)
                .tableName("Comment")
                .build());
        columns.put("createdAt", SQLiteColumn.builder()
                .name("createdAt")
                .fieldName("createdAt")
                .dataType(SQLiteDataType.TEXT)
                .isNonNull(false)
                .tableName("Comment")
                .build());

        SQLiteTable expected = SQLiteTable.builder()
                .columns(columns)
                .name("Comment")
                .build();

        SQLiteTable actual = SQLiteTable.fromSchema(schema);
        assertEquals(expected, actual);
        assertEquals("@@primaryKey", actual.getPrimaryKey().getFieldName());
    }

    /**
     * Test if a {@link ModelSchema} for {@link
     * com.amplifyframework.testmodels.customprimarykey.ModelCompositeMultiplePk}
     * returns an expected {@link SQLiteTable}.  This tests the general
     * use case for composite primary key with a field name id.
     * @throws AmplifyException on error deriving ModelSchema.
     */
    @Test
    public void createSQLiteTableForaModelWithParentHavingCPKWithId() throws AmplifyException {
        ModelSchema schema = ModelSchema.fromModelClass(ModelCompositeMultiplePk.class);
        Map<String, SQLiteColumn> columns = new HashMap<>();
        columns.put("id", SQLiteColumn.builder()
                .name("id")
                .fieldName("id")
                .dataType(SQLiteDataType.TEXT)
                .isNonNull(true)
                .tableName("ModelCompositeMultiplePk")
                .build());
        columns.put("@@primaryKey", SQLiteColumn.builder()
                .name("@@primaryKey")
                .fieldName("@@primaryKey")
                .dataType(SQLiteDataType.TEXT)
                .isNonNull(true)
                .tableName("ModelCompositeMultiplePk")
                .build());
        columns.put("location", SQLiteColumn.builder()
                .name("location")
                .fieldName("location")
                .dataType(SQLiteDataType.TEXT)
                .isNonNull(true)
                .tableName("ModelCompositeMultiplePk")
                .build());
        columns.put("name", SQLiteColumn.builder()
                .name("name")
                .fieldName("name")
                .dataType(SQLiteDataType.TEXT)
                .isNonNull(true)
                .tableName("ModelCompositeMultiplePk")
                .build());
        columns.put("lastName", SQLiteColumn.builder()
                .name("lastName")
                .fieldName("lastName")
                .dataType(SQLiteDataType.TEXT)
                .isNonNull(false)
                .tableName("ModelCompositeMultiplePk")
                .build());
        columns.put("updatedAt", SQLiteColumn.builder()
                .name("updatedAt")
                .fieldName("updatedAt")
                .dataType(SQLiteDataType.TEXT)
                .isNonNull(false)
                .tableName("ModelCompositeMultiplePk")
                .build());
        columns.put("createdAt", SQLiteColumn.builder()
                .name("createdAt")
                .fieldName("createdAt")
                .dataType(SQLiteDataType.TEXT)
                .isNonNull(false)
                .tableName("ModelCompositeMultiplePk")
                .build());

        SQLiteTable expected = SQLiteTable.builder()
                .columns(columns)
                .name("ModelCompositeMultiplePk")
                .build();

        SQLiteTable actual = SQLiteTable.fromSchema(schema);
        assertEquals(expected, actual);
        assertEquals("@@primaryKey", actual.getPrimaryKey().getFieldName());
    }

    /**
     * Test if a {@link ModelSchema} for {@link com.amplifyframework.testmodels.customprimarykey.Item}
     * returns an expected {@link SQLiteTable}.  This tests the general use case for CPK with
     * no sort key.
     * @throws AmplifyException on error deriving ModelSchema.
     */
    @Test
    public void createSQLiteTableForaModelWithCPKButNoSortKeys() throws AmplifyException {
        ModelSchema schema = ModelSchema.fromModelClass(Item.class);
        Map<String, SQLiteColumn> columns = new HashMap<>();

        columns.put("customKey", SQLiteColumn.builder()
                .name("customKey")
                .fieldName("customKey")
                .dataType(SQLiteDataType.TEXT)
                .isNonNull(true)
                .tableName("Item")
                .build());

        columns.put("name", SQLiteColumn.builder()
                .name("name")
                .fieldName("name")
                .dataType(SQLiteDataType.TEXT)
                .isNonNull(true)
                .tableName("Item")
                .build());

        columns.put("updatedAt", SQLiteColumn.builder()
                .name("updatedAt")
                .fieldName("updatedAt")
                .dataType(SQLiteDataType.TEXT)
                .isNonNull(false)
                .tableName("Item")
                .build());
        columns.put("createdAt", SQLiteColumn.builder()
                .name("createdAt")
                .fieldName("createdAt")
                .dataType(SQLiteDataType.TEXT)
                .isNonNull(false)
                .tableName("Item")
                .build());

        SQLiteTable expected = SQLiteTable.builder()
                .columns(columns)
                .name("Item")
                .build();

        SQLiteTable actual = SQLiteTable.fromSchema(schema);
        assertEquals(expected, actual);
        assertEquals("customKey", actual.getPrimaryKey().getFieldName());
    }
}
