/*
 * Copyright 2023 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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
package com.amplifyframework.logging.cloudwatch.models

import kotlinx.serialization.Serializable

/**
 * Configuration to configure the plugin.
 */
@Serializable
data class AWSCloudWatchLoggingPluginConfiguration @JvmOverloads constructor(
    val logGroupName: String,
    val region: String,
    val enable: Boolean = true,
    val localStoreMaxSizeInMB: Int = 5,
    val flushIntervalInSeconds: Int = 60,
    val defaultRemoteConfiguration: DefaultRemoteConfiguration? = null,
    val loggingConstraints: LoggingConstraints = LoggingConstraints()
)

/**
 * Configuration to fetch remote logging constraints.
 */
@Serializable
data class DefaultRemoteConfiguration @JvmOverloads constructor(
    val endpoint: String,
    val refreshIntervalInSeconds: Int = 1200
)
