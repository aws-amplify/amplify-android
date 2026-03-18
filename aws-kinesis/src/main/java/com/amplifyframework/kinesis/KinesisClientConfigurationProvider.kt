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

import aws.sdk.kotlin.services.kinesis.KinesisClient

/**
 * Provides custom configuration for the underlying [KinesisClient].
 *
 * Implement this interface to customize the [KinesisClient.Config.Builder] used
 * by [AmplifyKinesisClient]. The builder passed to [applyConfiguration] will already
 * have the default configurations (region, credentials) applied — your overrides
 * are applied on top.
 *
 * This is a SAM interface, so it can be used as a lambda in Kotlin:
 * ```kotlin
 * AmplifyKinesisClient(
 *     context = context,
 *     region = "us-east-1",
 *     credentialsProvider = provider,
 *     options = AmplifyKinesisClientOptions {
 *         configureClient {
 *             retryStrategy {
 *                 maxAttempts = 10
 *             }
 *         }
 *     }
 * )
 * ```
 */
fun interface KinesisClientConfigurationProvider {
    /**
     * Applies custom configuration to the KinesisClient builder.
     *
     * The [builder] will already have default configurations (region, credentials) applied.
     * Any values set here will override the defaults.
     *
     * @param builder A [KinesisClient.Config.Builder] instance with defaults pre-applied
     */
    fun applyConfiguration(builder: KinesisClient.Config.Builder)
}
