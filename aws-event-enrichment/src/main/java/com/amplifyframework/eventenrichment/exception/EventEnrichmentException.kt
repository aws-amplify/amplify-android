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
package com.amplifyframework.eventenrichment.exception

import com.amplifyframework.foundation.exceptions.AmplifyException

/**
 * Base exception for all event enrichment operations.
 *
 * This is a sealed hierarchy. Callers can exhaustively match on the subtype:
 * - [EventEnrichmentClosedException] — the client has been closed.
 *
 * @param message Error message describing what went wrong.
 * @param recoverySuggestion Suggested action to resolve the error.
 * @param cause Underlying cause of the exception.
 */
sealed class EventEnrichmentException(
    message: String,
    recoverySuggestion: String,
    cause: Throwable? = null
) : AmplifyException(message, recoverySuggestion, cause)

/** Thrown when an operation is attempted on a closed client. */
class EventEnrichmentClosedException : EventEnrichmentException(
    message = "Client has been closed",
    recoverySuggestion = "Create a new EventEnrichmentClient instance."
)
