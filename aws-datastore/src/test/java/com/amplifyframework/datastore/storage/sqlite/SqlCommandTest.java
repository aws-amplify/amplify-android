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
import com.amplifyframework.core.model.Model;
import com.amplifyframework.core.model.ModelField;
import com.amplifyframework.core.model.ModelIndex;
import com.amplifyframework.core.model.ModelSchema;
import com.amplifyframework.core.model.SchemaRegistry;
import com.amplifyframework.core.model.query.Page;
import com.amplifyframework.core.model.query.QueryOptions;
import com.amplifyframework.core.model.query.QueryPaginationInput;
import com.amplifyframework.core.model.query.QuerySortBy;
import com.amplifyframework.core.model.query.QuerySortOrder;
import com.amplifyframework.core.model.query.Where;
import com.amplifyframework.core.model.query.predicate.QueryPredicate;
import com.amplifyframework.core.model.temporal.Temporal;
import com.amplifyframework.datastore.DataStoreException;
import com.amplifyframework.datastore.syncengine.PendingMutation;
import com.amplifyframework.testmodels.commentsblog.Blog3;
import com.amplifyframework.testmodels.commentsblog.BlogOwner3;
import com.amplifyframework.testmodels.commentsblog.Post2;
import com.amplifyframework.testmodels.commentsblog.PostStatus;
import com.amplifyframework.testmodels.personcar.Person;
import com.amplifyframework.testmodels.personcar.PersonWithCPK;
import com.amplifyframework.testutils.random.RandomString;
import com.amplifyframework.util.GsonFactory;
import com.amplifyframework.util.UserAgent;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.UUID;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
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
            "SELECT `Person`.`id` AS `Person_id`, `Person`.`age` AS `Person_age`, `Person`.`firstName` AS " +
                    "`Person_firstName`, `Person`.`lastName` AS `Person_lastName` FROM `Person`";

    private SQLCommandFactory sqlCommandFactory;

    /**
     * Setup before each test.
     */
    @Before
    public void createSqlCommandFactory() {
        SchemaRegistry schemaRegistry = SchemaRegistry.instance();
        sqlCommandFactory = new SQLiteCommandFactory(schemaRegistry, GsonFactory.instance());
    }

    /**
     * Test if a valid {@link ModelSchema} returns an expected
     * CREATE TABLE SQL command.
     */
    @Test
    public void validModelSchemaReturnsExpectedSqlCommandWhenNoCustomPrimaryKeyIsDefined() {
        final ModelSchema personSchema = getPersonModelSchema();

        final SqlCommand sqlCommand = sqlCommandFactory.createTableFor(personSchema);
        assertEquals("Person", sqlCommand.tableName());
        assertEquals("CREATE TABLE IF NOT EXISTS `Person` (" +
                "`id` TEXT NOT NULL, " +
                "`age` INTEGER, " +
                "`firstName` TEXT NOT NULL, " +
                "`lastName` TEXT NOT NULL, " +
                "PRIMARY KEY ( 'id'));", sqlCommand.sqlStatement());
    }

    /**
     * Test if a valid {@link ModelSchema} returns an expected
     * CREATE TABLE SQL command when custom primary key is defined.
     */
    @Test
    public void validModelSchemaReturnsExpectedSqlCommandWhenCustomPrimaryKeyIsDefined() {
        final ModelSchema personSchema = getPersonModelSchemaWithCompositePrimaryKeyWithMultipleFields();

        final SqlCommand sqlCommand = sqlCommandFactory.createTableFor(personSchema);
        assertEquals("Person", sqlCommand.tableName());
        assertEquals("CREATE TABLE IF NOT EXISTS `Person` (" +
                "`age` INTEGER, " +
                "`firstName` TEXT NOT NULL, " +
                "`hobbies` TEXT NOT NULL, " +
                "`lastName` TEXT NOT NULL, " +
                "PRIMARY KEY ( 'firstName'));", sqlCommand.sqlStatement());
    }

    /**
     * Test if a valid {@link ModelSchema} returns an expected
     * CREATE TABLE SQL command.
     * @throws AmplifyException On unable to parse schema
     */
    @Test
    public void validModelSchemaReturnsExpectedInsertSqlCommandWhenCustomPrimaryKeyIsDefined() throws AmplifyException {
        SchemaRegistry schemaRegistry = SchemaRegistry.instance();
        schemaRegistry.register(Collections.singleton(PersonWithCPK.class));
        final ModelSchema personSchema = schemaRegistry.getModelSchemaForModelClass(PersonWithCPK.class);
        final PersonWithCPK person = PersonWithCPK.builder()
                .firstName("Test")
                .lastName("Last")
                .age(12)
                .build();

        final SqlCommand sqlCommand = sqlCommandFactory.insertFor(personSchema, person);
        assertEquals("PersonWithCPK", sqlCommand.tableName());
        assertEquals("INSERT INTO `PersonWithCPK` (" +
                "`@@primaryKey`, `age`, `createdAt`, `dob`, `first_name`, `last_name`, `relationship`, `updatedAt`)" +
                " VALUES (?, ?, ?, ?, ?, ?, ?, ?)", sqlCommand.sqlStatement());
    }

    /**
     * Test if a valid {@link ModelSchema} returns an expected
     * CREATE TABLE SQL command when custom primary key is defined.
     */
    @Test
    public void validModelSchemaReturnsExpectedSqlCommandWhenCustomPrimaryKeyWithMultipleFieldsIsDefined() {
        final ModelSchema personSchema = getPersonModelSchemaWithCompositePrimaryKey();

        final SqlCommand sqlCommand = sqlCommandFactory.createTableFor(personSchema);
        assertEquals("Person", sqlCommand.tableName());
        assertEquals("CREATE TABLE IF NOT EXISTS `Person` (" +
                "`@@primaryKey` TEXT NOT NULL, `age` INTEGER, " +
                "`firstName` TEXT NOT NULL, " +
                "`hobbies` TEXT NOT NULL, " +
                "`lastName` TEXT NOT NULL, " +
                "PRIMARY KEY ( '@@primaryKey'));", sqlCommand.sqlStatement());
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
                .modelType(Model.Type.SYSTEM)
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
                .modelType(Model.Type.SYSTEM)
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
     * Test if {@link ModelSchema} with index returns an expected
     * CREATE INDEX SQL command. The Create Index command should only be generated if the Primary key is composite.
     */
    @Test
    public void expectedCreateIndexCommandNotCreatedForSingleModelIdentifier() {
        final ModelIndex index = ModelIndex.builder()
                .indexName(SQLiteCommandFactory.UNDEFINED)
                .indexFieldNames(Collections.singletonList("id"))
                .build();

        final ModelSchema modelSchema = ModelSchema.builder()
                .name("Person")
                .indexes(Collections.singletonMap(SQLiteCommandFactory.UNDEFINED, index))
                .modelType(Model.Type.SYSTEM)
                .build();

        final Iterator<SqlCommand> sqlCommandIterator = sqlCommandFactory
                .createIndexesFor(modelSchema)
                .iterator();
        assertFalse(sqlCommandIterator.hasNext());
    }

    /**
     * Test if {@link ModelSchema} with index returns an expected
     * CREATE INDEX SQL command.
     */
    @Test
    public void expectedCreateIndexCommandForModelCompositePrimaryKey() {
        List<String> keyList = new ArrayList<>();
        keyList.add("name");
        keyList.add("age");
        final ModelIndex index = ModelIndex.builder()
                .indexName(SQLiteCommandFactory.UNDEFINED)
                .indexFieldNames(keyList)
                .build();

        final ModelSchema modelSchema = ModelSchema.builder()
                .name("Person")
                .indexes(Collections.singletonMap(SQLiteCommandFactory.UNDEFINED, index))
                .modelType(Model.Type.SYSTEM)
                .build();

        final Iterator<SqlCommand> sqlCommandIterator = sqlCommandFactory
                .createIndexesFor(modelSchema)
                .iterator();
        assertTrue(sqlCommandIterator.hasNext());

        final SqlCommand createIndexSqlCommand = sqlCommandIterator.next();
        assertEquals("Person", createIndexSqlCommand.tableName());
        assertEquals("CREATE INDEX IF NOT EXISTS `undefined_name_age` ON `Person` (`name`, `age`);",
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

    /**
     * Validates that a query, with an order by clause is generated correctly.
     * @throws DataStoreException From {@link SQLCommandFactory#queryFor(ModelSchema, QueryOptions)}
     */
    @Test
    public void queryWithSortBy() throws DataStoreException {
        final ModelSchema personSchema = getPersonModelSchema();
        final SqlCommand sqlCommand = sqlCommandFactory.queryFor(
                personSchema,
                Where.matchesAll().sorted(
                        new QuerySortBy("lastName", QuerySortOrder.ASCENDING),
                        new QuerySortBy("firstName", QuerySortOrder.DESCENDING))
        );
        assertNotNull(sqlCommand);
        assertEquals(
                PERSON_BASE_QUERY + " ORDER BY `Person`.`lastName` ASC, `Person`.`firstName` DESC;",
                sqlCommand.sqlStatement()
        );
        assertEquals(0, sqlCommand.getBindings().size());
    }

    /**
     * Validates that a query, with an order by clause is generated correctly.
     * @throws AmplifyException From {@link SQLCommandFactory#queryFor(ModelSchema, QueryOptions)}
     */
    @Test
    public void queryWithPredicateForFlutter() throws AmplifyException {
        setUserAgent();
        final ModelSchema personSchema = getPersonModelSchema();
        final String testName = "name";
        final SqlCommand sqlCommand = sqlCommandFactory.queryFor(
                personSchema,
                Where.matches(Person.FIRST_NAME.eq(testName))
        );
        assertNotNull(sqlCommand);
        assertEquals(
                PERSON_BASE_QUERY + " WHERE first_name = ?;",
                sqlCommand.sqlStatement()
        );
        assertEquals(1, sqlCommand.getBindings().size());
        assertTrue(sqlCommand.getBindings().contains(testName));
    }

    /**
     * Verify the SqlCommand generated to check if a model exists is as expected.
     * @throws DataStoreException From {@link SQLCommandFactory#existsFor(ModelSchema, QueryPredicate)}
     */
    @Test
    public void existsFor() throws DataStoreException {
        final ModelSchema personSchema = getPersonModelSchema();
        String personId = RandomString.string();
        final SqlCommand sqlCommand = sqlCommandFactory.existsFor(personSchema, Person.ID.eq(personId));
        assertEquals("SELECT EXISTS(SELECT 1 FROM `Person` WHERE id = ?);",
            sqlCommand.sqlStatement());
        assertEquals(Collections.singletonList(personId), sqlCommand.getBindings());
    }

    private static ModelSchema getPersonModelSchema() {
        final SortedMap<String, ModelField> fields = getFieldsMap();
        return ModelSchema.builder()
                .name("Person")
                .fields(fields)
                .modelType(Model.Type.SYSTEM)
                .build();
    }

    private static SortedMap<String, ModelField> getFieldsMap() {
        final SortedMap<String, ModelField> fields = new TreeMap<>();
        fields.put("id", ModelField.builder()
                .name("id")
                .isRequired(true)
                .targetType("String")
                .javaClassForValue(String.class)
                .build());
        fields.put("firstName", ModelField.builder()
                .name("firstName")
                .isRequired(true)
                .targetType("String")
                .javaClassForValue(String.class)
                .build());
        fields.put("lastName", ModelField.builder()
                .name("lastName")
                .isRequired(true)
                .targetType("String")
                .javaClassForValue(String.class)
                .build());
        fields.put("age", ModelField.builder()
                .name("age")
                .targetType("Int")
                .javaClassForValue(Integer.class)
                .build());
        return fields;
    }

    private static ModelSchema getPersonModelSchemaWithCompositePrimaryKey() {
        final SortedMap<String, ModelField> fields = getFieldsMap();
        fields.remove("id");
        fields.put("hobbies", ModelField.builder()
                .name("hobbies")
                .isRequired(true)
                .targetType("String")
                .javaClassForValue(String.class)
                .build());
        final List<String> indexFieldNames = new ArrayList<>();
        indexFieldNames.add("firstName");
        indexFieldNames.add("lastName");
        indexFieldNames.add("age");
        final ModelIndex index = ModelIndex.builder()
                .indexName("undefined")
                .indexFieldNames(Collections.unmodifiableList(indexFieldNames))
                .build();

        return ModelSchema.builder()
                .name("Person")
                .fields(fields)
                .indexes(Collections.singletonMap("undefined", index))
                .modelType(Model.Type.USER)
                .build();
    }

    private static ModelSchema getPersonModelSchemaWithCompositePrimaryKeyWithMultipleFields() {
        final SortedMap<String, ModelField> fields = getFieldsMap();
        fields.remove("id");
        fields.put("hobbies", ModelField.builder()
                .name("hobbies")
                .isRequired(true)
                .targetType("String")
                .javaClassForValue(String.class)
                .build());
        final List<String> indexFieldNames = new ArrayList<>();
        indexFieldNames.add("firstName");
        final ModelIndex index = ModelIndex.builder()
                .indexName("undefined")
                .indexFieldNames(Collections.unmodifiableList(indexFieldNames))
                .build();

        return ModelSchema.builder()
                .name("Person")
                .fields(fields)
                .indexes(Collections.singletonMap("undefined", index))
                .modelType(Model.Type.USER)
                .build();
    }

    /**
     * Set user agent to Flutter.
     * @throws AmplifyException not expected.
     */
    private void setUserAgent() throws AmplifyException {
        Map<UserAgent.Platform, String> map = new HashMap<>();
        map.put(UserAgent.Platform.FLUTTER, "1.0");
        UserAgent.configure(map);
    }

    /**
     * Validates the generation of SQL queries involving nested schemas, ensuring the correct formation
     * of JOIN clauses and WHERE conditions. This method specifically tests queries that span multiple
     * related tables (e.g., BlogOwner3 -> Blog3 -> Post2), verifying that the query correctly selects
     * data from Post2 based on a condition on BlogId and BlogOwnerId that involves a foreign key relationship.
     *
     * <p>Example scenario: The method tests a query for selecting data from Post2, using a
     * condition on BlogId and BlogOwnerId which are foreign keys in Post2 for Blog3 and BlogOwner3 resp.
     * It ensures that the join and WHERE clause are formed as expected.
     *
     * <p>Primary focus:
     * <ul>
     *   <li>Correct formation of JOIN clauses between nested tables.</li>
     *   <li>Appropriate referencing of column names in the WHERE clause to ensure the use of relevant
     *       foreign key relationships.</li>
     * </ul>
     *
     * @throws AmplifyException From {@link SQLCommandFactory#queryFor(ModelSchema, QueryOptions)}
     */
    @Test
    public void validNestedSchemaReturnQueryWithExpectedJoinsAndNestedWhereClause() throws AmplifyException {
        UUID blogOwnerId = UUID.randomUUID();
        UUID blogId = UUID.randomUUID();
        SchemaRegistry schemaRegistry = SchemaRegistry.instance();
        schemaRegistry.register(Collections.singleton(BlogOwner3.class));
        schemaRegistry.register(Collections.singleton(Blog3.class));
        schemaRegistry.register(Collections.singleton(Post2.class));

        final ModelSchema postSchema = schemaRegistry.getModelSchemaForModelClass(Post2.class);

        final BlogOwner3 blogOwner = BlogOwner3.builder()
                .name("Owner")
                .id(blogOwnerId.toString())
                .createdAt(new Temporal.DateTime("2023-12-15T16:22:30.48Z"))
                .build();

        final Blog3 blog = Blog3.builder()
                .name("Blog")
                .id(blogId.toString())
                .owner(blogOwner)
                .createdAt(new Temporal.DateTime("2023-12-15T16:22:35.48Z"))
                .build();

        final Post2 post = Post2.builder()
                .title("Test Post")
                .status(PostStatus.ACTIVE)
                .rating(5)
                .blogOwner(blogOwner)
                .blog(blog)
                .createdAt(new Temporal.DateTime("2023-12-15T16:22:38.48Z"))
                .build();

        // Create an instance of SQLiteCommandFactory
        SQLiteCommandFactory sqlCommandFactory = new SQLiteCommandFactory(schemaRegistry, GsonFactory.instance());
        // Create QueryOptions with a predicate to filter Posts by BlogOwnerId
        QueryPredicate predicate = Post2.BLOG.eq(blog.getId()).and(Post2.BLOG_OWNER.eq(blogOwner.getId()));
        QueryOptions options = Where.matches(predicate);

        // Call queryFor for the Post entity
        SqlCommand sqlCommand = sqlCommandFactory.queryFor(postSchema, options);

        // The expected SQL query
        String expectedQuery = "SELECT `Blog3_BlogOwner3`.`id` AS `Blog3_BlogOwner3_id`, " +
                "`Blog3_BlogOwner3`.`createdAt` AS" +
                " `Blog3_BlogOwner3_createdAt`, `Blog3_BlogOwner3`.`name` AS `Blog3_BlogOwner3_name`," +
                " `Blog3_BlogOwner3`.`updatedAt` AS `Blog3_BlogOwner3_updatedAt`," +
                " `BlogOwner3`.`id` AS `BlogOwner3_id`, " +
                "`BlogOwner3`.`createdAt` AS `BlogOwner3_createdAt`, `BlogOwner3`.`name` " +
                "AS `BlogOwner3_name`, `BlogOwner3`.`updatedAt` AS `BlogOwner3_updatedAt`, " +

                "`Blog3`.`id` AS `Blog3_id`, `Blog3`.`createdAt` AS `Blog3_createdAt`, `Blog3`.`name` " +
                "AS `Blog3_name`, `Blog3`.`updatedAt` AS `Blog3_updatedAt`, `Blog3`.`blogOwnerID` " +
                "AS `Blog3_blogOwnerID`, `Post2`.`id` AS `Post2_id`, `Post2`.`createdAt` AS" +
                " `Post2_createdAt`, `Post2`.`rating` AS `Post2_rating`, `Post2`.`status` AS " +
                "`Post2_status`, `Post2`.`title` AS `Post2_title`, `Post2`.`updatedAt` AS " +
                "`Post2_updatedAt`, `Post2`.`blogID` AS `Post2_blogID`, `Post2`.`blogOwnerID` AS " +
                "`Post2_blogOwnerID` " +
                "FROM `Post2` " +
                "LEFT JOIN `Blog3` AS `Blog3` ON `Post2`.`blogID` = `Blog3`.`id` " +
                "LEFT JOIN `BlogOwner3` AS `Blog3_BlogOwner3` ON `Blog3`.`blogOwnerID` = `Blog3_BlogOwner3`.`id` " +
                "LEFT JOIN `BlogOwner3` AS `BlogOwner3` ON `Post2`.`blogOwnerID` = `BlogOwner3`.`id`  " +
                "WHERE (`Post2`.`blogID` = ? AND `Post2`.`blogOwnerID` = ?);";

        // Assert the SQL command is as expected
        assertEquals(expectedQuery, sqlCommand.sqlStatement());
        assertEquals(sqlCommand.getBindings().size(), 2);
        assertEquals(blog.getId(), sqlCommand.getBindings().get(0));
        assertEquals(blogOwner.getId(), sqlCommand.getBindings().get(1));

    }

    /**
     * Validates the generation of SQL queries involving nested schemas, ensuring the correct formation
     * of JOIN clauses and WHERE conditions. This method specifically tests queries that span multiple
     * related tables (e.g., BlogOwner3 -> Blog3 -> Post2), verifying that the query correctly selects
     * data from Post2 based on a condition that involves a foreign key relationship.
     *
     * <p>Example scenario: The method tests a query for selecting data from Post2, using a
     * condition on BlogOwnerId column which is a foreign key in Post2 pointing to primary key in BlogOwner3.
     * It ensures that the WHERE clause correctly utilizes the BlogOwner3's foreign key in Post2
     * and not from Blog3.
     *
     * <p>Primary focus:
     * <ul>
     *   <li>Correct formation of JOIN clauses between nested tables.</li>
     *   <li>Appropriate referencing of column names in the WHERE clause to ensure the use of relevant
     *       foreign key relationships.</li>
     * </ul>
     *
     * @throws AmplifyException From {@link SQLCommandFactory#queryFor(ModelSchema, QueryOptions)}
     */
    @Test
    public void validNestedSchemaReturnQueryWithExpectedJoinsAndWhereClause() throws AmplifyException {
        UUID blogOwnerId = UUID.randomUUID();
        UUID blogId = UUID.randomUUID();
        SchemaRegistry schemaRegistry = SchemaRegistry.instance();
        schemaRegistry.register(Collections.singleton(BlogOwner3.class));
        schemaRegistry.register(Collections.singleton(Blog3.class));
        schemaRegistry.register(Collections.singleton(Post2.class));

        final ModelSchema postSchema = schemaRegistry.getModelSchemaForModelClass(Post2.class);

        final BlogOwner3 blogOwner = BlogOwner3.builder()
                .name("Owner")
                .id(blogOwnerId.toString())
                .createdAt(new Temporal.DateTime("2023-12-15T16:22:30.48Z"))
                .build();

        final Blog3 blog = Blog3.builder()
                .name("Blog")
                .id(blogId.toString())
                .owner(blogOwner)
                .createdAt(new Temporal.DateTime("2023-12-15T16:22:35.48Z"))
                .build();

        final Post2 post = Post2.builder()
                .title("Test Post")
                .status(PostStatus.ACTIVE)
                .rating(5)
                .blogOwner(blogOwner)
                .blog(blog)
                .createdAt(new Temporal.DateTime("2023-12-15T16:22:38.48Z"))
                .build();

        // Create an instance of SQLiteCommandFactory
        SQLiteCommandFactory sqlCommandFactory = new SQLiteCommandFactory(schemaRegistry, GsonFactory.instance());
        // Create QueryOptions with a predicate to filter Posts by BlogOwnerId
        QueryPredicate predicate = Post2.BLOG_OWNER.eq(blogOwner.getId());
        QueryOptions options = Where.matches(predicate);

        // Call queryFor for the Post entity
        SqlCommand sqlCommand = sqlCommandFactory.queryFor(postSchema, options);

        // The expected SQL query
        String expectedQuery = "SELECT `Blog3_BlogOwner3`.`id` AS `Blog3_BlogOwner3_id`, " +
                "`Blog3_BlogOwner3`.`createdAt` AS" +
                " `Blog3_BlogOwner3_createdAt`, `Blog3_BlogOwner3`.`name` AS `Blog3_BlogOwner3_name`," +
                " `Blog3_BlogOwner3`.`updatedAt` AS `Blog3_BlogOwner3_updatedAt`, " +
                "`BlogOwner3`.`id` AS `BlogOwner3_id`, `BlogOwner3`.`createdAt`" +
                " AS `BlogOwner3_createdAt`, " +
                "`BlogOwner3`.`name` AS `BlogOwner3_name`, `BlogOwner3`.`updatedAt` AS `BlogOwner3_updatedAt`, " +

                "`Blog3`.`id` AS `Blog3_id`, `Blog3`.`createdAt` AS `Blog3_createdAt`, `Blog3`.`name`" +
                " AS `Blog3_name`, " +
                "`Blog3`.`updatedAt` AS `Blog3_updatedAt`, " +
                "`Blog3`.`blogOwnerID` AS `Blog3_blogOwnerID`, " +
                "`Post2`.`id` AS `Post2_id`, `Post2`.`createdAt` AS `Post2_createdAt`, `Post2`.`rating`" +
                " AS `Post2_rating`, " +
                "`Post2`.`status` AS `Post2_status`, `Post2`.`title` AS `Post2_title`, " +
                "`Post2`.`updatedAt` AS `Post2_updatedAt`, `Post2`.`blogID` AS `Post2_blogID`, `Post2`.`blogOwnerID`" +
                " AS `Post2_blogOwnerID` " +
                "FROM `Post2` " +
                "LEFT JOIN `Blog3` AS `Blog3` ON `Post2`.`blogID` = `Blog3`.`id` " +
                "LEFT JOIN `BlogOwner3` AS `Blog3_BlogOwner3` ON `Blog3`.`blogOwnerID` = `Blog3_BlogOwner3`.`id` " +
                "LEFT JOIN `BlogOwner3` AS `BlogOwner3` ON `Post2`.`blogOwnerID` = `BlogOwner3`.`id`  " +
                "WHERE `Post2`.`blogOwnerID` = ?;";

        // Assert the SQL command is as expected
        assertEquals(expectedQuery, sqlCommand.sqlStatement());
        assertEquals(sqlCommand.getBindings().size(), 1);
        assertEquals(blogOwner.getId(), sqlCommand.getBindings().get(0));
    }
}
