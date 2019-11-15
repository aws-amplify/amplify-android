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

package com.amplifyframework.api.aws;

import android.content.Context;
import androidx.test.core.app.ApplicationProvider;

import com.amplifyframework.api.ApiCategoryBehavior;
import com.amplifyframework.api.aws.test.R;
import com.amplifyframework.api.graphql.GraphQLOperation;
import com.amplifyframework.api.graphql.GraphQLResponse;
import com.amplifyframework.core.Amplify;
import com.amplifyframework.core.AmplifyConfiguration;
import com.amplifyframework.core.Immutable;
import com.amplifyframework.core.ResultListener;
import com.amplifyframework.core.StreamListener;

import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * Validates the functionality of the {@link AWSApiPlugin}.
 *
 * Note: for the time-being, this test is not run by default. The expectation is that
 * a developer can run this, after performing some configuration steps.
 *
 * 1. Create a new "Event App" AppSync endpoint via "Create with Wizard" at
 *    https://us-west-2.console.aws.amazon.com/appsync/home?region=us-west-2#/create
 *
 * 2. In the App Sync console, find the settings for the API you just created. Get
 *    the API URL and API Key, and populate them into the
 *    src/androidTest/res/raw/amplifyconfiguration.json.
 *    Name the API "GraphQLInstrumentationTest". Ensure the region is set correctly.
 *
 * 3. Remove @Ignore from this test.
 *
 * 4. Run the test. From the command line, you can do ./gradlew aws-amplify-api-aws:connectedAndroidTest
 */
@Ignore("This is a developer-only test, requiring some backend configuration. See Javadoc for details.")
public final class GraphQLInstrumentationTest {
    private static final String API_NAME = GraphQLInstrumentationTest.class.getSimpleName();
    private static final int DEFAULT_RESPONSE_TIMEOUT = 5 /* seconds */; // 5 is chosen arbitrarily
    private static final int THREAD_WAIT_DURATION = 300;

    /**
     * Before any test is run, configure Amplify to use an
     * {@link AWSApiPlugin} to satisfy the Api category.
     */
    @BeforeClass
    public static void configureAmplify() {
        Context context = ApplicationProvider.getApplicationContext();
        AmplifyConfiguration configuration = new AmplifyConfiguration();
        configuration.populateFromConfigFile(context, R.raw.amplifyconfiguration);
        Amplify.addPlugin(new AWSApiPlugin());
        Amplify.configure(configuration, context);
    }

    /**
     * Validates that we can receive notification of a mutation over a WebSocket
     * subscription. Specifically, this test will:
     * 1. Create an event, and validate it;
     * 2. Setup a subscription to listen for comments on that event;
     * 3. Post a comment about the event, validate that comment;
     * 4. Expect the comment to arrive on the subscription;
     * 5. Validate that the subscription can be torn down gracefully.
     * @throws Throwable If we timeout while talking to the endpoint,
     *                   or if any response comes back invalid
     */
    @Test
    public void subscriptionReceivesMutation() throws Throwable {
        // Create an event
        String eventId = createEvent();

        // Start listening for comments on that event
        BlockingStreamListener<Comment> streamListener = new BlockingStreamListener<>(1);
        GraphQLOperation<Comment> operation = Amplify.API.subscribe(
            API_NAME,
            TestAssets.readAsString("subscribe-event-comments.graphql"),
            Collections.singletonMap("eventId", eventId),
            Comment.class,
            streamListener
        );

        // Create a comment
        createComment(eventId);

        // Validate that subscription received the comment.
        validateSubscribedComments(streamListener.awaitItems());

        // Cancel the subscription.
        operation.cancel();

        // Ensure that onComplete() is called as a response to canceling
        // the operation.
        streamListener.awaitCompletion();
    }

    /**
     * Creates a comment, associated to an event whose ID is {@see eventId}.
     * @param eventId ID of event to which this comment will be associated
     * @throws Throwable For various reasons, but commonly if we fail to receive
     *                   a response from the GraphQL endpoint within a few seconds.
     *                   Potentially also if validations fail.
     */
    private void createComment(String eventId) throws Throwable {
        String commentId = UUID.randomUUID().toString();

        final Map<String, String> variables = new HashMap<>();
        variables.put("eventId", eventId);
        variables.put("commentId", commentId);
        variables.put("content", "It's going to be fun!");
        variables.put("createdAt", Iso8601Timestamp.now());

        BlockingResultListener<Comment> creationListener = new BlockingResultListener<>();
        Amplify.API.mutate(
            API_NAME,
            TestAssets.readAsString("create-comment.graphql"),
            variables,
            Comment.class,
            creationListener
        );
        GraphQLResponse<Comment> response = creationListener.awaitResult();
        assertFalse(response.hasErrors());
        assertTrue(response.hasData());
        Comment comment = response.getData();
        assertEquals("It's going to be fun!", comment.content());
    }

    /**
     * Validates a list of GraphQLResponse objects which contains comments. Our test
     * code only attempts to create a single subscription and to create a single comment,
     * so this list should have size one and its singleton item should not have errors.
     * @param subscriptionResponses List of responses received by subscription
     */
    private void validateSubscribedComments(List<GraphQLResponse<Comment>> subscriptionResponses) {
        // Validate that the subscription received data.
        assertEquals(1, subscriptionResponses.size());
        assertFalse(subscriptionResponses.get(0).hasErrors());
        Comment subscriptionComment = subscriptionResponses.get(0).getData();
        assertEquals("It's going to be fun!", subscriptionComment.content());
    }

    /**
     * Create an Event against the GraphQL endpoint, using a mutation.
     * Validate the response to ensure that what was created is what we requested.
     * @return The unique ID of the newly created event. This ID may be used
     *         to associate comments to this event object.
     * @throws Throwable On test failure. One common source of failure is if
     *                   creation listener times out, which means that we never
     *                   got a response back from the server. Other possible
     *                   failures may arise from failed assert*() calls.
     */
    private String createEvent() throws Throwable {
        // Arrange a creation request, including a map of plug-able variables
        final Map<String, String> variables = new HashMap<>();
        variables.put("name", "Pizza Party");
        variables.put("when", "Tomorrow");
        variables.put("where", "Mario's Pizza Emporium");
        variables.put("description", "RSVP for the best possible pizza toppings.");

        // Act: call the API to create the event.
        // Block this test runner until a response is rendered.
        BlockingResultListener<Event> creationListener = new BlockingResultListener<>();
        Amplify.API.mutate(
            API_NAME,
            TestAssets.readAsString("create-event.graphql"),
            variables,
            Event.class,
            creationListener
        );

        // Validate the response. No errors are expected.
        GraphQLResponse<Event> creationResponse = creationListener.awaitResult();
        assertFalse(creationResponse.hasErrors());

        // The echo-d response mimics what we provided.
        Event createdEvent = creationResponse.getData();
        assertEquals("Pizza Party", createdEvent.name());
        assertEquals("Tomorrow", createdEvent.when());
        assertEquals("Mario's Pizza Emporium", createdEvent.where());
        assertEquals("RSVP for the best possible pizza toppings.", createdEvent.description());

        return createdEvent.id();
    }

    /**
     * There's more to a Comment, as understood by our GraphQL schema, but
     * for the purposes of our tests, we just model a Comment as a thing containing
     * a String content message. This is enough for a simple assertion: "yup, same one".
     */
    static final class Comment {
        private final String content;

        Comment(final String content) {
            this.content = content;
        }

        String content() {
            return content;
        }
    }

    /**
     * Model of an Event, which we create as part of this test, so that we can
     * associate comments to the event.
     */
    static final class Event {
        private final String id;
        private final String name;
        private final String when;
        private final String where;
        private final String description;

        Event(
                @SuppressWarnings("ParameterName")
                final String id,
                final String name,
                final String when,
                final String where,
                final String description) {
            this.id = id;
            this.name = name;
            this.when = when;
            this.where = where;
            this.description = description;
        }

        String id() {
            return id;
        }

        String name() {
            return name;
        }

        String when() {
            return when;
        }

        String where() {
            return where;
        }

        String description() {
            return description;
        }
    }

    /**
     * An implementation of the {@link BlockingStreamListener} which can also await a number
     * of responses, and await a completion callback, sort of like resolving a promise.
     *
     * Construct an instance of this {@link BlockingStreamListener} by providing the number
     * of expected items, in the call to {@link BlockingStreamListener#BlockingStreamListener(int)}.
     *
     * Then, provide the instance to the
     * {@link ApiCategoryBehavior#subscribe(String, String, Map, Class, StreamListener)}
     * call.
     *
     * You can await results and completion by calling {@link BlockingStreamListener#awaitItems()}
     * and/or {@link BlockingStreamListener#awaitCompletion()}.
     * @param <T> The type of data expected in the {@link GraphQLResponse}s.
     */
    static final class BlockingStreamListener<T> implements StreamListener<GraphQLResponse<T>> {
        private final CountDownLatch allItemsLatch;
        private final CountDownLatch completedLatch;
        private final List<GraphQLResponse<T>> items;
        private Throwable error;

        BlockingStreamListener(int countOfItemsExpected) {
            this.allItemsLatch = new CountDownLatch(countOfItemsExpected);
            this.completedLatch = new CountDownLatch(1);
            this.items = new ArrayList<>();
            this.error = null;
        }

        @Override
        public void onNext(final GraphQLResponse<T> item) {
            items.add(item);
            allItemsLatch.countDown();
        }

        @Override
        public void onComplete() {
            completedLatch.countDown();
        }

        @Override
        public void onError(final Throwable error) {
            this.error = error;
            while (0 != allItemsLatch.getCount()) {
                allItemsLatch.countDown();
            }
        }

        List<GraphQLResponse<T>> awaitItems() throws Throwable {
            if (!allItemsLatch.await(DEFAULT_RESPONSE_TIMEOUT, TimeUnit.SECONDS)) {
                throw new IllegalStateException("Items count down latch did not count down.");
            }
            if (error != null) {
                throw error;
            }
            return Immutable.of(items);
        }

        void awaitCompletion() throws Throwable {
            if (!completedLatch.await(DEFAULT_RESPONSE_TIMEOUT, TimeUnit.SECONDS)) {
                throw new IllegalStateException("Completion latch did not count down.");
            }
        }
    }

    /**
     * An implementation of an {@link ResultListener} which can also await for a response,
     * kind of like resolving a promise. Provide an instance of this {@link BlockingResultListener}
     * to an API call, and then wait for a response by calling {@link BlockingResultListener#awaitResult()}.
     * @param <T> The type of data in the {@link GraphQLResponse}.
     */
    static final class BlockingResultListener<T> implements ResultListener<GraphQLResponse<T>> {
        private final CountDownLatch latch;
        private Throwable error;
        private GraphQLResponse<T> response;

        BlockingResultListener() {
            this.latch = new CountDownLatch(1);
        }

        @Override
        public void onResult(final GraphQLResponse<T> response) {
            this.response = response;
            latch.countDown();
        }

        @Override
        public void onError(final Throwable error) {
            this.error = error;
            latch.countDown();
        }

        GraphQLResponse<T> awaitResult() throws Throwable {
            if (!latch.await(DEFAULT_RESPONSE_TIMEOUT, TimeUnit.SECONDS)) {
                throw new IllegalStateException("Latch never counted down.");
            }
            if (error != null) {
                throw error;
            }
            return response;
        }
    }
}
