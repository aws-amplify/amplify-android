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

package com.amplifyframework.datastore;

import com.amplifyframework.AmplifyException;
import com.amplifyframework.api.graphql.GraphQLResponse;
import com.amplifyframework.core.Amplify;
import com.amplifyframework.core.async.Cancelable;
import com.amplifyframework.datastore.network.AppSyncApi;
import com.amplifyframework.datastore.network.ModelWithMetadata;
import com.amplifyframework.testmodels.commentsblog.Blog;
import com.amplifyframework.testmodels.commentsblog.BlogOwner;
import com.amplifyframework.testmodels.commentsblog.Post;
import com.amplifyframework.testmodels.commentsblog.PostStatus;
import com.amplifyframework.testutils.LatchedResponseStreamListener;
import com.amplifyframework.testutils.LatchedSingleResponseListener;

import org.junit.BeforeClass;
import org.junit.Test;

import java.util.Date;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

/**
 * Tests the DataStore API Interface.
 */
@SuppressWarnings("magicnumber")
public class AppSyncApiTest {
    private static AppSyncApi api;

    /**
     * Configure Amplify for API tests, if it has not been configured, yet.
     * @throws AmplifyException From Amplify configuration
     */
    @BeforeClass
    public static void onceBeforeTests() throws AmplifyException {
        TestConfiguration.configureIfNotConfigured();
        api = new AppSyncApi(Amplify.API);
    }

    /**
     * Tests the operations in AppSyncApi.
     * @throws Throwable for timeouts or invalid responses
     */
    @Test
    @SuppressWarnings("MethodLength")
    public void testAllOperations() throws Throwable {

        Long startTime = new Date().getTime();

        // Create simple model with no relationship
        LatchedSingleResponseListener<ModelWithMetadata<BlogOwner>> blogOwnerCreateListener =
                new LatchedSingleResponseListener<>();

        BlogOwner owner = BlogOwner.builder().name("David").build();

        api.create(
            owner,
                blogOwnerCreateListener
        );

        ModelWithMetadata<BlogOwner> blogOwnerCreateResult = blogOwnerCreateListener.awaitSuccessResponse();
        assertEquals(owner, blogOwnerCreateResult.getModel());
        assertEquals(new Integer(1), blogOwnerCreateResult.getSyncMetadata().getVersion());
        // TODO: BE AWARE THAT THE DELETED PROPERTY RETURNS NULL INSTEAD OF FALSE
        assertNull(blogOwnerCreateResult.getSyncMetadata().isDeleted());
        assertTrue(new Date().getTime() > blogOwnerCreateResult.getSyncMetadata().getLastChangedAt());
        assertEquals(owner.getId(), blogOwnerCreateResult.getSyncMetadata().getId());

        // Create model with BelongsTo relationship and a subscriber listening for the creation
        LatchedResponseStreamListener<ModelWithMetadata<Blog>> blogCreateSubscriptionListener =
                new LatchedResponseStreamListener<>(1);

        Cancelable subscription = api.onCreate(
                Blog.class,
                blogCreateSubscriptionListener
        );

        LatchedSingleResponseListener<ModelWithMetadata<Blog>> blogCreateListener =
                new LatchedSingleResponseListener<>();

        Blog blog = Blog.builder().name("Create test").owner(owner).build();

        api.create(
            blog,
            blogCreateListener
        );

        // Currently cannot do BlogOwner.justId because it will assign the id to the name field.
        // This is being fixed
        ModelWithMetadata<Blog> blogCreateResult = blogCreateListener.awaitSuccessResponse();
        assertEquals(blog.getId(), blogCreateResult.getModel().getId());
        assertEquals(blog.getName(), blogCreateResult.getModel().getName());
        assertEquals(blog.getOwner().getId(), blogCreateResult.getModel().getOwner().getId());
        assertEquals(new Integer(1), blogCreateResult.getSyncMetadata().getVersion());
        assertNull(blogCreateResult.getSyncMetadata().isDeleted());
        assertTrue(new Date().getTime() > blogCreateResult.getSyncMetadata().getLastChangedAt());
        assertTrue(blogCreateResult.getSyncMetadata().getLastChangedAt() > startTime);
        assertEquals(blog.getId(), blogCreateResult.getSyncMetadata().getId());

        // Validate that subscription picked up the mutation
        List<ModelWithMetadata<Blog>> blogCreateSubscriptionResult =
                blogCreateSubscriptionListener.awaitSuccessfulResponses();
        assertEquals(1, blogCreateSubscriptionResult.size());
        assertEquals(blogCreateResult, blogCreateSubscriptionResult.get(0));
        subscription.cancel();

        // Create Posts which Blog hasMany of
        LatchedSingleResponseListener<ModelWithMetadata<Post>> post1CreateListener =
                new LatchedSingleResponseListener<>();
        LatchedSingleResponseListener<ModelWithMetadata<Post>> post2CreateListener =
                new LatchedSingleResponseListener<>();

        Post post1 = Post.builder().title("Post 1").status(PostStatus.ACTIVE).rating(4).blog(blog).build();
        Post post2 = Post.builder().title("Post 2").status(PostStatus.INACTIVE).rating(-1).blog(blog).build();

        api.create(
                post1,
                post1CreateListener
        );

        api.create(
                post2,
                post2CreateListener
        );

        Post post1ModelResult = post1CreateListener.awaitSuccessResponse().getModel();
        Post post2ModelResult = post2CreateListener.awaitSuccessResponse().getModel();

        // Results only have blog ID so strip out other information from the original post blog
        assertEquals(post1.copyOfBuilder().blog(Blog.justId(blog.getId())).build(), post1ModelResult);
        assertEquals(post2.copyOfBuilder().blog(Blog.justId(blog.getId())).build(), post2ModelResult);

        // Update model
        LatchedSingleResponseListener<ModelWithMetadata<Blog>> blogUpdateListener =
                new LatchedSingleResponseListener<>();
        Blog updatedBlog = blog.copyOfBuilder().name("Updated blog").build();
        Long updateBlogStartTime = new Date().getTime();

        api.update(
                updatedBlog,
                1,
                blogUpdateListener
        );

        ModelWithMetadata<Blog> blogUpdateResult = blogUpdateListener.awaitSuccessResponse();
        assertEquals(updatedBlog.getName(), blogUpdateResult.getModel().getName());
        assertEquals(updatedBlog.getOwner().getId(), blogUpdateResult.getModel().getOwner().getId());
        assertEquals(updatedBlog.getId(), blogUpdateResult.getModel().getId());
        assertEquals(2, blogUpdateResult.getModel().getPosts().size());
        assertEquals(new Integer(2), blogUpdateResult.getSyncMetadata().getVersion());
        assertNull(blogUpdateResult.getSyncMetadata().isDeleted());
        assertTrue(new Date().getTime() > blogUpdateResult.getSyncMetadata().getLastChangedAt());
        assertTrue(blogUpdateResult.getSyncMetadata().getLastChangedAt() > updateBlogStartTime);

        // Delete one of the posts
        LatchedSingleResponseListener<ModelWithMetadata<Post>> post1DeleteListener =
                new LatchedSingleResponseListener<>();

        api.delete(
                Post.class,
                post1.getId(),
                1,
                post1DeleteListener
        );

        ModelWithMetadata<Post> post1DeleteResult = post1DeleteListener.awaitSuccessResponse();
        assertEquals(post1.copyOfBuilder().blog(Blog.justId(blog.getId())).build(), post1DeleteResult.getModel());
        assertTrue(post1DeleteResult.getSyncMetadata().isDeleted());

        // Try to delete a post with a bad version number
        LatchedSingleResponseListener<ModelWithMetadata<Post>> post2DeleteListener =
                new LatchedSingleResponseListener<>();

        api.delete(
                Post.class,
                post2.getId(),
                0,
                post2DeleteListener
        );

        List<GraphQLResponse.Error> post2DeleteErrors = post2DeleteListener.awaitErrors();
        assertEquals("Conflict resolver rejects mutation.", post2DeleteErrors.get(0).getMessage());

        // Run sync on Blogs
        // TODO: This is currently a pretty worthless test - mainly for setting a debug point and manually inspecting
        LatchedSingleResponseListener<Iterable<ModelWithMetadata<Blog>>> blogSyncListener =
                new LatchedSingleResponseListener<>();

        // When you call sync with a null lastSync it gives only one entry per object (the latest state)
        api.sync(
                Blog.class,
                null,
                blogSyncListener
        );

        Iterable<ModelWithMetadata<Blog>> blogSyncResult = blogSyncListener.awaitSuccessResponse();
        assertTrue(blogSyncResult.iterator().hasNext());

        // Run sync on Posts
        // TODO: This is currently a pretty worthless test - mainly for setting a debug point and manually inspecting
        LatchedSingleResponseListener<Iterable<ModelWithMetadata<Post>>> postSyncListener =
                new LatchedSingleResponseListener<>();

        // When you call sync with a lastSyncTime it gives you one entry per version of that object which was created
        // since that time.
        api.sync(
                Post.class,
                startTime,
                postSyncListener
        );

        Iterable<ModelWithMetadata<Post>> postSyncResult = postSyncListener.awaitSuccessResponse();
        assertTrue(postSyncResult.iterator().hasNext());
    }
}
