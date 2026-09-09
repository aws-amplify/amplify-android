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

package com.amplifyframework.predictions.aws.exceptions

import com.amplifyframework.annotations.InternalAmplifyApi
import com.amplifyframework.predictions.PredictionsException

/**
 * Raised when an established connection to the face liveness service is lost before the check completes, for example
 * because the host app was backgrounded long enough for the socket to close, or because the device lost network
 * connectivity mid-check.
 *
 * Any transport-level [java.io.IOException] other than a protocol violation is reported as this type, so it also
 * covers read timeouts, DNS failures and TLS teardown. A connection that never opened is not reported as this type,
 * since that indicates an unreachable or misconfigured endpoint rather than an interruption.
 */
@InternalAmplifyApi
class FaceLivenessSessionInterruptedException internal constructor(
    message: String = "The face liveness session was interrupted.",
    cause: Throwable? = null,
    recoverySuggestion: String = "Retry the face liveness check."
) : PredictionsException(message, cause, recoverySuggestion)
