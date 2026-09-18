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

import com.amplifyframework.api.graphql.GraphQLRequest

/**
 * A loader that records the requests it is asked to send and answers them from [answer].
 *
 * The recorded requests are how the tests assert on the query a lazy relationship built for itself,
 * and [calls] is how they assert that a cached value was served without issuing a request at all.
 */
internal class RecordingModelLoader(
    private val answer: suspend (GraphQLRequest<*>) -> Any? = { null }
) : AppSyncModelLoader {

    val requests = mutableListOf<GraphQLRequest<*>>()

    val calls: Int
        get() = requests.size

    @Suppress("UNCHECKED_CAST")
    override suspend fun <M> load(request: GraphQLRequest<M>): M? {
        requests += request
        return answer(request) as M?
    }
}

/** A loader that fails every call, for the tests that exercise the error path. */
internal fun failingLoader(error: Throwable) = RecordingModelLoader { throw error }
