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
package com.amplifyframework.connect.internal

import okhttp3.Interceptor
import okhttp3.Response

/**
 * OkHttp interceptor that adds the Amplify user agent to Customer Profiles
 * requests.
 *
 * The Connect client talks to the backend over plain HTTP rather than through
 * an AWS SDK Kotlin client, so it cannot reuse the Smithy-based
 * `AmplifyUserAgentInterceptor`. This interceptor emits the same two tokens on
 * the `User-Agent` header:
 *
 * 1. `md/amplify-connect#<version>` identifies the Amplify component.
 * 2. `lib/amplify-android#<version>` identifies the Amplify framework.
 *
 * @param version The Amplify library version (from `BuildConfig.VERSION_NAME`)
 */
internal class ConnectUserAgentInterceptor(version: String) : Interceptor {
    private val userAgent = "md/$COMPONENT_NAME#$version lib/amplify-android#$version"

    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request().newBuilder()
            .header("User-Agent", userAgent)
            .build()
        return chain.proceed(request)
    }

    internal companion object {
        const val COMPONENT_NAME = "amplify-connect"
    }
}
