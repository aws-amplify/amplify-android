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

import com.amplifyframework.api.graphql.SubscriptionType;
import com.amplifyframework.datastore.DataStoreException;
import com.amplifyframework.testmodels.commentsblog.Blog;
import com.amplifyframework.testmodels.commentsblog.BlogOwner;
import com.amplifyframework.testmodels.commentsblog.Comment;
import com.amplifyframework.testmodels.commentsblog.Post;
import com.amplifyframework.testutils.Resources;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

/**
 * Tests the {@link AppSyncRequestFactory}.
 */
public final class AppSyncRequestFactoryTest {

    /**
     * Validates the construction of a base-sync query document.
     * @throws DataStoreException On failure to interrogate fields in Blog.class
     */
    @Test
    public void validateRequestGenerationForBaseSync() throws DataStoreException {
        assertEquals(
            Resources.readAsString("base-sync-request-document-for-blog-owner.txt"),
            AppSyncRequestFactory.buildSyncDoc(BlogOwner.class, null, null)
        );
    }

    /**
     * Validates the construction of a delta-sync query document.
     * @throws DataStoreException On failure to interrogate fields in Blog.class.
     */
    @SuppressWarnings("checkstyle:MagicNumber") // lastSync time is an arbitrarily-chosen, stable value
    @Test
    public void validateRequestGenerationForDeltaSync() throws DataStoreException {
        assertEquals(
            Resources.readAsString("delta-sync-request-document-for-post.txt"),
            AppSyncRequestFactory.buildSyncDoc(Post.class, 123123123L, null)
        );
    }

    /**
     * Validates that the nextToken parameter is correctly generate for a Sync query.
     * @throws DataStoreException On failure to interrogate the BlogOwner.class.
     */
    @Test
    public void validateRequestGenerationForPagination() throws DataStoreException {
        final String nextToken = Resources.readAsString("base-sync-request-next-token-value.txt").trim();
        assertEquals(
            Resources.readAsString("base-sync-request-paginating-blog-owners.txt"),
            AppSyncRequestFactory.buildSyncDoc(BlogOwner.class, null, nextToken)
        );
    }

    /**
     * Validates that a GraphQL request document can be created, to get onCreate
     * subscription notifications for a Blog.class.
     * @throws DataStoreException On failure to interrogate the Blog.class.
     */
    @Test
    public void validateSubscriptionGenerationOnCreateBlog() throws DataStoreException {
        assertEquals(
            Resources.readAsString("on-create-request-for-blog.txt"),
            AppSyncRequestFactory.buildSubscriptionDoc(Blog.class, SubscriptionType.ON_CREATE)
        );
    }

    /**
     * Validates generation of a GraphQL document which requests a subscription for updates
     * to the Blog.class.
     * @throws DataStoreException On failure to interrogate fields in Blog.class.
     */
    @Test
    public void validateSubscriptionGenerationOnUpdatePost() throws DataStoreException {
        assertEquals(
            Resources.readAsString("on-update-request-for-post.txt"),
            AppSyncRequestFactory.buildSubscriptionDoc(Post.class, SubscriptionType.ON_UPDATE)
        );
    }

    /**
     * Validates generation of a GraphQL document which requests a subscription for updates
     * for the BlogOwner.class.
     * @throws DataStoreException On failure to interrogate the fields in BlogOwner.class.
     */
    @Test
    public void validateSubscriptionGenerationOnDeleteBlogOwner() throws DataStoreException {
        assertEquals(
            Resources.readAsString("on-delete-request-for-blog-owner.txt"),
            AppSyncRequestFactory.buildSubscriptionDoc(BlogOwner.class, SubscriptionType.ON_DELETE)
        );
    }

    /**
     * Validates creation of a "create a model" request.
     * @throws DataStoreException On failure to interrogate the model fields
     */
    @Test
    public void validateMutationGenerationOnCreateComment() throws DataStoreException {
        assertEquals(
            Resources.readAsString("create-comment-request.txt"),
            AppSyncRequestFactory.buildCreationDoc(Comment.class)
        );
    }
}
