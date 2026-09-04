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
package com.amplifyframework.cloudwatch

import aws.sdk.kotlin.services.cloudwatchlogs.model.CloudWatchLogsException
import com.amplifyframework.annotations.ExperimentalAmplifyApi
import com.amplifyframework.foundation.exceptions.AmplifyException

/**
 * Base exception for all standalone CloudWatch client operations.
 *
 * This is a sealed hierarchy. Callers can exhaustively match on the subtype to
 * determine the category of failure:
 * - [AmplifyCloudWatchStorageException] — local file I/O or log-rotation errors
 * - [AmplifyCloudWatchServiceException] — CloudWatch Logs API call failed
 * - [AmplifyCloudWatchConfigurationException] — client is misconfigured
 * - [AmplifyCloudWatchUnknownException] — unexpected / uncategorized errors
 *
 * @param message Error message describing what went wrong
 * @param recoverySuggestion Suggested action to resolve the error
 * @param cause Underlying cause of the exception
 */
@ExperimentalAmplifyApi
sealed class AmplifyCloudWatchException(
    message: String,
    recoverySuggestion: String,
    cause: Throwable? = null
) : AmplifyException(message, recoverySuggestion, cause) {
    companion object {
        /** Maps an arbitrary [error] to the most appropriate [AmplifyCloudWatchException] subtype. */
        internal fun from(error: Throwable): AmplifyCloudWatchException = when (error) {
            is AmplifyCloudWatchException -> error
            is CloudWatchLogsException -> AmplifyCloudWatchServiceException(
                message = error.message ?: "A CloudWatch Logs API call failed.",
                recoverySuggestion = "Verify the log group name, region, and credentials, then retry.",
                cause = error
            )
            else -> AmplifyCloudWatchUnknownException(
                message = error.message ?: "An unknown error occurred.",
                recoverySuggestion = "See the attached exception for more details.",
                cause = error
            )
        }
    }
}

/** Local file I/O or log-rotation error. */
@ExperimentalAmplifyApi
class AmplifyCloudWatchStorageException(
    message: String,
    recoverySuggestion: String,
    cause: Throwable? = null
) : AmplifyCloudWatchException(message, recoverySuggestion, cause)

/** A CloudWatch Logs API call failed. */
@ExperimentalAmplifyApi
class AmplifyCloudWatchServiceException(
    message: String,
    recoverySuggestion: String,
    cause: Throwable? = null
) : AmplifyCloudWatchException(message, recoverySuggestion, cause)

/** The client is misconfigured. */
@ExperimentalAmplifyApi
class AmplifyCloudWatchConfigurationException(
    message: String,
    recoverySuggestion: String,
    cause: Throwable? = null
) : AmplifyCloudWatchException(message, recoverySuggestion, cause)

/** Unexpected / uncategorized error. */
@ExperimentalAmplifyApi
class AmplifyCloudWatchUnknownException(
    message: String,
    recoverySuggestion: String,
    cause: Throwable? = null
) : AmplifyCloudWatchException(message, recoverySuggestion, cause)
