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
package com.amplifyframework.kinesis

import aws.sdk.kotlin.runtime.http.operation.customUserAgentMetadata
import aws.smithy.kotlin.runtime.client.ProtocolRequestInterceptorContext
import aws.smithy.kotlin.runtime.client.RequestInterceptorContext
import aws.smithy.kotlin.runtime.http.interceptors.HttpInterceptor
import aws.smithy.kotlin.runtime.http.request.HttpRequest
import aws.smithy.kotlin.runtime.http.request.toBuilder

/**
 * HTTP interceptor that injects Amplify user agent metadata into Kinesis SDK requests.
 *
 * Adds two pieces of tracking information:
 *
 * 1. `md/amplify-kinesis#<version>` — Identifies this request as coming from the AmplifyKinesisClient.
 *    Added via [customUserAgentMetadata] during serialization, which the SDK formats into the
 *    standard `md/` metadata section of the User-Agent header.
 *
 * 2. `lib/amplify-android#<version>` — Identifies the Amplify framework. When used alongside
 *    `Amplify.configure()`, the SDK already includes this via the `aws.frameworkMetadata` system
 *    property. For standalone usage (without Amplify core), this interceptor appends it directly
 *    to the User-Agent header if not already present.
 */
internal class KinesisUserAgentInterceptor : HttpInterceptor {

    private val libToken = "lib/amplify-android#${BuildConfig.VERSION_NAME}"

    /**
     * Adds `md/amplify-kinesis#<version>` to the SDK's custom user agent metadata.
     * The SDK picks this up and formats it into the User-Agent header automatically.
     */
    override suspend fun modifyBeforeSerialization(context: RequestInterceptorContext<Any>): Any {
        context.executionContext.customUserAgentMetadata.add("amplify-kinesis", BuildConfig.VERSION_NAME)
        return super.modifyBeforeSerialization(context)
    }

    /**
     * Appends `lib/amplify-android#<version>` to the User-Agent header if not already present.
     * Runs before signing so the header is included in the request signature.
     */
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
