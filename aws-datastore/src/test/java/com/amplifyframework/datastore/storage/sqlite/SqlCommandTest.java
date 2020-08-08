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

import com.amplifyframework.AmplifyException;
import com.amplifyframework.core.model.ModelField;
import com.amplifyframework.core.model.ModelIndex;
import com.amplifyframework.core.model.ModelSchema;
import com.amplifyframework.core.model.ModelSchemaRegistry;
import com.amplifyframework.core.model.query.Page;
import com.amplifyframework.core.model.query.QueryOptions;
import com.amplifyframework.core.model.query.QueryPaginationInput;
import com.amplifyframework.core.model.query.Where;
import com.amplifyframework.datastore.DataStoreException;
import com.amplifyframework.datastore.syncengine.PendingMutation;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.SortedMap;
import java.util.TreeMap;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * Tests {@link SQLiteCommandFactory#createTableFor(ModelSchema)}
 * and {@link SQLiteCommandFactory#createIndexesFor(ModelSchema)}.
 */
@Config(sdk = Build.VERSION_CODES.P, manifest = Config.NONE)
@RunWith(RobolectricTestRunner.class)
public class SqlCommandTest {

    private static final String PERSON_BASE_QUERY =
            "SELECT Person.id AS Person_id, Person.age AS Person_age, Person.firstName AS Person_firstName, " +
                    "Person.lastName AS Person_lastName FROM `Person`";

    private SQLCommandFactory sqlCommandFactory;

    /**
     * Setup before each test.
     */
    @Before
    public void createSqlCommandFactory() {
        ModelSchemaRegistry modelSchemaRegistry = ModelSchemaRegistry.instance();
        sqlCommandFactory = new SQLiteCommandFactory(modelSchemaRegistry);
    }

    /**
     * Test if a valid {@link ModelSchema} returns an expected
     * CREATE TABLE SQL command.
     */
    @Test
    public void validModelSchemaReturnsExpectedSqlCommand() {
        final ModelSchema personSchema = getPersonModelSchema();

        final SqlCommand sqlCommand = sqlCommandFactory.createTableFor(personSchema);
        assertEquals("Person", sqlCommand.tableName());
        assertEquals("CREATE TABLE IF NOT EXISTS `Person` (" +
                "`id` TEXT PRIMARY KEY NOT NULL, " +
                "`age` INTEGER, " +
                "`firstName` TEXT NOT NULL, " +
                "`lastName` TEXT NOT NULL);", sqlCommand.sqlStatement());
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

        final SqlCommand sqlCommand = sqlCommandFactory.createTableFor(modelSchema);
        assertEquals("Guitar", sqlCommand.tableName());
        assertEquals("CREATE TABLE IF NOT EXISTS `Guitar` ", sqlCommand.sqlStatement());
    }

    /**
     * Test if {@link ModelSchema} with index returns an expected
     * CREATE INDEX SQL command.
     */
    @Test
    public void modelWithIndexReturnsExpectedCreateIndexCommand() {
        final ModelIndex index = ModelIndex.builder()
                .indexName("idBasedIndex")
                .indexFieldNames(Collections.singletonList("id"))
                .build();

        final ModelSchema modelSchema = ModelSchema.builder()
                .name("Person")
                .indexes(Collections.singletonMap("idBasedIndex", index))
                .build();

        final Iterator<SqlCommand> sqlCommandIterator = sqlCommandFactory
                .createIndexesFor(modelSchema)
                .iterator();
        assertTrue(sqlCommandIterator.hasNext());

        final SqlCommand createIndexSqlCommand = sqlCommandIterator.next();
        assertEquals("Person", createIndexSqlCommand.tableName());
        assertEquals("CREATE INDEX IF NOT EXISTS `idBasedIndex` ON `Person` (`id`);",
                createIndexSqlCommand.sqlStatement());
    }

    /**
     * Tests that a CREATE index command is correctly constructs for the
     * {@link PendingMutation.PersistentRecord}.
     * @throws AmplifyException from Amplify config
     */
    @Test
    public void createIndexForPendingMutationRecord() throws AmplifyException {
        final Iterator<SqlCommand> sqlCommandIterator = sqlCommandFactory
                .createIndexesFor(ModelSchema.fromModelClass(PendingMutation.PersistentRecord.class))
                .iterator();
        assertTrue(sqlCommandIterator.hasNext());
        assertEquals(
            // expected
            new SqlCommand(
                "PersistentRecord",
                "CREATE INDEX IF NOT EXISTS `containedModelClassNameBasedIndex` " +
                    "ON `PersistentRecord` (`containedModelClassName`);"
            ),
            // actual
            sqlCommandIterator.next()
        );
    }

    /**
     * Verifies that the correct SQL bindings are generated when specifying the
     * {@link Page#startingAt(int)} and {@link QueryPaginationInput#withLimit(Integer)}
     * page details.
     * @throws DataStoreException From {@link SQLCommandFactory#queryFor(ModelSchema, QueryOptions)} 
     */
    @Test
    public void queryWithCustomPaginationInput() throws DataStoreException {
        final ModelSchema personSchema = getPersonModelSchema();
        final SqlCommand sqlCommand = sqlCommandFactory.queryFor(
                personSchema,
                Where.matchesAll().paginated(Page.startingAt(2).withLimit(20))
        );
        assertNotNull(sqlCommand);
        assertEquals(
                PERSON_BASE_QUERY + " LIMIT ? OFFSET ?;",
                sqlCommand.sqlStatement()
        );
        final List<Object> bindings = sqlCommand.getBindings();
        assertEquals(2, bindings.size());
        assertEquals(20, bindings.get(0));
        assertEquals(40, bindings.get(1));
    }

    /**
     * Validates that the correct SQL bindings are created when using the
     * {@link Page#firstPage()} query option.
     * @throws DataStoreException From {@link SQLCommandFactory#queryFor(ModelSchema, QueryOptions)}
     */
    @Test
    public void queryWithFirstPagePaginationInput() throws DataStoreException {
        final ModelSchema personSchema = getPersonModelSchema();
        final SqlCommand sqlCommand = sqlCommandFactory.queryFor(
                personSchema,
                Where.matchesAll().paginated(Page.firstPage())
        );
        assertNotNull(sqlCommand);
        assertEquals(
                PERSON_BASE_QUERY + " LIMIT ? OFFSET ?;",
                sqlCommand.sqlStatement()
        );
        final List<Object> bindings = sqlCommand.getBindings();
        assertEquals(2, bindings.size());
        assertEquals(100, bindings.get(0));
        assertEquals(0, bindings.get(1));
    }

    /**
     * Validates that the correct bindings are generated when usign the {@link Page#firstResult()}
     * pagination option.
     * @throws DataStoreException From {@link SQLCommandFactory#queryFor(ModelSchema, QueryOptions)}
     */
    @Test
    public void queryWithFirstResultPaginationInput() throws DataStoreException {
        final ModelSchema personSchema = getPersonModelSchema();
        final SqlCommand sqlCommand = sqlCommandFactory.queryFor(
                personSchema,
                Where.matchesAll().paginated(Page.firstResult())
        );
        assertNotNull(sqlCommand);
        assertEquals(
                PERSON_BASE_QUERY + " LIMIT ? OFFSET ?;",
                sqlCommand.sqlStatement()
        );
        final List<Object> bindings = sqlCommand.getBindings();
        assertEquals(2, bindings.size());
        assertEquals(1, bindings.get(0));
        assertEquals(0, bindings.get(1));
    }

    private static ModelSchema getPersonModelSchema() {
        final SortedMap<String, ModelField> fields = getFieldsMap();
        return ModelSchema.builder()
                .name("Person")
                .fields(fields)
                .build();
    }

    private static SortedMap<String, ModelField> getFieldsMap() {
        final SortedMap<String, ModelField> fields = new TreeMap<>();
        fields.put("id", ModelField.builder()
                .name("id")
                .isRequired(true)
                .targetType("String")
                .type(String.class)
                .build());
        fields.put("firstName", ModelField.builder()
                .name("firstName")
                .isRequired(true)
                .targetType("String")
                .type(String.class)
                .build());
        fields.put("lastName", ModelField.builder()
                .name("lastName")
                .isRequired(true)
                .targetType("String")
                .type(String.class)
                .build());
        fields.put("age", ModelField.builder()
                .name("age")
                .targetType("Int")
                .type(Integer.class)
                .build());
        return fields;
    }
}
