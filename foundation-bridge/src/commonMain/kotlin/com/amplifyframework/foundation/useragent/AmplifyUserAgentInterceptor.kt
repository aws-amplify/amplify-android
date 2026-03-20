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
package com.amplifyframework.foundation.useragent

import aws.sdk.kotlin.runtime.http.operation.customUserAgentMetadata
import aws.smithy.kotlin.runtime.client.ProtocolRequestInterceptorContext
import aws.smithy.kotlin.runtime.client.RequestInterceptorContext
import aws.smithy.kotlin.runtime.http.interceptors.HttpInterceptor
import aws.smithy.kotlin.runtime.http.request.HttpRequest
import aws.smithy.kotlin.runtime.http.request.toBuilder
import com.amplifyframework.annotations.InternalAmplifyApi

/**
 * HTTP interceptor that injects Amplify user agent metadata into AWS SDK requests.
 *
 * Adds two pieces of tracking information:
 *
 * 1. `md/<componentName>#<componentVersion>` — Identifies the Amplify component making the request.
 * 2. `lib/amplify-android#<amplifyVersion>` — Identifies the Amplify framework.
 *
 * @param componentName The component identifier (e.g. "amplify-kinesis", "amplify-firehose")
 * @param componentVersion The version of the component
 * @param amplifyVersion The version of the Amplify framework
 */
@InternalAmplifyApi
class AmplifyUserAgentInterceptor(
    private val componentName: String,
    private val componentVersion: String,
    private val amplifyVersion: String = componentVersion
) : HttpInterceptor {

    private val libToken = "lib/amplify-android#$amplifyVersion"

    override suspend fun modifyBeforeSerialization(context: RequestInterceptorContext<Any>): Any {
        context.executionContext.customUserAgentMetadata.add(componentName, componentVersion)
        return super.modifyBeforeSerialization(context)
    }

    override suspend fun modifyBeforeSigning(
        context: ProtocolRequestInterceptorContext<Any, HttpRequest>
    ): HttpRequest {
        val existing = context.protocolRequest.headers["User-Agent"] ?: ""
        if (!existing.contains(libToken)) {
            val builder = context.protocolRequest.toBuilder()
            builder.headers["User-Agent"] = "$existing $libToken"
            return builder.build()
        }
        return context.protocolRequest
    }
}
