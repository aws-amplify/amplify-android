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

import com.amplifyframework.datastore.model.ModelField;
import com.amplifyframework.datastore.model.ModelSchema;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.junit.MockitoJUnitRunner;

import java.util.Collections;
import java.util.SortedMap;
import java.util.TreeMap;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.when;

/**
 * Tests {@link CreateSqlCommand#fromModelSchema(ModelSchema)}.
 */
@RunWith(MockitoJUnitRunner.class)
public class CreateSqlCommandTest {

    @Mock
    private ModelSchema mockModelSchema;

    /**
     * Initialize MockitoAnnotations.
     */
    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
    }

    /**
     * Test if a valid {@link ModelSchema} returns an expected
     * CREATE TABLE SQL command.
     */
    @Test
    public void validModelSchemaReturnsExpectedSqlCommand() {
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
        final ModelSchema personSchema = new ModelSchema("Person", "Person", fields);

        final CreateSqlCommand createSqlCommand = CreateSqlCommand.fromModelSchema(personSchema);
        assertEquals("Person", createSqlCommand.tableName());
        assertEquals("CREATE TABLE IF NOT EXISTS Person " +
                "(age INTEGER , " +
                "firstName TEXT NOT NULL, " +
                "id TEXT PRIMARY KEY, " +
                "lastName TEXT NOT NULL);", createSqlCommand.sqlStatement());

    }

    /**
     * Test if {@link ModelSchema} with no fields return an expected
     * CREATE TABLE SQL command with no columns.
     */
    @Test
    public void noFieldsModelSchemaReturnsNoColumnsSqlCommand() {
        when(mockModelSchema.getFields())
                .thenReturn(Collections.emptyMap());
        when(mockModelSchema.getName())
                .thenReturn("Guitar");
        final CreateSqlCommand createSqlCommand = CreateSqlCommand.fromModelSchema(mockModelSchema);
        assertEquals("Guitar", createSqlCommand.tableName());
        assertEquals("CREATE TABLE IF NOT EXISTS Guitar ", createSqlCommand.sqlStatement());
    }
}
