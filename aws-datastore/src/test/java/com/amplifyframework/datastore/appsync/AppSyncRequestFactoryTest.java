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

package com.amplifyframework.datastore.appsync;

import androidx.annotation.NonNull;

import com.amplifyframework.AmplifyException;
import com.amplifyframework.api.aws.AppSyncGraphQLRequest;
import com.amplifyframework.api.aws.AuthModeStrategyType;
import com.amplifyframework.api.aws.GraphQLRequestHelper;
import com.amplifyframework.api.graphql.GraphQLRequest;
import com.amplifyframework.api.graphql.SubscriptionType;
import com.amplifyframework.core.model.AuthStrategy;
import com.amplifyframework.core.model.CustomTypeField;
import com.amplifyframework.core.model.CustomTypeSchema;
import com.amplifyframework.core.model.Model;
import com.amplifyframework.core.model.ModelField;
import com.amplifyframework.core.model.ModelSchema;
import com.amplifyframework.core.model.SchemaRegistry;
import com.amplifyframework.core.model.SerializedCustomType;
import com.amplifyframework.core.model.SerializedModel;
import com.amplifyframework.core.model.annotations.AuthRule;
import com.amplifyframework.core.model.annotations.ModelConfig;
import com.amplifyframework.core.model.query.predicate.QueryPredicate;
import com.amplifyframework.core.model.query.predicate.QueryPredicates;
import com.amplifyframework.core.model.temporal.Temporal;
import com.amplifyframework.datastore.DataStoreException;
import com.amplifyframework.testmodels.commentsblog.Blog;
import com.amplifyframework.testmodels.commentsblog.Blog2;
import com.amplifyframework.testmodels.commentsblog.BlogOwner;
import com.amplifyframework.testmodels.commentsblog.BlogOwner2;
import com.amplifyframework.testmodels.commentsblog.BlogOwnerWithCustomPK;
import com.amplifyframework.testmodels.commentsblog.Comment;
import com.amplifyframework.testmodels.commentsblog.OtherBlog;
import com.amplifyframework.testmodels.commentsblog.Post;
import com.amplifyframework.testmodels.ecommerce.Item;
import com.amplifyframework.testmodels.ecommerce.Status;
import com.amplifyframework.testmodels.parenting.Address;
import com.amplifyframework.testmodels.parenting.Child;
import com.amplifyframework.testmodels.parenting.City;
import com.amplifyframework.testmodels.parenting.Parent;
import com.amplifyframework.testmodels.parenting.Phonenumber;
import com.amplifyframework.testmodels.personcar.Person;
import com.amplifyframework.testutils.Resources;

import org.json.JSONException;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.skyscreamer.jsonassert.JSONAssert;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.assertEquals;

/**
 * Tests the {@link AppSyncRequestFactory}.
 */
@RunWith(RobolectricTestRunner.class) // Adds Android library to make TextUtils.join available for tests.
public final class AppSyncRequestFactoryTest {
    private static final AuthModeStrategyType DEFAULT_STRATEGY =
        AuthModeStrategyType.DEFAULT;

    /**
     * Validates the construction of a base-sync query document.
     * @throws DataStoreException On failure to interrogate fields in Blog.class
     * @throws AmplifyException On failure to parse ModelSchema from model class
     * @throws JSONException from JSONAssert.assertEquals
     */
    @Test
    public void validateRequestGenerationForBaseSync() throws AmplifyException, JSONException {
        ModelSchema schema = ModelSchema.fromModelClass(BlogOwner.class);
        JSONAssert.assertEquals(
            Resources.readAsString("base-sync-request-document-for-blog-owner.txt"),
            AppSyncRequestFactory.buildSyncRequest(schema,
                                                   null,
                                                   null,
                                                   QueryPredicates.all(),
                                                   DEFAULT_STRATEGY).getContent(),
            true
        );
    }

    /**
     * Validates the construction of a base-sync query document for models with custom types.
     * @throws DataStoreException On failure to interrogate fields in Parent.class
     * @throws AmplifyException On failure to parse ModelSchema from model class
     * @throws JSONException from JSONAssert.assertEquals
     */
    @Test
    public void validateCustomTypeRequestGenerationForBaseSync() throws AmplifyException, JSONException {
        ModelSchema schema = ModelSchema.fromModelClass(Parent.class);
        JSONAssert.assertEquals(
            Resources.readAsString("base-sync-request-document-for-parent.txt"),
            AppSyncRequestFactory.buildSyncRequest(schema,
                                                   null,
                                                   null,
                                                   QueryPredicates.all(),
                                                   DEFAULT_STRATEGY).getContent(),
            true
        );
    }

    /**
     * Validates the construction of a delta-sync query document.
     * @throws DataStoreException On failure to interrogate fields in Blog.class.
     * @throws AmplifyException On failure to parse ModelSchema from model class
     * @throws JSONException from JSONAssert.assertEquals
     */
    @Test
    public void validateRequestGenerationForDeltaSync() throws AmplifyException, JSONException {
        ModelSchema schema = ModelSchema.fromModelClass(Post.class);
        JSONAssert.assertEquals(Resources.readAsString("delta-sync-request-document-for-post.txt"),
                                AppSyncRequestFactory.buildSyncRequest(schema,
                                                                       123123123L,
                                                                       null,
                                                                       QueryPredicates.all(),
                                                                       DEFAULT_STRATEGY).getContent(),
            true);
    }

    /**
     * Validates that the nextToken parameter is correctly generate for a Sync query.
     * @throws DataStoreException On failure to interrogate the BlogOwner.class.
     * @throws AmplifyException On failure to parse ModelSchema from model class
     * @throws JSONException from JSONAssert.assertEquals.
     */
    @Test
    public void validateRequestGenerationForPagination() throws AmplifyException, JSONException {
        Integer limit = 1000;
        ModelSchema schema = ModelSchema.fromModelClass(BlogOwner.class);
        final GraphQLRequest<Iterable<Post>> request =
                AppSyncRequestFactory.buildSyncRequest(schema, null, limit, QueryPredicates.all(), DEFAULT_STRATEGY);
        JSONAssert.assertEquals(Resources.readAsString("base-sync-request-paginating-blog-owners.txt"),
                request.getContent(),
                true);
    }

    /**
     * If a QueryPredicateOperation is provided, it should be wrapped in an AND group.  This enables AppSync to
     * optimize by performing an DDB query instead of scan.
     * @throws AmplifyException On failure to parse ModelSchema from model class
     * @throws JSONException from JSONAssert.assertEquals.
     */
    @Test
    public void validatePredicateOperationForSyncExpressionIsWrappedWithAnd() throws AmplifyException, JSONException {
        String id = "426f8e8d-ea0f-4839-a73f-6a2a38565ba1";
        ModelSchema schema = ModelSchema.fromModelClass(BlogOwner.class);
        final GraphQLRequest<Iterable<Post>> request =
                AppSyncRequestFactory.buildSyncRequest(schema, null, null, BlogOwner.ID.eq(id));
        JSONAssert.assertEquals(Resources.readAsString("base-sync-request-with-predicate-operation.txt"),
                request.getContent(),
                true);
    }

    /**
     * If a QueryPredicateGroup is provided, it should be parsed as is, and not be wrapped with another AND group.
     * @throws AmplifyException On failure to parse ModelSchema from model class
     * @throws JSONException from JSONAssert.assertEquals.
     */
    @Test
    public void validatePredicateGroupForSyncExpressionIsNotWrappedWithAnd() throws AmplifyException, JSONException {
        String id = "426f8e8d-ea0f-4839-a73f-6a2a38565ba1";
        ModelSchema schema = ModelSchema.fromModelClass(BlogOwner.class);
        QueryPredicate predicate = BlogOwner.ID.eq(id).and(Blog.NAME.eq("Spaghetti"));
        final GraphQLRequest<Iterable<Post>> request =
                AppSyncRequestFactory.buildSyncRequest(schema, null, null, predicate);
        JSONAssert.assertEquals(Resources.readAsString("base-sync-request-with-predicate-group.txt"),
                request.getContent(),
                true);
    }


    /**
     * Checks that we're getting the expected output for a mutation with predicate.
     * @throws DataStoreException If the output does not match.
     * @throws AmplifyException On failure to parse ModelSchema from model class
     * @throws JSONException from JSONAssert.assertEquals.
     */
    @Test
    public void validateUpdateWithPredicateGeneration() throws AmplifyException, JSONException {
        String blogOwnerId = "926d7ee8-4ea5-40c0-8e62-3fb80b2a2edd";
        BlogOwner owner = BlogOwner.builder().name("John Doe").id(blogOwnerId).build();
        ModelSchema schema = ModelSchema.fromModelClass(BlogOwner.class);
        AppSyncGraphQLRequest<?> request = AppSyncRequestFactory.buildUpdateRequest(schema,
                                                                                    owner,
                                                                                    42,
                                                                                    BlogOwner.WEA.contains("ther"),
                                                                                    DEFAULT_STRATEGY);
        JSONAssert.assertEquals(
            Resources.readAsString("update-blog-owner-with-predicate.txt"),
            request.getContent(),
            true
        );
    }

    /**
     * Checks that we're getting the expected output for a mutation with predicate.
     * @throws DataStoreException If the output does not match.
     * @throws AmplifyException On failure to parse ModelSchema from model class
     * @throws JSONException from JSONAssert.assertEquals.
     */
    @Test
    public void validateUpdateNestedCustomTypeWithPredicateGeneration() throws AmplifyException, JSONException {
        ModelSchema schema = ModelSchema.fromModelClass(Parent.class);
        JSONAssert.assertEquals(
                Resources.readAsString("update-parent-with-predicate.txt"),
                AppSyncRequestFactory.buildUpdateRequest(
                        schema,
                        buildTestParentModel(),
                        42,
                        Parent.NAME.contains("Jane Doe"),
                        DEFAULT_STRATEGY
                ).getContent(),
                true
        );
    }

    /**
     * Checks that we're getting the expected output for a mutation with predicate.
     * @throws DataStoreException If the output does not match.
     * @throws AmplifyException On failure to parse ModelSchema from model class
     * @throws JSONException from JSONAssert.assertEquals.
     */
    @Test
    public void validateDeleteWithPredicateGeneration() throws AmplifyException, JSONException {
        ModelSchema schema = ModelSchema.fromModelClass(Person.class);
        Person person = Person.justId("17521540-ccaa-4357-a622-34c42d8cfa24");
        JSONAssert.assertEquals(
            Resources.readAsString("delete-person-with-predicate.txt"),
            AppSyncRequestFactory.buildDeletionRequest(schema,
                                                       person,
                                                       456,
                                                       Person.AGE.gt(40),
                                                       DEFAULT_STRATEGY).getContent(),
            true
        );
    }


    /**
     * Checks that we're getting the expected output for a delete mutation for an object with a custom primary key.
     * @throws DataStoreException If the output does not match.
     * @throws AmplifyException On failure to parse ModelSchema from model class
     * @throws JSONException from JSONAssert.assertEquals.
     */
    @Test
    public void validateDeleteWithCustomPrimaryKey() throws AmplifyException, JSONException {
        ModelSchema schema = ModelSchema.fromModelClass(Item.class);
        final Item item = Item.builder()
                .orderId("123a7asa")
                .status(Status.IN_TRANSIT)
                .createdAt(new Temporal.DateTime("2021-04-20T15:20:32.651Z"))
                .name("Gummy Bears")
                .build();
        JSONAssert.assertEquals(
            Resources.readAsString("delete-item.txt"),
            AppSyncRequestFactory.buildDeletionRequest(schema,
                                                       item,
                                                       1,
                                                       QueryPredicates.all(),
                                                       DEFAULT_STRATEGY).getContent(),
            true
        );
    }

    /**
     * Checks that the predicate expression matches the expected value.
     * @throws AmplifyException If the output does not match.
     */
    @Test
    public void validatePredicateGeneration() throws AmplifyException {
        assertEquals(
            Collections.singletonMap("name", Collections.singletonMap("eq", "Test Dummy")),
            GraphQLRequestHelper.parsePredicate(BlogOwner.NAME.eq("Test Dummy"))
        );

        assertEquals(
            Collections.singletonMap("and", Arrays.asList(
                Collections.singletonMap("name", Collections.singletonMap("beginsWith", "A day in the life of a...")),
                Collections.singletonMap("blogOwnerBlogId", Collections.singletonMap("eq", "DUMMY_OWNER_ID"))
            )),
                GraphQLRequestHelper.parsePredicate(
                Blog.NAME.beginsWith("A day in the life of a...").and(Blog.OWNER.eq("DUMMY_OWNER_ID"))
            )
        );
    }

    /**
     * Validates that a GraphQL request document can be created, to get onCreate
     * subscription notifications for a Blog.class.
     * @throws DataStoreException On failure to interrogate the Blog.class.
     * @throws AmplifyException On failure to parse ModelSchema from model class
     * @throws JSONException from JSONAssert.assertEquals.
     */
    @Test
    public void validateSubscriptionGenerationOnCreateBlog() throws AmplifyException, JSONException {
        ModelSchema schema = ModelSchema.fromModelClass(Blog.class);
        JSONAssert.assertEquals(
            Resources.readAsString("on-create-request-for-blog.txt"),
            AppSyncRequestFactory.buildSubscriptionRequest(schema,
                                                           SubscriptionType.ON_CREATE,
                                                           DEFAULT_STRATEGY).getContent(),
            true
        );
    }

    /**
     * Validates that a GraphQL request document can be created, to get onCreate for nested custom type
     * subscription notifications for a Parent.class.
     * @throws DataStoreException On failure to interrogate the Blog.class.
     * @throws AmplifyException On failure to parse ModelSchema from model class
     * @throws JSONException from JSONAssert.assertEquals.
     */
    @Test
    public void validateSubscriptionGenerationOnCreateForNestedCustomType() throws AmplifyException, JSONException {
        ModelSchema schema = ModelSchema.fromModelClass(Parent.class);
        JSONAssert.assertEquals(
            Resources.readAsString("on-create-request-for-parent.txt"),
            AppSyncRequestFactory.buildSubscriptionRequest(schema,
                                                           SubscriptionType.ON_CREATE,
                                                           DEFAULT_STRATEGY).getContent(),
            true
        );
    }

    /**
     * Validates generation of a GraphQL document which requests a subscription for updates
     * to the Blog.class.
     * @throws DataStoreException On failure to interrogate fields in Blog.class.
     * @throws AmplifyException On failure to parse ModelSchema from model class
     * @throws JSONException from JSONAssert.assertEquals.
     */
    @Test
    public void validateSubscriptionGenerationOnUpdatePost() throws AmplifyException, JSONException {
        ModelSchema schema = ModelSchema.fromModelClass(Post.class);
        JSONAssert.assertEquals(
            Resources.readAsString("on-update-request-for-post.txt"),
            AppSyncRequestFactory.buildSubscriptionRequest(schema,
                                                           SubscriptionType.ON_UPDATE,
                                                           DEFAULT_STRATEGY).getContent(),
            true
        );
    }

    /**
     * Validates generation of a GraphQL document which requests a subscription for deletes.
     * for the BlogOwner.class.
     * @throws DataStoreException On failure to interrogate the fields in BlogOwner.class.
     * @throws AmplifyException On failure to parse ModelSchema from model class
     * @throws JSONException from JSONAssert.assertEquals.
     */
    @Test
    public void validateSubscriptionGenerationOnDeleteBlogOwner() throws AmplifyException, JSONException {
        ModelSchema schema = ModelSchema.fromModelClass(BlogOwner.class);
        JSONAssert.assertEquals(
            Resources.readAsString("on-delete-request-for-blog-owner.txt"),
            AppSyncRequestFactory.buildSubscriptionRequest(schema,
                                                           SubscriptionType.ON_DELETE,
                                                           DEFAULT_STRATEGY).getContent(),
            true
        );
    }

    /**
     * Validates creation of a "create a model" request.
     * @throws DataStoreException On failure to interrogate the model fields.
     * @throws AmplifyException On failure to parse ModelSchema from model class
     * @throws JSONException from JSONAssert.assertEquals.
     */
    @Test
    public void validateMutationGenerationOnCreateComment() throws AmplifyException, JSONException {
        Post post = Post.justId("9a4295d6-8225-495a-a531-beffc8b7ae7d");
        Comment comment = Comment.builder()
            .id("426f8e8d-ea0f-4839-a73f-6a2a38565ba1")
            .content("toast")
            .post(post)
            .build();
        ModelSchema schema = ModelSchema.fromModelClass(Comment.class);
        JSONAssert.assertEquals(
            Resources.readAsString("create-comment-request.txt"),
            AppSyncRequestFactory.buildCreationRequest(schema, comment, DEFAULT_STRATEGY).getContent(),
            true
        );
    }

    /**
     * Validates creation of a "create a model" request on a model with a custom foreign key and sort key.
     * @throws DataStoreException On failure to interrogate the model fields.
     * @throws AmplifyException On failure to parse ModelSchema from model class
     * @throws JSONException from JSONAssert.assertEquals.
     */
    @Test
    public void validateMutationGenerationOnCreateItemWithCustomForeignKeyAndSortKey() throws
            AmplifyException, JSONException {
        final BlogOwnerWithCustomPK blogOwner = BlogOwnerWithCustomPK.builder()
                .id("b0792b4b-2b38-4ab7-a12d-42b35583171e")
                .name("Stanley")
                .wea("WEA")
                .build();
        final OtherBlog blog = OtherBlog.builder()
                .name("My Other Blog")
                .owner(blogOwner)
                .id("5a90f4dc-2dd7-49bd-85f8-d45119c30790")
                .build();
        ModelSchema schema = ModelSchema.fromModelClass(OtherBlog.class);
        String expected = Resources.readAsString("create-other-blog.txt");
        String actual = AppSyncRequestFactory.buildCreationRequest(schema, blog, DEFAULT_STRATEGY).getContent();
        JSONAssert.assertEquals(
                expected,
                actual,
                true
        );
    }

    /**
     * Validates creation of a "create a model" request on a model with a custom foreign key and sort key.
     * @throws DataStoreException On failure to interrogate the model fields.
     * @throws AmplifyException On failure to parse ModelSchema from model class
     * @throws JSONException from JSONAssert.assertEquals.
     */
    @Test
    public void validateMutationGenerationOnCreateItemWithCustomForeignKeyAndSortKeyWithANestedSerializedModel() throws
            AmplifyException, JSONException {
        final BlogOwnerWithCustomPK blogOwner = BlogOwnerWithCustomPK.builder()
                .id("b0792b4b-2b38-4ab7-a12d-42b35583171e")
                .name("Stanley")
                .wea("WEA")
                .build();
        Map<String, Object> serializedBlogOwnerData = new HashMap<>();
        serializedBlogOwnerData.put("id", blogOwner.getId());
        serializedBlogOwnerData.put("name", blogOwner.getName());
        serializedBlogOwnerData.put("wea", blogOwner.getWea());
        SerializedModel serializedModel = SerializedModel.builder()
                .modelSchema(ModelSchema.fromModelClass(BlogOwnerWithCustomPK.class))
                .serializedData(serializedBlogOwnerData).build();
        final OtherBlog blog = OtherBlog.builder()
                .name("My Other Blog")
                .owner(blogOwner)
                .id("5a90f4dc-2dd7-49bd-85f8-d45119c30790")
                .build();
        Map<String, Object> serializedBlogData = new HashMap<>();
        serializedBlogData.put("id", blog.getId());
        serializedBlogData.put("name", blog.getName());
        serializedBlogData.put("owner", serializedModel);
        serializedBlogData.put("createdAt", null);
        ModelSchema schema = ModelSchema.fromModelClass(OtherBlog.class);
        SerializedModel serializedModelBlog = SerializedModel.builder()
                .modelSchema(schema)
                .serializedData(serializedBlogData).build();
        String expected = Resources.readAsString("create-other-blog.txt");
        String actual = AppSyncRequestFactory.buildCreationRequest(schema, serializedModelBlog, DEFAULT_STRATEGY)
                .getContent();
        System.out.println("  Actual: " + actual);
        System.out.println("Expected: " + expected);
        JSONAssert.assertEquals(
                expected,
                actual,
                true
        );
    }


    /**
     * Validates creation of a "create a model" request on a model with a custom foreign key and sort key.
     * @throws DataStoreException On failure to interrogate the model fields.
     * @throws AmplifyException On failure to parse ModelSchema from model class
     * @throws JSONException from JSONAssert.assertEquals.
     */
    @Test
    public void validateMutationGenerationOnCreateItemWithCustomForeignKeyNoSortKey()
            throws AmplifyException, JSONException {
        final BlogOwner2 blogOwner = BlogOwner2.builder()
                .id("b0792b4b-2b38-4ab7-a12d-42b35583171e")
                .name("Stanley")
                .build();
        final Blog2 blog = Blog2.builder()
                .name("My Other Blog")
                .owner(blogOwner)
                .id("5a90f4dc-2dd7-49bd-85f8-d45119c30790")
                .build();
        ModelSchema schema = ModelSchema.fromModelClass(Blog2.class);
        String expected = Resources.readAsString("create-blog2.txt");
        String actual = AppSyncRequestFactory.buildCreationRequest(schema, blog, DEFAULT_STRATEGY).getContent();
        JSONAssert.assertEquals(
                expected,
                actual,
                true
        );
    }

    /**
     * Validates creation of a "create a model" request for nested custom type.
     * @throws AmplifyException On failure to interrogate the model fields.
     * @throws AmplifyException On failure to parse ModelSchema from model class
     * @throws JSONException from JSONAssert.assertEquals.
     */
    @Test
    public void validateMutationGenerationOnCreateNestedCustomType() throws AmplifyException, JSONException {
        ModelSchema schema = ModelSchema.fromModelClass(Parent.class);
        JSONAssert.assertEquals(
            Resources.readAsString("create-parent-request.txt"),
            AppSyncRequestFactory.buildCreationRequest(schema, buildTestParentModel(), DEFAULT_STRATEGY).getContent(),
            true
        );
    }

    /**
     * Verify that only the fields included in serializedData on the SerializedModel are included on the API request.
     * To test this, create a SerializedModel of a BlogOwner, which only has "id", and "name", not the "wea" field.
     * Then, verify that the request does not contain "wea" field.
     *
     * @throws JSONException from JSONAssert.assertEquals JSON parsing error
     * @throws AmplifyException from ModelSchema.fromModelClass to convert model to schema
     */
    @Test
    public void validateUpdateMutationOnlyContainsChangedFields() throws JSONException, AmplifyException {
        ModelSchema modelSchema = ModelSchema.fromModelClass(BlogOwner.class);
        Map<String, Object> serializedData = new HashMap<>();
        serializedData.put("id", "5aef1282-64d6-4fa8-ba2c-290f9d9c6973");
        serializedData.put("name", "John Smith");

        SerializedModel blogOwner = SerializedModel.builder()
                .modelSchema(modelSchema)
                .serializedData(serializedData)
                .build();

        // Assert
        JSONAssert.assertEquals(Resources.readAsString("update-blog-owner-only-changed-fields.txt"),
            AppSyncRequestFactory.buildUpdateRequest(
                modelSchema, blogOwner, 1, QueryPredicates.all(), DEFAULT_STRATEGY).getContent(), true);
    }

    /**
     * Validates creation of a serialized model nests serialized custom types.
     *
     * @throws JSONException from JSONAssert.assertEquals JSON parsing error
     * @throws AmplifyException from ModelSchema.fromModelClass to convert model to schema
     */
    @Test
    public void validateMutationOnCreationSerializedModelNestsSerializedCustomType()
            throws JSONException, AmplifyException {
        buildSerializedModelNestsSerializedCustomTypeSchemas();
        SchemaRegistry schemaRegistry = SchemaRegistry.instance();

        Map<String, Object> phoneSerializedData = new HashMap<>();
        phoneSerializedData.put("country", "+1");
        phoneSerializedData.put("area", "415");
        phoneSerializedData.put("number", "6666666");
        SerializedCustomType phone = SerializedCustomType.builder()
                .serializedData(phoneSerializedData)
                .customTypeSchema(schemaRegistry.getCustomTypeSchemaMap().get("Phone"))
                .build();

        Map<String, Object> bioSerializedData = new HashMap<>();
        bioSerializedData.put("email", "test@testing.com");
        bioSerializedData.put("phone", phone);
        SerializedCustomType bio = SerializedCustomType.builder()
                .serializedData(bioSerializedData)
                .customTypeSchema(schemaRegistry.getCustomTypeSchemaMap().get("Bio"))
                .build();

        Map<String, Object> addressSerializedData1 = new HashMap<>();
        addressSerializedData1.put("line1", "222 Somewhere far");
        addressSerializedData1.put("line2", "apt 3");
        addressSerializedData1.put("city", "SFO");
        addressSerializedData1.put("state", "CA");
        addressSerializedData1.put("postalCode", "94105");
        SerializedCustomType address1 = SerializedCustomType.builder()
                .serializedData(addressSerializedData1)
                .customTypeSchema(schemaRegistry.getCustomTypeSchemaMap().get("Address"))
                .build();

        Map<String, Object> addressSerializedData2 = new HashMap<>();
        addressSerializedData2.put("line1", "333 Somewhere close");
        addressSerializedData2.put("line2", null);
        addressSerializedData2.put("city", "SEA");
        addressSerializedData2.put("state", "WA");
        addressSerializedData2.put("postalCode", "00000");
        SerializedCustomType address2 = SerializedCustomType.builder()
                .serializedData(addressSerializedData2)
                .customTypeSchema(schemaRegistry.getCustomTypeSchemaMap().get("Address"))
                .build();

        ArrayList<SerializedCustomType> addressesList = new ArrayList<>();
        addressesList.add(address1);
        addressesList.add(address2);

        Map<String, Object> personSerializedData = new HashMap<>();
        personSerializedData.put("id", "123456");
        personSerializedData.put("name", "Tester Testing");
        personSerializedData.put("bio", bio);
        personSerializedData.put("mailingAddresses", addressesList);

        SerializedModel person = SerializedModel.builder()
                .modelSchema(schemaRegistry.getModelSchemaForModelClass("Person"))
                .serializedData(personSerializedData)
                .build();

        String actual = AppSyncRequestFactory.buildCreationRequest(
                schemaRegistry.getModelSchemaForModelClass("Person"), person, DEFAULT_STRATEGY)
                .getContent();

        JSONAssert.assertEquals(
                Resources.readAsString("create-nested-serialized-model-custom-type.txt"),
                actual,
                true
        );

        clearSerializedModelNestsSerializedCustomTypeSchemas();
    }

    /**
     * Validates creation of a serialized model nests serialized custom types.
     *
     * @throws JSONException from JSONAssert.assertEquals JSON parsing error
     * @throws AmplifyException from ModelSchema.fromModelClass to convert model to schema
     */
    @Test
    public void validateMutationOnUpdateSerializedModelNestsSerializedCustomTypeWithOnlyChangedFields()
            throws JSONException, AmplifyException {
        buildSerializedModelNestsSerializedCustomTypeSchemas();
        SchemaRegistry schemaRegistry = SchemaRegistry.instance();

        Map<String, Object> phoneSerializedData = new HashMap<>();
        phoneSerializedData.put("country", "+1");
        phoneSerializedData.put("area", "415");
        phoneSerializedData.put("number", "6666666");
        SerializedCustomType phone = SerializedCustomType.builder()
                .serializedData(phoneSerializedData)
                .customTypeSchema(schemaRegistry.getCustomTypeSchemaMap().get("Phone"))
                .build();

        Map<String, Object> bioSerializedData = new HashMap<>();
        bioSerializedData.put("email", "test@testing.com");
        bioSerializedData.put("phone", phone);
        SerializedCustomType bio = SerializedCustomType.builder()
                .serializedData(bioSerializedData)
                .customTypeSchema(schemaRegistry.getCustomTypeSchemaMap().get("Bio"))
                .build();

        Map<String, Object> personSerializedData = new HashMap<>();
        personSerializedData.put("id", "123456");
        personSerializedData.put("bio", bio);

        SerializedModel person = SerializedModel.builder()
                .modelSchema(schemaRegistry.getModelSchemaForModelClass("Person"))
                .serializedData(personSerializedData)
                .build();

        String actual = AppSyncRequestFactory.buildUpdateRequest(
                schemaRegistry.getModelSchemaForModelClass("Person"),
                person,
                1,
                QueryPredicates.all(),
                DEFAULT_STRATEGY
        ).getContent();

        JSONAssert.assertEquals(
                Resources.readAsString("update-nested-serialized-model-custom-type.txt"),
                actual,
                true
        );

        clearSerializedModelNestsSerializedCustomTypeSchemas();
    }

    private Parent buildTestParentModel() {
        Address address = Address.builder()
                .street("555 Five Fiver")
                .street2("township")
                .city(City.BO)
                .phonenumber(
                        Phonenumber.builder()
                                .code(232)
                                .carrier(54)
                                .number(11111111)
                                .build()
                )
                .country("Sierra Leone")
                .build();
        Child child1 = Child.builder()
                .name("SAM")
                .address(address)
                .build();
        Child child2 = Child.builder()
                .name("MAS")
                .address(address)
                .build();
        return Parent.builder()
                .name("Jane Doe")
                .address(address)
                .children(Arrays.asList(child1, child2))
                .id("426f8e8d-ea0f-4839-a73f-6a2a38565ba1")
                .build();
    }

    private void buildSerializedModelNestsSerializedCustomTypeSchemas() {
        SchemaRegistry schemaRegistry = SchemaRegistry.instance();

        CustomTypeField addressLine1Field = CustomTypeField.builder()
                .name("line1")
                .isRequired(true)
                .targetType("String")
                .build();
        CustomTypeField addressLine2Field = CustomTypeField.builder()
                .name("line2")
                .isRequired(false)
                .targetType("String")
                .build();
        CustomTypeField addressCityField = CustomTypeField.builder()
                .name("city")
                .isRequired(true)
                .targetType("String")
                .build();
        CustomTypeField addressStateField = CustomTypeField.builder()
                .name("state")
                .isRequired(true)
                .targetType("String")
                .build();
        CustomTypeField addressPostalCodeField = CustomTypeField.builder()
                .name("postalCode")
                .isRequired(true)
                .targetType("String")
                .build();

        Map<String, CustomTypeField> addressFields = new HashMap<>();
        addressFields.put("line1", addressLine1Field);
        addressFields.put("line2", addressLine2Field);
        addressFields.put("city", addressCityField);
        addressFields.put("state", addressStateField);
        addressFields.put("postalCode", addressPostalCodeField);
        CustomTypeSchema addressSchema = CustomTypeSchema.builder()
                .name("Address")
                .pluralName("Addresses")
                .fields(addressFields)
                .build();

        CustomTypeField phoneCountryField = CustomTypeField.builder()
                .name("country")
                .isRequired(true)
                .targetType("String")
                .build();
        CustomTypeField phoneAreaField = CustomTypeField.builder()
                .name("area")
                .isRequired(true)
                .targetType("String")
                .build();
        CustomTypeField phoneNumberField = CustomTypeField.builder()
                .name("number")
                .isRequired(true)
                .targetType("String")
                .build();
        Map<String, CustomTypeField> phoneFields = new HashMap<>();
        phoneFields.put("country", phoneCountryField);
        phoneFields.put("area", phoneAreaField);
        phoneFields.put("number", phoneNumberField);
        CustomTypeSchema phoneSchema = CustomTypeSchema.builder()
                .name("Phone")
                .pluralName("Phones")
                .fields(phoneFields)
                .build();

        CustomTypeField bioEmailField = CustomTypeField.builder()
                .name("email")
                .isRequired(false)
                .targetType("String")
                .build();
        CustomTypeField bioPhoneField = CustomTypeField.builder()
                .name("phone")
                .isCustomType(true)
                .targetType("Phone")
                .build();
        Map<String, CustomTypeField> bioFields = new HashMap<>();
        bioFields.put("email", bioEmailField);
        bioFields.put("phone", bioPhoneField);
        CustomTypeSchema bioSchema = CustomTypeSchema.builder()
                .name("Bio")
                .fields(bioFields)
                .build();

        ModelField personBioField = ModelField.builder()
                .name("bio")
                .targetType("Bio")
                .isCustomType(true)
                .isRequired(true)
                .build();
        ModelField personNameField = ModelField.builder()
                .name("name")
                .targetType("String")
                .isRequired(true)
                .build();
        ModelField personMailingAddressesField = ModelField.builder()
                .name("mailingAddresses")
                .targetType("Address")
                .isCustomType(true)
                .isArray(true)
                .build();
        ModelField personIdField = ModelField.builder()
                .name("id")
                .targetType("String")
                .isRequired(true)
                .build();
        Map<String, ModelField> personFields = new HashMap<>();
        personFields.put("bio", personBioField);
        personFields.put("name", personNameField);
        personFields.put("mailingAddresses", personMailingAddressesField);
        personFields.put("id", personIdField);
        ModelSchema personSchema = ModelSchema.builder()
                .name("Person")
                .pluralName("People")
                .fields(personFields)
                .modelClass(SerializedModel.class)
                .build();

        schemaRegistry.register("Address", addressSchema);
        schemaRegistry.register("Phone", phoneSchema);
        schemaRegistry.register("Bio", bioSchema);
        schemaRegistry.register("Person", personSchema);
    }

    private void clearSerializedModelNestsSerializedCustomTypeSchemas() {
        SchemaRegistry schemaRegistry = SchemaRegistry.instance();
        schemaRegistry.clear();
    }

    /**
     * Verify that the owner field is removed if the value is null.
     * @throws AmplifyException On failure to build schema
     */
    @Test
    public void ownerFieldIsRemovedIfNull() throws AmplifyException {
        // Expect
        Map<String, Object> expected = new HashMap<>();
        expected.put("id", "111");
        expected.put("description", "Mop the floor");
        expected.put("_version", 1);

        // Act
        Todo todo = new Todo("111", "Mop the floor", null);
        ModelSchema schema = ModelSchema.fromModelClass(Todo.class);
        @SuppressWarnings("unchecked")
        Map<String, Object> actual = (Map<String, Object>)
            AppSyncRequestFactory.buildUpdateRequest(schema, todo, 1, QueryPredicates.all(), DEFAULT_STRATEGY)
                .getVariables()
                .get("input");

        // Assert
        assertEquals(expected, actual);
    }

    /**
     * Verify that the owner field is NOT removed if the value is set.
     * @throws AmplifyException On failure to build schema
     */
    @Test
    public void ownerFieldIsNotRemovedIfSet() throws AmplifyException {
        // Expect
        Map<String, Object> expected = new HashMap<>();
        expected.put("id", "111");
        expected.put("description", "Mop the floor");
        expected.put("owner", "johndoe");

        // Act
        Todo todo = new Todo("111", "Mop the floor", "johndoe");
        ModelSchema schema = ModelSchema.fromModelClass(Todo.class);
        @SuppressWarnings("unchecked")
        Map<String, Object> actual = (Map<String, Object>)
            AppSyncRequestFactory.buildCreationRequest(schema, todo, DEFAULT_STRATEGY)
                .getVariables()
                .get("input");

        // Assert
        assertEquals(expected, actual);
    }

    @ModelConfig(authRules = { @AuthRule(allow = AuthStrategy.OWNER) })
    static final class Todo implements Model {
        @com.amplifyframework.core.model.annotations.ModelField(targetType = "ID", isRequired = true)
        private final String id;

        @com.amplifyframework.core.model.annotations.ModelField(isRequired = true)
        private final String description;

        @com.amplifyframework.core.model.annotations.ModelField
        private final String owner;

        @SuppressWarnings("ParameterName") // checkstyle wants variable names to be >2 chars, but id is only 2.
        Todo(String id, String description, String owner) {
            this.id = id;
            this.description = description;
            this.owner = owner;
        }

        @NonNull
        @Override
        public String resolveIdentifier() {
            return "111";
        }
    }
}
