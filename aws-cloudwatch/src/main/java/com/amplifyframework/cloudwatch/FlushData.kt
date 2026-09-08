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

import com.amplifyframework.annotations.ExperimentalAmplifyApi
import com.amplifyframework.foundation.result.Result

/**
 * Data returned by a successful [AmplifyCloudWatchClient.flushLogs] call.
 *
 * @param flushed `true` if this call performed the flush; `false` if it was skipped because another
 *   flush (an interval worker or a cache-full write) was already in progress. A caller that awaits
 *   [AmplifyCloudWatchClient.flushLogs] before reading CloudWatch can use this to tell "flushed" from
 *   "skipped".
 */
@ExperimentalAmplifyApi
class FlushData internal constructor(val flushed: Boolean)

/** Result of [AmplifyCloudWatchClient.flushLogs]. */
@ExperimentalAmplifyApi
typealias FlushResult = Result<FlushData, AmplifyCloudWatchException>
