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

import android.content.Context;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RawRes;

import com.amplifyframework.AmplifyException;
import com.amplifyframework.api.ApiCategory;
import com.amplifyframework.api.aws.AWSApiPlugin;
import com.amplifyframework.api.graphql.GraphQLResponse;
import com.amplifyframework.core.AmplifyConfiguration;
import com.amplifyframework.core.Consumer;
import com.amplifyframework.core.async.Cancelable;
import com.amplifyframework.core.category.CategoryType;
import com.amplifyframework.core.model.Model;
import com.amplifyframework.core.model.query.predicate.QueryPredicate;
import com.amplifyframework.core.model.temporal.Temporal;
import com.amplifyframework.datastore.appsync.AppSync;
import com.amplifyframework.datastore.appsync.AppSyncClient;
import com.amplifyframework.datastore.appsync.ModelWithMetadata;
import com.amplifyframework.testmodels.commentsblog.Blog;
import com.amplifyframework.testmodels.commentsblog.BlogOwner;
import com.amplifyframework.testmodels.commentsblog.Post;
import com.amplifyframework.testmodels.commentsblog.PostStatus;
import com.amplifyframework.testutils.Await;
import com.amplifyframework.testutils.Resources;

import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;

import java.util.Date;
import java.util.List;
import java.util.concurrent.TimeUnit;

import io.reactivex.Observable;
import io.reactivex.observers.TestObserver;

import static androidx.test.core.app.ApplicationProvider.getApplicationContext;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

/**
 * Tests the DataStore API Interface.
 */
@Ignore("Subscription issues")
public final class AppSyncClientInstrumentationTest {
    private static AppSync api;

    /**
     * Configure Amplify for API tests, if it has not been configured, yet.
     * @throws AmplifyException From Amplify configuration
     */
    @BeforeClass
    public static void onceBeforeTests() throws AmplifyException {
        Context context = getApplicationContext();
        @RawRes int resourceId = Resources.getRawResourceId(context, "amplifyconfiguration");

        ApiCategory asyncDelegate = new ApiCategory();
        asyncDelegate.addPlugin(new AWSApiPlugin());
        asyncDelegate.configure(AmplifyConfiguration.fromConfigFile(context, resourceId)
            .forCategoryType(CategoryType.API), context);
        asyncDelegate.initialize(context);

        api = AppSyncClient.via(asyncDelegate);
    }

    /**
     * Tests the operations in AppSyncClient.
     * @throws DataStoreException If any call to AppSync endpoint fails to return a response
     */
    @Test
    @SuppressWarnings("MethodLength")
    public void testAllOperations() throws DataStoreException {
        Long startTimeSeconds = TimeUnit.MILLISECONDS.toSeconds(new Date().getTime());

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
        Observable<GraphQLResponse<ModelWithMetadata<Blog>>> blogCreations = onCreate(Blog.class);

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
        Temporal.Timestamp createdBlogLastChangedAt = blogCreateResult.getSyncMetadata().getLastChangedAt();
        assertNotNull(createdBlogLastChangedAt);
        assertTrue(createdBlogLastChangedAt.getSecondsSinceEpoch() > startTimeSeconds);
        assertEquals(blog.getId(), blogCreateResult.getSyncMetadata().getId());

        // Validate that subscription picked up the mutation
        // & End the subscription since we're done with.
        TestObserver<ModelWithMetadata<Blog>> blogCreationSubscriber = TestObserver.create();
        blogCreations
            .map(GraphQLResponse::getData)
            .subscribe(blogCreationSubscriber);
        blogCreationSubscriber.assertValue(blogCreateResult);

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
        Long updateBlogStartTimeSeconds = TimeUnit.MILLISECONDS.toSeconds(new Date().getTime());

        ModelWithMetadata<Blog> blogUpdateResult = update(updatedBlog, 1);

        assertEquals(updatedBlog.getName(), blogUpdateResult.getModel().getName());
        assertEquals(updatedBlog.getOwner().getId(), blogUpdateResult.getModel().getOwner().getId());
        assertEquals(updatedBlog.getId(), blogUpdateResult.getModel().getId());
        assertEquals(2, blogUpdateResult.getModel().getPosts().size());
        assertEquals(new Integer(2), blogUpdateResult.getSyncMetadata().getVersion());
        assertNull(blogUpdateResult.getSyncMetadata().isDeleted());
        Temporal.Timestamp updatedBlogLastChangedAt = blogUpdateResult.getSyncMetadata().getLastChangedAt();
        assertNotNull(updatedBlogLastChangedAt);
        assertTrue(updatedBlogLastChangedAt.getSecondsSinceEpoch() > updateBlogStartTimeSeconds);

        // Delete one of the posts
        ModelWithMetadata<Post> post1DeleteResult = delete(Post.class, post1.getId(), 1);
        assertEquals(
            post1.copyOfBuilder()
                .blog(Blog.justId(blog.getId()))
                .build(),
            post1DeleteResult.getModel()
        );
        Boolean isDeleted = post1DeleteResult.getSyncMetadata().isDeleted();
        assertEquals(Boolean.TRUE, isDeleted);

        // Try to delete a post with a bad version number
        List<GraphQLResponse.Error> post2DeleteErrors = deleteExpectingResponseErrors(Post.class, post2.getId(), 0);
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
        Iterable<ModelWithMetadata<Post>> postSyncResult = sync(Post.class, startTimeSeconds);
        assertTrue(postSyncResult.iterator().hasNext());
    }

    /**
     * Create a model via the App Sync API, and return the App Sync API's
     * understood version of that model, along with server's metadata for the model.
     * @param model Model to create in remote App Sync API
     * @param <T> Type of model being created
     * @return Endpoint's version of the model, along with metadata about the model
     * @throws DataStoreException If API create call fails to render any response from AppSync endpoint
     */
    @NonNull
    private <T extends Model> ModelWithMetadata<T> create(@NonNull T model) throws DataStoreException {
        return awaitResponseData((onResult, onError) ->
            api.create(model, onResult, onError));
    }

    /**
     * Updates an existing item in the App Sync API, whose remote version is the expected value.
     * @param model Updated model, to persist remotely
     * @param version Current version of the model that we (the client) know about
     * @param <T> The type of model being updated
     * @return Server's version of the model after update, along with new metadata
     * @throws DataStoreException If API update call fails to render any response from AppSync endpoint
     */
    @NonNull
    private <T extends Model> ModelWithMetadata<T> update(@NonNull T model, int version)
        throws DataStoreException {
        return update(model, version, null);
    }

    @NonNull
    private <T extends Model> ModelWithMetadata<T> update(@NonNull T model, int version, QueryPredicate predicate)
            throws DataStoreException {
        return awaitResponseData((onResult, onError) ->
            api.update(model, version, predicate, onResult, onError));
    }

    /**
     * Deletes an instance of a model.
     * @param clazz The class of model being deleted
     * @param modelId The ID of the model instance to delete
     * @param version The version of the model being deleted as understood by client
     * @param <T> Type of model being deleted
     * @return Model hat was deleted from endpoint, coupled with metadata about the deletion
     * @throws DataStoreException If API delete call fails to render any response from AppSync endpoint
     */
    @NonNull
    private <T extends Model> ModelWithMetadata<T> delete(
        @NonNull Class<T> clazz, String modelId, int version)
        throws DataStoreException {
        return delete(clazz, modelId, version, null);
    }

    @NonNull
    private <T extends Model> ModelWithMetadata<T> delete(
            @NonNull Class<T> clazz, String modelId, int version, QueryPredicate predicate)
            throws DataStoreException {
        return awaitResponseData((onResult, onError) ->
            api.delete(clazz, modelId, version, predicate, onResult, onError));
    }

    /**
     * Try to delete an item, but expect it to error.
     * Return the errors that were contained in the GraphQLResponse returned from endpoint.
     * @param clazz Class of item for which a delete is attempted
     * @param modelId ID of item for which delete is attempted
     * @param version Version of item for which deleted is attempted
     * @param <T> Type of item for which delete is attempted
     * @return List of GraphQLResponse.Error which explain why delete failed
     * @throws DataStoreException If API delete call fails to render any response from AppSync endpoint
     */
    private <T extends Model> List<GraphQLResponse.Error> deleteExpectingResponseErrors(
            @NonNull Class<T> clazz, String modelId, int version)
            throws DataStoreException {
        return awaitResponseErrors(
            (Consumer<GraphQLResponse<ModelWithMetadata<T>>> onResult, Consumer<DataStoreException> onError) ->
                api.delete(clazz, modelId, version, onResult, onError)
        );
    }

    /**
     * Sync models of a given class, that have been updated since the provided last sync time.
     * @param clazz Class of models being sync'd
     * @param lastSyncTime Last time a sync occurred
     * @param <T> Type of models
     * @return An iterable collection of models with metadata describing models state on remote endpoint
     * @throws DataStoreException If API sync fails to render and response from AppSync endpoint
     */
    private <T extends Model> Iterable<ModelWithMetadata<T>> sync(
            @NonNull Class<T> clazz, @Nullable Long lastSyncTime) throws DataStoreException {
        return awaitResponseData((onResult, onError) ->
            api.sync(clazz, lastSyncTime, onResult, onError));
    }

    private <T> T awaitResponseData(
            Await.ResultErrorEmitter<GraphQLResponse<T>, DataStoreException> resultErrorEmitter)
            throws DataStoreException {
        final GraphQLResponse<T> response = Await.result(resultErrorEmitter);
        if (response.hasErrors()) {
            String firstErrorMessage = response.getErrors().get(0).getMessage();
            throw new DataStoreException("Response contained errors: " + firstErrorMessage, "Check request.");
        } else if (!response.hasData()) {
            throw new DataStoreException("Response had no data.", "Check request.");
        }
        return response.getData();
    }

    @SuppressWarnings("UnusedReturnValue")
    private <T> List<GraphQLResponse.Error> awaitResponseErrors(
            Await.ResultErrorEmitter<GraphQLResponse<T>, DataStoreException> resultErrorEmitter)
            throws DataStoreException {
        final GraphQLResponse<T> response = Await.result(resultErrorEmitter);
        if (!response.hasErrors()) {
            throw new DataStoreException("Response did not contain any errors.", "Was it supposed to?");
        }
        return response.getErrors();
    }

    @SuppressWarnings({"CodeBlock2Expr", "SameParameterValue"})
    private <T extends Model> Observable<GraphQLResponse<ModelWithMetadata<T>>> onCreate(
            @NonNull Class<T> clazz) {
        return Observable.create(emitter -> {
            Await.result((onSubscriptionStarted, ignored) -> {
                Cancelable cancelable = api.onCreate(
                    clazz,
                    onSubscriptionStarted::accept,
                    emitter::onNext,
                    emitter::onError,
                    emitter::onComplete
                );
                emitter.setDisposable(AmplifyDisposables.fromCancelable(cancelable));
            });
        });
    }
}
