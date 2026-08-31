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
 * The client's own Gson instance, deliberately private to this module rather than shared with the
 * API plugin's `GsonFactory` — per #3377 the client reimplements what it needs instead of depending
 * on `aws-api`.
 *
 * It registers the same adapters as the plugin except the two lazy-loading deserializers
 * (`ModelListDeserializer`, `ModelPageDeserializer`), which live in `aws-api` and are Phase 7 work.
 * Every adapter used here is in `aws-api-appsync`, which this module already exposes as `api`.
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
            .create()
    }
}
