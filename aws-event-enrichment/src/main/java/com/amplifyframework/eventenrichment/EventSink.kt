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
package com.amplifyframework.eventenrichment

/**
 * Interface for transporting enriched events to a destination.
 *
 * Implement this to pipe events to Kinesis, Firehose, or any custom transport.
 * No default implementation is provided, keeping this module free of transport
 * dependencies.
 */
fun interface EventSink {
    /** Sends an enriched event to the configured destination. */
    fun send(event: EnrichedEvent)
}
