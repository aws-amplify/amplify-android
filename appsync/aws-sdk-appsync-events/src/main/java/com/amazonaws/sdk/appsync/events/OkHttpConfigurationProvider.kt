/*
 * Copyright 2025 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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
package com.amazonaws.sdk.appsync.events

import okhttp3.OkHttpClient

/**
 * An OkHttpConfigurationProvider is a hook provided to a customer, enabling them to customize
 * the OkHttp client used by the Events Library.
 *
 * This hook is for advanced use cases, such as where a user may want to append some of
 * their own request headers, configure timeouts, or otherwise manipulate an outgoing request.
 */
fun interface OkHttpConfigurationProvider {
    /**
     * The OkHttp.Builder() used for the Events library is provided. This mutable builder allows for setting custom
     * configurations on the OkHttp.Builder() instance. The library will run this configuration when the Events
     * class is constructed and then build() the OkHttp client which will be used for all library network calls.
     * @param okHttpClientBuilder An [OkHttpClient.Builder] instance
     */
    fun applyConfiguration(okHttpClientBuilder: OkHttpClient.Builder)
}
