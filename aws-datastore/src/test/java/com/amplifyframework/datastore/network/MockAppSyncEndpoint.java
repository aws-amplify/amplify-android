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
import com.amplifyframework.core.Consumer;
import com.amplifyframework.core.async.NoOpCancelable;
import com.amplifyframework.core.model.Model;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;

import static org.mockito.Mockito.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.eq;

/**
 * A utility to mock behaviors of an {@link AppSyncEndpoint} from test code.
 */
final class MockAppSyncEndpoint {
    @SuppressWarnings("checkstyle:all") private MockAppSyncEndpoint() {}

    static Configurator configure(AppSyncEndpoint mock) {
        return new Configurator(mock);
    }

    static final class Configurator {
        private AppSyncEndpoint endpoint;

        Configurator(AppSyncEndpoint appSyncEndpoint) {
            this.endpoint = appSyncEndpoint;
        }

        /**
         * Creates an instance of an {@link AppSyncEndpoint}, which will provide a fake response when asked to
         * to {@link AppSyncEndpoint#sync(Class, Long, Consumer, Consumer)}.
         * @param modelClass Class of models for which the endpoint should respond
         * @param responseItems The items that should be included in the mocked response, for the model class
         * @param <T> Type of models for which a response is mocked
         */
        @SuppressWarnings("varargs")
        @SafeVarargs
        final <T extends Model> Configurator mockSuccessResponse(
                Class<T> modelClass, ModelWithMetadata<T>... responseItems) {
            doAnswer(invocation -> {
                // Get a handle to the response consumer that is passed into the sync() method
                // Response consumer is the third param, at index 2 (@0, @1, @2, @3).
                final int argumentPositionForResponseConsumer = 2;
                final Consumer<GraphQLResponse<Iterable<ModelWithMetadata<T>>>> consumer =
                    invocation.getArgument(argumentPositionForResponseConsumer);

                // Call the response consumer, and pass the mocked items
                // inside of a GraphQLResponse wrapper
                final Iterable<ModelWithMetadata<T>> data = new HashSet<>(Arrays.asList(responseItems));
                consumer.accept(new GraphQLResponse<>(data, Collections.emptyList()));

                // Return a NoOp cancelable via the sync() method's return.
                return new NoOpCancelable();
            }).when(endpoint).sync(
                eq(modelClass), // Item class to sync
                any(), // last sync time
                any(), // Consumer<Iterable<ModelWithMetadata<T>>>
                any() // Consumer<DataStoreException>
            );
            return Configurator.this;
        }
    }
}
