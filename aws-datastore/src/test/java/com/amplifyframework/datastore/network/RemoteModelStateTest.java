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

package com.amplifyframework.datastore.network;

import com.amplifyframework.api.graphql.GraphQLResponse;
import com.amplifyframework.core.ResultListener;
import com.amplifyframework.core.model.Model;
import com.amplifyframework.core.model.ModelProvider;
import com.amplifyframework.testmodels.commentsblog.BlogOwner;
import com.amplifyframework.testmodels.commentsblog.Post;
import com.amplifyframework.testmodels.commentsblog.PostStatus;
import com.amplifyframework.testutils.RandomString;

import org.junit.Before;
import org.junit.Test;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import io.reactivex.observers.TestObserver;

import static org.mockito.Mockito.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Tests the {@link RemoteModelState}.
 */
@SuppressWarnings("checkstyle:MagicNumber") // Arranged data picked arbitrarily
public final class RemoteModelStateTest {
    private static final ModelWithMetadata<BlogOwner> BLOGGER_JAMESON =
        new ModelWithMetadata<>(
            BlogOwner.builder()
                .name("Jameson")
                .id("d5b44350-b8e9-4deb-94c2-7fe986d6a0e1")
                .build(),
            new ModelMetadata("d5b44350-b8e9-4deb-94c2-7fe986d6a0e1", null, 3, 223344L)
        );
    private static final ModelWithMetadata<BlogOwner> BLOGGER_ISLA =
        new ModelWithMetadata<>(
            BlogOwner.builder()
            .name("Isla")
                .id("c0601168-2931-4bc0-bf13-5963cd31f828")
                .build(),
            new ModelMetadata("c0601168-2931-4bc0-bf13-5963cd31f828", null, 11, 998877L)
        );
    private static final ModelWithMetadata<Post> DRUM_POST =
        new ModelWithMetadata<>(
            Post.builder()
                .title("Inactive Post About Drums")
                .status(PostStatus.INACTIVE)
                .rating(3)
                .id("83ceb757-c8c8-4b6a-bee0-a43afb53a73a")
                .build(),
            new ModelMetadata("83ceb757-c8c8-4b6a-bee0-a43afb53a73a", null, 5, 123123L)
        );
    private static final ModelWithMetadata<Post> GUITAR_POST =
        new ModelWithMetadata<>(
            Post.builder()
                .title("Active Post About Guitars")
                .status(PostStatus.ACTIVE)
                .rating(9)
                .id("28a02356-c560-4b63-b629-efc4b75b63c2")
                .build(),
            new ModelMetadata("28a02356-c560-4b63-b629-efc4b75b63c2", Boolean.TRUE, 12, 333222L)
        );

    private String apiName;
    private AppSyncEndpoint endpoint;
    private ModelProvider modelProvider;
    private RemoteModelState remoteModelState;

    /**
     * Mock out our dependencies. We'll want to pretend we have some different models
     * being provided by the system. And, we want to mock away the AppSyncEndpoint, so we
     * can test our logic in isolation without going out to the network / depending
     * on some backend to have a certain configuration / state.
     */
    @Before
    public void setup() {
        apiName = RandomString.string();
        endpoint = mock(AppSyncEndpoint.class);
        modelProvider = mock(ModelProvider.class);
        remoteModelState = new RemoteModelState(endpoint, modelProvider, () -> apiName);
    }

    /**
     * Observe the remote model state, via {@link RemoteModelState#observe()}.
     * Validate that the observed items are the ones that were provided by our
     * arranged {@link AppSyncEndpoint} mock.
     */
    @Test
    public void observeReceivesAllModelInstances() {
        // Arrange: ModelProvider deals with Post and BlogOwner types.
        provideModels(Post.class, BlogOwner.class);

        // Arrange: the AppSync endpoint will give us some MetaData for items
        // having these types.
        mockSuccessfulResponse(Post.class, DRUM_POST, GUITAR_POST);
        mockSuccessfulResponse(BlogOwner.class, BLOGGER_JAMESON, BLOGGER_ISLA);

        // Act: Observe the RemoteModelState via observe().
        TestObserver<ModelWithMetadata<? extends Model>> observer = TestObserver.create();
        remoteModelState.observe().subscribe(observer);
        observer.awaitTerminalEvent();
        observer.assertValueCount(4);

        // assertValueSet(..., varargs, ...) would be cleanest. But equals() is broken
        // on the generated models right now. So, instead, use our own assertEquals() for right now...
        assertEquals(
            Arrays.asList(BLOGGER_JAMESON, BLOGGER_ISLA, DRUM_POST, GUITAR_POST),
            observer.values()
        );
        observer.dispose();
    }

    /**
     * Configures the {@link ModelProvider} mock to return the named models.
     * @param models Classes of models to return from ModelProvider mock
     */
    @SafeVarargs
    @SuppressWarnings("varargs") // arguments
    private final void provideModels(Class<? extends Model>... models) {
        when(modelProvider.models()).thenReturn(new HashSet<>(Arrays.asList(models)));
    }

    /**
     * Mocks a response from the {@link AppSyncEndpoint}.
     * @param clazz Class of models for which to respond
     * @param responseItems The items that should be included in the mocked response, for the model class
     * @param <T> Type of models for which a response is mocked
     */
    @SuppressWarnings("varargs") // matchers, arguments
    @SafeVarargs
    private final <T extends Model> void mockSuccessfulResponse(
        Class<T> clazz, ModelWithMetadata<T>... responseItems) {
        doAnswer(invocation -> {
            // Get a handle to the listener that is passed into the sync() method
            // ResultListener is the fourth and final param (@0, @1, @2, @3).
            final int argumentPositionForResultListener = 3;
            final ResultListener<GraphQLResponse<Iterable<ModelWithMetadata<T>>>> listener =
                invocation.getArgument(argumentPositionForResultListener);

            // Call its onResult(), and pass the mocked items inside of a GraphQLResponse wrapper
            final Iterable<ModelWithMetadata<T>> data = new HashSet<>(Arrays.asList(responseItems));
            listener.onResult(new GraphQLResponse<>(data, Collections.emptyList()));

            // Return a NoOp cancelable via the sync() method's return.
            return new NoOpCancelable();
        }).when(endpoint).sync(
            eq(apiName),
            eq(clazz),
            any(),
            any()
        );
    }

    private void assertEquals(
            Collection<ModelWithMetadata<? extends Model>> expected,
            Collection<ModelWithMetadata<? extends Model>> actual) {

        final Set<String> actualModelIds = new HashSet<>();
        for (final ModelWithMetadata<? extends Model> modelWithMetadata : actual) {
            actualModelIds.add(modelWithMetadata.getModel().getId());
        }
        final Set<String> expectedModelIds = new HashSet<>();
        for (final ModelWithMetadata<? extends Model> modelWithMetadata : expected) {
            expectedModelIds.add(modelWithMetadata.getModel().getId());
        }
        org.junit.Assert.assertEquals(expectedModelIds, actualModelIds);
    }
}
