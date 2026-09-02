/*
 * Copyright 2026 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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
package com.amazonaws.appsync

import com.amplifyframework.api.graphql.GsonResponseAdapters
import com.amplifyframework.core.model.query.predicate.GsonPredicateAdapters
import com.amplifyframework.core.model.temporal.GsonTemporalAdapters
import com.amplifyframework.core.model.types.GsonJavaTypeAdapters
import com.amplifyframework.datastore.appsync.ModelWithMetadataAdapter
import com.amplifyframework.datastore.appsync.SerializedCustomTypeAdapter
import com.amplifyframework.datastore.appsync.SerializedModelAdapter
import com.google.gson.Gson
import com.google.gson.GsonBuilder

/**
 * The Gson instance the client serializes requests and deserializes responses with.
 *
 * Private to the client rather than shared, so neither the adapter set nor the null handling below can
 * be altered from outside.
 *
 * TODO: register deserializers for lazily-loaded model lists and pages, which this set does not cover.
 */
internal object AppSyncGson {

    val instance: Gson by lazy {
        GsonBuilder()
            .also {
                GsonTemporalAdapters.register(it)
                GsonJavaTypeAdapters.register(it)
                GsonPredicateAdapters.register(it)
                GsonResponseAdapters.register(it)
                ModelWithMetadataAdapter.register(it)
                SerializedModelAdapter.register(it)
                SerializedCustomTypeAdapter.register(it)
            }
            // A mutation that clears a field needs an explicit `"field": null` in the payload, because
            // AppSync reads an absent field as "leave unchanged" rather than "set to null".
            .serializeNulls()
            .create()
    }
}
