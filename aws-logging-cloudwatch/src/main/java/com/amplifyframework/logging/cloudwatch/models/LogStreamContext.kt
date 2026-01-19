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
package com.amplifyframework.logging.cloudwatch.models

/**
 * Contains contextual information used when generating a CloudWatch log stream name.
 * This data class is passed to [LogStreamNameFormatter.format] to allow custom log stream naming.
 *
 * Using a data class allows for future expansion of available context data without breaking
 * existing implementations.
 *
 * @property deviceId A unique identifier for the device
 * @property userId The user's identity ID, or null if not authenticated
 */
data class LogStreamContext(
    val deviceId: String,
    val userId: String?
)
