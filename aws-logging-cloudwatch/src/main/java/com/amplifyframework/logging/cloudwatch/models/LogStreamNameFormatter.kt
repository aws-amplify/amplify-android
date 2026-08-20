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
 * A functional interface for customizing CloudWatch log stream names.
 *
 * Implement this interface to provide custom log stream naming logic. The formatter receives
 * a [LogStreamContext] containing relevant information about the device and user, which can
 * be used to construct a meaningful log stream name.
 *
 * Example usage:
 * ```kotlin
 * val formatter = LogStreamNameFormatter { context ->
 *     "my-app-${context.deviceId}-${context.userId ?: "anonymous"}"
 * }
 * ```
 *
 * @see LogStreamContext
 */
fun interface LogStreamNameFormatter {
    /**
     * Generates a log stream name based on the provided context.
     *
     * @param context The [LogStreamContext] containing device and user information
     * @return A string to be used as the CloudWatch log stream name
     */
    fun format(context: LogStreamContext): String
}
