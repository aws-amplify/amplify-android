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

import com.amplifyframework.api.graphql.GsonResponseAdapters;
import com.amplifyframework.core.model.query.predicate.GsonPredicateAdapters;
import com.amplifyframework.core.model.temporal.GsonTemporalAdapters;
import com.amplifyframework.core.model.types.GsonJavaTypeAdapters;
import com.amplifyframework.datastore.appsync.ModelWithMetadataAdapter;
import com.amplifyframework.datastore.appsync.SerializedCustomTypeAdapter;
import com.amplifyframework.datastore.appsync.SerializedModelAdapter;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

/**
 * Creates a {@link Gson} instance which may be used around the API plugin.
 */
final class GsonFactory {
    private static Gson gson = null;

    private GsonFactory() {}

    /**
     * Obtains a singleton instance of {@link Gson}, configured with adapters sufficient
     * to serialize and deserialize all types the API plugin will encounter.
     * @return A configured Gson instance.
     */
    public static synchronized Gson instance() {
        if (gson == null) {
            gson = create();
        }
        return gson;
    }

    private static Gson create() {
        GsonBuilder builder = new GsonBuilder();
        GsonTemporalAdapters.register(builder);
        GsonJavaTypeAdapters.register(builder);
        GsonPredicateAdapters.register(builder);
        GsonResponseAdapters.register(builder);
        ModelWithMetadataAdapter.register(builder);
        SerializedModelAdapter.register(builder);
        SerializedCustomTypeAdapter.register(builder);
        ModelListDeserializer.register(builder);
        ModelPageDeserializer.register(builder);
        builder.serializeNulls();
        return builder.create();
    }
}
