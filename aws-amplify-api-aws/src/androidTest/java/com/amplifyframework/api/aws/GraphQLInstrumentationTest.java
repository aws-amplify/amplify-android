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

import com.amplifyframework.api.aws.test.R;
import com.amplifyframework.api.graphql.GraphQLResponse;
import com.amplifyframework.core.Amplify;
import com.amplifyframework.core.AmplifyConfiguration;
import com.amplifyframework.core.async.Listener;

import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * Validates the functionality of the {@link AWSApiPlugin}.
 *
 * For the time-being, this test is not regularly run as part of the
 * instrumentation test suite. For now, a developer must manually
 * specify the configuration for their (compatible!) GraphQL endpoint in
 * the androidTest/res/raw/amplifyconfiguration.json. It is expected tha
 * the developer will have setup a ListTodos GraphQL endpoint using the
 * amplify CLI and the standard models from the AppSync public docs
 * (TODO: which docs, which standard models?).
 */
//TODO: Use CircleCI to automatically use configured amplifyconfiguration.json
@Ignore("First, config your dev endpoint in androidTest/res/raw/amplifyconfiguration.json.")
public final class GraphQLInstrumentationTest {

    private static final String TAG = GraphQLInstrumentationTest.class.getSimpleName();

    private static final int THREAD_WAIT_DURATION = 300;
    private static CountDownLatch latch;

    /**
     * Before any test is run, configure Amplify to use an
     * {@link AWSApiPlugin} to satisfy the Api category.
     */
    @BeforeClass
    public static void setUpBeforeClass() {
        Context context = ApplicationProvider.getApplicationContext();
        AmplifyConfiguration configuration = new AmplifyConfiguration();
        configuration.populateFromConfigFile(context, R.raw.amplifyconfiguration);
        Amplify.addPlugin(new AWSApiPlugin());
        Amplify.configure(configuration, context);
    }

    /**
     * Tests API graphql query.
     * @throws Exception when interrupted
     */
    @Test
    public void testQuery() throws Exception {
        String document = TestAssets.readAsString("get-todo.graphql");
        latch = new CountDownLatch(1);
        Amplify.API.query(
                "mygraphql",
                document,
                Todo.class,
                new TestGraphQLListener<>());
        latch.await(THREAD_WAIT_DURATION, TimeUnit.SECONDS);
    }

    /**
     * Tests API graphql mutation.
     * @throws Exception when interrupted
     */
    @Test
    public void testMutation() throws Exception {
        String document = TestAssets.readAsString("update-todo.graphql");
        latch = new CountDownLatch(1);
        Amplify.API.mutate(
                "mygraphql",
                document,
                Todo.class,
                new TestGraphQLListener<>());
        latch.await(THREAD_WAIT_DURATION, TimeUnit.SECONDS);
    }

    class Todo {
        private final String id;
        private final String name;
        private final String description;

        Todo(String todoId, String name, String description) {
            this.id = todoId;
            this.name = name;
            this.description = description;
        }

        public String getId() {
            return id;
        }

        public String getName() {
            return name;
        }

        public String getDescription() {
            return description;
        }
    }

    class TestGraphQLListener<T> implements Listener<GraphQLResponse<T>> {
        @Override
        public void onResult(GraphQLResponse<T> response) {
            assertNotNull(response);
            assertTrue(response.hasData());
            assertFalse(response.hasErrors());
            latch.countDown();
        }

        @Override
        public void onError(Exception error) {
            fail(error.getLocalizedMessage());
        }
    }
}

