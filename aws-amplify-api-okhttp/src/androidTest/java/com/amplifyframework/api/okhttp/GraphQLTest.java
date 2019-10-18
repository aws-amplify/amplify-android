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

package com.amplifyframework.api.okhttp;

import androidx.test.core.app.ApplicationProvider;

import com.amplifyframework.api.graphql.GraphQLCallback;
import com.amplifyframework.api.graphql.OperationType;
import com.amplifyframework.api.graphql.Response;
import com.amplifyframework.core.Amplify;

import org.junit.BeforeClass;
import org.junit.Test;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * Validates the functionality of the {@link OkHttpApiPlugin}.
 */
public final class GraphQLTest {

    private static final String TAG = GraphQLTest.class.getSimpleName();

    private static final int THREAD_WAIT_DURATION = 300;
    private static CountDownLatch latch;

    /**
     * Before any test is run, configure Amplify to use an
     * {@link OkHttpApiPlugin} to satisfy the Api category.
     */
    @BeforeClass
    public static void setUpBeforeClass() {
        //TODO: Read these from file
        final String apiName = "{API_NAME}";
        final String endpoint = "{ENDPOINT}";
        final String region = "us-west-2";
        final String apiKey = "{API_KEY}";

        OkHttpApiPlugin apiPlugin = new OkHttpApiPlugin();
        OkHttpApiPluginConfiguration mockConfig = new OkHttpApiPluginConfiguration();
        mockConfig.addApi(apiName, ApiConfiguration.builder()
            .endpoint(endpoint)
            .region(region)
            .authType(AuthType.API_KEY)
            .apiKey(apiKey)
            .build());
        Amplify.addPlugin(apiPlugin);
        Amplify.configure(ApplicationProvider.getApplicationContext());

        apiPlugin.configure(mockConfig, ApplicationProvider.getApplicationContext());
    }

    /**
     * Tests API graphql query.
     * @throws Exception when interrupted
     */
    @Test
    public void testQuery() throws Exception {
        /* List not supported yet

        String document = "list {" +
                "  listTodos {" +
                "    items {" +
                "      id" +
                "      name" +
                "      description" +
                "    }" +
                "  }" +
                "}";
        */

        String document = "get {" +
                "  getTodo(id:1) {" +
                "    id" +
                "    name" +
                "    description" +
                "  }" +
                "}";

        latch = new CountDownLatch(1);
        Amplify.API.graphql("mygraphql",
                OperationType.QUERY,
                document,
                Todo.class,
                new TestGraphQLCallback<>());
        latch.await(THREAD_WAIT_DURATION, TimeUnit.SECONDS);
    }

    /**
     * Tests API graphql mutation.
     * @throws Exception when interrupted
     */
    @Test
    public void testMutation() throws Exception {
        String document = "update { " +
                "  updateTodo(input:{ " +
                "    id:\"1\"" +
                "    name:\"my todo\"" +
                "    description:\"test gql\"" +
                "  }){" +
                "    id" +
                "    name" +
                "    description" +
                "  }" +
                "}";

        latch = new CountDownLatch(1);
        Amplify.API.graphql("mygraphql",
                OperationType.MUTATION,
                document,
                String.class,
                new TestGraphQLCallback<>());
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

    class TestGraphQLCallback<T> implements GraphQLCallback<T> {
        @Override
        public void onResponse(Response<T> response) {
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

