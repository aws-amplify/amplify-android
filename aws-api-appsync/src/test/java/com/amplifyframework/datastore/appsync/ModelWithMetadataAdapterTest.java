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

package com.amplifyframework.datastore.appsync;

import com.amplifyframework.core.model.temporal.GsonTemporalAdapters;
import com.amplifyframework.core.model.temporal.Temporal;
import com.amplifyframework.testmodels.commentsblog.BlogOwner;
import com.amplifyframework.testutils.Resources;
import com.amplifyframework.util.TypeMaker;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.skyscreamer.jsonassert.JSONAssert;

import java.lang.reflect.Type;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * Tests the {@link ModelWithMetadataAdapter}.
 */
public final class ModelWithMetadataAdapterTest {
    private Gson gson;

    /**
     * We test the {@link ModelWithMetadataAdapter} through a vanilla Gson instance.
     */
    @Before
    public void setup() {
        GsonBuilder builder = new GsonBuilder();
        ModelWithMetadataAdapter.register(builder);
        GsonTemporalAdapters.register(builder);
        this.gson = builder.create();
    }

    /**
     * The Gson adapter can be used to serialize a ModelWithMetadata to JSON.
     * @throws JSONException From JSONAssert.assert(...) call, if invalid JSON
     *                       is provided in either position
     */
    @Test
    public void adapterCanSerializeMwm() throws JSONException {
        Temporal.Timestamp lastChangedAt = Temporal.Timestamp.now();
        String modelId = UUID.randomUUID().toString();
        ModelMetadata metadata = new ModelMetadata(modelId, false, 4, lastChangedAt);
        BlogOwner model = BlogOwner.builder()
            .name("Blog Owner")
            .build();
        ModelWithMetadata<BlogOwner> mwm = new ModelWithMetadata<>(model, metadata);

        String expected = new JSONObject()
            .put("id", model.getId())
            .put("name", model.getName())
            .put("itemModelName", model.getModelName())
            .put("_lastChangedAt", metadata.getLastChangedAt().getSecondsSinceEpoch())
            .put("_deleted", metadata.isDeleted())
            .put("_version", metadata.getVersion())
            .toString();
        String actual = gson.toJson(mwm);
        JSONAssert.assertEquals(expected, actual, true);
    }

    /**
     * The Gson adapter can be used to deserialize JSON into a ModelWithMetadata object.
     */
    @Test
    public void adapterCanDeserializeJsonIntoMwm() {
        // Arrange expected value
        BlogOwner model = BlogOwner.builder()
            .name("Tony Danielsen")
            .id("45a5f600-8aa8-41ac-a529-aed75036f5be")
            .build();
        Temporal.Timestamp lastChangedAt = new Temporal.Timestamp(1594858827, TimeUnit.SECONDS);
        ModelMetadata metadata = new ModelMetadata(model.getId(), false, 3, lastChangedAt);
        ModelWithMetadata<BlogOwner> expected = new ModelWithMetadata<>(model, metadata);

        // Arrange some JSON, and then try to deserialize it
        String json = Resources.readAsString("blog-owner-with-metadata.json");
        Type type = TypeMaker.getParameterizedType(ModelWithMetadata.class, BlogOwner.class);
        ModelWithMetadata<BlogOwner> actual = gson.fromJson(json, type);

        // Assert that the deserialized output matches out expected value
        Assert.assertEquals(expected, actual);
    }
}
