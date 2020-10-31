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

import com.amplifyframework.api.graphql.GraphQLRequest;
import com.amplifyframework.api.graphql.SubscriptionType;
import com.amplifyframework.datastore.DataStoreException;
import com.amplifyframework.testmodels.commentsblog.Blog;
import com.amplifyframework.testmodels.commentsblog.BlogOwner;
import com.amplifyframework.testmodels.commentsblog.Comment;
import com.amplifyframework.testmodels.commentsblog.Post;
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

import java.util.Arrays;
import java.util.Map;

import static org.junit.Assert.assertEquals;

/**
 * Tests the {@link AppSyncRequestFactory}.
 */
@RunWith(RobolectricTestRunner.class) // Adds Android library to make TextUtils.join available for tests.
public final class AppSyncRequestFactoryTest {

    /**
     * Validates the construction of a base-sync query document.
     * @throws DataStoreException On failure to interrogate fields in Blog.class
     * @throws JSONException from JSONAssert.assertEquals
     */
    @Test
    public void validateRequestGenerationForBaseSync() throws DataStoreException, JSONException {
        JSONAssert.assertEquals(
            Resources.readAsString("base-sync-request-document-for-blog-owner.txt"),
            AppSyncRequestFactory.buildSyncRequest(BlogOwner.class, null, null).getContent(),
            true
        );
    }

    /**
     * Validates the construction of a base-sync query document for models with custom types.
     * @throws DataStoreException On failure to interrogate fields in Parent.class
     * @throws JSONException from JSONAssert.assertEquals
     */
    @Test
    public void validateCustomTypeRequestGenerationForBaseSync() throws DataStoreException, JSONException {
        JSONAssert.assertEquals(
                Resources.readAsString("base-sync-request-document-for-parent.txt"),
                AppSyncRequestFactory.buildSyncRequest(Parent.class, null, null).getContent(),
                true
        );
    }

    /**
     * Validates the construction of a delta-sync query document.
     * @throws DataStoreException On failure to interrogate fields in Blog.class.
     * @throws JSONException from JSONAssert.assertEquals
     */
    @Test
    public void validateRequestGenerationForDeltaSync() throws DataStoreException, JSONException {
        JSONAssert.assertEquals(Resources.readAsString("delta-sync-request-document-for-post.txt"),
                AppSyncRequestFactory.buildSyncRequest(Post.class, 123123123L, null).getContent(),
                true);
    }

    /**
     * Validates that the nextToken parameter is correctly generate for a Sync query.
     * @throws DataStoreException On failure to interrogate the BlogOwner.class.
     * @throws JSONException from JSONAssert.assertEquals.
     */
    @Test
    public void validateRequestGenerationForPagination() throws DataStoreException, JSONException {
        Integer limit = 1000;
        final GraphQLRequest<Iterable<Post>> request =
                AppSyncRequestFactory.buildSyncRequest(BlogOwner.class, null, limit);
        JSONAssert.assertEquals(Resources.readAsString("base-sync-request-paginating-blog-owners.txt"),
                request.getContent(),
                true);
    }

    /**
     * Checks that we're getting the expected output for a mutation with predicate.
     * @throws DataStoreException If the output does not match.
     * @throws JSONException from JSONAssert.assertEquals.
     */
    @Test
    public void validateUpdateWithPredicateGeneration() throws DataStoreException, JSONException {
        String blogOwnerId = "926d7ee8-4ea5-40c0-8e62-3fb80b2a2edd";
        BlogOwner owner = BlogOwner.builder().name("John Doe").id(blogOwnerId).build();
        JSONAssert.assertEquals(
            Resources.readAsString("update-blog-owner-with-predicate.txt"),
                AppSyncRequestFactory.buildUpdateRequest(owner, 42, BlogOwner.WEA.contains("ther")).getContent(),
            true
        );
    }

    /**
     * Checks that we're getting the expected output for a mutation with predicate.
     * @throws DataStoreException If the output does not match.
     * @throws JSONException from JSONAssert.assertEquals.
     */
    @Test
    public void validateUpdateNestedCustomTypeWithPredicateGeneration() throws DataStoreException, JSONException {
        JSONAssert.assertEquals(
                Resources.readAsString("update-parent-with-predicate.txt"),
                AppSyncRequestFactory.buildUpdateRequest(
                        buildTestParentModel(),
                        42,
                        Parent.NAME.contains("Jane Doe")
                ).getContent(),
                true
        );
    }

    /**
     * Checks that we're getting the expected output for a mutation with predicate.
     * @throws DataStoreException If the output does not match.
     * @throws JSONException from JSONAssert.assertEquals.
     */
    @Test
    public void validateDeleteWithPredicateGeneration() throws DataStoreException, JSONException {
        JSONAssert.assertEquals(
            Resources.readAsString("delete-person-with-predicate.txt"),
            AppSyncRequestFactory.buildDeletionRequest(Person.class, "123", 456, Person.AGE.gt(40)).getContent(),
            true
        );
    }

    /**
     * Checks that the predicate expression matches the expected value.
     * @throws DataStoreException If the output does not match.
     */
    @Test
    public void validatePredicateGeneration() throws DataStoreException {
        Map<String, Object> predicate =
            AppSyncRequestFactory.parsePredicate(BlogOwner.NAME.eq("Test Dummy"));
        assertEquals(
            "{name={eq=Test Dummy}}",
            predicate.toString()
        );

        AppSyncRequestFactory.parsePredicate(
            Blog.NAME.beginsWith("A day in the life of a...").and(Blog.OWNER.eq("DUMMY_OWNER_ID"))
        );
    }

    /**
     * Validates that a GraphQL request document can be created, to get onCreate
     * subscription notifications for a Blog.class.
     * @throws DataStoreException On failure to interrogate the Blog.class.
     * @throws JSONException from JSONAssert.assertEquals.
     */
    @Test
    public void validateSubscriptionGenerationOnCreateBlog() throws DataStoreException, JSONException {
        JSONAssert.assertEquals(
            Resources.readAsString("on-create-request-for-blog.txt"),
            AppSyncRequestFactory.buildSubscriptionRequest(Blog.class, SubscriptionType.ON_CREATE).getContent(),
            true
        );
    }

    /**
     * Validates that a GraphQL request document can be created, to get onCreate for nested custom type
     * subscription notifications for a Parent.class.
     * @throws DataStoreException On failure to interrogate the Blog.class.
     * @throws JSONException from JSONAssert.assertEquals.
     */
    @Test
    public void validateSubscriptionGenerationOnCreateForNestedCustomType() throws DataStoreException, JSONException {
        JSONAssert.assertEquals(
                Resources.readAsString("on-create-request-for-parent.txt"),
                AppSyncRequestFactory.buildSubscriptionRequest(Parent.class, SubscriptionType.ON_CREATE).getContent(),
                true
        );
    }

    /**
     * Validates generation of a GraphQL document which requests a subscription for updates
     * to the Blog.class.
     * @throws DataStoreException On failure to interrogate fields in Blog.class.
     * @throws JSONException from JSONAssert.assertEquals.
     */
    @Test
    public void validateSubscriptionGenerationOnUpdatePost() throws DataStoreException, JSONException {
        JSONAssert.assertEquals(
            Resources.readAsString("on-update-request-for-post.txt"),
            AppSyncRequestFactory.buildSubscriptionRequest(Post.class, SubscriptionType.ON_UPDATE).getContent(),
            true
        );
    }

    /**
     * Validates generation of a GraphQL document which requests a subscription for deletes.
     * for the BlogOwner.class.
     * @throws DataStoreException On failure to interrogate the fields in BlogOwner.class.
     * @throws JSONException from JSONAssert.assertEquals.
     */
    @Test
    public void validateSubscriptionGenerationOnDeleteBlogOwner() throws DataStoreException, JSONException {
        JSONAssert.assertEquals(
            Resources.readAsString("on-delete-request-for-blog-owner.txt"),
            AppSyncRequestFactory.buildSubscriptionRequest(BlogOwner.class, SubscriptionType.ON_DELETE).getContent(),
            true
        );
    }

    /**
     * Validates creation of a "create a model" request.
     * @throws DataStoreException On failure to interrogate the model fields.
     * @throws JSONException from JSONAssert.assertEquals.
     */
    @Test
    public void validateMutationGenerationOnCreateComment() throws DataStoreException, JSONException {
        Post post = Post.justId("9a4295d6-8225-495a-a531-beffc8b7ae7d");
        Comment comment = Comment.builder()
                .id("426f8e8d-ea0f-4839-a73f-6a2a38565ba1")
                .content("toast")
                .post(post)
                .build();
        JSONAssert.assertEquals(
            Resources.readAsString("create-comment-request.txt"),
            AppSyncRequestFactory.buildCreationRequest(comment).getContent(),
            true

        );
    }

    /**
     * Validates creation of a "create a model" request for nested custom type.
     * @throws DataStoreException On failure to interrogate the model fields.
     * @throws JSONException from JSONAssert.assertEquals.
     */
    @Test
    public void validateMutationGenerationOnCreateNestedCustomType() throws DataStoreException, JSONException {
        JSONAssert.assertEquals(
                Resources.readAsString("create-parent-request.txt"),
                AppSyncRequestFactory.buildCreationRequest(buildTestParentModel()).getContent(),
                true
        );
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
}
