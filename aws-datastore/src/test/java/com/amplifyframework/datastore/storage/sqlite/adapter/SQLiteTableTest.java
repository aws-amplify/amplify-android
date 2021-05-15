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
        columns.put("title", SQLiteColumn.builder()
                .name("title")
                .fieldName("title")
                .dataType(SQLiteDataType.TEXT)
                .isNonNull(true)
                .tableName("Post")
                .build());
        columns.put("blog", SQLiteColumn.builder()
                .name("postBlogId")
                .fieldName("blog")
                .dataType(SQLiteDataType.TEXT)
                .isNonNull(false)
                .tableName("Post")
                .ownerOf("Blog")
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
}
