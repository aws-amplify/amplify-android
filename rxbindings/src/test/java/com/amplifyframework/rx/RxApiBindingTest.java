/*
 * Copyright 2020 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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

package com.amplifyframework.rx;

import android.content.Context;

import com.amplifyframework.AmplifyException;
import com.amplifyframework.api.ApiCategory;
import com.amplifyframework.api.ApiCategoryConfiguration;
import com.amplifyframework.api.ApiException;
import com.amplifyframework.api.ApiPlugin;
import com.amplifyframework.api.aws.ModelMutation;
import com.amplifyframework.api.aws.ModelQuery;
import com.amplifyframework.api.aws.ModelSubscription;
import com.amplifyframework.api.graphql.GraphQLResponse;
import com.amplifyframework.api.graphql.SubscriptionType;
import com.amplifyframework.api.rest.RestOptions;
import com.amplifyframework.api.rest.RestResponse;
import com.amplifyframework.core.Action;
import com.amplifyframework.core.Consumer;
import com.amplifyframework.core.model.Model;
import com.amplifyframework.testutils.random.RandomModel;
import com.amplifyframework.testutils.random.RandomString;

import org.junit.Before;
import org.junit.Test;

import java.util.Collections;

import io.reactivex.observers.TestObserver;

import static com.amplifyframework.rx.Matchers.anyAction;
import static com.amplifyframework.rx.Matchers.anyConsumer;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Tests the {@link RxApiBinding}.
 */
public final class RxApiBindingTest {
    private ApiPlugin<?> delegate;
    private RxApiCategoryBehavior rxApi;

    /**
     * To test the binding, we construct a category that has been configured
     * with a mock plugin. The binding delegates to the category.
     * @throws AmplifyException On failure to add plugin or configure category
     */
    @Before
    public void createBindingInFrontOfMockPlugin() throws AmplifyException {
        // Mock plugin on which we will simulate API responses/failures
        this.delegate = mock(ApiPlugin.class);
        when(delegate.getPluginKey()).thenReturn(RandomString.string());

        // Build a category, add the mock plugin, configure and init the category.
        final ApiCategory apiCategory = new ApiCategory();
        apiCategory.addPlugin(delegate);
        apiCategory.configure(new ApiCategoryConfiguration(), mock(Context.class));
        apiCategory.initialize(mock(Context.class));

        // Provide that category as a backing to our binding.
        this.rxApi = new RxApiBinding(apiCategory);
    }

    /**
     * When the API behavior emits results for a query, so too should the Rx binding.
     */
    @Test
    public void queryEmitsResults() {
        GraphQLResponse<Iterable<Model>> response =
            new GraphQLResponse<>(Collections.singleton(RandomModel.model()), Collections.emptyList());
        doAnswer(invocation -> {
            final int positionOfResultConsumer = 1; // 0 = clazz, 1 = onResult, 2 = onFailure
            Consumer<GraphQLResponse<Iterable<Model>>> onResponse = invocation.getArgument(positionOfResultConsumer);
            onResponse.accept(response);
            return null;
        }).when(delegate)
            .query(eq(ModelQuery.list(Model.class)), anyConsumer(), anyConsumer());

        // Act: query the Api via the Rx Binding
        TestObserver<GraphQLResponse<Iterable<Model>>> observer = rxApi.query(
                ModelQuery.list(Model.class)).test();

        // Assert: got back a the same response as from category behavior
        observer.awaitTerminalEvent();
        observer.assertValue(response);

        verify(delegate)
            .query(eq(ModelQuery.list(Model.class)), anyConsumer(), anyConsumer());
    }

    /**
     * When the API behavior emits a failure for a query, so too should the Rx binding.
     */
    @Test
    public void queryEmitsFailure() {
        // Arrange: category behavior emits a failure
        ApiException expectedFailure = new ApiException("Expected", "Failure");
        doAnswer(invocation -> {
            final int positionOfOnFailure = 2; // 0 = clazz, 1 = onResponse, 2 = onFailure
            Consumer<ApiException> onFailure = invocation.getArgument(positionOfOnFailure);
            onFailure.accept(expectedFailure);
            return null;
        }).when(delegate)
            .query(eq(ModelQuery.list(Model.class)), anyConsumer(), anyConsumer());

        // Act: access query() method via Rx binding
        TestObserver<GraphQLResponse<Iterable<Model>>> observer = rxApi.query(
                ModelQuery.list(Model.class)).test();

        // Assert: failure bubbles up to Rx
        observer.awaitTerminalEvent();
        observer.assertError(expectedFailure);

        verify(delegate)
            .query(eq(ModelQuery.list(Model.class)), anyConsumer(), anyConsumer());
    }

    /**
     * When the API behavior emits a result for a mutation, so too should the Rx binding.
     */
    @Test
    public void mutateEmitsResult() {
        // Arrange: category behaviour will yield a response
        Model model = RandomModel.model();
        GraphQLResponse<Model> response = new GraphQLResponse<>(model, Collections.emptyList());
        doAnswer(invocation -> {
            final int positionOfResultConsumer = 2;
            Consumer<GraphQLResponse<Model>> onResponse = invocation.getArgument(positionOfResultConsumer);
            onResponse.accept(response);
            return null;
        }).when(delegate)
            .mutate(eq(ModelMutation.delete(model)), anyConsumer(), anyConsumer());

        // Act: mutation via the Rx binding
        TestObserver<GraphQLResponse<Model>> observer = rxApi.mutate(ModelMutation.delete(model)).test();

        // Assert: response is propagated via Rx
        observer.awaitTerminalEvent();
        observer.assertValue(response);

        verify(delegate)
            .mutate(eq(ModelMutation.delete(model)), anyConsumer(), anyConsumer());
    }

    /**
     * When the API behavior emits a failure for a mutation, so too should the Rx binding.
     */
    @Test
    public void mutateEmitsFailure() {
        // Arrange category behavior to fail
        Model model = RandomModel.model();
        ApiException expectedFailure = new ApiException("Expected", "Failure");
        doAnswer(invocation -> {
            final int positionOfFailureConsumer = 3;
            Consumer<ApiException> onFailure = invocation.getArgument(positionOfFailureConsumer);
            onFailure.accept(expectedFailure);
            return null;
        }).when(delegate)
            .mutate(eq(ModelMutation.delete(model)), anyConsumer(), anyConsumer());

        // Act: access it via binding
        TestObserver<GraphQLResponse<Model>> observer = rxApi.mutate(
                ModelMutation.delete(model)
        ).test();

        // Assert: failure is propagated
        observer.awaitTerminalEvent();
        observer.assertError(expectedFailure);

        verify(delegate)
            .mutate(eq(ModelMutation.delete(model)), anyConsumer(), anyConsumer());
    }

    /**
     * When the API subscribe operation emits values and then completes, the Rx
     * binding should follow suit.
     */
    @Test
    public void subscribeStartsEmitsValuesAndCompletes() {
        // Arrange a category behavior which emits an expected sequence of callback events
        String token = RandomString.string();
        Model model = RandomModel.model();
        GraphQLResponse<Model> response = new GraphQLResponse<>(model, Collections.emptyList());
        doAnswer(invocation -> {
            final int onStartPosition = 2;
            final int onNextPosition = 3;
            final int onCompletePosition = 5;
            Consumer<String> onStart = invocation.getArgument(onStartPosition);
            Consumer<GraphQLResponse<Model>> onNext = invocation.getArgument(onNextPosition);
            Action onComplete = invocation.getArgument(onCompletePosition);
            onStart.accept(token);
            onNext.accept(response);
            onComplete.call();
            return null;
        }).when(delegate).subscribe(
            eq(ModelSubscription.of(Model.class, SubscriptionType.ON_CREATE)),
            anyConsumer(),
            anyConsumer(),
            anyConsumer(),
            anyAction()
        );

        // Act: subscribe via binding
        TestObserver<GraphQLResponse<Model>> observer =
            rxApi.subscribe(ModelSubscription.of(Model.class, SubscriptionType.ON_CREATE)).test();

        observer.awaitTerminalEvent();
        observer.assertValue(response);
        observer.assertNoErrors();
    }

    /**
     * When the subscribe API behavior starts and then immediately fails,
     * the Rx binding should emit that same failure.
     */
    @Test
    public void subscribeStartsAndFails() {
        // Arrange a category behavior which starts and then fails
        ApiException expectedFailure = new ApiException("Expected", "Failure");
        String token = RandomString.string();
        doAnswer(invocation -> {
            final int onStartPosition = 2;
            final int onFailurePosition = 4;
            Consumer<String> onStart = invocation.getArgument(onStartPosition);
            Consumer<ApiException> onFailure = invocation.getArgument(onFailurePosition);
            onStart.accept(token);
            onFailure.accept(expectedFailure);
            return null;
        }).when(delegate).subscribe(
            eq(ModelSubscription.of(Model.class, SubscriptionType.ON_CREATE)),
            anyConsumer(),
            anyConsumer(),
            anyConsumer(),
            anyAction()
        );

        // Act: subscribe via binding
        TestObserver<GraphQLResponse<Model>> observer =
            rxApi.subscribe(ModelSubscription.of(Model.class, SubscriptionType.ON_CREATE)).test();

        observer.awaitTerminalEvent();
        observer.assertNoValues();
        observer.assertError(expectedFailure);
    }

    /**
     * When the REST GET behavior emits a failure, the Rx binding should
     * emit that same failure as well.
     */
    @Test
    public void httpGetEmitsFailure() {
        RestOptions options = RestOptions.builder()
            .addPath("/api/v1/movies")
            .build();
        ApiException expectedFailure = new ApiException("Expected", "Failure");
        doAnswer(invocation -> {
            final int positionOfFailureConsumer = 2;
            Consumer<ApiException> onFailure = invocation.getArgument(positionOfFailureConsumer);
            onFailure.accept(expectedFailure);
            return null;
        }).when(delegate)
            .get(eq(options), anyConsumer(), anyConsumer());

        TestObserver<RestResponse> observer = rxApi.get(options).test();

        observer.awaitTerminalEvent();
        observer.assertError(expectedFailure);

        verify(delegate)
            .get(eq(options), anyConsumer(), anyConsumer());
    }

    /**
     * When REST GET behavior emits a result, the Rx binding
     * should emit it, too.
     */
    @Test
    public void httpGetEmitsResult() {
        RestOptions options = RestOptions.builder()
            .addPath("/api/v1/movies")
            .build();
        byte[] data = "{\"movies\":[\"Spider Man\"]}".getBytes(); // JSONObject would need to bring in Robolectric
        final int httpOkStatus = 200;
        RestResponse response = new RestResponse(httpOkStatus, data);
        doAnswer(invocation -> {
            final int positionOfResponseConsumer = 1;
            Consumer<RestResponse> onResponse = invocation.getArgument(positionOfResponseConsumer);
            onResponse.accept(response);
            return null;
        }).when(delegate)
            .get(eq(options), anyConsumer(), anyConsumer());

        TestObserver<RestResponse> observer = rxApi.get(options).test();

        observer.awaitTerminalEvent();
        observer.assertValue(response);

        verify(delegate)
            .get(eq(options), anyConsumer(), anyConsumer());
    }

    /**
     * When the REST POST behavior emits a failure, the Rx binding
     * should do the same.
     */
    @Test
    public void httpPostEmitsFailure() {
        byte[] body = RandomString.string().getBytes();
        RestOptions options = RestOptions.builder()
            .addBody(body)
            .addPath("/some/path")
            .build();
        ApiException expectedFailure = new ApiException("Expected", "Failure");
        doAnswer(invocation -> {
            final int positionOfFailureConsumer = 2;
            Consumer<ApiException> onFailure = invocation.getArgument(positionOfFailureConsumer);
            onFailure.accept(expectedFailure);
            return null;
        }).when(delegate)
            .post(eq(options), anyConsumer(), anyConsumer());

        // Act: post via the Rx binding
        TestObserver<RestResponse> observer = rxApi.post(options).test();

        // Assert: failure bubbles through
        observer.awaitTerminalEvent();
        observer.assertError(expectedFailure);

        verify(delegate)
            .post(eq(options), anyConsumer(), anyConsumer());
    }

    /**
     * When the REST POST behavior emits a result, the Rx binding
     * should do the same.
     */
    @Test
    public void httpPostEmitsResult() {
        // Arrange response from category behavior
        byte[] body = RandomString.string().getBytes();
        RestOptions options = RestOptions.builder()
            .addPath("/api/v1/your_name")
            .addBody(body)
            .build();
        final int httpOkStatus = 200;
        RestResponse response = new RestResponse(httpOkStatus, body); // Re-use body
        doAnswer(invocation -> {
            final int positionOfResponseConsumer = 1; // 0 = options, 1 = onResponse, 2 = onFailure
            Consumer<RestResponse> onResponse = invocation.getArgument(positionOfResponseConsumer);
            onResponse.accept(response);
            return null;
        }).when(delegate)
            .post(eq(options), anyConsumer(), anyConsumer());

        // Act: post via Rx binding
        TestObserver<RestResponse> observer = rxApi.post(options).test();

        // Asset: it worked!
        observer.awaitTerminalEvent();
        observer.assertValue(response);

        verify(delegate)
            .post(eq(options), anyConsumer(), anyConsumer());
    }
}
