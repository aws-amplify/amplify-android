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
package com.amplifyframework.foundation.config

import aws.sdk.kotlin.runtime.config.AwsSdkClientConfig
import com.amplifyframework.annotations.InternalAmplifyApi

/**
 * Provides custom configuration for an underlying AWS SDK client.
 *
 * This is a SAM interface, so it can be used as a lambda in Kotlin:
 * ```kotlin
 * configureClient {
 *     retryStrategy {
 *         maxAttempts = 10
 *     }
 * }
 * ```
 *
 * @param B The SDK client's [AwsSdkClientConfig.Builder] subtype
 */
@InternalAmplifyApi
fun interface SdkClientConfigurationProvider<B : AwsSdkClientConfig.Builder> {
    /**
     * Applies custom configuration to the SDK client builder.
     *
     * The [builder] will already have default configurations (region, credentials) applied.
     * Any values set here will override the defaults.
     *
     * @param builder An SDK client config builder instance with defaults pre-applied
     */
    fun applyConfiguration(builder: B)
}
