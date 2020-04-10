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

package com.amplifyframework.datastore;

import android.content.Context;

import com.amplifyframework.AmplifyException;
import com.amplifyframework.api.ApiPlugin;
import com.amplifyframework.api.graphql.GraphQLRequest;
import com.amplifyframework.api.graphql.GraphQLResponse;
import com.amplifyframework.core.Action;
import com.amplifyframework.core.Amplify;
import com.amplifyframework.core.AmplifyConfiguration;
import com.amplifyframework.core.Consumer;
import com.amplifyframework.core.category.CategoryType;
import com.amplifyframework.core.model.ModelProvider;
import com.amplifyframework.testmodels.personcar.Person;
import com.amplifyframework.testutils.random.RandomString;

import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

import java.util.Collections;

import static androidx.test.core.app.ApplicationProvider.getApplicationContext;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@RunWith(RobolectricTestRunner.class)
public final class AWSDataStorePluginTest {
    private Context context;
    private AWSDataStorePlugin awsDataStorePlugin;

    @Before
    public void setup() {
        this.context = getApplicationContext();
        ModelProvider modelProvider = SimpleModelProvider.builder()
            .version(RandomString.string())
            .addModel(Person.class)
            .build();
        this.awsDataStorePlugin = new AWSDataStorePlugin(modelProvider);
    }

    /**
     * Configuring and initializing the plugin succeeds without freezing or
     * crashing the calling thread. Basic. 🙄
     * @throws JSONException Not expected; on failure to arrange configuration object
     * @throws DataStoreException Not expected; on failure to configure of initialize plugin
     */
    @Ignore("TODO: need a mechanism to re-enable this scenario........")
    @Test
    public void configureAndInitializeInLocalMode() throws DataStoreException, JSONException {
        JSONObject pluginJson = new JSONObject().put("syncMode", "none");
        awsDataStorePlugin.configure(pluginJson, context);
        awsDataStorePlugin.initialize(context);
    }

    /**
     * Configuring and initialization the plugin when in API sync mode succeeds without
     * freezing or crashing the the calling thread.
     * @throws JSONException on failure to arrange plugin config
     * @throws DataStoreException on failure to configure
     * @throws AmplifyException on failure to arrange API plugin via Amplify facade
     */
    @Test
    public void configureAndInitializeInApiMode() throws JSONException, AmplifyException {
        Amplify.addPlugin(mockApiPlugin());
        JSONObject amplifyJson = new JSONObject().put("api", new JSONObject());
        AmplifyConfiguration configuration = AmplifyConfiguration.fromJson(amplifyJson);
        Amplify.configure(configuration, context);

        JSONObject pluginJson = new JSONObject()
            .put("syncMode", "api")
            .put("baseSyncIntervalMs", 1_000);
        awsDataStorePlugin.configure(pluginJson, context);
        awsDataStorePlugin.initialize(context);
    }

    @SuppressWarnings("unchecked")
    private static ApiPlugin<Void> mockApiPlugin() {
        ApiPlugin<Void> mockApiPlugin = mock(ApiPlugin.class);
        when(mockApiPlugin.getPluginKey()).thenReturn("awsApiPlugin");
        when(mockApiPlugin.getCategoryType()).thenReturn(CategoryType.API);

        // Make believe that queries return response immediately
        doAnswer(invocation -> {
            int indexOfResponseConsumer = 1;
            Consumer<GraphQLResponse<Iterable<String>>> onResponse = invocation.getArgument(indexOfResponseConsumer);
            onResponse.accept(new GraphQLResponse<>(Collections.emptyList(), Collections.emptyList()));
            return null;
        }).when(mockApiPlugin).query(any(GraphQLRequest.class), any(Consumer.class), any(Consumer.class));

        // Make believe that mutations return response immediately
        doAnswer(invocation -> {
            int indexOfResponseConsumer = 1;
            Consumer<GraphQLResponse<String>> onResponse = invocation.getArgument(indexOfResponseConsumer);
            onResponse.accept(new GraphQLResponse<>("{}", Collections.emptyList()));
            return null;
        }).when(mockApiPlugin).mutate(any(GraphQLRequest.class), any(Consumer.class), any(Consumer.class));

        // Make believe that subscriptions return response immediately
        doAnswer(invocation -> {
            int indexOfStartConsumer = 2;
            Consumer<String> onResponse = invocation.getArgument(indexOfStartConsumer);
            onResponse.accept(RandomString.string());
            return null;
        }).when(mockApiPlugin).subscribe(
            any(GraphQLRequest.class),
            any(Consumer.class),
            any(Consumer.class),
            any(Consumer.class),
            any(Action.class)
        );

        return mockApiPlugin;
    }
}
