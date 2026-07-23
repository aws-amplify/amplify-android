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
package com.amplifyframework.eventenrichment.session

/**
 * Represents an app session with start/stop timestamps and duration.
 *
 * @param id Unique session identifier.
 * @param startTimestamp ISO 8601 UTC timestamp when the session started.
 * @param stopTimestamp ISO 8601 UTC timestamp when the session stopped, or null
 *   while the session is active.
 * @param duration Duration of the session in milliseconds, or null while the
 *   session is active.
 */
data class Session(
    val id: String,
    val startTimestamp: String,
    val stopTimestamp: String? = null,
    val duration: Long? = null
)
