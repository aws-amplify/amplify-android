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

package com.amplifyframework.foundation.logging

/**
 * Data class that represents a single log message
 * @param level The LogLevel for this message
 * @param name The name of the logger that emitted the message
 * @param content The content of the message
 * @param cause The throwable that caused the message to be emitted, if any
 */
data class LogMessage(
    val level: LogLevel,
    val name: String,
    val content: String,
    val cause: Throwable?
)
