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

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.amplifyframework.AmplifyException;
import com.amplifyframework.api.graphql.GraphQLResponse;
import com.amplifyframework.core.ResultListener;
import com.amplifyframework.core.StreamListener;
import com.amplifyframework.core.async.Cancelable;
import com.amplifyframework.core.model.Model;
import com.amplifyframework.datastore.network.AppSyncApi;
import com.amplifyframework.datastore.network.ModelWithMetadata;
import com.amplifyframework.testmodels.commentsblog.Blog;
import com.amplifyframework.testmodels.commentsblog.BlogOwner;
import com.amplifyframework.testmodels.commentsblog.Post;
import com.amplifyframework.testmodels.commentsblog.PostStatus;
import com.amplifyframework.testutils.EmptyConsumer;
import com.amplifyframework.testutils.LatchedAction;
import com.amplifyframework.testutils.LatchedResponseConsumer;

import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;

import java.util.Date;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

/**
 * Tests the DataStore API Interface.
 */
@Ignore("Multiple tests can't subscribe in a single process right now.")
@SuppressWarnings("magicnumber")
public class AppSyncApiInstrumentationTest {
    private static AppSyncApi api;

    /**
     * Configure Amplify for API tests, if it has not been configured, yet.
     * @throws AmplifyException From Amplify configuration
     */
    @BeforeClass
    public static void onceBeforeTests() throws AmplifyException {
        TestConfiguration.configureIfNotConfigured();
        api = AppSyncApi.instance();
    }

    /**
     * Tests the operations in AppSyncApi.
     */
    @Test
    @SuppressWarnings("MethodLength")
    public void testAllOperations() {
        Long startTime = new Date().getTime();

        // Create simple model with no relationship
        BlogOwner owner = BlogOwner.builder()
            .name("David")
            .build();
        ModelWithMetadata<BlogOwner> blogOwnerCreateResult = create(owner);

        assertEquals(owner, blogOwnerCreateResult.getModel());
        assertEquals(new Integer(1), blogOwnerCreateResult.getSyncMetadata().getVersion());
        // TODO: BE AWARE THAT THE DELETED PROPERTY RETURNS NULL INSTEAD OF FALSE
        assertNull(blogOwnerCreateResult.getSyncMetadata().isDeleted());
        assertEquals(owner.getId(), blogOwnerCreateResult.getSyncMetadata().getId());

        // Subscribe to Blog creations
        Subscription<Blog> subscription = Subscription.onCreate(Blog.class);

        // Now, actually create a Blog
        Blog blog = Blog.builder()
            .name("Create test")
            .owner(owner)
            .build();
        ModelWithMetadata<Blog> blogCreateResult = create(blog);

        // Currently cannot do BlogOwner.justId because it will assign the id to the name field.
        // This is being fixed
        assertEquals(blog.getId(), blogCreateResult.getModel().getId());
        assertEquals(blog.getName(), blogCreateResult.getModel().getName());
        assertEquals(blog.getOwner().getId(), blogCreateResult.getModel().getOwner().getId());
        assertEquals(new Integer(1), blogCreateResult.getSyncMetadata().getVersion());
        assertNull(blogCreateResult.getSyncMetadata().isDeleted());
        assertTrue(blogCreateResult.getSyncMetadata().getLastChangedAt() > startTime);
        assertEquals(blog.getId(), blogCreateResult.getSyncMetadata().getId());

        // Validate that subscription picked up the mutation
        // & End the subscription since we're done with.
        assertEquals(blogCreateResult, subscription.awaitNextItem());
        subscription.cancel();
        subscription.awaitSubscriptionCompletion();

        // Create Posts which Blog hasMany of
        Post post1 = Post.builder()
            .title("Post 1")
            .status(PostStatus.ACTIVE)
            .rating(4)
            .blog(blog)
            .build();
        Post post2 = Post.builder()
            .title("Post 2")
            .status(PostStatus.INACTIVE)
            .rating(-1)
            .blog(blog)
            .build();
        Post post1ModelResult = create(post1).getModel();
        Post post2ModelResult = create(post2).getModel();

        // Results only have blog ID so strip out other information from the original post blog
        assertEquals(
            post1.copyOfBuilder()
                .blog(Blog.justId(blog.getId()))
                .build(),
            post1ModelResult
        );
        assertEquals(
            post2.copyOfBuilder()
                .blog(Blog.justId(blog.getId()))
                .build(),
            post2ModelResult
        );

        // Update model
        Blog updatedBlog = blog.copyOfBuilder()
            .name("Updated blog")
            .build();
        Long updateBlogStartTime = new Date().getTime();

        ModelWithMetadata<Blog> blogUpdateResult = update(updatedBlog, 1);

        assertEquals(updatedBlog.getName(), blogUpdateResult.getModel().getName());
        assertEquals(updatedBlog.getOwner().getId(), blogUpdateResult.getModel().getOwner().getId());
        assertEquals(updatedBlog.getId(), blogUpdateResult.getModel().getId());
        assertEquals(2, blogUpdateResult.getModel().getPosts().size());
        assertEquals(new Integer(2), blogUpdateResult.getSyncMetadata().getVersion());
        assertNull(blogUpdateResult.getSyncMetadata().isDeleted());
        assertTrue(blogUpdateResult.getSyncMetadata().getLastChangedAt() > updateBlogStartTime);

        // Delete one of the posts
        ModelWithMetadata<Post> post1DeleteResult = delete(Post.class, post1.getId(), 1);
        assertEquals(
            post1.copyOfBuilder()
                .blog(Blog.justId(blog.getId()))
                .build(),
            post1DeleteResult.getModel()
        );
        assertTrue(post1DeleteResult.getSyncMetadata().isDeleted());

        // Try to delete a post with a bad version number
        List<GraphQLResponse.Error> post2DeleteErrors = deleteExpectingErrors(Post.class, post2.getId(), 0);
        assertEquals("Conflict resolver rejects mutation.", post2DeleteErrors.get(0).getMessage());

        // Run sync on Blogs
        // TODO: This is currently a pretty worthless test - mainly for setting a debug point and manually inspecting
        // When you call sync with a null lastSync it gives only one entry per object (the latest state)
        Iterable<ModelWithMetadata<Blog>> blogSyncResult = sync(Blog.class, null);
        assertTrue(blogSyncResult.iterator().hasNext());

        // Run sync on Posts
        // TODO: This is currently a pretty worthless test - mainly for setting a debug point and manually inspecting
        // When you call sync with a lastSyncTime it gives you one entry per version of that object which was created
        // since that time.
        Iterable<ModelWithMetadata<Post>> postSyncResult = sync(Post.class, startTime);
        assertTrue(postSyncResult.iterator().hasNext());
    }

    /**
     * Create a model via the App Sync API, and return the App Sync API's
     * understood version of that model, along with server's metadata for the model.
     * @param model Model to create in remote App Sync API
     * @param <T> Type of model being created
     * @return Endpoint's version of the model, along with metadata about the model
     */
    @NonNull
    private <T extends Model> ModelWithMetadata<T> create(@NonNull T model) {
        LatchedResponseConsumer<ModelWithMetadata<T>> createdItemConsumer =
            LatchedResponseConsumer.instance();
        ResultListener<GraphQLResponse<ModelWithMetadata<T>>, DataStoreException> listener =
            ResultListener.instance(createdItemConsumer, EmptyConsumer.of(DataStoreException.class));
        api.create(model, listener);
        return createdItemConsumer.awaitResponseData();
    }

    /**
     * Updates an existing item in the App Sync API, whose remote version is the expected value.
     * @param model Updated model, to persist remotely
     * @param version Current version of the model that we (the client) know about
     * @param <T> The type of model being updated
     * @return Server's version of the model after update, along with new metadata
     */
    @SuppressWarnings("SameParameterValue") // Keep details in the actual test.
    @NonNull
    private <T extends Model> ModelWithMetadata<T> update(@NonNull T model, int version) {
        LatchedResponseConsumer<ModelWithMetadata<T>> updatedItemConsumer =
            LatchedResponseConsumer.instance();
        ResultListener<GraphQLResponse<ModelWithMetadata<T>>, DataStoreException> listener =
            ResultListener.instance(updatedItemConsumer, EmptyConsumer.of(DataStoreException.class));
        api.update(model, version, listener);
        return updatedItemConsumer.awaitResponseData();
    }

    /**
     * Deletes an instance of a model.
     * @param clazz The class of model being deleted
     * @param modelId The ID of the model instance to delete
     * @param version The version of the model being deleted as understood by client
     * @param <T> Type of model being deleted
     * @return A record of the item that was deleted from endpoint, along with metadata about the deletion
     */
    @SuppressWarnings("SameParameterValue") // Reads better with details in one place
    private <T extends Model> ModelWithMetadata<T> delete(
            @NonNull Class<T> clazz, String modelId, int version) {
        LatchedResponseConsumer<ModelWithMetadata<T>> deleteResultConsumer =
            LatchedResponseConsumer.instance();
        ResultListener<GraphQLResponse<ModelWithMetadata<T>>, DataStoreException> listener =
            ResultListener.instance(deleteResultConsumer, EmptyConsumer.of(DataStoreException.class));
        api.delete(clazz, modelId, version, listener);
        return deleteResultConsumer.awaitResponseData();
    }

    /**
     * Try to delete an item, but expect it to error.
     * Return the errors that were contained in the GraphQLResponse returned from endpoint.
     * @param clazz Class of item for which a delete is attempted
     * @param modelId ID of item for which delete is attempted
     * @param version Version of item for which deleted is attempted
     * @param <T> Type of item for which delete is attempted
     * @return List of GraphQLResponse.Error which explain why delete failed
     */
    @SuppressWarnings("SameParameterValue") // It'll read better if we keep details in the call line
    private <T extends Model> List<GraphQLResponse.Error> deleteExpectingErrors(
            @NonNull Class<T> clazz, String modelId, int version) {
        LatchedResponseConsumer<ModelWithMetadata<T>> deleteResultConsumer =
            LatchedResponseConsumer.instance();
        ResultListener<GraphQLResponse<ModelWithMetadata<T>>, DataStoreException> listener =
            ResultListener.instance(deleteResultConsumer, EmptyConsumer.of(DataStoreException.class));
        api.delete(clazz, modelId, version, listener);
        return deleteResultConsumer.awaitErrorsInNextResponse();
    }

    /**
     * Sync models of a given class, that have been updated since the provided last sync time.
     * @param clazz Class of models being sync'd
     * @param lastSyncTime Last time a sync occurred
     * @param <T> Type of models
     * @return An iterable collection of models with metadata describing models state on remote endpoint
     */
    private <T extends Model> Iterable<ModelWithMetadata<T>> sync(
            @NonNull Class<T> clazz, @Nullable Long lastSyncTime) {
        LatchedResponseConsumer<Iterable<ModelWithMetadata<T>>> syncConsumer =
            LatchedResponseConsumer.instance();
        ResultListener<GraphQLResponse<Iterable<ModelWithMetadata<T>>>, DataStoreException> syncListener =
            ResultListener.instance(syncConsumer, EmptyConsumer.of(DataStoreException.class));
        api.sync(clazz, lastSyncTime, syncListener);
        return syncConsumer.awaitResponseData();
    }

    static final class Subscription<T extends Model> {
        private final Cancelable cancelable;
        private final LatchedResponseConsumer<ModelWithMetadata<T>> responseConsumer;
        private final LatchedAction completionAction;

        private Subscription(
                Cancelable cancelable,
                LatchedResponseConsumer<ModelWithMetadata<T>> responseConsumer,
                LatchedAction completionAction) {
            this.cancelable = cancelable;
            this.responseConsumer = responseConsumer;
            this.completionAction = completionAction;
        }

        /**
         * Being a new subscription to model creations.
         * @param <T> Type of model being monitored
         * @param clazz Class of model being monitored
         * @return A subscription for the model class
         */
        @SuppressWarnings("SameParameterValue") // Keep details in the @Test method
        @NonNull
        static <T extends Model> Subscription<T> onCreate(Class<T> clazz) {
            LatchedResponseConsumer<ModelWithMetadata<T>> itemConsumer = LatchedResponseConsumer.instance();
            LatchedAction completionAction = LatchedAction.instance();
            StreamListener<GraphQLResponse<ModelWithMetadata<T>>, DataStoreException> listener =
                StreamListener.instance(itemConsumer, EmptyConsumer.of(DataStoreException.class), completionAction);
            Cancelable cancelable = api.onCreate(clazz, listener);
            return new Subscription<>(cancelable, itemConsumer, completionAction);
        }

        /**
         * Cancel the subscription.
         */
        void cancel() {
            cancelable.cancel();
        }

        /**
         * Await the next item that arrives on the subscription.
         * @return Next item on subscription
         */
        ModelWithMetadata<T> awaitNextItem() {
            return responseConsumer.awaitResponseData();
        }

        /**
         * Wait for the subscription to complete.
         */
        void awaitSubscriptionCompletion() {
            completionAction.awaitCall();
        }
    }
}
