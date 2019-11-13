/*
 * Copyright 2019 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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

import android.os.Build;

import com.amplifyframework.core.model.ModelField;
import com.amplifyframework.core.model.ModelIndex;
import com.amplifyframework.core.model.ModelSchema;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import java.util.Collections;
import java.util.SortedMap;
import java.util.TreeMap;

import static org.junit.Assert.assertEquals;

/**
 * Tests {@link SQLiteCommandFactory#createTableFor(ModelSchema)}
 * and {@link SQLiteCommandFactory#createIndexFor(ModelSchema)}.
 */
@Config(sdk = Build.VERSION_CODES.P, manifest = Config.NONE)
@RunWith(RobolectricTestRunner.class)
public class SqlCommandTest {

    /**
     * Test if a valid {@link ModelSchema} returns an expected
     * CREATE TABLE SQL command.
     */
    @Test
    public void validModelSchemaReturnsExpectedSqlCommand() {
        final SortedMap<String, ModelField> fields = getFieldsMap();
        final ModelSchema personSchema = ModelSchema.builder()
                .name("Person")
                .targetModelName(null)
                .fields(fields)
                .build();

        final SqlCommand sqlCommand = SQLiteCommandFactory.getInstance()
                .createTableFor(personSchema);
        assertEquals("Person", sqlCommand.tableName());
        assertEquals("CREATE TABLE IF NOT EXISTS Person " +
                "(age INTEGER , " +
                "firstName TEXT NOT NULL, " +
                "id TEXT PRIMARY KEY NOT NULL, " +
                "lastName TEXT NOT NULL);", sqlCommand.sqlStatement());
    }

    /**
     * Test if {@link ModelSchema} with no fields return an expected
     * CREATE TABLE SQL command with no columns.
     */
    @Test
    public void noFieldsModelSchemaReturnsNoColumnsSqlCommand() {
        final ModelSchema modelSchema = ModelSchema.builder()
                .fields(Collections.emptyMap())
                .name("Guitar")
                .build();

        final SqlCommand sqlCommand = SQLiteCommandFactory.getInstance()
                .createTableFor(modelSchema);

        assertEquals("Guitar", sqlCommand.tableName());
        assertEquals("CREATE TABLE IF NOT EXISTS Guitar ", sqlCommand.sqlStatement());
    }

    /**
     * Test if {@link ModelSchema} with index returns an expected
     * CREATE INDEX SQL command.
     */
    @Test
    public void modelWithIndexReturnsExpectedCreateIndexCommand() {
        final ModelSchema modelSchema = ModelSchema.builder()
                .name("Person")
                .modelIndex(ModelIndex.builder()
                        .indexName("idBasedIndex")
                        .indexFieldNames(Collections.singletonList("id"))
                        .build())
                .build();

        final SqlCommand createIndexSqlCommand = SQLiteCommandFactory.getInstance()
                .createIndexFor(modelSchema);

        assertEquals("Person", createIndexSqlCommand.tableName());
        assertEquals("CREATE INDEX IF NOT EXISTS idBasedIndex ON Person (id);",
                createIndexSqlCommand.sqlStatement());
    }

    private static SortedMap<String, ModelField> getFieldsMap() {
        final SortedMap<String, ModelField> fields = new TreeMap<>();
        fields.put("id", ModelField.builder()
                .name("id")
                .targetName("id")
                .isPrimaryKey(true)
                .isRequired(true)
                .targetType("String")
                .build());
        fields.put("firstName", ModelField.builder()
                .name("firstName")
                .targetName("first_name")
                .isRequired(true)
                .targetType("String")
                .build());
        fields.put("lastName", ModelField.builder()
                .name("lastName")
                .targetName("last_name")
                .isRequired(true)
                .targetType("String")
                .build());
        fields.put("age", ModelField.builder()
                .name("age")
                .targetName("age")
                .targetType("Int")
                .build());
        return fields;
    }
}
