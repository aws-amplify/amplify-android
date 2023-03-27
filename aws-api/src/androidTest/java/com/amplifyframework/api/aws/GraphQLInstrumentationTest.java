/*
 * Copyright 2023 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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

import com.amplifyframework.AmplifyException;
import com.amplifyframework.api.ApiCategory;
import com.amplifyframework.api.ApiException;
import com.amplifyframework.api.aws.test.R;
import com.amplifyframework.api.graphql.GraphQLResponse;
import com.amplifyframework.api.graphql.SimpleGraphQLRequest;
import com.amplifyframework.auth.AuthException;
import com.amplifyframework.auth.cognito.AWSCognitoAuthPlugin;
import com.amplifyframework.core.model.Model;
import com.amplifyframework.testutils.Assets;
import com.amplifyframework.testutils.sync.SynchronousApi;
import com.amplifyframework.testutils.sync.SynchronousAuth;

import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import io.reactivex.rxjava3.observers.TestObserver;

import static androidx.test.core.app.ApplicationProvider.getApplicationContext;
import static org.junit.Assert.assertEquals;

/**
 * Validates the functionality of the {@link AWSApiPlugin}.
 *
 * To configure an endpoint for this:
 *
 * 1. Create a new "Event App" AppSync endpoint via "Create with Wizard" at
 *    https://us-west-2.console.aws.amazon.com/appsync/home?region=us-west-2#/create
 *
 * 2. In the App Sync console, find the settings for the API you just created. Get
 *    the API URL and API Key, and populate them into the
 *    src/androidTest/res/raw/amplifyconfiguration.json.
 *    Name the API "GraphQLInstrumentationTest". Ensure the region is set correctly.
 *
 * 3. Run the test. From the command line, you can do ./gradlew aws-amplify-api-aws:connectedAndroidTest
 */
public final class GraphQLInstrumentationTest {
    private static final String API_WITH_API_KEY = "eventsApi";
    private static final String API_WITH_COGNITO_USER_POOLS = "eventsApiWithUserPools";

    private static SynchronousApi api;
    private static SynchronousAuth synchronousAuth;

    private String currentApiName;

    /**
     * Configure the Amplify framework, if that hasn't already happened in this process instance.
     * @throws AmplifyException From Amplify configuration
     * @throws InterruptedException From failure to initialize auth
     */
    @BeforeClass
    public static void setUp() throws AmplifyException, InterruptedException {
        // Set up and configure API category
        ApiCategory asyncDelegate = TestApiCategory.fromConfiguration(R.raw.amplifyconfiguration);
        api = SynchronousApi.delegatingTo(asyncDelegate);

        // Set up Auth
        synchronousAuth = SynchronousAuth.delegatingToCognito(getApplicationContext(),
                new AWSCognitoAuthPlugin());
    }

    /**
     * Reset all the assigned static fields.
     */
    @AfterClass
    public static void tearDown() {
        synchronousAuth = null;
        api = null;
    }

    /**
     * Start auth with signed out state.
     * @throws AuthException if sign out fails.
     */
    @Before
    public void setUpAuth() throws AuthException {
        synchronousAuth.signOut();
    }

    /**
     * Test that subscription is authorized properly when using API key as
     * authorization provider.
     * @throws ApiException On failure to reach the endpoint or receive
     *          expected response from the endpoint
     */
    @Test
    public void subscriptionReceivesMutationOverApiKey() throws ApiException {
        currentApiName = API_WITH_API_KEY;
        subscriptionReceivesMutation();
    }

    /**
     * Test that subscription fails when using Cognito User Pools as
     * authorization provider, and is not signed in.
     * @throws ApiException On failure to reach the endpoint or receive
     *          expected response from the endpoint
     */
    @Test(expected = ApiException.class)
    public void subscriptionWithCognitoUserPoolsFailsAsGuest() throws ApiException {
        currentApiName = API_WITH_COGNITO_USER_POOLS;
        subscriptionReceivesMutation();
    }

    /**
     * Validates that we can receive notification of a mutation over a WebSocket
     * subscription. Specifically, this test will:
     * 1. Create an event, and validate it;
     * 2. Setup a subscription to listen for comments on that event;
     * 3. Post a comment about the event, validate that comment;
     * 4. Expect the comment to arrive on the subscription;
     * 5. Validate that the subscription can be torn down gracefully.
     * @throws ApiException On failure to obtain a valid response from endpoint
     */
    private void subscriptionReceivesMutation() throws ApiException {
        // Create an event
        String eventId = createEvent();

        // Start listening for comments on that event
        TestObserver<GraphQLResponse<Comment>> observer = api.onCreate(
            currentApiName,
            new SimpleGraphQLRequest<Comment>(
                Assets.readAsString("subscribe-event-comments.graphql"),
                Collections.singletonMap("eventId", eventId),
                Comment.class,
                new GsonVariablesSerializer()
            )
        ).test();

        // Create a comment
        createComment(eventId);

        // Validate that the comment was received over the subscription
        Comment firstValue = observer.awaitCount(1).values().get(0).getData();
        assertEquals("It's going to be fun!", firstValue.content());

        // Cancel the subscription.
        observer.dispose();
    }

    /**
     * Creates a comment, associated to an event whose ID is {@see eventId}.
     * @param eventId ID of event to which this comment will be associated
     * @throws ApiException On failure to obtain a valid response from endpoint
     */
    private void createComment(String eventId) throws ApiException {
        String commentId = UUID.randomUUID().toString();

        final Map<String, Object> variables = new HashMap<>();
        variables.put("eventId", eventId);
        variables.put("commentId", commentId);
        variables.put("content", "It's going to be fun!");
        variables.put("createdAt", Iso8601Timestamp.now());

        Comment createdComment = api.create(
            currentApiName,
            new SimpleGraphQLRequest<>(
                Assets.readAsString("create-comment.graphql"),
                variables,
                Comment.class,
                new GsonVariablesSerializer()
            )
        );
        assertEquals("It's going to be fun!", createdComment.content());
    }

    /**
     * Create an Event against the GraphQL endpoint, using a mutation.
     * Validate the response to ensure that what was created is what we requested.
     * @return The unique ID of the newly created event. This ID may be used
     *         to associate comments to this event object.
     * @throws ApiException On failure to obtain a valid response from endpoint
     */
    private String createEvent() throws ApiException {
        // Arrange a creation request, including a map of plug-able variables
        final Map<String, Object> variables = new HashMap<>();
        variables.put("name", "Pizza Party");
        variables.put("when", "Tomorrow");
        variables.put("where", "Mario's Pizza Emporium");
        variables.put("description", "RSVP for the best possible pizza toppings.");

        Event createdEvent = api.create(
            currentApiName,
            new SimpleGraphQLRequest<>(
                Assets.readAsString("create-event.graphql"),
                variables,
                Event.class,
                new GsonVariablesSerializer()
            )
        );

        // Validate the response. No errors are expected.
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
    static final class Comment implements Model {
        private final String content;

        @SuppressWarnings("checkstyle:ParameterName")
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
    static final class Event implements Model {
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
}

