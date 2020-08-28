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
import com.amplifyframework.api.graphql.GraphQLOperation;
import com.amplifyframework.api.graphql.GraphQLRequest;
import com.amplifyframework.api.graphql.GraphQLResponse;
import com.amplifyframework.api.graphql.SimpleGraphQLRequest;
import com.amplifyframework.api.rest.RestOptions;
import com.amplifyframework.api.rest.RestResponse;
import com.amplifyframework.core.Action;
import com.amplifyframework.core.Consumer;
import com.amplifyframework.core.model.Model;
import com.amplifyframework.rx.RxOperations.RxSubscriptionOperation.ConnectionState;
import com.amplifyframework.rx.RxOperations.RxSubscriptionOperation.ConnectionStateEvent;
import com.amplifyframework.testutils.random.RandomModel;
import com.amplifyframework.testutils.random.RandomString;

import org.junit.Before;
import org.junit.Test;

import java.util.Collections;
import java.util.concurrent.TimeUnit;

import io.reactivex.rxjava3.core.Completable;
import io.reactivex.rxjava3.observers.TestObserver;

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
    private static final long TIMEOUT_SECONDS = 2;

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
     * @throws InterruptedException If interrupted while test observer is awaiting terminal event
     */
    @Test
    public void queryEmitsResults() throws InterruptedException {
        GraphQLResponse<Iterable<Model>> response =
            new GraphQLResponse<>(Collections.singleton(RandomModel.model()), Collections.emptyList());
        GraphQLRequest<Iterable<Model>> listRequest = createMockListRequest(Model.class);
        doAnswer(invocation -> {
            final int positionOfResultConsumer = 1; // 0 = clazz, 1 = onResult, 2 = onFailure
            Consumer<GraphQLResponse<Iterable<Model>>> onResponse = invocation.getArgument(positionOfResultConsumer);
            onResponse.accept(response);
            return null;
        }).when(delegate)
            .query(eq(listRequest), anyConsumer(), anyConsumer());

        // Act: query the Api via the Rx Binding
        TestObserver<GraphQLResponse<Iterable<Model>>> observer = rxApi.query(listRequest).test();

        // Assert: got back a the same response as from category behavior
        observer.await(TIMEOUT_SECONDS, TimeUnit.SECONDS);
        observer.assertValue(response);

        verify(delegate)
            .query(eq(listRequest), anyConsumer(), anyConsumer());
    }

    /**
     * When the API behavior emits a failure for a query, so too should the Rx binding.
     * @throws InterruptedException If interrupted while test observer is awaiting terminal event
     */
    @Test
    public void queryEmitsFailure() throws InterruptedException {
        // Arrange: category behavior emits a failure
        ApiException expectedFailure = new ApiException("Expected", "Failure");
        GraphQLRequest<Iterable<Model>> listRequest = createMockListRequest(Model.class);
        doAnswer(invocation -> {
            final int positionOfOnFailure = 2; // 0 = clazz, 1 = onResponse, 2 = onFailure
            Consumer<ApiException> onFailure = invocation.getArgument(positionOfOnFailure);
            onFailure.accept(expectedFailure);
            return null;
        }).when(delegate)
            .query(eq(listRequest), anyConsumer(), anyConsumer());

        // Act: access query() method via Rx binding
        TestObserver<GraphQLResponse<Iterable<Model>>> observer = rxApi.query(listRequest).test();

        // Assert: failure bubbles up to Rx
        observer.await(TIMEOUT_SECONDS, TimeUnit.SECONDS);
        observer.assertError(expectedFailure);

        verify(delegate)
            .query(eq(listRequest), anyConsumer(), anyConsumer());
    }

    /**
     * When the API behavior emits a result for a mutation, so too should the Rx binding.
     * @throws InterruptedException If interrupted while test observer is awaiting terminal event
     */
    @Test
    public void mutateEmitsResult() throws InterruptedException {
        // Arrange: category behaviour will yield a response
        Model model = RandomModel.model();
        GraphQLResponse<Model> response = new GraphQLResponse<>(model, Collections.emptyList());
        GraphQLRequest<Model> deleteRequest = createMockMutationRequest(Model.class);
        doAnswer(invocation -> {
            final int positionOfResultConsumer = 1;
            Consumer<GraphQLResponse<Model>> onResponse = invocation.getArgument(positionOfResultConsumer);
            onResponse.accept(response);
            return null;
        }).when(delegate)
            .mutate(eq(deleteRequest), anyConsumer(), anyConsumer());

        // Act: mutation via the Rx binding
        TestObserver<GraphQLResponse<Model>> observer = rxApi.mutate(deleteRequest).test();

        // Assert: response is propagated via Rx
        observer.await(TIMEOUT_SECONDS, TimeUnit.SECONDS);
        observer.assertValue(response);

        verify(delegate)
            .mutate(eq(deleteRequest), anyConsumer(), anyConsumer());
    }

    /**
     * When the API behavior emits a failure for a mutation, so too should the Rx binding.
     * @throws InterruptedException If interrupted while test observer is awaiting terminal event
     */
    @Test
    public void mutateEmitsFailure() throws InterruptedException {
        // Arrange category behavior to fail
        ApiException expectedFailure = new ApiException("Expected", "Failure");
        GraphQLRequest<Model> deleteRequest = createMockMutationRequest(Model.class);
        doAnswer(invocation -> {
            final int positionOfFailureConsumer = 2;
            Consumer<ApiException> onFailure = invocation.getArgument(positionOfFailureConsumer);
            onFailure.accept(expectedFailure);
            return null;
        }).when(delegate)
            .mutate(eq(deleteRequest), anyConsumer(), anyConsumer());

        // Act: access it via binding
        TestObserver<GraphQLResponse<Model>> observer = rxApi.mutate(deleteRequest).test();

        // Assert: failure is propagated
        observer.await(TIMEOUT_SECONDS, TimeUnit.SECONDS);
        observer.assertError(expectedFailure);

        verify(delegate)
            .mutate(eq(deleteRequest), anyConsumer(), anyConsumer());
    }

    /**
     * When the API subscribe operation emits values and then completes, the Rx
     * binding should follow suit.
     * @throws InterruptedException If interrupted while test observer is awaiting terminal event
     */
    @Test
    public void subscribeStartsEmitsValuesAndCompletes() throws InterruptedException {
        // Arrange a category behavior which emits an expected sequence of callback events
        String token = RandomString.string();
        Model model = RandomModel.model();
        ConnectionStateEvent expectedConnectionStateEvent =
            new ConnectionStateEvent(ConnectionState.CONNECTED, token);
        GraphQLResponse<Model> response = new GraphQLResponse<>(model, Collections.emptyList());
        GraphQLRequest<Model> request = createMockSubscriptionRequest(Model.class);
        doAnswer(invocation -> {
            final int onStartPosition = 1;
            final int onNextPosition = 2;
            final int onCompletePosition = 4;
            Consumer<String> onStart = invocation.getArgument(onStartPosition);
            Consumer<GraphQLResponse<Model>> onNext = invocation.getArgument(onNextPosition);
            Action onComplete = invocation.getArgument(onCompletePosition);
            onStart.accept(token);
            onNext.accept(response);
            onComplete.call();
            return null;
        }).when(delegate).subscribe(
            eq(request),
            anyConsumer(),
            anyConsumer(),
            anyConsumer(),
            anyAction()
        );

        // Act: subscribe via binding
        RxOperations.RxSubscriptionOperation<GraphQLResponse<Model>> rxOperation = rxApi.subscribe(request);
        // Act: subscribe via binding
        TestObserver<GraphQLResponse<Model>> dataObserver = rxOperation.observeSubscriptionData().test();
        TestObserver<ConnectionStateEvent> startObserver = rxOperation.observeConnectionState().test();

        startObserver.await(TIMEOUT_SECONDS, TimeUnit.SECONDS);
        startObserver.assertValue(expectedConnectionStateEvent);
        startObserver.assertNoErrors();

        dataObserver.await(TIMEOUT_SECONDS, TimeUnit.SECONDS);
        dataObserver.assertValue(response);
        dataObserver.assertNoErrors();
    }

    /**
     * When the subscribe API behavior starts and then immediately fails,
     * the Rx binding should emit that same failure.
     * @throws InterruptedException If interrupted while test observer is awaiting terminal event
     */
    @Test
    public void subscribeStartsAndFails() throws InterruptedException {
        // Arrange a category behavior which starts and then fails
        ApiException expectedFailure = new ApiException("Expected", "Failure");
        String token = RandomString.string();
        ConnectionStateEvent expectedConnectionStateEvent =
            new ConnectionStateEvent(ConnectionState.CONNECTED, token);
        final GraphQLRequest<Model> request = createMockSubscriptionRequest(Model.class);
        doAnswer(invocation -> {
            final int onStartPosition = 1;
            final int onFailurePosition = 3;
            Consumer<String> onStart = invocation.getArgument(onStartPosition);
            Consumer<ApiException> onFailure = invocation.getArgument(onFailurePosition);
            onStart.accept(token);
            onFailure.accept(expectedFailure);
            return null;
        }).when(delegate).subscribe(
            eq(request),
            anyConsumer(),
            anyConsumer(),
            anyConsumer(),
            anyAction()
        );
        RxOperations.RxSubscriptionOperation<GraphQLResponse<Model>> rxOperation = rxApi.subscribe(request);
        // Act: subscribe via binding
        TestObserver<GraphQLResponse<Model>> dataObserver = rxOperation.observeSubscriptionData().test();
        TestObserver<ConnectionStateEvent> startObserver = rxOperation.observeConnectionState().test();

        startObserver.await(TIMEOUT_SECONDS, TimeUnit.SECONDS);
        startObserver.assertValue(expectedConnectionStateEvent);
        startObserver.assertNoErrors();

        dataObserver.await(TIMEOUT_SECONDS, TimeUnit.SECONDS);
        dataObserver.assertNoValues();
        dataObserver.assertError(expectedFailure);
    }

    /**
     * Verify that the subscription starts and is cancelled gracefully.
     * @throws InterruptedException Not expected.
     */
    @SuppressWarnings("rawtypes")
    @Test
    public void subscribeStartsAndGetsCancelled() throws InterruptedException {
        // Arrange a category behavior which emits an expected sequence of callback events
        String token = RandomString.string();
        GraphQLRequest<Model> request = createMockSubscriptionRequest(Model.class);
        ConnectionStateEvent expectedConnectionStateEvent =
            new ConnectionStateEvent(ConnectionState.CONNECTED, token);

        doAnswer(invocation -> {
            final int onStartPosition = 1;
            final int onCompletePosition = 4;
            Consumer<String> onStart = invocation.getArgument(onStartPosition);
            Action onComplete = invocation.getArgument(onCompletePosition);
            onStart.accept(token);

            GraphQLOperation mockApiOperation = mock(GraphQLOperation.class);
            doAnswer(apiCancelInvocation -> {
                onComplete.call();
                return null;
            }).when(mockApiOperation).cancel();
            return mockApiOperation;
        }).when(delegate).subscribe(
            eq(request),
            anyConsumer(),
            anyConsumer(),
            anyConsumer(),
            anyAction()
        );

        // Act: subscribe via binding
        RxOperations.RxSubscriptionOperation<GraphQLResponse<Model>> rxOperation = rxApi.subscribe(request);
        // Act: subscribe via binding
        TestObserver<GraphQLResponse<Model>> dataObserver = rxOperation.observeSubscriptionData().test();
        TestObserver<ConnectionStateEvent> startObserver = rxOperation.observeConnectionState().test();

        startObserver.await(TIMEOUT_SECONDS, TimeUnit.SECONDS);
        startObserver.assertValue(expectedConnectionStateEvent);
        startObserver.assertNoErrors();

        // Act: cancel the subscription
        Completable.timer(1, TimeUnit.SECONDS).andThen(Completable.fromAction(rxOperation::cancel)).subscribe();

        dataObserver.await(TIMEOUT_SECONDS, TimeUnit.SECONDS);
        dataObserver.assertNoValues();
        dataObserver.assertNoErrors();
        dataObserver.assertComplete();

        startObserver.assertComplete();
    }

    /**
     * When the REST GET behavior emits a failure, the Rx binding should
     * emit that same failure as well.
     * @throws InterruptedException If interrupted while test observer is awaiting terminal event
     */
    @Test
    public void httpGetEmitsFailure() throws InterruptedException {
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

        observer.await(TIMEOUT_SECONDS, TimeUnit.SECONDS);
        observer.assertError(expectedFailure);

        verify(delegate)
            .get(eq(options), anyConsumer(), anyConsumer());
    }

    /**
     * When REST GET behavior emits a result, the Rx binding
     * should emit it, too.
     * @throws InterruptedException If interrupted while test observer is awaiting terminal event
     */
    @Test
    public void httpGetEmitsResult() throws InterruptedException {
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

        observer.await(TIMEOUT_SECONDS, TimeUnit.SECONDS);
        observer.assertValue(response);

        verify(delegate)
            .get(eq(options), anyConsumer(), anyConsumer());
    }

    /**
     * When the REST POST behavior emits a failure, the Rx binding
     * should do the same.
     * @throws InterruptedException If interrupted while test observer is awaiting terminal event
     */
    @Test
    public void httpPostEmitsFailure() throws InterruptedException {
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
        observer.await(TIMEOUT_SECONDS, TimeUnit.SECONDS);
        observer.assertError(expectedFailure);

        verify(delegate)
            .post(eq(options), anyConsumer(), anyConsumer());
    }

    /**
     * When the REST POST behavior emits a result, the Rx binding
     * should do the same.
     * @throws InterruptedException If interrupted while test observer is awaiting terminal event
     */
    @Test
    public void httpPostEmitsResult() throws InterruptedException {
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
        observer.await(TIMEOUT_SECONDS, TimeUnit.SECONDS);
        observer.assertValue(response);

        verify(delegate)
            .post(eq(options), anyConsumer(), anyConsumer());
    }

    private static <T> GraphQLRequest<T> createMockMutationRequest(Class<T> responseType) {
        return new SimpleGraphQLRequest<>("", responseType, null);
    }

    private static <T> GraphQLRequest<Iterable<T>> createMockListRequest(Class<T> responseType) {
        return new SimpleGraphQLRequest<>("", responseType, null);
    }

    private static <T> GraphQLRequest<T> createMockSubscriptionRequest(Class<T> responseType) {
        return new SimpleGraphQLRequest<>("", responseType, null);
    }
}
