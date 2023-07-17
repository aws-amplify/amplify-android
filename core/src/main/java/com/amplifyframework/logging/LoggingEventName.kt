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
package com.amplifyframework.logging

/**
 * An enumeration of the names of events relating the {@link LoggingCategory},
 * that are published via {@link HubCategory#publish(HubChannel, HubEvent)} on the
 * {@link HubChannel#LOGGING} channel.
 */
enum class LoggingEventName(name: String) {
    WRITE_LOG_FAILURE("writeLogFailure"),
    FLUSH_LOG_FAILURE("flushLogFailure")
}
