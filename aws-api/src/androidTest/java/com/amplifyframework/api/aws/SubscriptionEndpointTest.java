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

package com.amplifyframework.api.aws;

import com.amplifyframework.api.ApiException;
import com.amplifyframework.api.aws.test.R;
import com.amplifyframework.api.graphql.GraphQLRequest;
import com.amplifyframework.api.graphql.GraphQLResponse;
import com.amplifyframework.api.graphql.SimpleGraphQLRequest;
import com.amplifyframework.testutils.Assets;
import com.amplifyframework.testutils.Await;
import com.amplifyframework.testutils.Resources;
import com.amplifyframework.testutils.random.RandomString;

import org.json.JSONException;
import org.json.JSONObject;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import static androidx.test.core.app.ApplicationProvider.getApplicationContext;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;

/**
 * Tests the {@link SubscriptionEndpoint}.
 */
public final class SubscriptionEndpointTest {
    private Executor executor;
    private SubscriptionEndpoint subscriptionEndpoint;
    private String eventId;
    private Set<String> subscriptionIdsForRelease;

    /**
     * Create an {@link SubscriptionEndpoint}.
     * @throws ApiException On failure to load API configuration from config file
     * @throws JSONException On failure to manipulate configuration JSON during test arrangement
     */
    @Before
    public void setup() throws ApiException, JSONException {
        this.executor = Executors.newCachedThreadPool();

        String endpointConfigKey = "eventsApi"; // The endpoint config in amplifyconfiguration.json with this name
        JSONObject configJson = Resources.readAsJson(getApplicationContext(), R.raw.amplifyconfiguration)
            .getJSONObject("api")
            .getJSONObject("plugins")
            .getJSONObject("awsAPIPlugin");
        AWSApiPluginConfiguration pluginConfiguration = AWSApiPluginConfigurationReader.readFrom(configJson);
        ApiConfiguration apiConfiguration = pluginConfiguration.getApi(endpointConfigKey);
        assertNotNull(apiConfiguration);

        final GraphQLResponse.Factory responseFactory = new GsonGraphQLResponseFactory();
        final SubscriptionAuthorizer authorizer = new SubscriptionAuthorizer(apiConfiguration);
        this.subscriptionEndpoint = new SubscriptionEndpoint(apiConfiguration, null, responseFactory, authorizer, null);

        this.eventId = RandomString.string();
        this.subscriptionIdsForRelease = new HashSet<>();
    }

    /**
     * After the test, tear down the subscriptions.
     * @throws RuntimeException On failure to release subscription
     */
    @After
    public void releaseSubscriptions() throws RuntimeException {
        for (String subscriptionId : subscriptionIdsForRelease) {
            executor.execute(() ->
                Await.result((onResult, onError) -> {
                    try {
                        subscriptionEndpoint.releaseSubscription(subscriptionId);
                    } catch (ApiException failureToLetGo) {
                        // Friend, it is time. It is time.
                        throw new RuntimeException(failureToLetGo);
                    }
                })
            );
        }
    }

    /**
     * It should be possible to create two subscriptions to the same type of model.
     * @throws ApiException On failure to subscribe
     */
    @Test
    public void twoSubscriptionsToTheSameThing() throws ApiException {
        // Okay, request a first subscription.
        String firstSubscriptionId = subscribeToEventComments(eventId);
        assertNotNull(firstSubscriptionId);
        subscriptionIdsForRelease.add(firstSubscriptionId);

        // Now, request a second subscription, with the same request data.
        // This is around where the test fails.
        String secondSubscriptionId = subscribeToEventComments(eventId);
        assertNotNull(secondSubscriptionId);
        subscriptionIdsForRelease.add(secondSubscriptionId);

        // Theoretically would expect that we'd have two subscriptions,
        // and that their subscription IDs would be different/unique.
        assertNotEquals(firstSubscriptionId, secondSubscriptionId);
    }

    /**
     * It uses a configurator if present.
     *
     * This test verifies that a configurator can add an interceptor and that it will be run when establishing
     * subscriptions.
     * @throws ApiException On failure to load API configuration from config file
     * @throws JSONException On failure to manipulate configuration JSON during test arrangement
     */
    @Test
    public void usesConfiguratorIfPresent() throws ApiException, JSONException {
        String endpointConfigKey = "eventsApi"; // The endpoint config in amplifyconfiguration.json with this name
        JSONObject configJson = Resources.readAsJson(getApplicationContext(), R.raw.amplifyconfiguration)
                .getJSONObject("api")
                .getJSONObject("plugins")
                .getJSONObject("awsAPIPlugin");
        AWSApiPluginConfiguration pluginConfiguration = AWSApiPluginConfigurationReader.readFrom(configJson);
        ApiConfiguration apiConfiguration = pluginConfiguration.getApi(endpointConfigKey);
        assertNotNull(apiConfiguration);

        final GraphQLResponse.Factory responseFactory = new GsonGraphQLResponseFactory();
        final SubscriptionAuthorizer authorizer = new SubscriptionAuthorizer(apiConfiguration);

        final AtomicInteger counter = new AtomicInteger();

        final OkHttpConfigurator configurator = okHttpBuilder -> {
            counter.incrementAndGet();

            okHttpBuilder.addInterceptor(chain -> {
                counter.incrementAndGet();

                return chain.proceed(chain.request());
            });
        };

        this.subscriptionEndpoint = new SubscriptionEndpoint(apiConfiguration, configurator, responseFactory,
                authorizer, null);

        String firstSubscriptionId = subscribeToEventComments(eventId);
        assertNotNull(firstSubscriptionId);

        subscriptionIdsForRelease.add(firstSubscriptionId);

        assertEquals(2, counter.get());
    }

    /**
     * Subscribe to comments on an event.
     * @param eventId ID of event for which comments are being made
     * @return Subscription ID received from subscription_ack message payload
     * @throws ApiException If outcome of subscription request is anything other than an ACK w/ new ID
     */
    private String subscribeToEventComments(String eventId) throws ApiException {
        // Arrange a request to start a subscription.
        String document = Assets.readAsString("subscribe-event-comments.graphql");

        GsonVariablesSerializer serializer = new GsonVariablesSerializer();
        Map<String, Object> variables = Collections.singletonMap("eventId", eventId);
        GraphQLRequest<String> request = new SimpleGraphQLRequest<>(document, variables, String.class, serializer);

        return Await.<String, ApiException>result((onResult, onError) ->
            executor.execute(() ->
                subscriptionEndpoint.requestSubscription(
                    request,
                    onResult,
                    item -> {
                        final String message;
                        if (item.hasErrors()) {
                            message = "Subscription error: " + item.getErrors().toString();
                        } else {
                            message = "Unexpected subscription data: " + item.getData();
                        }
                        ApiException apiException = new ApiException(message, "Not expected.");
                        onError.accept(apiException);

                    },
                    error -> onError.accept(new ApiException("Subscription failed.", error, "Not expected.")),
                    () -> onError.accept(new ApiException("Subscription completed too soon.", "Not expected."))
                )
            )
        );
    }
}
