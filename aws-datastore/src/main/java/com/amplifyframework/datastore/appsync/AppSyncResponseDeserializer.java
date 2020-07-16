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

package com.amplifyframework.datastore.appsync;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.amplifyframework.api.ApiCategoryBehavior;
import com.amplifyframework.api.aws.TemporalDeserializers;
import com.amplifyframework.api.graphql.GraphQLResponse;
import com.amplifyframework.core.model.Model;
import com.amplifyframework.core.model.temporal.Temporal;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * This is an implementation detail of the {@link AppSyncClient}.
 *
 * Since the {@link AppSyncClient} makes requests to AppSync using raw GraphQL document strings,
 * the {@link ApiCategoryBehavior}s also return raw strings in their responses.
 *
 * This {@link AppSyncResponseDeserializer} takes string responses, and converts them into data models:
 * {@link ModelWithMetadata} (or, a collection of {@link ModelWithMetadata}). The shape of these
 * responses is unique to the AppSync protocol.
 */
final class AppSyncResponseDeserializer implements AppSyncClient.ResponseDeserializer {
    private final Gson gson;

    AppSyncResponseDeserializer() {
        this.gson = new GsonBuilder()
                .registerTypeAdapter(List.class, new GsonListDeserializer())
                .registerTypeAdapter(Temporal.Date.class, new TemporalDeserializers.DateDeserializer())
                .registerTypeAdapter(Temporal.Time.class, new TemporalDeserializers.TimeDeserializer())
                .registerTypeAdapter(Temporal.Timestamp.class, new TemporalDeserializers.TimestampDeserializer())
                .registerTypeAdapter(Temporal.DateTime.class, new TemporalDeserializers.DateTimeDeserializer())
                .create();
    }

    @Override
    public <T extends Model> GraphQLResponse<ModelWithMetadata<T>> deserialize(String json, Class<T> clazz) {
        ModelWithMetadata<T> modelWithMetadata = toModelWithMetadata(json, clazz);
        return new GraphQLResponse<>(modelWithMetadata, Collections.emptyList());
    }

    @Override
    public <T extends Model> GraphQLResponse<Iterable<ModelWithMetadata<T>>> deserialize(
            Iterable<String> jsons, Class<T> memberClazz) {
        List<ModelWithMetadata<T>> modelWithMetadataList = new ArrayList<>();
        if (jsons != null) {
            for (String jsonItem : jsons) {
                modelWithMetadataList.add(toModelWithMetadata(jsonItem, memberClazz));
            }
        }
        return new GraphQLResponse<>(modelWithMetadataList, Collections.emptyList());
    }

    private <T extends Model> ModelWithMetadata<T> toModelWithMetadata(String json, Class<T> clazz) {
        T model = gson.fromJson(json, clazz);
        ModelMetadata metadata = gson.fromJson(json, ModelMetadata.class);
        return new ModelWithMetadata<>(model, metadata);
    }

    /**
     * Custom list deserializer since some lists come back not as an array of the items but as an object which contains
     * an items property with the list of items and a nextToken property for pagination purposes.
     */
    public static final class GsonListDeserializer implements JsonDeserializer<List<Object>> {
        @Override
        @Nullable
        @SuppressWarnings("unchecked")
        public List<Object> deserialize(
                @NonNull JsonElement json,
                @NonNull Type typeOfT,
                @Nullable JsonDeserializationContext context
        ) throws JsonParseException {

            // If the json we got is not really a List and this List has a generics type...
            if (json.isJsonObject() && typeOfT instanceof ParameterizedType) {
                // Because this is a list and typeOfT is ParameterizedType we can be sure this is a safe cast.
                Class<Object> clazz = (Class<Object>) ((ParameterizedType) typeOfT).getActualTypeArguments()[0];
                JsonObject jsonObject = json.getAsJsonObject();
                Gson gson = new GsonBuilder()
                    .registerTypeAdapter(List.class, new GsonListDeserializer())
                    .create();

                // ...and it is in the format we expect from AppSync for a list of objects in a relationship
                if (jsonObject.has("items") && jsonObject.get("items").isJsonArray()) {
                    List<Object> response = new ArrayList<>();
                    JsonArray items = jsonObject.get("items").getAsJsonArray();

                    if (items.size() == 0) {
                        return null;
                    } else {
                        for (JsonElement item : items) {
                            response.add(gson.fromJson(item, clazz));
                        }

                        return response;
                    }
                } else {
                    throw new JsonParseException("Got JSON from an API call which was supposed to go with a List " +
                            "but is in the form of an object rather than an array. It also is not in the standard " +
                            "format of having an items property with the actual array of data so we do not know how " +
                            "to deserialize it.");
                }
            }

            return new Gson().fromJson(json, typeOfT);
        }
    }
}
