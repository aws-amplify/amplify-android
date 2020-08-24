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

import androidx.annotation.NonNull;

import com.amplifyframework.api.graphql.GraphQLResponse;
import com.amplifyframework.core.model.temporal.Temporal;
import com.amplifyframework.datastore.appsync.ModelWithMetadata;
import com.amplifyframework.datastore.appsync.ModelWithMetadataDeserializer;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.util.Collections;
import java.util.Map;

final class GsonFactory {
    private GsonFactory() {}

    static Gson create() {
        return create(Collections.emptyMap());
    }

    static Gson create(@NonNull Map<Class<?>, Object> additionalAdapters) {
        GsonBuilder builder = new GsonBuilder();
        withDeserializers(builder);
        withAdditionalAdapters(builder, additionalAdapters);
        return builder.create();
    }

    private static void withDeserializers(GsonBuilder builder) {
        builder
            .registerTypeAdapter(Temporal.Date.class, new TemporalDeserializers.DateDeserializer())
            .registerTypeAdapter(Temporal.Time.class, new TemporalDeserializers.TimeDeserializer())
            .registerTypeAdapter(Temporal.Timestamp.class, new TemporalDeserializers.TimestampDeserializer())
            .registerTypeAdapter(Temporal.DateTime.class, new TemporalDeserializers.DateTimeDeserializer())
            .registerTypeAdapter(GraphQLResponse.class, new GraphQLResponseDeserializer())
            .registerTypeAdapter(GraphQLResponse.Error.class, new GsonErrorDeserializer())
            .registerTypeAdapter(String.class, new StringDeserializer())
            .registerTypeAdapter(ModelWithMetadata.class, new ModelWithMetadataDeserializer());
    }

    private static void withAdditionalAdapters(GsonBuilder builder, Map<Class<?>, Object> additionalAdapters) {
        for (Map.Entry<Class<?>, Object> entry : additionalAdapters.entrySet()) {
            builder.registerTypeAdapter(entry.getKey(), entry.getValue());
        }
    }
}
